package org.example.sampleordersystem.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderIdGeneratorTest {

    @Test
    @DisplayName("ORD-YYYYMMDD-NNNN 형식의 주문번호를 생성한다")
    void generatesCorrectFormat() {
        TimeProvider fixed = () -> LocalDateTime.of(2024, 1, 1, 0, 0);
        OrderIdGenerator gen = new OrderIdGenerator(0, fixed);

        assertEquals("ORD-20240101-0001", gen.next());
    }

    @Test
    @DisplayName("호출할 때마다 순번이 1씩 증가한다")
    void incrementsSequencePerCall() {
        TimeProvider fixed = () -> LocalDateTime.of(2024, 1, 1, 0, 0);
        OrderIdGenerator gen = new OrderIdGenerator(0, fixed);

        assertEquals("ORD-20240101-0001", gen.next());
        assertEquals("ORD-20240101-0002", gen.next());
        assertEquals("ORD-20240101-0003", gen.next());
    }

    @Test
    @DisplayName("재시작 후 마지막 순번 이후부터 이어받는다")
    void resumesSequenceFromLastSeq() {
        TimeProvider fixed = () -> LocalDateTime.of(2024, 1, 1, 0, 0);
        OrderIdGenerator gen = new OrderIdGenerator(5, fixed);

        assertEquals("ORD-20240101-0006", gen.next());
    }

    @Test
    @DisplayName("날짜가 바뀌면 순번을 1로 리셋한다")
    void resetsSequenceOnNewDay() {
        AtomicReference<LocalDateTime> time =
            new AtomicReference<>(LocalDateTime.of(2024, 1, 1, 0, 0));
        OrderIdGenerator gen = new OrderIdGenerator(0, time::get);

        gen.next(); // 2024-01-01, seq=1

        time.set(LocalDateTime.of(2024, 1, 2, 0, 0));
        assertEquals("ORD-20240102-0001", gen.next());
    }
}
