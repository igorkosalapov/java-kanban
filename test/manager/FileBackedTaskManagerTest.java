package manager;

import manager.task.FileBackedTaskManager;
import model.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты FileBackedTaskManager")
class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {

    private File tempFile;

    @BeforeEach
    void setUpFile() throws IOException {
        tempFile = File.createTempFile("test_tasks", ".csv");
        manager = createManager();
    }

    @AfterEach
    void cleanup() {
        if (!tempFile.delete()) {
            System.err.println("Не удалось удалить временный файл: " + tempFile.getAbsolutePath());
        }
    }

    @Override
    protected FileBackedTaskManager createManager() {
        return new FileBackedTaskManager(tempFile);
    }

    @Test
    @DisplayName("Сохранение и загрузка пустого файла")
    void shouldSaveAndLoadEmptyFile() {
        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertTrue(loaded.getAllTasks().isEmpty());
        assertTrue(loaded.getAllEpics().isEmpty());
        assertTrue(loaded.getAllSubtasks().isEmpty());
        assertTrue(loaded.getHistory().isEmpty());
    }

    @Test
    @DisplayName("Сохранение и загрузка задач")
    void shouldSaveAndLoadTasksCorrectly() {
        Task task = new Task("Test Task", "Description", Status.NEW,
                LocalDateTime.of(2025, 1, 1, 10, 0), Duration.ofMinutes(30));
        manager.createTask(task);

        Epic epic = new Epic("Epic", "Epic Desc");
        manager.createEpic(epic);

        Subtask sub = new Subtask("Sub", "Sub Desc", Status.DONE, epic.getId(),
                LocalDateTime.of(2025, 1, 1, 11, 0), Duration.ofMinutes(45));
        manager.createSubtask(sub);

        manager.getTaskById(task.getId());
        manager.getSubtaskById(sub.getId());

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertEquals(1, loaded.getAllTasks().size());
        assertEquals(1, loaded.getAllEpics().size());
        assertEquals(1, loaded.getAllSubtasks().size());

        assertEquals(task.getName(), loaded.getAllTasks().getFirst().getName());
        assertEquals(epic.getDescription(), loaded.getAllEpics().getFirst().getDescription());
        assertEquals(sub.getStatus(), loaded.getAllSubtasks().getFirst().getStatus());
        assertEquals(epic.getId(), loaded.getAllSubtasks().getFirst().getEpicId());

        List<Task> history = loaded.getHistory();
        assertEquals(2, history.size());
        assertEquals(task.getId(), history.get(0).getId());
        assertEquals(sub.getId(), history.get(1).getId());
    }

    @Test
    @DisplayName("Восстановление временных полей эпика из подзадач")
    void shouldRestoreEpicTimeFieldsFromSubtasks() {
        Epic epic = new Epic("Epic with Time", "Time test");
        manager.createEpic(epic);

        Subtask sub1 = new Subtask("Sub1", "First", Status.NEW, epic.getId(),
                LocalDateTime.of(2023, 1, 1, 10, 0), Duration.ofMinutes(60));
        Subtask sub2 = new Subtask("Sub2", "Second", Status.NEW, epic.getId(),
                LocalDateTime.of(2023, 1, 1, 11, 30), Duration.ofMinutes(90));

        System.out.println("Sub1 start: " + sub1.getStartTime());
        System.out.println("Sub2 start: " + sub2.getStartTime());

        manager.createSubtask(sub1);
        manager.createSubtask(sub2);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);
        Epic loadedEpic = loaded.getEpicById(epic.getId());

        System.out.println("Epic start: " + loadedEpic.getStartTime());

        assertEquals(LocalDateTime.of(2023, 1, 1, 10, 0),
                loadedEpic.getStartTime(),
                "Начало эпика должно совпадать с началом самой ранней подзадачи");

        assertEquals(LocalDateTime.of(2023, 1, 1, 13, 0),
                loadedEpic.getEndTime(),
                "Окончание эпика должно совпадать с окончанием самой поздней подзадачи");

        assertEquals(Duration.ofMinutes(150), loadedEpic.getDuration(),
                "Продолжительность эпика должна быть суммой продолжительностей подзадач");
    }

    @Test
    @DisplayName("Исключение при загрузке некорректной строки CSV")
    void shouldThrowWhenLoadingBrokenLine() throws IOException {
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("id,type,name,status,description,epic,startTime,duration,endTime\n");
            writer.write("1,TASK,BrokenLine,NEW,desc,XXX,???,abc,notadate\n");
        }

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> FileBackedTaskManager.loadFromFile(tempFile));

        assertTrue(exception.getMessage().toLowerCase().contains("ошибка"),
                "Ожидалось сообщение об ошибке парсинга");
    }

    @Test
    @DisplayName("Восстановление истории после загрузки")
    void shouldRestoreHistoryCorrectlyAfterLoad() {
        Task task = new Task("T", "desc", Status.NEW,
                LocalDateTime.of(2025, 5, 5, 10, 0), Duration.ofMinutes(30));
        Epic epic = new Epic("Epic", "epic desc");
        manager.createTask(task);
        manager.createEpic(epic);

        Subtask sub = new Subtask("Sub", "desc", Status.NEW, epic.getId(),
                LocalDateTime.of(2025, 5, 5, 12, 0), Duration.ofMinutes(15));
        manager.createSubtask(sub);

        manager.getTaskById(task.getId());
        manager.getEpicById(epic.getId());
        manager.getSubtaskById(sub.getId());

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        List<Task> history = loaded.getHistory();
        assertEquals(3, history.size());
        assertEquals(task.getId(), history.get(0).getId());
        assertEquals(epic.getId(), history.get(1).getId());
        assertEquals(sub.getId(), history.get(2).getId());
    }

    @Test
    @DisplayName("Восстановление упорядоченных задач после загрузки")
    void shouldRestorePrioritizedTasksCorrectly() {
        Task task1 = new Task("T1", "desc", Status.NEW,
                LocalDateTime.of(2025, 5, 5, 9, 0), Duration.ofMinutes(30));
        Task task2 = new Task("T2", "desc", Status.NEW,
                LocalDateTime.of(2025, 5, 5, 8, 0), Duration.ofMinutes(30));
        manager.createTask(task1);
        manager.createTask(task2);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);
        List<Task> prioritized = loaded.getPrioritizedTasks();

        assertEquals(2, prioritized.size());
        assertEquals(task2.getName(), prioritized.get(0).getName());
        assertEquals(task1.getName(), prioritized.get(1).getName());
    }

    @Test
    @DisplayName("Назначение уникальных id после загрузки")
    void shouldAssignUniqueIdsAfterLoad() {
        Task t1 = new Task("T", "desc", Status.NEW,
                LocalDateTime.of(2025, 1, 1, 10, 0), Duration.ofMinutes(20));
        manager.createTask(t1);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        Task t2 = new Task("New", "desc", Status.NEW,
                LocalDateTime.of(2025, 1, 1, 12, 0), Duration.ofMinutes(20));
        loaded.createTask(t2);

        assertNotEquals(t1.getId(), t2.getId());
        assertTrue(t2.getId() > t1.getId());
    }
}
