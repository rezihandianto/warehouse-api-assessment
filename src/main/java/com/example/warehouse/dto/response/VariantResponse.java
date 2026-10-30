package com.example.warehouse.dto.response;

import com.example.warehouse.entity.ItemVariant;

import java.math.BigDecimal;
import java.util.Map;


public record VariantResponse(
        Long id,
        Long itemId,
        String sku,
        String name,
        Map<String, String> attributes,
        BigDecimal price,
        int stockQuantity,
        boolean inStock) {

    public static VariantResponse from(ItemVariant v) {
        return new VariantResponse(v.getId(), v.getItem().getId(), v.getSku(), v.getName(),
                Map.copyOf(v.getAttributes()), v.getEffectivePrice(), v.getStockQuantity(),
                v.getStockQuantity() > 0);
    }
}