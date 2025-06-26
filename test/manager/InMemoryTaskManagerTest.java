package manager;

import manager.task.TaskManager;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest {
    private TaskManager manager;

    @BeforeEach
    void setUp() {
        manager = Managers.getDefault();
    }

    @Test
    void shouldAddAndFindDifferentTaskTypes() {
        Task task = new Task("Task", "Desc", Status.NEW);
        Epic epic = new Epic("Epic", "Desc");
        Subtask subtask = new Subtask("Sub", "Desc", Status.NEW, 2);

        manager.createTask(task);
        manager.createEpic(epic);
        manager.createSubtask(subtask);

        assertNotNull(manager.getTaskById(task.getId()));
        assertNotNull(manager.getEpicById(epic.getId()));
        assertNotNull(manager.getSubtaskById(subtask.getId()));
    }

    @Test
    void shouldNotConflictWithGeneratedAndManualIds() {
        Task task1 = new Task("Task 1", "Desc", Status.NEW);
        task1.setId(100);
        manager.createTask(task1);

        Task task2 = new Task("Task 2", "Desc", Status.NEW);
        manager.createTask(task2);

        assertNotNull(manager.getTaskById(task1.getId()));
        assertNotNull(manager.getTaskById(task2.getId()));

        assertNotEquals(task1.getId(), task2.getId());
    }

    @Test
    void taskInManagerReflectsExternalChanges() {

        Task task = new Task("Задача", "Описание", Status.NEW);
        manager.createTask(task);
        int taskId = task.getId();

        task.setName("Изменённая задача");

        Task stored = manager.getTaskById(taskId);

        assertEquals("Изменённая задача", stored.getName(),
                "Изменения в исходном объекте отражаются и в менеджере (передаётся ссылка)");
    }
}