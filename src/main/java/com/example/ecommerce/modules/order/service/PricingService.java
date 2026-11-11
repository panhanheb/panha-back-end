package com.example.ecommerce.modules.order.service;

import lombok.RequiredArgsConstructor;
import com.example.ecommerce.common.config.AppProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money arithmetic shared by the cart preview and checkout so both always agree. */
@Component
@RequiredArgsConstructor
public class PricingService {

    private final AppProperties appProperties;

    public String currency() {
        return appProperties.shop().currency();
    }

    public BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        return money(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    /** Flat fee, waived when the subtotal reaches the free-shipping threshold. Empty carts ship for free. */
    public BigDecimal shippingFee(BigDecimal subtotal) {
        if (subtotal.signum() <= 0) {
            return money(BigDecimal.ZERO);
        }
        AppProperties.Shop shop = appProperties.shop();
        BigDecimal threshold = shop.freeShippingThreshold();
        if (threshold.signum() > 0 && subtotal.compareTo(threshold) >= 0) {
            return money(BigDecimal.ZERO);
        }
        return money(shop.shippingFee());
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
