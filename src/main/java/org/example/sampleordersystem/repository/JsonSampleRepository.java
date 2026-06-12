package org.example.sampleordersystem.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.sampleordersystem.model.Sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonSampleRepository implements SampleRepository {

    private final Path filePath;
    private final ObjectMapper mapper;

    public JsonSampleRepository(Path filePath) {
        this.filePath = filePath;
        this.mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void save(Sample sample) {
        List<Sample> list = readJson();
        list.removeIf(s -> s.getId().equals(sample.getId()));
        list.add(sample);
        writeJson(list);
    }

    @Override
    public Optional<Sample> findById(String id) {
        return readJson().stream()
            .filter(s -> s.getId().equals(id))
            .findFirst();
    }

    @Override
    public List<Sample> findAll() {
        return readJson();
    }

    @Override
    public List<Sample> findByNameContaining(String keyword) {
        return readJson().stream()
            .filter(s -> s.getName().contains(keyword))
            .toList();
    }

    private List<Sample> readJson() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try {
            return mapper.readValue(filePath.toFile(), new TypeReference<List<Sample>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeJson(List<Sample> data) {
        Path parent = filePath.toAbsolutePath().getParent();
        Path tmp = null;
        try {
            tmp = Files.createTempFile(parent, "tmp-", ".json");
            mapper.writeValue(tmp.toFile(), data);
            Files.move(tmp, filePath, StandardCopyOption.ATOMIC_MOVE,
                                      StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (tmp != null) {
                tmp.toFile().delete();
            }
        }
    }
}
