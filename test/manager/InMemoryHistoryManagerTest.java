package manager;

import manager.history.HistoryManager;
import manager.task.TaskManager;
import model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryHistoryManagerTest {
    private final HistoryManager history = Managers.getDefaultHistory();

    @Test
    void shouldRemoveTasksFromDifferentPositions() {
        Task task1 = new Task("Задача 1", "Описание", Status.NEW);
        Task task2 = new Task("Задача 2", "Описание", Status.NEW);
        Task task3 = new Task("Задача 3", "Описание", Status.NEW);

        task1.setId(1);
        task2.setId(2);
        task3.setId(3);

        history.add(task1);
        history.add(task2);
        history.add(task3);

        history.remove(1);
        history.remove(2);
        history.remove(3);

        assertTrue(history.getHistory().isEmpty(), "История должна быть пуста после удаления всех задач");
    }

    @Test
    void shouldAddTasksToHistory() {
        Task task = new Task("Задача", "Описание", Status.NEW);
        task.setId(1);
        history.add(task);

        Assertions.assertFalse(history.getHistory().isEmpty());
        assertEquals(task, history.getHistory().getFirst());
    }

    @Test
    void shouldRemoveTaskFromHistory() {
        Task task1 = new Task("Задача 1", "Описание 1", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание 2", Status.NEW);
        task2.setId(2);

        history.add(task1);
        history.add(task2);

        history.remove(1);

        List<Task> historyList = history.getHistory();

        assertEquals(1, historyList.size(), "После удаления в истории должна остаться одна задача");
        assertEquals(task2, historyList.getFirst(), "Оставшаяся задача — task2");
    }

    @Test
    void shouldMoveTaskToEnd() {
        Task task1 = new Task("Задача 1", "Описание 1", Status.NEW);
        Task task2 = new Task("Задача 2", "Описание 2", Status.NEW);
        Task task3 = new Task("Задача 3", "Описание 3", Status.NEW);

        task1.setId(1);
        task2.setId(2);
        task3.setId(3);

        history.add(task1);
        history.add(task2);
        history.add(task3);
        history.add(task2);

        List<Task> expected = List.of(task1, task3, task2);
        List<Task> actual = history.getHistory();

        assertEquals(expected, actual, "Повторное добавление должно переместить задачу в конец истории");
    }

    @Test
    void shouldNotThrowWhenRemovingNonExistentTask() {
        Task task = new Task("Задача", "Описание", Status.NEW);
        task.setId(1);
        history.add(task);

        assertDoesNotThrow(() -> history.remove(999), "Удаление несуществующей задачи не должно" +
                " выбрасывать исключения");

        List<Task> historyList = history.getHistory();
        assertEquals(1, historyList.size(), "История не должна измениться после удаления" +
                " несуществующей задачи");
        assertEquals(task, historyList.getFirst());
    }

    @Test
    void historyReflectsLatestTaskState() {

        Task task = new Task("Исходное имя", "Описание", Status.NEW);
        task.setId(1);
        history.add(task);

        task.setName("Обновлённое имя");

        List<Task> historyList = history.getHistory();
        Task fromHistory = historyList.getFirst();

        assertEquals("Обновлённое имя", fromHistory.getName(),
                "История отражает текущее состояние задачи (используется ссылка)");
    }

    @Test
    void shouldMaintainCorrectOrderInHistory() {
        Task task1 = new Task("Задача 1", "Описание 1", Status.NEW);
        Task task2 = new Task("Задача 2", "Описание 2", Status.NEW);
        Task task3 = new Task("Задача 3", "Описание 3", Status.NEW);

        task1.setId(1);
        task2.setId(2);
        task3.setId(3);

        history.add(task1);
        history.add(task2);
        history.add(task3);

        List<Task> expected = List.of(task1, task2, task3);
        List<Task> actual = history.getHistory();

        assertEquals(expected, actual, "История должна сохранять порядок добавления задач");
    }

    @Test
    void allTaskTypesShouldReflectLatestChanges() {
        TaskManager manager = Managers.getDefault();

        Task task = new Task("Задача", "Описание задачи", Status.NEW);
        Epic epic = new Epic("Эпик", "Описание эпика");
        Subtask subtask = new Subtask("Подзадача", "Описание подзадачи", Status.NEW, 2);

        manager.createTask(task);
        manager.createEpic(epic);
        manager.createSubtask(subtask);

        history.add(task);
        history.add(epic);
        history.add(subtask);

        task.setName("Измененная задача");
        epic.setName("Измененный эпик");
        subtask.setName("Измененная подзадача");

        assertEquals("Измененная задача", manager.getTaskById(task.getId()).getName(),
                "Менеджер должен отражать последнее состояние задачи");

        assertEquals("Измененный эпик", manager.getEpicById(epic.getId()).getName(),
                "Менеджер должен отражать последнее состояние эпика");

        assertEquals("Измененная подзадача", manager.getSubtaskById(subtask.getId()).getName(),
                "Менеджер должен отражать последнее состояние подзадачи");

        List<Task> historyList = history.getHistory();
        assertEquals("Измененная задача", historyList.get(0).getName(),
                "История должна отражать последнее состояние задачи");
        assertEquals("Измененный эпик", historyList.get(1).getName(),
                "История должна отражать последнее состояние эпика");
        assertEquals("Измененная подзадача", historyList.get(2).getName(),
                "История должна отражать последнее состояние подзадачи");
    }

    @Test
    void shouldNotContainDuplicatesInHistory() {
        Task task = new Task("Задача", "Описание", Status.NEW);
        task.setId(1);
        history.add(task);
        history.add(task);

        List<Task> list = history.getHistory();
        assertEquals(1, list.size(), "История не должна содержать дубликаты");
        assertEquals(task, list.getFirst(), "Задача должна быть в истории один раз");
    }

    @Test
    void shouldRemoveTaskFromHistoryById() {
        Task task1 = new Task("Задача 1", "Описание 1", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание 2", Status.NEW);
        task2.setId(2);

        history.add(task1);
        history.add(task2);
        history.remove(1);

        List<Task> list = history.getHistory();
        assertEquals(1, list.size());
        assertEquals(task2, list.getFirst());
    }
}