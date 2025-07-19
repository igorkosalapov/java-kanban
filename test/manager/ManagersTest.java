package manager;

import manager.history.HistoryManager;
import manager.task.TaskManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class ManagersTest {
    @Test
    @DisplayName("Инициализация менеджеров")
    void shouldReturnInitializedManagers() {
        TaskManager taskManager = Managers.getDefault();
        HistoryManager historyManager = Managers.getDefaultHistory();

        assertNotNull(taskManager, "Менеджер задач должен быть проинициализирован.");
        assertNotNull(historyManager, "Менеджер истории должен быть проинициализирован.");
    }
}