package com.example.warehouse.dto.response;

import com.example.warehouse.entity.Item;
import com.example.warehouse.entity.ItemVariant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ItemResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        boolean inStock,
        List<VariantResponse> variants,
        Instant createdAt,
        Instant updatedAt) {

    public static ItemResponse from(Item item, List<ItemVariant> variants) {
        int total = variants.isEmpty()
                ? item.getStockQuantity()
                : variants.stream().mapToInt(ItemVariant::getStockQuantity).sum();
        return new ItemResponse(item.getId(), item.getSku(), item.getName(), item.getDescription(),
                item.getPrice(), total, total > 0,
                variants.stream().map(VariantResponse::from).toList(),
                item.getCreatedAt(), item.getUpdatedAt());
    }
}