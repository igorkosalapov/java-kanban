package manager.task;

import manager.Managers;
import manager.history.HistoryManager;
import model.*;

import java.time.LocalDateTime;
import java.util.*;

public class InMemoryTaskManager implements TaskManager {
    protected int nextId = 1;

    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, Subtask> subtasks = new HashMap<>();
    protected final HistoryManager historyManager = Managers.getDefaultHistory();

    protected final TreeSet<Task> prioritizedTasks = new TreeSet<>(
            Comparator.comparing(Task::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Task::getId));

    @Override
    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritizedTasks);
    }

    private boolean hasTimeIntersection(Task newTask) {
        if (newTask.getStartTime() == null || newTask.getDuration() == null) return false;

        LocalDateTime newStart = newTask.getStartTime();
        LocalDateTime newEnd = newTask.getEndTime();

        return prioritizedTasks.stream()
                .filter(existing -> existing.getId() != newTask.getId())
                .anyMatch(existing -> {
                    if (existing.getStartTime() == null || existing.getDuration() == null) {
                        return false;
                    }
                    LocalDateTime existingStart = existing.getStartTime();
                    LocalDateTime existingEnd = existing.getEndTime();
                    return !(newEnd.isBefore(existingStart) || newStart.isAfter(existingEnd));
                });
    }

    private int generateId() {
        return nextId++;
    }

    @Override
    public void createTask(Task task) {
        task.setId(generateId());
        if (hasTimeIntersection(task)) {
            throw new IllegalArgumentException("Пересечение задач по времени");
        }
        tasks.put(task.getId(), task);
        prioritizedTasks.add(task);
    }

    @Override
    public void createEpic(Epic epic) {
        epic.setId(generateId());
        epics.put(epic.getId(), epic);
    }

    @Override
    public void createSubtask(Subtask subtask) {
        Epic epic = epics.get(subtask.getEpicId());
        if (epic == null) {
            throw new IllegalArgumentException("Эпик для подзадачи не найден");
        }
        subtask.setId(generateId());
        if (hasTimeIntersection(subtask)) {
            throw new IllegalArgumentException("Пересечение задач по времени");
        }
        subtasks.put(subtask.getId(), subtask);
        epic.addSubtaskId(subtask.getId());
        updateEpic(epic);
        prioritizedTasks.add(subtask);
    }

    @Override
    public void updateTask(Task task) {
        if (!tasks.containsKey(task.getId())) {
            return;
        }
        Task oldTask = tasks.get(task.getId());
        prioritizedTasks.remove(oldTask);

        if (hasTimeIntersection(task)) {
            prioritizedTasks.add(oldTask);
            throw new IllegalArgumentException("Пересечение задач по времени");
        }

        tasks.put(task.getId(), task);
        prioritizedTasks.add(task);
    }

    @Override
    public void updateEpic(Epic epic) {
        Epic stored = epics.get(epic.getId());
        if (stored == null) {
            return;
        }

        stored.setName(epic.getName());
        stored.setDescription(epic.getDescription());

        List<Subtask> subtaskList = getSubtasksByEpicId(stored.getId());

        if (subtaskList.isEmpty()) {
            stored.setStatus(Status.NEW);
        } else {
            boolean allNew = true;
            boolean allDone = true;

            for (Subtask subtask : subtaskList) {
                Status status = subtask.getStatus();
                if (status != Status.NEW) {
                    allNew = false;
                }
                if (status != Status.DONE) {
                    allDone = false;
                }
            }

            if (allDone) {
                stored.setStatus(Status.DONE);
            } else if (allNew) {
                stored.setStatus(Status.NEW);
            } else {
                stored.setStatus(Status.IN_PROGRESS);
            }
        }

        stored.updateTimeFields(subtaskList);

        epics.put(stored.getId(), stored);
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (!subtasks.containsKey(subtask.getId())) {
            return;
        }
        Subtask oldSubtask = subtasks.get(subtask.getId());
        prioritizedTasks.remove(oldSubtask);

        if (hasTimeIntersection(subtask)) {
            prioritizedTasks.add(oldSubtask);
            throw new IllegalArgumentException("Пересечение задач по времени");
        }

        subtasks.put(subtask.getId(), subtask);
        prioritizedTasks.add(subtask);

        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null) {
            updateEpic(epic);
        }
    }

    @Override
    public void deleteTaskById(int id) {
        Task task = tasks.remove(id);
        if (task != null) {
            prioritizedTasks.remove(task);
            historyManager.remove(id);
        }
    }

    @Override
    public void deleteEpicById(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            for (int subtaskId : epic.getSubtaskIds()) {
                Subtask subtask = subtasks.remove(subtaskId);
                if (subtask != null) {
                    prioritizedTasks.remove(subtask);
                    historyManager.remove(subtaskId);
                }
            }
            historyManager.remove(id);
        }
    }

    @Override
    public void deleteSubtaskById(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            prioritizedTasks.remove(subtask);
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtaskId(id);
                updateEpic(epic);
            }
            historyManager.remove(id);
        }
    }

    @Override
    public void clearTasks() {
        for (Task task : tasks.values()) {
            prioritizedTasks.remove(task);
            historyManager.remove(task.getId());
        }
        tasks.clear();
    }

    @Override
    public void clearEpics() {
        for (Epic epic : epics.values()) {
            for (int subtaskId : epic.getSubtaskIds()) {
                Subtask subtask = subtasks.remove(subtaskId);
                if (subtask != null) {
                    prioritizedTasks.remove(subtask);
                    historyManager.remove(subtaskId);
                }
            }
            historyManager.remove(epic.getId());
        }
        epics.clear();
    }

    @Override
    public void clearSubtasks() {
        for (Subtask subtask : subtasks.values()) {
            prioritizedTasks.remove(subtask);
            historyManager.remove(subtask.getId());
        }
        subtasks.clear();

        for (Epic epic : epics.values()) {
            epic.getSubtaskIds().clear();
            updateEpic(epic);
        }
    }

    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
        }
        return task;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
        }
        return subtask;
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public List<Subtask> getSubtasksByEpicId(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) {
            return Collections.emptyList();
        }

        List<Subtask> result = new ArrayList<>();
        for (int subtaskId : epic.getSubtaskIds()) {
            Subtask subtask = subtasks.get(subtaskId);
            if (subtask != null) {
                result.add(subtask);
            }
        }
        return result;
    }
}
