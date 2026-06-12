package org.example.sampleordersystem.service;

import org.example.sampleordersystem.model.Sample;
import org.example.sampleordersystem.repository.SampleRepository;

import java.util.List;
import java.util.Optional;

public class SampleService {

    private final SampleRepository sampleRepository;

    public SampleService(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    public Sample register(String id, String name, double avgProductionMinutes, double yield, int stock) {
        if (sampleRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 시료 ID입니다");
        }
        Sample sample = new Sample(id, name, avgProductionMinutes, yield, stock);
        sampleRepository.save(sample);
        return sample;
    }

    public Optional<Sample> findById(String id) {
        return sampleRepository.findById(id);
    }

    public List<Sample> findAll() {
        return sampleRepository.findAll();
    }

    public List<Sample> findByNameContaining(String keyword) {
        return sampleRepository.findByNameContaining(keyword);
    }
}
