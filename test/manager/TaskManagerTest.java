package manager;

import model.*;
import manager.task.TaskManager;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Базовые тесты TaskManager")
public abstract class TaskManagerTest<T extends TaskManager> {

    protected T manager;

    protected abstract T createManager();

    @BeforeEach
    void setUp() {
        manager = createManager();
    }

    @Test
    @DisplayName("Добавление и поиск разных типов задач")
    void shouldAddAndFindDifferentTaskTypes() {
        Task task = new Task("Task", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(30));
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);
        Subtask subtask = new Subtask("Sub", "Desc", Status.NEW, epic.getId(),
                LocalDateTime.of(2025, 7, 16, 11, 0), Duration.ofMinutes(20));
        manager.createTask(task);
        manager.createSubtask(subtask);

        assertNotNull(manager.getTaskById(task.getId()));
        assertNotNull(manager.getEpicById(epic.getId()));
        assertNotNull(manager.getSubtaskById(subtask.getId()));
    }

    @Test
    @DisplayName("Статус эпика — все подзадачи NEW")
    void epicStatusAllNew() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);
        Subtask s1 = new Subtask("Sub1", "Desc", Status.NEW, epic.getId(), LocalDateTime.now(),
                Duration.ofMinutes(10));
        Subtask s2 = new Subtask("Sub2", "Desc", Status.NEW, epic.getId(),
                LocalDateTime.now().plusMinutes(15), Duration.ofMinutes(20));
        manager.createSubtask(s1);
        manager.createSubtask(s2);
        assertEquals(Status.NEW, manager.getEpicById(epic.getId()).getStatus());
    }

    @Test
    @DisplayName("Статус эпика — все подзадачи DONE")
    void epicStatusAllDone() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);
        Subtask s1 = new Subtask("Sub1", "Desc", Status.DONE, epic.getId(), LocalDateTime.now(),
                Duration.ofMinutes(10));
        Subtask s2 = new Subtask("Sub2", "Desc", Status.DONE, epic.getId(),
                LocalDateTime.now().plusMinutes(15), Duration.ofMinutes(20));
        manager.createSubtask(s1);
        manager.createSubtask(s2);
        assertEquals(Status.DONE, manager.getEpicById(epic.getId()).getStatus());
    }

    @Test
    @DisplayName("Статус эпика — подзадачи NEW и DONE")
    void epicStatusNewAndDone() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);
        Subtask sNew = new Subtask("SubNew", "Desc", Status.NEW, epic.getId(), LocalDateTime.now(),
                Duration.ofMinutes(10));
        Subtask sDone = new Subtask("SubDone", "Desc", Status.DONE, epic.getId(),
                LocalDateTime.now().plusMinutes(15), Duration.ofMinutes(20));
        manager.createSubtask(sNew);
        manager.createSubtask(sDone);
        assertEquals(Status.IN_PROGRESS, manager.getEpicById(epic.getId()).getStatus());
    }

    @Test
    @DisplayName("Статус эпика — подзадачи IN_PROGRESS и DONE")
    void epicStatusInProgress() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);
        Subtask sInProgress = new Subtask("SubInProgress", "Desc", Status.IN_PROGRESS, epic.getId(),
                LocalDateTime.now(), Duration.ofMinutes(10));
        Subtask sDone = new Subtask("SubDone", "Desc", Status.DONE, epic.getId(),
                LocalDateTime.now().plusMinutes(15), Duration.ofMinutes(20));
        manager.createSubtask(sInProgress);
        manager.createSubtask(sDone);
        assertEquals(Status.IN_PROGRESS, manager.getEpicById(epic.getId()).getStatus());
    }

    @Test
    @DisplayName("Проверка исключения при пересечении времени задач")
    void shouldThrowOnTimeIntersection() {
        Task task1 = new Task("Task1", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(60));
        manager.createTask(task1);

        Task task2 = new Task("Task2", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 30), Duration.ofMinutes(30));

        assertThrows(IllegalArgumentException.class, () -> manager.createTask(task2));
    }

    @Test
    @DisplayName("Задача до существующей — не должно быть пересечения")
    void shouldAllowTaskBeforeExisting() {
        Task task1 = new Task("Task1", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(60));
        Task task2 = new Task("Task2", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 8, 0), Duration.ofMinutes(90)); // 8:00–9:30

        manager.createTask(task1);
        assertDoesNotThrow(() -> manager.createTask(task2));
    }

    @Test
    @DisplayName("Задача после существующей — не должно быть пересечения")
    void shouldAllowTaskAfterExisting() {
        Task task1 = new Task("Task1", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(60)); // 10:00–11:00
        Task task2 = new Task("Task2", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 11, 30), Duration.ofMinutes(30)); // 11:30–12:00

        manager.createTask(task1);
        assertDoesNotThrow(() -> manager.createTask(task2));
    }

    @Test
    @DisplayName("Новая задача полностью внутри существующей — должно быть пересечение")
    void shouldFailIfTaskIsInsideExisting() {
        Task task1 = new Task("Task1", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(90)); // 10:00–11:30
        Task task2 = new Task("Inside", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 30), Duration.ofMinutes(30)); // 10:30–11:00

        manager.createTask(task1);
        assertThrows(IllegalArgumentException.class, () -> manager.createTask(task2));
    }

    @Test
    @DisplayName("Новая задача частично пересекает начало существующей")
    void shouldFailIfTaskOverlapsStart() {
        Task task1 = new Task("Task1", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(60)); // 10:00–11:00
        Task task2 = new Task("OverlapStart", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 9, 30), Duration.ofMinutes(45)); // 9:30–10:15

        manager.createTask(task1);
        assertThrows(IllegalArgumentException.class, () -> manager.createTask(task2));
    }

    @Test
    @DisplayName("Новая задача частично пересекает конец существующей")
    void shouldFailIfTaskOverlapsEnd() {
        Task task1 = new Task("Task1", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(60)); // 10:00–11:00
        Task task2 = new Task("OverlapEnd", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 45), Duration.ofMinutes(30)); // 10:45–11:15

        manager.createTask(task1);
        assertThrows(IllegalArgumentException.class, () -> manager.createTask(task2));
    }

    @Test
    @DisplayName("Новая задача полностью перекрывает существующую")
    void shouldFailIfTaskWrapsExisting() {
        Task task1 = new Task("Task1", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(60)); // 10:00–11:00
        Task task2 = new Task("Wrapper", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 9, 0), Duration.ofMinutes(180)); // 9:00–12:00

        manager.createTask(task1);
        assertThrows(IllegalArgumentException.class, () -> manager.createTask(task2));
    }

    @Test
    @DisplayName("Новая задача точно совпадает по времени")
    void shouldFailIfExactTimeMatch() {
        Task task1 = new Task("Task1", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(60));
        Task task2 = new Task("ExactMatch", "Desc", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(60));

        manager.createTask(task1);
        assertThrows(IllegalArgumentException.class, () -> manager.createTask(task2));
    }

    @Test
    @DisplayName("Пустая история возвращает пустой список")
    void emptyHistoryIsEmpty() {
        assertTrue(manager.getHistory().isEmpty());
    }

    @Test
    @DisplayName("История не содержит дубликатов")
    void historyDoesNotDuplicate() {
        Task task = new Task("Task", "Desc", Status.NEW, LocalDateTime.now(), Duration.ofMinutes(10));
        manager.createTask(task);
        manager.getTaskById(task.getId());
        manager.getTaskById(task.getId());
        assertEquals(1, manager.getHistory().size());
    }

    @Test
    @DisplayName("Удаление задач из истории из разных позиций")
    void historyRemoveFromDifferentPositions() {
        Task t1 = new Task("T1", "desc", Status.NEW, LocalDateTime.now(), Duration.ofMinutes(10));
        Epic e1 = new Epic("E1", "desc");
        manager.createTask(t1);
        manager.createEpic(e1);
        Subtask s1 = new Subtask("S1", "desc", Status.NEW, e1.getId(),
                LocalDateTime.now().plusMinutes(15), Duration.ofMinutes(20));
        manager.createSubtask(s1);
        manager.getTaskById(t1.getId());
        manager.getEpicById(e1.getId());
        manager.getSubtaskById(s1.getId());

        manager.deleteTaskById(t1.getId());
        assertFalse(manager.getHistory().stream().anyMatch(t -> t.getId() == t1.getId()));

        manager.deleteEpicById(e1.getId());
        assertFalse(manager.getHistory().stream().anyMatch(t -> t.getId() == e1.getId()));

        manager.deleteSubtaskById(s1.getId());
        assertFalse(manager.getHistory().stream().anyMatch(t -> t.getId() == s1.getId()));
    }

    @Test
    @DisplayName("Обновление обычной задачи")
    void shouldUpdateTask() {
        Task task = new Task("Initial", "Desc", Status.NEW,
                LocalDateTime.now(), Duration.ofMinutes(15));
        manager.createTask(task);

        task.setName("Updated");
        task.setStatus(Status.IN_PROGRESS);
        manager.updateTask(task);

        Task updated = manager.getTaskById(task.getId());
        assertEquals("Updated", updated.getName());
        assertEquals(Status.IN_PROGRESS, updated.getStatus());
    }

    @Test
    @DisplayName("Обновление эпика")
    void shouldUpdateEpic() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);

        epic.setName("Updated Epic");
        manager.updateEpic(epic);

        Epic updated = manager.getEpicById(epic.getId());
        assertEquals("Updated Epic", updated.getName());
    }

    @Test
    @DisplayName("Обновление подзадачи")
    void shouldUpdateSubtask() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);
        Subtask subtask = new Subtask("Sub", "Desc", Status.NEW, epic.getId(),
                LocalDateTime.now(), Duration.ofMinutes(20));
        manager.createSubtask(subtask);

        subtask.setStatus(Status.DONE);
        manager.updateSubtask(subtask);

        Subtask updated = manager.getSubtaskById(subtask.getId());
        assertEquals(Status.DONE, updated.getStatus());
    }

    @Test
    @DisplayName("Очистка всех задач")
    void shouldClearAllTasks() {
        Task task = new Task("T", "Desc", Status.NEW, LocalDateTime.now(), Duration.ofMinutes(15));
        manager.createTask(task);
        manager.clearTasks();
        assertTrue(manager.getAllTasks().isEmpty());
    }

    @Test
    @DisplayName("Очистка всех эпиков и подзадач")
    void shouldClearAllEpicsAndSubtasks() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);
        Subtask sub = new Subtask("Sub", "Desc", Status.NEW, epic.getId(),
                LocalDateTime.now(), Duration.ofMinutes(20));
        manager.createSubtask(sub);

        manager.clearEpics();
        assertTrue(manager.getAllEpics().isEmpty());
        assertTrue(manager.getAllSubtasks().isEmpty());
    }

    @Test
    @DisplayName("Очистка подзадач не удаляет эпики")
    void shouldClearOnlySubtasks() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);
        Subtask sub = new Subtask("Sub", "Desc", Status.NEW, epic.getId(),
                LocalDateTime.now(), Duration.ofMinutes(20));
        manager.createSubtask(sub);

        manager.clearSubtasks();
        assertTrue(manager.getAllSubtasks().isEmpty());
        assertFalse(manager.getAllEpics().isEmpty());
    }

    @Test
    @DisplayName("Получение подзадач по id эпика")
    void shouldReturnSubtasksByEpicId() {
        Epic epic = new Epic("Epic", "Desc");
        manager.createEpic(epic);
        Subtask s1 = new Subtask("S1", "Desc", Status.NEW, epic.getId(),
                LocalDateTime.now(), Duration.ofMinutes(15));
        Subtask s2 = new Subtask("S2", "Desc", Status.NEW, epic.getId(),
                LocalDateTime.now().plusMinutes(20), Duration.ofMinutes(10));
        manager.createSubtask(s1);
        manager.createSubtask(s2);

        List<Subtask> subtasks = manager.getSubtasksByEpicId(epic.getId());
        assertEquals(2, subtasks.size());
        assertTrue(subtasks.contains(s1));
        assertTrue(subtasks.contains(s2));
    }

}
