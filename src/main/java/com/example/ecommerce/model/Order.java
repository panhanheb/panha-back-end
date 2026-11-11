package com.example.ecommerce.model;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.ecommerce.model.enums.OrderStatus;
import com.example.ecommerce.model.enums.PaymentMethod;
import com.example.ecommerce.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A placed order. Product names, SKUs and prices are copied into {@link OrderItem} at checkout so the
 * order stays correct when the catalog changes later. The shipping address is likewise a snapshot.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user", columnList = "user_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_created", columnList = "created_at")
})
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    // ---- shipping address snapshot
    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "note", length = 500)
    private String note;

    // ---- payment (simulated)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "payment_reference", length = 60)
    private String paymentReference;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    /** Prevents two concurrent status changes (e.g. pay and cancel) from both succeeding. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }
}
