package com.example.warehouse.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.warehouse.dto.request.CreateItemRequest;
import com.example.warehouse.dto.request.CreateVariantRequest;
import com.example.warehouse.dto.request.StockAdjustmentRequest;
import com.example.warehouse.dto.request.UpdateItemRequest;
import com.example.warehouse.dto.request.UpdateVariantRequest;
import com.example.warehouse.dto.response.ItemResponse;
import com.example.warehouse.dto.response.VariantResponse;
import com.example.warehouse.entity.Item;
import com.example.warehouse.entity.ItemVariant;
import com.example.warehouse.exception.ApiException;
import com.example.warehouse.repository.ItemRepository;
import com.example.warehouse.repository.ItemVariantRepository;

@Service
@Transactional
public class ItemService {

    private final ItemRepository items;
    private final ItemVariantRepository variants;

    public ItemService(ItemRepository items, ItemVariantRepository variants) {
        this.items = items;
        this.variants = variants;
    }

    // ---------- Items ----------

    public ItemResponse createItem(CreateItemRequest req) {
        if (items.existsBySku(req.sku())) {
            throw ApiException.conflict("Item SKU already exists: " + req.sku());
        }
        Item item = items.save(new Item(req.sku(), req.name(), req.description(), req.price(),
                req.initialStock() == null ? 0 : req.initialStock()));
        return ItemResponse.from(item, List.of());
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> listItems(Pageable pageable) {
        return items.findAll(pageable)
                .map(i -> ItemResponse.from(i, variants.findByItemIdOrderById(i.getId())));
    }

    @Transactional(readOnly = true)
    public ItemResponse getItem(Long id) {
        Item item = findItem(id);
        return ItemResponse.from(item, variants.findByItemIdOrderById(id));
    }

    public ItemResponse updateItem(Long id, UpdateItemRequest req) {
        Item item = findItem(id);
        item.update(req.name(), req.description(), req.price());
        return ItemResponse.from(item, variants.findByItemIdOrderById(id));
    }

    public void deleteItem(Long id) {
        items.delete(findItem(id)); // cascades to variants
    }

    public ItemResponse adjustItemStock(Long id, StockAdjustmentRequest req) {
        findItem(id);
        if (variants.existsByItemId(id)) {
            throw ApiException.badRequest("Item has variants; adjust stock on a variant instead");
        }
        applyAdjustment(items.adjustStock(id, req.delta()), "item " + id);
        return getItem(id);
    }

    // ---------- Variants ----------

    public VariantResponse createVariant(Long itemId, CreateVariantRequest req) {
        Item item = findItem(itemId);
        if (variants.existsBySku(req.sku())) {
            throw ApiException.conflict("Variant SKU already exists: " + req.sku());
        }
        if (item.getStockQuantity() > 0) {
            throw ApiException.conflict(
                    "Item still holds its own stock; move it to variants (adjust to 0) before adding variants");
        }
        ItemVariant v = variants.save(new ItemVariant(item, req.sku(), req.name(), req.attributes(),
                req.price(), req.initialStock() == null ? 0 : req.initialStock()));
        return VariantResponse.from(v);
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> listVariants(Long itemId) {
        findItem(itemId);
        return variants.findByItemIdOrderById(itemId).stream().map(VariantResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VariantResponse getVariant(Long itemId, Long variantId) {
        return VariantResponse.from(findVariant(itemId, variantId));
    }

    public VariantResponse updateVariant(Long itemId, Long variantId, UpdateVariantRequest req) {
        ItemVariant v = findVariant(itemId, variantId);
        v.update(req.name(), req.attributes(), req.price());
        return VariantResponse.from(v);
    }

    public void deleteVariant(Long itemId, Long variantId) {
        ItemVariant v = findVariant(itemId, variantId);
        v.getItem().getVariants().remove(v);
        variants.delete(v);
    }

    public VariantResponse adjustVariantStock(Long itemId, Long variantId, StockAdjustmentRequest req) {
        findVariant(itemId, variantId);
        applyAdjustment(variants.adjustStock(variantId, req.delta()), "variant " + variantId);
        return getVariant(itemId, variantId);
    }

    // ---------- helpers ----------

    private void applyAdjustment(int updatedRows, String what) {
        if (updatedRows == 0) {
            throw ApiException.insufficientStock("Adjustment would make stock negative for " + what);
        }
    }

    private Item findItem(Long id) {
        return items.findById(id).orElseThrow(() -> ApiException.notFound("Item", id));
    }

    private ItemVariant findVariant(Long itemId, Long variantId) {
        return variants.findByIdAndItemId(variantId, itemId)
                .orElseThrow(() -> ApiException.notFound("Variant", variantId + " of item " + itemId));
    }
}