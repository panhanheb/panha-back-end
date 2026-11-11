package com.example.ecommerce.service.impl;

import lombok.RequiredArgsConstructor;
import com.example.ecommerce.dto.request.AddCartItemRequest;
import com.example.ecommerce.dto.response.CartItemResponse;
import com.example.ecommerce.util.CartLimits;
import com.example.ecommerce.dto.response.CartResponse;
import com.example.ecommerce.dto.request.UpdateCartItemRequest;
import com.example.ecommerce.model.Cart;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.User;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.SecurityUtils;
import com.example.ecommerce.service.CartService;
import com.example.ecommerce.service.PricingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        return cartRepository.findByUserId(currentUserId())
                .map(this::toResponse)
                .orElseGet(this::emptyCart);
    }

    @Override
    @Transactional
    public CartResponse addItem(AddCartItemRequest request) {
        Cart cart = currentCartForUpdate();
        Product product = purchasableProduct(request.productId());
        CartItem existing = cart.findItem(product.getId()).orElse(null);
        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + request.quantity();
        if (newQuantity > CartLimits.MAX_QUANTITY_PER_ITEM) {
            throw new BadRequestException("At most " + CartLimits.MAX_QUANTITY_PER_ITEM
                    + " units of a product can be in the cart");
        }
        assertStock(product, newQuantity);
        if (existing == null) {
            if (cart.getItems().size() >= CartLimits.MAX_DISTINCT_ITEMS) {
                throw new BadRequestException("A cart can hold at most " + CartLimits.MAX_DISTINCT_ITEMS + " products");
            }
            cart.getItems().add(new CartItem(cart, product, newQuantity));
        } else {
            existing.setQuantity(newQuantity);
        }
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long productId, UpdateCartItemRequest request) {
        Cart cart = currentCartForUpdate();
        CartItem item = cart.findItem(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " is not in your cart"));
        Product product = item.getProduct();
        if (!product.isPurchasable()) {
            throw new BadRequestException("Product " + product.getSku() + " is no longer available; remove it from your cart");
        }
        assertStock(product, request.quantity());
        item.setQuantity(request.quantity());
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long productId) {
        Cart cart = currentCartForUpdate();
        boolean removed = cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        if (!removed) {
            throw new ResourceNotFoundException("Product " + productId + " is not in your cart");
        }
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse clear() {
        cartRepository.findByUserId(currentUserId()).ifPresent(cart -> cart.getItems().clear());
        return emptyCart();
    }

    // ------------------------------------------------------------------ helpers

    private Cart currentCartForUpdate() {
        Long userId = currentUserId();
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            return cartRepository.save(new Cart(user));
        });
    }

    private Product purchasableProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (!product.isPurchasable()) {
            // Same message as a missing product: hidden products are not revealed to customers.
            throw new ResourceNotFoundException("Product", productId);
        }
        return product;
    }

    private static void assertStock(Product product, int quantity) {
        if (quantity > product.getStockQuantity()) {
            throw new BadRequestException(product.getStockQuantity() == 0
                    ? "Product " + product.getSku() + " is out of stock"
                    : "Only " + product.getStockQuantity() + " unit(s) of " + product.getSku() + " in stock");
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(this::toResponse).toList();
        BigDecimal subtotal = items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = pricingService.shippingFee(subtotal);
        int totalQuantity = items.stream().mapToInt(CartItemResponse::quantity).sum();
        boolean ready = !items.isEmpty() && items.stream().allMatch(CartItemResponse::available);
        return new CartResponse(items, totalQuantity, PricingService.money(subtotal), shipping,
                PricingService.money(subtotal.add(shipping)), pricingService.currency(), ready);
    }

    private CartItemResponse toResponse(CartItem item) {
        Product p = item.getProduct();
        boolean available = p.isPurchasable() && item.getQuantity() <= p.getStockQuantity();
        return new CartItemResponse(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getImageUrl(),
                p.getPrice(),
                item.getQuantity(),
                pricingService.lineTotal(p.getPrice(), item.getQuantity()),
                p.getStockQuantity(),
                available);
    }

    private CartResponse emptyCart() {
        BigDecimal zero = PricingService.money(BigDecimal.ZERO);
        return new CartResponse(List.of(), 0, zero, zero, zero, pricingService.currency(), false);
    }

    private static Long currentUserId() {
        return SecurityUtils.requirePrincipal().getId();
    }
}
