package org.example.sampleordersystem.repository;

import org.example.sampleordersystem.model.Sample;

import java.util.List;
import java.util.Optional;

public interface SampleRepository {
    void save(Sample sample);
    Optional<Sample> findById(String id);
    List<Sample> findAll();
    List<Sample> findByNameContaining(String keyword);
}
