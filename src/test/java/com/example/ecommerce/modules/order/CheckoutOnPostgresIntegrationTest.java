package com.example.ecommerce.modules.order;

import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.modules.user.service.UserAccountService;

import com.example.ecommerce.modules.cart.dto.AddCartItemRequest;
import com.example.ecommerce.modules.cart.service.CartService;
import com.example.ecommerce.modules.category.dto.CategoryRequest;
import com.example.ecommerce.modules.category.service.CategoryService;
import com.example.ecommerce.modules.order.dto.CheckoutRequest;
import com.example.ecommerce.modules.order.dto.OrderResponse;
import com.example.ecommerce.modules.order.dto.PaymentRequest;
import com.example.ecommerce.modules.order.entity.OrderStatus;
import com.example.ecommerce.modules.order.entity.PaymentMethod;
import com.example.ecommerce.modules.order.entity.PaymentStatus;
import com.example.ecommerce.modules.order.service.OrderService;
import com.example.ecommerce.modules.product.dto.ProductRequest;
import com.example.ecommerce.modules.product.service.ProductService;
import com.example.ecommerce.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The checkout carried over from the first version, now on real PostgreSQL: row locks
 * ({@code SELECT ... FOR UPDATE}), stock deduction and return, price snapshots and the new error codes.
 * The HTTP endpoints for these services are rebuilt in Phases 6-9.
 */
@IntegrationTest
class CheckoutOnPostgresIntegrationTest {

    @Autowired private UserAccountService userAccountService;
    @Autowired private CategoryService categoryService;
    @Autowired private ProductService productService;
    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;
    @Autowired private UserDetailsService userDetailsService;

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkoutReservesStockPaymentWorksAndCancellationReturnsStock() {
        String tag = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        signInAs("admin");
        Long categoryId = categoryService.create(new CategoryRequest("Lamps " + tag, null, null, true)).id();
        Long productId = productService.create(new ProductRequest("LAMP-" + tag, "Desk lamp", null,
                new BigDecimal("30.00"), 5, null, categoryId, true)).id();

        String customer = "buyer" + tag.toLowerCase();
        userAccountService.registerCustomer(customer, customer + "@mail.test", "Secret@123", "Sok Dara", null);
        signInAs(customer);

        cartService.addItem(new AddCartItemRequest(productId, 2));
        OrderResponse order = orderService.checkout(new CheckoutRequest("Sok Dara", "+855 12 345 678",
                "12 Street 271", "Phnom Penh", null, "Cambodia", PaymentMethod.CARD, null));

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.total()).isEqualByComparingTo("60.00"); // free shipping from 50.00
        assertThat(stockOf(productId)).isEqualTo(3);
        assertThat(cartService.getCart().items()).isEmpty();

        assertThatThrownBy(() -> orderService.pay(order.id(), new PaymentRequest(PaymentRequest.DECLINED_TEST_CARD)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(ErrorCode.PAYMENT_DECLINED);

        OrderResponse paid = orderService.pay(order.id(), new PaymentRequest("4242424242424242"));
        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.paymentReference()).startsWith("SIM-");

        OrderResponse cancelled = orderService.cancel(order.id());
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(stockOf(productId)).isEqualTo(5);
    }

    @Test
    void orderingMoreThanTheStockFailsWithInsufficientStock() {
        String tag = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        signInAs("admin");
        Long categoryId = categoryService.create(new CategoryRequest("Mugs " + tag, null, null, true)).id();
        Long productId = productService.create(new ProductRequest("MUG-" + tag, "Mug", null,
                new BigDecimal("8.50"), 3, null, categoryId, true)).id();

        String customer = "late" + tag.toLowerCase();
        userAccountService.registerCustomer(customer, customer + "@mail.test", "Secret@123", null, null);
        signInAs(customer);
        cartService.addItem(new AddCartItemRequest(productId, 3));

        // the stock drops after the item was put in the cart
        signInAs("admin");
        productService.updateStock(productId, new com.example.ecommerce.modules.product.dto.StockUpdateRequest(1));

        signInAs(customer);
        assertThatThrownBy(() -> orderService.checkout(new CheckoutRequest("A", "+855 12 345 678", "Street",
                "Phnom Penh", null, "Cambodia", PaymentMethod.CASH_ON_DELIVERY, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
        assertThat(stockOf(productId)).isEqualTo(1);
    }

    /** Reads the stock as the admin, then restores whoever was signed in. */
    private int stockOf(Long productId) {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        signInAs("admin");
        try {
            return productService.getById(productId).stockQuantity();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    private void signInAs(String username) {
        UserDetails principal = userDetailsService.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
