package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EpicTest {

    @Test
    void subtasksWithSameIdShouldBeEqual() {
        Epic epic1 = new Epic("Sub 1", "Desc");
        epic1.setId(5);
        Epic epic2 = new Epic("Sub 2", "Desc");
        epic2.setId(5);

        assertEquals(epic1, epic2, "Подзадачи с одинаковым ID должны быть равны");
        assertEquals(epic1.hashCode(), epic2.hashCode(), "HashCode должен совпадать");
    }

    @Test
    void epicWithoutSubtasksShouldHaveNewStatus() {
        Epic epic = new Epic("Epic", "Description");
        assertEquals(Status.NEW, epic.getStatus());
    }

        @Test
        void epicShouldNotBeSubtaskOfItself() {
            Epic epic = new Epic("Epic", "Description");
            epic.setId(1);

            try {
                epic.addSubtaskId(1);
                fail("Ожидалось исключение IllegalArgumentException, но оно не было выброшено");
            } catch (IllegalArgumentException ignored) {
            }
        }
    }