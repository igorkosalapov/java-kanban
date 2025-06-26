package manager;

import manager.history.HistoryManager;
import manager.history.InMemoryHistoryManager;
import manager.task.InMemoryTaskManager;
import manager.task.TaskManager;

public class Managers {
    private Managers() {
    }

    public static TaskManager getDefault() {
        return new InMemoryTaskManager();
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}
