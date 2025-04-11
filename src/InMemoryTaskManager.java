import java.io.*;
import java.util.*;

public class InMemoryTaskManager implements TaskManager {
    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final Map<Integer, Subtask> subtasks = new HashMap<>();
    private final InMemoryHistoryManager historyManager = new InMemoryHistoryManager();
    private int nextId = 1;


    @Override
    public Task createTask(Task task) throws ValidationException {
        if (task == null) {
            throw new ValidationException("Task cannot be null");
        }
        task.setId(nextId++);
        tasks.put(task.getId(), task);
        return task;
    }

    @Override
    public Task getTaskById(int id) throws NotFoundException {
        Task task = tasks.get(id);
        if (task == null) {
            throw new NotFoundException("Task with ID " + id + " not found");
        }
        historyManager.add(task);
        return task;
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public void updateTask(Task task) throws NotFoundException, ValidationException {
        if (task == null) {
            throw new ValidationException("Task cannot be null");
        }
        if (!tasks.containsKey(task.getId())) {
            throw new NotFoundException("Task with ID " + task.getId() + " not found");
        }
        tasks.put(task.getId(), task);
    }

    @Override
    public void deleteTaskById(int id) throws NotFoundException {
        if (!tasks.containsKey(id)) {
            throw new NotFoundException("Task with ID " + id + " not found");
        }
        tasks.remove(id);
    }

    @Override
    public void deleteAllTasks() {
        tasks.clear();
    }


    @Override
    public Epic createEpic(Epic epic) throws ValidationException {
        if (epic == null) {
            throw new ValidationException("Epic cannot be null");
        }
        epic.setId(nextId++);
        epics.put(epic.getId(), epic);
        tasks.put(epic.getId(), epic);
        return epic;
    }

    @Override
    public Epic getEpicById(int id) throws NotFoundException {
        Epic epic = epics.get(id);
        if (epic == null) {
            throw new NotFoundException("Epic with ID " + id + " not found");
        }
        historyManager.add(epic);
        return epic;
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public void updateEpic(Epic epic) throws NotFoundException, ValidationException {
        if (epic == null) {
            throw new ValidationException("Epic cannot be null");
        }
        if (!epics.containsKey(epic.getId())) {
            throw new NotFoundException("Epic with ID " + epic.getId() + " not found");
        }
        Epic savedEpic = epics.get(epic.getId());
        savedEpic.setName(epic.getName());
        savedEpic.setDescription(epic.getDescription());
    }

    @Override
    public void deleteEpicById(int id) throws NotFoundException {
        if (!epics.containsKey(id)) {
            throw new NotFoundException("Epic with ID " + id + " not found");
        }
        Epic epic = epics.remove(id);
        for (int subtaskId : epic.getSubtaskIds()) {
            subtasks.remove(subtaskId);
            tasks.remove(subtaskId);
        }
        tasks.remove(id);
    }

    @Override
    public void deleteAllEpics() {
        for (Epic epic : epics.values()) {
            for (int subtaskId : epic.getSubtaskIds()) {
                subtasks.remove(subtaskId);
                tasks.remove(subtaskId);
            }
        }
        epics.clear();
    }


    @Override
    public Subtask createSubtask(Subtask subtask) throws NotFoundException, ValidationException {
        if (subtask == null) {
            throw new ValidationException("Subtask cannot be null");
        }
        if (!epics.containsKey(subtask.getEpicId())) {
            throw new NotFoundException("Epic with ID " + subtask.getEpicId() + " not found");
        }
        subtask.setId(nextId++);
        subtasks.put(subtask.getId(), subtask);
        tasks.put(subtask.getId(), subtask);

        Epic epic = epics.get(subtask.getEpicId());
        epic.addSubtaskId(subtask.getId());
        updateEpicStatus(epic);

        return subtask;
    }

    @Override
    public Subtask getSubtaskById(int id) throws NotFoundException {
        Subtask subtask = subtasks.get(id);
        if (subtask == null) {
            throw new NotFoundException("Subtask with ID " + id + " not found");
        }
        historyManager.add(subtask);
        return subtask;
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public void updateSubtask(Subtask subtask) throws NotFoundException, ValidationException {
        if (subtask == null) {
            throw new ValidationException("Subtask cannot be null");
        }
        if (!subtasks.containsKey(subtask.getId())) {
            throw new NotFoundException("Subtask with ID " + subtask.getId() + " not found");
        }
        Subtask savedSubtask = subtasks.get(subtask.getId());
        savedSubtask.setName(subtask.getName());
        savedSubtask.setDescription(subtask.getDescription());
        savedSubtask.setStatus(subtask.getStatus());

        Epic epic = epics.get(savedSubtask.getEpicId());
        updateEpicStatus(epic);
    }

    @Override
    public void deleteSubtaskById(int id) throws NotFoundException {
        if (!subtasks.containsKey(id)) {
            throw new NotFoundException("Subtask with ID " + id + " not found");
        }
        Subtask subtask = subtasks.remove(id);
        tasks.remove(id);
        Epic epic = epics.get(subtask.getEpicId());
        epic.removeSubtaskId(id);
        updateEpicStatus(epic);
    }

    @Override
    public void deleteAllSubtasks() {
        for (Epic epic : epics.values()) {
            epic.clearSubtaskIds();
            updateEpicStatus(epic);
        }
        subtasks.clear();
    }


    @Override
    public List<Subtask> getSubtasksByEpicId(int epicId) throws NotFoundException {
        if (!epics.containsKey(epicId)) {
            throw new NotFoundException("Epic with ID " + epicId + " not found");
        }
        Epic epic = epics.get(epicId);
        List<Subtask> result = new ArrayList<>();
        for (int subtaskId : epic.getSubtaskIds()) {
            Subtask subtask = subtasks.get(subtaskId);
            if (subtask != null) {
                result.add(subtask);
            }
        }
        return result;
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public void saveToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {

            for (Task task : tasks.values()) {
                writer.write(taskToString(task));
                writer.newLine();
            }

            for (Epic epic : epics.values()) {
                writer.write(epicToString(epic));
                writer.newLine();
            }

            for (Subtask subtask : subtasks.values()) {
                writer.write(subtaskToString(subtask));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save to file: " + e.getMessage());
        }
    }

    private String taskToString(Task task) {
        return String.format("%s,%d,%s,%s,%s",
                task.getClass().getSimpleName(),
                task.getId(),
                escapeCommas(task.getName()),
                escapeCommas(task.getDescription()),
                task.getStatus());
    }

    private String epicToString(Epic epic) {
        return String.format("%s,%d,%s,%s,%s",
                epic.getClass().getSimpleName(),
                epic.getId(),
                escapeCommas(epic.getName()),
                escapeCommas(epic.getDescription()),
                epic.getStatus());
    }

    private String subtaskToString(Subtask subtask) {
        return String.format("%s,%d,%s,%s,%s,%d",
                subtask.getClass().getSimpleName(),
                subtask.getId(),
                escapeCommas(subtask.getName()),
                escapeCommas(subtask.getDescription()),
                subtask.getStatus(),
                subtask.getEpicId());
    }

    private String escapeCommas(String text) {
        return text.replace(",", "\\,");
    }

    private void updateEpicStatus(Epic epic) {
        List<Subtask> epicSubtasks = getSubtasksByEpicId(epic.getId());

        if (epicSubtasks.isEmpty()) {
            epic.setStatus(Status.NEW);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;

        for (Subtask subtask : epicSubtasks) {
            if (subtask.getStatus() != Status.NEW) {
                allNew = false;
            }
            if (subtask.getStatus() != Status.DONE) {
                allDone = false;
            }
        }

        if (allDone) {
            epic.setStatus(Status.DONE);
        } else if (allNew) {
            epic.setStatus(Status.NEW);
        } else {
            epic.setStatus(Status.IN_PROGRESS);
        }
    }
}