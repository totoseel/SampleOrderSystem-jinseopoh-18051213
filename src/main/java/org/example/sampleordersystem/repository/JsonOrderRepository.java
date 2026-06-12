package org.example.sampleordersystem.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.sampleordersystem.model.Order;
import org.example.sampleordersystem.model.OrderStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonOrderRepository implements OrderRepository {

    private final Path filePath;
    private final ObjectMapper mapper;

    public JsonOrderRepository(Path filePath) {
        this.filePath = filePath;
        this.mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void save(Order order) {
        List<Order> list = readJson();
        list.removeIf(o -> o.getOrderId().equals(order.getOrderId()));
        list.add(order);
        writeJson(list);
    }

    @Override
    public Optional<Order> findById(String id) {
        return readJson().stream()
            .filter(o -> o.getOrderId().equals(id))
            .findFirst();
    }

    @Override
    public List<Order> findAll() {
        return readJson();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return readJson().stream()
            .filter(o -> o.getStatus() == status)
            .toList();
    }

    @Override
    public int countByDatePrefix(String yyyymmdd) {
        return (int) readJson().stream()
            .filter(o -> o.getOrderId().startsWith("ORD-" + yyyymmdd))
            .count();
    }

    private List<Order> readJson() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try {
            return mapper.readValue(filePath.toFile(), new TypeReference<List<Order>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeJson(List<Order> data) {
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
