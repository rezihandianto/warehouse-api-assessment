package com.example.warehouse.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** A variant of an item (e.g. size M / color red) with its own stock and optional price override. */
@Entity
@Table(name = "item_variants")
public class ItemVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "item_variant_attributes", joinColumns = @JoinColumn(name = "variant_id"))
    @MapKeyColumn(name = "attr_key")
    @Column(name = "attr_value")
    private Map<String, String> attributes = new LinkedHashMap<>();

    /** Null means "inherit the item's price". */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ItemVariant() {
    }

    public ItemVariant(Item item, String sku, String name, Map<String, String> attributes,
                       BigDecimal price, int stockQuantity) {
        this.item = item;
        this.sku = sku;
        this.name = name;
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public Map<String, String> getAttributes() { return attributes; }
    public BigDecimal getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }

    public BigDecimal getEffectivePrice() {
        return price != null ? price : item.getPrice();
    }

    public void update(String name, Map<String, String> attributes, BigDecimal price) {
        this.name = name;
        this.attributes.clear();
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
        this.price = price;
    }
}
