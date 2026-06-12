package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.ProductionEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryProductionRepository implements ProductionRepository {

    private final Map<String, ProductionEntry> store = new LinkedHashMap<>();

    @Override
    public void save(ProductionEntry entry) {
        store.put(entry.getOrderId(), entry);
    }

    @Override
    public List<ProductionEntry> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<ProductionEntry> findByOrderId(String orderId) {
        return Optional.ofNullable(store.get(orderId));
    }

    @Override
    public void delete(String orderId) {
        store.remove(orderId);
    }
}
