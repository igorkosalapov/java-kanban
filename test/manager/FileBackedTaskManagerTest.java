package manager;

import manager.task.FileBackedTaskManager;
import model.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest {

    private File tempFile;

    @BeforeEach
    void setup() throws IOException {
        tempFile = File.createTempFile("test_tasks", ".csv");
    }

    @AfterEach
    void cleanup() {
        if (!tempFile.delete()) {
            System.err.println("Не удалось удалить временный файл: " + tempFile.getAbsolutePath());
        }
    }

    @Test
    void shouldSaveAndLoadEmptyFile() {

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertTrue(loaded.getAllTasks().isEmpty(), "Список задач должен быть пуст");
        assertTrue(loaded.getAllEpics().isEmpty(), "Список эпиков должен быть пуст");
        assertTrue(loaded.getAllSubtasks().isEmpty(), "Список подзадач должен быть пуст");
        assertTrue(loaded.getHistory().isEmpty(), "История должна быть пустой");
    }

    @Test
    void shouldSaveAndLoadTasksCorrectly() {
        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile);

        Task task = new Task("Test Task", "Description", Status.NEW);
        Epic epic = new Epic("Epic", "Epic Desc");
        manager.createTask(task);
        manager.createEpic(epic);

        Subtask sub = new Subtask("Sub", "Sub Desc", Status.DONE, epic.getId());
        manager.createSubtask(sub);

        manager.getTaskById(task.getId());
        manager.getSubtaskById(sub.getId());

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertEquals(1, loaded.getAllTasks().size());
        assertEquals(1, loaded.getAllEpics().size());
        assertEquals(1, loaded.getAllSubtasks().size());

        Task loadedTask = loaded.getAllTasks().getFirst();
        assertEquals(task.getName(), loadedTask.getName());

        Epic loadedEpic = loaded.getAllEpics().getFirst();
        assertEquals(epic.getDescription(), loadedEpic.getDescription());

        Subtask loadedSub = loaded.getAllSubtasks().getFirst();
        assertEquals(sub.getStatus(), loadedSub.getStatus());
        assertEquals(epic.getId(), loadedSub.getEpicId());

        List<Task> history = loaded.getHistory();
        assertEquals(2, history.size());
        assertEquals(task.getId(), history.get(0).getId());
        assertEquals(sub.getId(), history.get(1).getId());
    }
}
