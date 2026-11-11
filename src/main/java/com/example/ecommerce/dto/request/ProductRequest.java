package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * @param active defaults to true when omitted
 */
public record ProductRequest(
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "SKU may only contain letters, digits, '.', '_' and '-'")
        String sku,

        @NotBlank @Size(max = 200)
        String name,

        @Size(max = 4000)
        String description,

        @NotNull @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull @Min(0)
        Integer stockQuantity,

        @Size(max = 500)
        String imageUrl,

        @NotNull
        Long categoryId,

        Boolean active) {
}
