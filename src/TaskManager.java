import java.util.List;

public interface TaskManager {

    Task createTask(Task task) throws ValidationException;

    Task getTaskById(int id) throws NotFoundException;

    List<Task> getAllTasks();

    void updateTask(Task task) throws NotFoundException, ValidationException;

    void deleteTaskById(int id) throws NotFoundException;

    void deleteAllTasks();

    Epic createEpic(Epic epic) throws ValidationException;

    Epic getEpicById(int id) throws NotFoundException;

    List<Epic> getAllEpics();

    void updateEpic(Epic epic) throws NotFoundException, ValidationException;

    void deleteEpicById(int id) throws NotFoundException;

    void deleteAllEpics();

    Subtask createSubtask(Subtask subtask) throws NotFoundException, ValidationException;

    Subtask getSubtaskById(int id) throws NotFoundException;

    List<Subtask> getAllSubtasks();

    void updateSubtask(Subtask subtask) throws NotFoundException, ValidationException;

    void deleteSubtaskById(int id) throws NotFoundException;

    void deleteAllSubtasks();


    List<Subtask> getSubtasksByEpicId(int epicId) throws NotFoundException;

    List<Task> getHistory();

    void saveToFile(String filename);
}