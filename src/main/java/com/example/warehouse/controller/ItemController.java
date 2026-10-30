package com.example.warehouse.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.warehouse.dto.request.CreateItemRequest;
import com.example.warehouse.dto.request.CreateVariantRequest;
import com.example.warehouse.dto.request.StockAdjustmentRequest;
import com.example.warehouse.dto.request.UpdateItemRequest;
import com.example.warehouse.dto.request.UpdateVariantRequest;
import com.example.warehouse.dto.response.BaseResponse;
import com.example.warehouse.dto.response.ItemResponse;
import com.example.warehouse.dto.response.PageData;
import com.example.warehouse.dto.response.VariantResponse;
import com.example.warehouse.service.ItemService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Items")
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<BaseResponse<ItemResponse>> create(@Valid @RequestBody CreateItemRequest req) {
        ItemResponse created = service.createItem(req);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}").buildAndExpand(created.id()).toUri())
                .body(BaseResponse.success("Item created", created));
    }

    @GetMapping
    BaseResponse<PageData<ItemResponse>> list(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return BaseResponse.success("Items retrieved", PageData.from(service.listItems(pageable)));
    }

    @GetMapping("/{id}")
    BaseResponse<ItemResponse> get(@PathVariable Long id) {
        return BaseResponse.success("Item retrieved", service.getItem(id));
    }

    @PutMapping("/{id}")
    BaseResponse<ItemResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateItemRequest req) {
        return BaseResponse.success("Item updated", service.updateItem(id, req));
    }

    @DeleteMapping("/{id}")
    BaseResponse<Void> delete(@PathVariable Long id) {
        service.deleteItem(id);
        return BaseResponse.success("Item deleted", null);
    }

    @PostMapping("/{id}/stock-adjustments")
    BaseResponse<ItemResponse> adjustStock(@PathVariable Long id,
                                           @Valid @RequestBody StockAdjustmentRequest req) {
        return BaseResponse.success("Stock adjusted", service.adjustItemStock(id, req));
    }

    // ---------- variants ----------

    @PostMapping("/{itemId}/variants")
    ResponseEntity<BaseResponse<VariantResponse>> createVariant(@PathVariable Long itemId,
                                                                @Valid @RequestBody CreateVariantRequest req) {
        VariantResponse created = service.createVariant(itemId, req);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}").buildAndExpand(created.id()).toUri())
                .body(BaseResponse.success("Variant created", created));
    }

    @GetMapping("/{itemId}/variants")
    BaseResponse<List<VariantResponse>> listVariants(@PathVariable Long itemId) {
        return BaseResponse.success("Variants retrieved", service.listVariants(itemId));
    }

    @GetMapping("/{itemId}/variants/{variantId}")
    BaseResponse<VariantResponse> getVariant(@PathVariable Long itemId, @PathVariable Long variantId) {
        return BaseResponse.success("Variant retrieved", service.getVariant(itemId, variantId));
    }

    @PutMapping("/{itemId}/variants/{variantId}")
    BaseResponse<VariantResponse> updateVariant(@PathVariable Long itemId, @PathVariable Long variantId,
                                                @Valid @RequestBody UpdateVariantRequest req) {
        return BaseResponse.success("Variant updated", service.updateVariant(itemId, variantId, req));
    }

    @DeleteMapping("/{itemId}/variants/{variantId}")
    BaseResponse<Void> deleteVariant(@PathVariable Long itemId, @PathVariable Long variantId) {
        service.deleteVariant(itemId, variantId);
        return BaseResponse.success("Variant deleted", null);
    }

    @PostMapping("/{itemId}/variants/{variantId}/stock-adjustments")
    BaseResponse<VariantResponse> adjustVariantStock(@PathVariable Long itemId, @PathVariable Long variantId,
                                                     @Valid @RequestBody StockAdjustmentRequest req) {
        return BaseResponse.success("Variant stock adjusted", service.adjustVariantStock(itemId, variantId, req));
    }
}