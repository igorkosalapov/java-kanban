package model;

import org.junit.jupiter.api.*;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SubtaskTest {

    @Test
    @DisplayName("Подзадачи с одинаковым ID должны быть равны и иметь одинаковый hashCode")
    void subtasksWithSameIdShouldBeEqual() {
        Subtask sub1 = new Subtask("Sub 1", "Desc", Status.NEW, 1, LocalDateTime.of(2025,
                7, 16, 10, 0), Duration.ofMinutes(30));
        sub1.setId(5);

        Subtask sub2 = new Subtask("Sub 2", "Desc", Status.DONE, 2, LocalDateTime.of(2025,
                7, 16, 11, 0), Duration.ofMinutes(45));
        sub2.setId(5);

        assertEquals(sub1, sub2, "Подзадачи с одинаковым ID должны быть равны");
        assertEquals(sub1.hashCode(), sub2.hashCode(), "HashCode должен совпадать");
    }

    @Test
    @DisplayName("Метод getEpicId должен возвращать корректный ID эпика")
    void subtaskShouldReturnCorrectEpicId() {
        Subtask sub = new Subtask("Sub", "Desc", Status.NEW, 10, LocalDateTime.of(2025,
                7, 16, 10, 0),
                Duration.ofMinutes(30));
        assertEquals(10, sub.getEpicId(), "Должен возвращаться корректный epicId");
    }
}
