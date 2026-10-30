package com.example.warehouse.dto.request;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateVariantRequest(
        @NotBlank @Size(max = 255) String name,
        Map<String, @NotBlank String> attributes,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price) {
}
