package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param slug   optional; generated from the name when omitted
 * @param active defaults to true when omitted
 */
public record CategoryRequest(
        @NotBlank @Size(max = 100)
        String name,

        @Size(max = 120)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug may only contain lowercase letters, digits and single hyphens")
        String slug,

        @Size(max = 1000)
        String description,

        Boolean active) {
}
