package com.example.warehouse.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageData<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageData<T> from(Page<T> p) {
        return new PageData<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
