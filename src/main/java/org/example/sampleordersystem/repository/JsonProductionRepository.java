package org.example.sampleordersystem.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.sampleordersystem.model.ProductionEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonProductionRepository implements ProductionRepository {

    private final Path filePath;
    private final ObjectMapper mapper;

    public JsonProductionRepository(Path filePath) {
        this.filePath = filePath;
        this.mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void save(ProductionEntry entry) {
        List<ProductionEntry> list = readJson();
        list.removeIf(e -> e.getOrderId().equals(entry.getOrderId()));
        list.add(entry);
        writeJson(list);
    }

    @Override
    public List<ProductionEntry> findAll() {
        return readJson();
    }

    @Override
    public Optional<ProductionEntry> findByOrderId(String orderId) {
        return readJson().stream()
            .filter(e -> e.getOrderId().equals(orderId))
            .findFirst();
    }

    @Override
    public void delete(String orderId) {
        List<ProductionEntry> list = readJson();
        list.removeIf(e -> e.getOrderId().equals(orderId));
        writeJson(list);
    }

    private List<ProductionEntry> readJson() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try {
            return mapper.readValue(filePath.toFile(), new TypeReference<List<ProductionEntry>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeJson(List<ProductionEntry> data) {
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
