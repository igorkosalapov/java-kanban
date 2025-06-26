package model;

import manager.Managers;
import manager.task.TaskManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskTest {
    @Test
    void tasksWithSameIdShouldBeEqual() {
        Task task1 = new Task("Task 1", "Description", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Task 2", "Description", Status.IN_PROGRESS);
        task2.setId(1);

        assertEquals(task1, task2, "Задачи с одинаковым id должны быть равны");
        assertEquals(task1.hashCode(), task2.hashCode(), "HashCode должен совпадать");
    }

    @Test
    void taskShouldNotChangeAfterAddingToManager() {
        TaskManager manager = Managers.getDefault();
        Task task = new Task("Original", "Desc", Status.NEW);
        manager.createTask(task);

        Task savedTask = manager.getTaskById(task.getId());

        assertEquals("Original", savedTask.getName());
        assertEquals("Desc", savedTask.getDescription());
        assertEquals(Status.NEW, savedTask.getStatus());
    }
}