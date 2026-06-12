package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.Sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemorySampleRepository implements SampleRepository {

    private final Map<String, Sample> store = new HashMap<>();

    @Override
    public void save(Sample sample) {
        store.put(sample.getId(), sample);
    }

    @Override
    public Optional<Sample> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Sample> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Sample> findByNameContaining(String keyword) {
        return store.values().stream()
            .filter(s -> s.getName().contains(keyword))
            .toList();
    }
}
