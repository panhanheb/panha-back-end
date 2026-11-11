package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.Pattern;

/**
 * Simulated payment. No money moves and card numbers are never stored.
 *
 * @param cardNumber required for CARD orders (12-19 digits). The test number {@value #DECLINED_TEST_CARD}
 *                   is always declined so failure handling can be exercised.
 */
public record PaymentRequest(
        @Pattern(regexp = "^[0-9]{12,19}$", message = "Card number must be 12-19 digits")
        String cardNumber) {

    public static final String DECLINED_TEST_CARD = "4000000000000002";
}
