package com.example.warehouse.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** variantId is required when the item has variants, and must be omitted otherwise. */
public record SaleLineRequest(
        @NotNull Long itemId,
        Long variantId,
        @Min(1) int quantity) {
}