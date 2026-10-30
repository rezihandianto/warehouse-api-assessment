package com.example.warehouse.repository;

import java.util.List;
import java.util.Optional;

import com.example.warehouse.entity.ItemVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemVariantRepository extends JpaRepository<ItemVariant, Long> {

    boolean existsBySku(String sku);

    boolean existsByItemId(Long itemId);

    List<ItemVariant> findByItemIdOrderById(Long itemId);

    Optional<ItemVariant> findByIdAndItemId(Long id, Long itemId);

    /** See {@link ItemRepository#adjustStock(Long, int)}. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ItemVariant v set v.stockQuantity = v.stockQuantity + :delta
            where v.id = :id and v.stockQuantity + :delta >= 0
            """)
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);
}
