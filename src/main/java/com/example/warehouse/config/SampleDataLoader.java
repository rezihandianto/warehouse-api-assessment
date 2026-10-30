package com.example.warehouse.config;

import java.math.BigDecimal;
import java.util.Map;

import com.example.warehouse.dto.request.CreateItemRequest;
import com.example.warehouse.dto.request.CreateVariantRequest;
import com.example.warehouse.dto.response.ItemResponse;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.warehouse.repository.ItemRepository;
import com.example.warehouse.service.ItemService;

/** Seeds demo data on an empty database. Disable with app.seed-data=false. */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
class SampleDataLoader implements ApplicationRunner {

    private final ItemService itemService;
    private final ItemRepository itemRepository;

    SampleDataLoader(ItemService itemService, ItemRepository itemRepository) {
        this.itemService = itemService;
        this.itemRepository = itemRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (itemRepository.count() > 0) {
            return;
        }

        ItemResponse tee = itemService.createItem(new CreateItemRequest(
                "TEE-001", "Basic T-Shirt", "100% cotton crew neck", new BigDecimal("300000"), null));
        itemService.createVariant(tee.id(), new CreateVariantRequest(
                "TEE-001-S-BLK", "Small / Black", Map.of("size", "S", "color", "black"), null, 25));
        itemService.createVariant(tee.id(), new CreateVariantRequest(
                "TEE-001-M-BLK", "Medium / Black", Map.of("size", "M", "color", "black"), null, 40));
        itemService.createVariant(tee.id(), new CreateVariantRequest(
                "TEE-001-L-WHT", "Large / White", Map.of("size", "L", "color", "white"),
                new BigDecimal("1650000"), 0)); // starts out of stock

        itemService.createItem(new CreateItemRequest(
                "MUG-001", "Ceramic Mug", "350ml, dishwasher safe", new BigDecimal("8500"), 100));
        itemService.createItem(new CreateItemRequest(
                "CAP-001", "Baseball Cap", "Adjustable strap", new BigDecimal("1200000"), 5));
    }
}
