package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SubtaskTest {
    @Test
    void subtasksWithSameIdShouldBeEqual() {
        Subtask sub1 = new Subtask("Sub 1", "Desc", Status.NEW, 1);
        sub1.setId(5);
        Subtask sub2 = new Subtask("Sub 2", "Desc", Status.DONE, 2);
        sub2.setId(5);

        assertEquals(sub1, sub2, "Подзадачи с одинаковым ID должны быть равны");
        assertEquals(sub1.hashCode(), sub2.hashCode(), "HashCode должен совпадать");
    }

    @Test
    void subtaskShouldReturnCorrectEpicId() {
        Subtask sub = new Subtask("Sub", "Desc", Status.NEW, 10);
        assertEquals(10, sub.getEpicId(), "Должен возвращаться корректный epicId");
    }
}