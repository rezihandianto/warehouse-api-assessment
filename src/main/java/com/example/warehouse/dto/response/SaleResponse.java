package com.example.warehouse.dto.response;

import com.example.warehouse.entity.Sale;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


public record SaleResponse(Long id, Instant createdAt, BigDecimal total, List<SaleLineResponse> lines) {

    public static SaleResponse from(Sale s) {
        return new SaleResponse(s.getId(), s.getCreatedAt(), s.getTotal(),
                s.getLines().stream().map(SaleLineResponse::from).toList());
    }
}