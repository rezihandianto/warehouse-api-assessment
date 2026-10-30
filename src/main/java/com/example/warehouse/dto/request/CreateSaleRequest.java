package com.example.warehouse.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateSaleRequest(@NotEmpty List<@Valid @NotNull SaleLineRequest> lines) {
}