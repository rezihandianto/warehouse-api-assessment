package com.example.warehouse.dto.response;

import com.example.warehouse.entity.SaleLine;

import java.math.BigDecimal;

public record SaleLineResponse(
        Long itemId,
        Long variantId,
        String sku,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {

    public static SaleLineResponse from(SaleLine l) {
        return new SaleLineResponse(l.getItemId(), l.getVariantId(), l.getSku(), l.getName(),
                l.getQuantity(), l.getUnitPrice(), l.getLineTotal());
    }
}