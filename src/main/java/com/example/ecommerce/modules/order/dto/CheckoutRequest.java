package com.example.ecommerce.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.example.ecommerce.modules.order.entity.PaymentMethod;

/** Places an order for everything in the current customer's cart. */
public record CheckoutRequest(
        @NotBlank @Size(max = 120)
        String recipientName,

        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^[+0-9 ()-]{6,30}$", message = "Phone may only contain digits, spaces, '+', '-', '(' and ')'")
        String phone,

        @NotBlank @Size(max = 255)
        String addressLine,

        @NotBlank @Size(max = 100)
        String city,

        @Size(max = 20)
        String postalCode,

        @NotBlank @Size(max = 100)
        String country,

        @NotNull
        PaymentMethod paymentMethod,

        @Size(max = 500)
        String note) {
}
