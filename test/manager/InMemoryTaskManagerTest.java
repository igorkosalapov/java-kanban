package manager;

import manager.task.InMemoryTaskManager;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Тесты InMemoryTaskManager")
class InMemoryTaskManagerTest extends TaskManagerTest<InMemoryTaskManager> {
    @Override
    protected InMemoryTaskManager createManager() {
        return new InMemoryTaskManager();
    }
}
