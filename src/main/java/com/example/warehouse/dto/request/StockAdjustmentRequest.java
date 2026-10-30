package com.example.warehouse.dto.request;

import jakarta.validation.constraints.NotNull;

/** Positive delta = restock, negative = write-off / correction. Stock can never go below zero. */
public record StockAdjustmentRequest(@NotNull Integer delta) {
}