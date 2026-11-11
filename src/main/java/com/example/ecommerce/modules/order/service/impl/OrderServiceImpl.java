package com.example.ecommerce.modules.order.service.impl;

import lombok.RequiredArgsConstructor;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.modules.order.dto.CheckoutRequest;
import com.example.ecommerce.modules.order.dto.OrderResponse;
import com.example.ecommerce.modules.order.dto.OrderSummaryResponse;
import com.example.ecommerce.modules.order.dto.PaymentRequest;
import com.example.ecommerce.modules.order.dto.UpdateOrderStatusRequest;
import com.example.ecommerce.modules.cart.entity.Cart;
import com.example.ecommerce.modules.cart.entity.CartItem;
import com.example.ecommerce.modules.order.entity.Order;
import com.example.ecommerce.modules.order.entity.OrderItem;
import com.example.ecommerce.modules.product.entity.Product;
import com.example.ecommerce.modules.audit.service.AuditAction;
import com.example.ecommerce.modules.order.entity.OrderStatus;
import com.example.ecommerce.modules.order.entity.PaymentMethod;
import com.example.ecommerce.modules.order.entity.PaymentStatus;
import com.example.ecommerce.common.security.RoleName;
import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ConflictException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.modules.order.mapper.OrderMapper;
import com.example.ecommerce.modules.cart.repository.CartItemRepository;
import com.example.ecommerce.modules.cart.repository.CartRepository;
import com.example.ecommerce.modules.order.repository.OrderRepository;
import com.example.ecommerce.modules.product.repository.ProductRepository;
import com.example.ecommerce.common.security.SecurityUtils;
import com.example.ecommerce.common.security.UserPrincipal;
import com.example.ecommerce.modules.audit.service.AuditService;
import com.example.ecommerce.modules.order.service.OrderService;
import com.example.ecommerce.modules.order.service.PricingService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String ENTITY = "Order";
    private static final SecureRandom RANDOM = new SecureRandom();
    /** No 0/O or 1/I so order numbers can be read out over the phone. */
    private static final char[] ORDER_NUMBER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final PricingService pricingService;
    private final OrderMapper orderMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        Long userId = SecurityUtils.requirePrincipal().getId();

        // 1. Lock the products in the cart before reading their stock, so two checkouts racing for the
        //    last unit cannot both succeed. Locking happens before the cart is loaded so the rows are
        //    read fresh from the database under the lock.
        List<Long> productIds = cartItemRepository.findProductIdsByUserId(userId);
        if (productIds.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }
        Map<Long, Product> locked = productRepository.findAllByIdForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        // 2. Validate every line and report all problems at once.
        List<String> problems = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            Product product = locked.get(item.getProduct().getId());
            if (product == null || !product.isPurchasable()) {
                problems.add(item.getProduct().getSku() + ": no longer available");
            } else if (item.getQuantity() > product.getStockQuantity()) {
                problems.add(product.getSku() + ": requested " + item.getQuantity()
                        + ", only " + product.getStockQuantity() + " in stock");
            }
        }
        if (!problems.isEmpty()) {
            throw new ConflictException(ErrorCode.INSUFFICIENT_STOCK, "Some items in your cart cannot be ordered; update your cart and try again",
                    problems);
        }

        // 3. Build the order with price snapshots and reserve stock.
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderNumber(newOrderNumber());
        order.setCurrency(pricingService.currency());
        order.setPaymentMethod(request.paymentMethod());
        order.setRecipientName(request.recipientName().trim());
        order.setPhone(request.phone().trim());
        order.setAddressLine(request.addressLine().trim());
        order.setCity(request.city().trim());
        order.setPostalCode(StringUtils.hasText(request.postalCode()) ? request.postalCode().trim() : null);
        order.setCountry(request.country().trim());
        order.setNote(StringUtils.hasText(request.note()) ? request.note().trim() : null);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = locked.get(cartItem.getProduct().getId());
            OrderItem line = new OrderItem();
            line.setProduct(product);
            line.setProductName(product.getName());
            line.setSku(product.getSku());
            line.setUnitPrice(product.getPrice());
            line.setQuantity(cartItem.getQuantity());
            line.setLineTotal(pricingService.lineTotal(product.getPrice(), cartItem.getQuantity()));
            order.addItem(line);

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            subtotal = subtotal.add(line.getLineTotal());
        }
        BigDecimal shipping = pricingService.shippingFee(subtotal);
        order.setSubtotal(PricingService.money(subtotal));
        order.setShippingFee(shipping);
        order.setTotal(PricingService.money(subtotal.add(shipping)));

        orderRepository.save(order);
        cart.getItems().clear();

        auditService.log(AuditAction.CREATE, ENTITY, order.getId(),
                "Placed order " + order.getOrderNumber() + " total " + order.getTotal() + " " + order.getCurrency());
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse pay(Long orderId, PaymentRequest request) {
        Order order = findOrder(orderId);
        if (!order.getUser().getId().equals(SecurityUtils.requirePrincipal().getId())) {
            throw new ResourceNotFoundException(ENTITY, orderId);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException(ErrorCode.INVALID_ORDER_TRANSITION, "Order " + order.getOrderNumber() + " is " + order.getStatus()
                    + " and cannot be paid");
        }
        if (order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            throw new BadRequestException("Cash-on-delivery orders are paid to the courier on delivery");
        }
        String cardNumber = request == null ? null : request.cardNumber();
        if (order.getPaymentMethod() == PaymentMethod.CARD) {
            if (!StringUtils.hasText(cardNumber)) {
                throw new BadRequestException("Enter a card number to pay by card");
            }
            if (PaymentRequest.DECLINED_TEST_CARD.equals(cardNumber)) {
                auditService.log(AuditAction.UPDATE, ENTITY, orderId,
                        "Simulated card payment declined for " + order.getOrderNumber());
                throw new BadRequestException(ErrorCode.PAYMENT_DECLINED, "Payment declined by the card issuer (simulated)");
            }
        }

        order.setStatus(OrderStatus.PAID);
        markPaid(order, "SIM-");
        String detail = order.getPaymentMethod() == PaymentMethod.CARD
                ? " with card ending " + cardNumber.substring(cardNumber.length() - 4)
                : " by " + order.getPaymentMethod();
        auditService.log(AuditAction.UPDATE, ENTITY, orderId,
                "Order " + order.getOrderNumber() + " paid" + detail + " (ref " + order.getPaymentReference() + ")");
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = findOrder(orderId);
        assertOwnerOrAdmin(order);
        doCancel(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = findOrder(orderId);
        OrderStatus current = order.getStatus();
        OrderStatus target = request.status();
        LocalDateTime now = LocalDateTime.now();

        switch (target) {
            case SHIPPED -> {
                boolean codAwaitingShipment = current == OrderStatus.PENDING
                        && order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY;
                if (current != OrderStatus.PAID && !codAwaitingShipment) {
                    throw invalidTransition(order, target, current == OrderStatus.PENDING
                            ? "the order has not been paid yet" : null);
                }
                order.setStatus(OrderStatus.SHIPPED);
                order.setShippedAt(now);
            }
            case DELIVERED -> {
                if (current != OrderStatus.SHIPPED) {
                    throw invalidTransition(order, target, null);
                }
                order.setStatus(OrderStatus.DELIVERED);
                order.setDeliveredAt(now);
                if (order.getPaymentStatus() == PaymentStatus.UNPAID) {
                    markPaid(order, "COD-"); // cash collected by the courier
                }
            }
            case CANCELLED -> doCancel(order);
            case PENDING, PAID -> throw new BadRequestException(
                    "Status " + target + " cannot be set manually; customers pay from their order page");
        }

        if (target != OrderStatus.CANCELLED) {
            auditService.log(AuditAction.UPDATE, ENTITY, orderId,
                    "Order " + order.getOrderNumber() + " " + current + " -> " + order.getStatus());
        }
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId) {
        Order order = findOrder(orderId);
        assertOwnerOrAdmin(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> myOrders(OrderStatus status, Pageable pageable) {
        Long userId = SecurityUtils.requirePrincipal().getId();
        return PageResponse.from(orderRepository.findForUser(userId, status, pageable), orderMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> search(OrderStatus status, Long customerId, String keyword,
                                                     Pageable pageable) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return PageResponse.from(orderRepository.search(status, customerId, kw, pageable), orderMapper::toSummary);
    }

    // ------------------------------------------------------------------ helpers

    private void doCancel(Order order) {
        OrderStatus current = order.getStatus();
        if (current != OrderStatus.PENDING && current != OrderStatus.PAID) {
            throw invalidTransition(order, OrderStatus.CANCELLED,
                    current == OrderStatus.SHIPPED ? "it has already been shipped" : null);
        }
        // Return the reserved stock (rows locked like at checkout).
        List<Long> productIds = order.getItems().stream().map(i -> i.getProduct().getId()).distinct().toList();
        Map<Long, Product> products = productRepository.findAllByIdForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProduct().getId());
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }

        boolean refunded = order.getPaymentStatus() == PaymentStatus.PAID;
        if (refunded) {
            order.setPaymentStatus(PaymentStatus.REFUNDED); // simulated refund
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        auditService.log(AuditAction.UPDATE, ENTITY, order.getId(),
                "Order " + order.getOrderNumber() + " cancelled" + (refunded ? " and refunded" : ""));
    }

    /** Records a successful (simulated) payment. Does not change the order status. */
    private static void markPaid(Order order, String referencePrefix) {
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentReference(referencePrefix
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
    }

    private static ConflictException invalidTransition(Order order, OrderStatus target, String reason) {
        return new ConflictException(ErrorCode.INVALID_ORDER_TRANSITION, "Order " + order.getOrderNumber() + " cannot change from "
                + order.getStatus() + " to " + target + (reason == null ? "" : " because " + reason));
    }

    private void assertOwnerOrAdmin(Order order) {
        UserPrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole(RoleName.ROLE_ADMIN) && !order.getUser().getId().equals(principal.getId())) {
            // 404 rather than 403 so customers cannot probe which order ids exist.
            throw new ResourceNotFoundException(ENTITY, order.getId());
        }
    }

    private Order findOrder(Long id) {
        return orderRepository.findWithItemsById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    /** e.g. ORD-20261110-K7QM2X; retried on the (very unlikely) collision. */
    private String newOrderNumber() {
        String prefix = "ORD-" + LocalDate.now().format(ORDER_DATE) + "-";
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(prefix);
            for (int i = 0; i < 6; i++) {
                sb.append(ORDER_NUMBER_ALPHABET[RANDOM.nextInt(ORDER_NUMBER_ALPHABET.length)]);
            }
            String candidate = sb.toString();
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique order number");
    }
}
