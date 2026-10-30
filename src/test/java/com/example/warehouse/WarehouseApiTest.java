package com.example.warehouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.warehouse.dto.request.CreateSaleRequest;
import com.example.warehouse.dto.request.SaleLineRequest;
import com.example.warehouse.exception.ApiException;
import com.example.warehouse.service.SaleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.seed-data=false")
class WarehouseApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired SaleService saleService;

    // ---------- helpers ----------

    private long createItem(String sku, String price, Integer stock) throws Exception {
        String body = stock == null
                ? """
                  {"sku":"%s","name":"N-%s","price":%s}""".formatted(sku, sku, price)
                : """
                  {"sku":"%s","name":"N-%s","price":%s,"initialStock":%d}""".formatted(sku, sku, price, stock);
        String res = mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("data").get("id").asLong();
    }

    private long createVariant(long itemId, String sku, int stock) throws Exception {
        String body = """
                {"sku":"%s","name":"V-%s","attributes":{"size":"M"},"initialStock":%d}""".formatted(sku, sku, stock);
        String res = mvc.perform(post("/api/items/" + itemId + "/variants")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("data").get("id").asLong();
    }

    private String sale(long itemId, Long variantId, int qty) {
        return """
                {"lines":[{"itemId":%d,"variantId":%s,"quantity":%d}]}"""
                .formatted(itemId, variantId, qty);
    }

    // ---------- tests ----------

    @Test
    void crudAndValidation() throws Exception {
        long id = createItem("CRUD-1", "9.99", 3);

        mvc.perform(get("/api/items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("success"))
                .andExpect(jsonPath("$.responseMessage").value("Item retrieved"))
                .andExpect(jsonPath("$.data.stockQuantity").value(3))
                .andExpect(jsonPath("$.data.inStock").value(true));

        // duplicate SKU -> 409 failed
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"CRUD-1\",\"name\":\"x\",\"price\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseStatus").value("failed"));

        // invalid body -> 400 failed, field errors in data
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"\",\"name\":\"x\",\"price\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseStatus").value("failed"))
                .andExpect(jsonPath("$.responseMessage").value("Validation failed"))
                .andExpect(jsonPath("$.data.sku").exists())
                .andExpect(jsonPath("$.data.price").exists());

        mvc.perform(delete("/api/items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("success"));
        mvc.perform(get("/api/items/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseStatus").value("failed"));
    }

    @Test
    void listIsPaginated() throws Exception {
        createItem("PAGE-1", "1.00", 1);

        mvc.perform(get("/api/items?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.size").value(5));

        // unknown sort field -> 400 instead of 500
        mvc.perform(get("/api/items?sort=doesNotExist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseStatus").value("failed"));
    }

    @Test
    void cannotSellMoreThanInStock() throws Exception {
        long id = createItem("STK-1", "5.00", 2);

        mvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(sale(id, null, 3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseStatus").value("failed"));

        mvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(sale(id, null, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseStatus").value("success"))
                .andExpect(jsonPath("$.data.total").value(10.00));

        // now out of stock
        mvc.perform(get("/api/items/" + id)).andExpect(jsonPath("$.data.inStock").value(false));
        mvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(sale(id, null, 1)))
                .andExpect(status().isConflict());
    }

    @Test
    void variantsHaveOwnStockAndPriceFallback() throws Exception {
        long itemId = createItem("VAR-1", "20.00", null);
        long variantId = createVariant(itemId, "VAR-1-M", 4);

        // item with variants requires variantId
        mvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(sale(itemId, null, 1)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(sale(itemId, variantId, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lines[0].unitPrice").value(20.00)); // inherited from item

        mvc.perform(get("/api/items/" + itemId))
                .andExpect(jsonPath("$.data.stockQuantity").value(3)); // sum of variants
    }

    @Test
    void failedSaleRollsBackEarlierLines() throws Exception {
        long a = createItem("RB-A", "1.00", 10);
        long b = createItem("RB-B", "1.00", 1);

        String body = """
                {"lines":[{"itemId":%d,"quantity":5},{"itemId":%d,"quantity":2}]}""".formatted(a, b);
        mvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        // line A must not have been decremented
        mvc.perform(get("/api/items/" + a)).andExpect(jsonPath("$.data.stockQuantity").value(10));
    }

    @Test
    void concurrentSalesNeverOversell() throws Exception {
        long id = createItem("CONC-1", "1.00", 10);
        int threads = 25;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    saleService.createSale(new CreateSaleRequest(List.of(new SaleLineRequest(id, null, 1))));
                    ok.incrementAndGet();
                } catch (ApiException e) {
                    rejected.incrementAndGet();
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(ok.get()).isEqualTo(10);
        assertThat(rejected.get()).isEqualTo(threads - 10);

        String res = mvc.perform(get("/api/items/" + id)).andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(res);
        assertThat(node.get("data").get("stockQuantity").asInt()).isZero();
    }

    @Test
    void stockAdjustmentCannotGoNegative() throws Exception {
        long id = createItem("ADJ-1", "1.00", 2);

        mvc.perform(post("/api/items/" + id + "/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":-3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseStatus").value("failed"));

        mvc.perform(post("/api/items/" + id + "/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQuantity").value(10));
    }
}