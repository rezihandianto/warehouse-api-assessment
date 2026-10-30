package com.example.warehouse.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.warehouse.dto.request.CreateSaleRequest;
import com.example.warehouse.dto.response.BaseResponse;
import com.example.warehouse.dto.response.SaleResponse;
import com.example.warehouse.service.SaleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Sales")
@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @Operation(summary = "Create a sale",
            description = "Atomically decrements stock for every line; all-or-nothing.")
    @ApiResponse(responseCode = "201", description = "Sale created")
    @ApiResponse(responseCode = "409", description = "Insufficient stock")
    @PostMapping
    ResponseEntity<BaseResponse<SaleResponse>> create(@Valid @RequestBody CreateSaleRequest req) {
        SaleResponse created = service.createSale(req);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}").buildAndExpand(created.id()).toUri())
                .body(BaseResponse.success("Sale created", created));
    }

    @GetMapping("/{id}")
    BaseResponse<SaleResponse> get(@PathVariable Long id) {
        return BaseResponse.success("Sale retrieved", service.getSale(id));
    }
}