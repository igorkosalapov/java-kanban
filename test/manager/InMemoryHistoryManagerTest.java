package manager;

import manager.history.HistoryManager;
import manager.task.TaskManager;
import model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryHistoryManagerTest {
    private final HistoryManager history = Managers.getDefaultHistory();

    @Test
    void shouldAddTasksToHistory() {
        Task task = new Task("Task", "Desc", Status.NEW);
        task.setId(1);
        history.add(task);

        Assertions.assertFalse(history.getHistory().isEmpty());
        assertEquals(task, history.getHistory().getFirst());
    }

    @Test
    void shouldNotExceedHistoryLimit() {
        for (int i = 1; i <= 15; i++) {
            Task task = new Task("Task " + i, "Desc", Status.NEW);
            task.setId(i);
            history.add(task);
        }

        assertEquals(10, history.getHistory().size());
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
}