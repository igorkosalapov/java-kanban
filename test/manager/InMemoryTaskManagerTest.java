package manager;

import manager.task.TaskManager;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void deletingSubtaskRemovesIdFromEpic() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("Subtask 1", "Desc", Status.NEW, epic.getId());
        Subtask subtask2 = new Subtask("Subtask 2", "Desc", Status.NEW, epic.getId());
        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        assertEquals(2, manager.getSubtasksByEpicId(epic.getId()).size());

        manager.deleteSubtaskById(subtask1.getId());

        List<Subtask> subtasks = manager.getSubtasksByEpicId(epic.getId());
        assertEquals(1, subtasks.size());

        boolean containsDeleted = false;
        for (Subtask s : subtasks) {
            if (s.getId() == subtask1.getId()) {
                containsDeleted = true;
                break;
            }
        }
        assertFalse(containsDeleted, "Удалённой подзадачи не должно быть в списке эпика");
    }

    @Test
    void clearingSubtasksClearsEpicSubtaskIds() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);

        Subtask subtask = new Subtask("Subtask", "Desc", Status.NEW, epic.getId());
        manager.createSubtask(subtask);

        assertFalse(manager.getSubtasksByEpicId(epic.getId()).isEmpty());

        manager.clearSubtasks();

        assertTrue(manager.getSubtasksByEpicId(epic.getId()).isEmpty());
    }

    @Test
    void changingSubtaskStatusDirectlyDoesNotUpdateEpic() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);

        Subtask subtask = new Subtask("Subtask", "Desc", Status.NEW, epic.getId());
        manager.createSubtask(subtask);

        subtask.setStatus(Status.DONE);

        Epic storedEpic = manager.getEpicById(epic.getId());
        assertNotEquals(Status.DONE, storedEpic.getStatus(),
                "Статус эпика не должен обновиться, если статус подзадачи меняется напрямую без" +
                        " updateSubtask()");
    }

    @Test
    void changingTaskIdCausesKeyValueMismatch() {
        Task task = new Task("Task", "Desc", Status.NEW);
        manager.createTask(task);
        int originalId = task.getId();

        task.setId(999);

        Task retrieved = manager.getTaskById(originalId);

        assertNotNull(retrieved);
        assertNotEquals(originalId, retrieved.getId(),
                "id задачи в менеджере не совпадает с ключом в Map");
    }
}