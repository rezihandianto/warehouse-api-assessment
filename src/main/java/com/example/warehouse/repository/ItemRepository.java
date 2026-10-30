package com.example.warehouse.repository;

import com.example.warehouse.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsBySku(String sku);

    /**
     * Atomic, guarded stock change executed as a single SQL statement:
     * {@code UPDATE ... SET stock = stock + :delta WHERE id = :id AND stock + :delta >= 0}.
     * Returns 0 when the row is missing or the change would make stock negative,
     * so overselling is impossible even under concurrent requests.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Item i set i.stockQuantity = i.stockQuantity + :delta
            where i.id = :id and i.stockQuantity + :delta >= 0
            """)
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);
}
