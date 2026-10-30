package com.example.warehouse.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.warehouse.dto.request.CreateSaleRequest;
import com.example.warehouse.dto.request.SaleLineRequest;
import com.example.warehouse.dto.response.SaleResponse;
import com.example.warehouse.entity.Item;
import com.example.warehouse.entity.ItemVariant;
import com.example.warehouse.entity.Sale;
import com.example.warehouse.entity.SaleLine;
import com.example.warehouse.exception.ApiException;
import com.example.warehouse.repository.ItemRepository;
import com.example.warehouse.repository.ItemVariantRepository;
import com.example.warehouse.repository.SaleRepository;

@Service
public class SaleService {

    private final SaleRepository sales;
    private final ItemRepository items;
    private final ItemVariantRepository variants;

    public SaleService(SaleRepository sales, ItemRepository items, ItemVariantRepository variants) {
        this.sales = sales;
        this.items = items;
        this.variants = variants;
    }

    /**
     * All-or-nothing: if any line lacks stock, the exception rolls back every decrement
     * made so far in this transaction. Stock is decremented with a guarded UPDATE, so two
     * concurrent sales can never both take the last unit.
     */
    @Transactional
    public SaleResponse createSale(CreateSaleRequest req) {
        Sale sale = new Sale();

        for (SaleLineRequest line : mergeAndOrder(req.lines())) {
            Item item = items.findById(line.itemId())
                    .orElseThrow(() -> ApiException.notFound("Item", line.itemId()));

            if (line.variantId() == null) {
                if (variants.existsByItemId(item.getId())) {
                    throw ApiException.badRequest("Item " + item.getId() + " has variants; variantId is required");
                }
                take(items.adjustStock(item.getId(), -line.quantity()), item.getSku(), line.quantity());
                sale.addLine(new SaleLine(sale, item.getId(), null, item.getSku(), item.getName(),
                        line.quantity(), item.getPrice()));
            } else {
                ItemVariant v = variants.findByIdAndItemId(line.variantId(), item.getId())
                        .orElseThrow(() -> ApiException.notFound("Variant",
                                line.variantId() + " of item " + item.getId()));
                take(variants.adjustStock(v.getId(), -line.quantity()), v.getSku(), line.quantity());
                sale.addLine(new SaleLine(sale, item.getId(), v.getId(), v.getSku(),
                        item.getName() + " - " + v.getName(), line.quantity(), v.getEffectivePrice()));
            }
        }
        return SaleResponse.from(sales.save(sale));
    }

    @Transactional(readOnly = true)
    public SaleResponse getSale(Long id) {
        return SaleResponse.from(sales.findById(id).orElseThrow(() -> ApiException.notFound("Sale", id)));
    }

    private void take(int updatedRows, String sku, int qty) {
        if (updatedRows == 0) {
            throw ApiException.insufficientStock("Insufficient stock for " + sku + " (requested " + qty + ")");
        }
    }

    /**
     * Merges duplicate lines (same item+variant) so the stock check sees the true total,
     * and sorts them by key so concurrent multi-line sales lock rows in the same order
     * (avoids deadlocks).
     */
    private List<SaleLineRequest> mergeAndOrder(List<SaleLineRequest> lines) {
        Map<String, SaleLineRequest> merged = new LinkedHashMap<>();
        for (SaleLineRequest l : lines) {
            String key = l.itemId() + ":" + l.variantId();
            merged.merge(key, l, (a, b) -> new SaleLineRequest(a.itemId(), a.variantId(), a.quantity() + b.quantity()));
        }
        List<SaleLineRequest> ordered = new ArrayList<>(merged.values());
        ordered.sort(Comparator.comparing(SaleLineRequest::itemId)
                .thenComparing(SaleLineRequest::variantId, Comparator.nullsFirst(Comparator.naturalOrder())));
        return ordered;
    }
}