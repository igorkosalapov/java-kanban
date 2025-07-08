package manager;

import manager.history.HistoryManager;
import manager.history.InMemoryHistoryManager;
import manager.task.FileBackedTaskManager;
import manager.task.InMemoryTaskManager;
import manager.task.TaskManager;

import java.io.File;

public class Managers {
    private Managers() {
    }

    public static TaskManager getDefault() {
        return new FileBackedTaskManager(new File("resources/data.csv"));
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}
