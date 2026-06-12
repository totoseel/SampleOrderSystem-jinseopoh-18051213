package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.ProductionEntry;

import java.util.List;
import java.util.Optional;

public interface ProductionRepository {
    void save(ProductionEntry entry);
    List<ProductionEntry> findAll();
    Optional<ProductionEntry> findByOrderId(String orderId);
    void delete(String orderId);
}
