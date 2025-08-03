package manager.task;

import manager.Managers;
import manager.history.HistoryManager;
import model.Epic;
import model.Subtask;
import model.Task;
import model.Status;
import manager.task.exception.NotFoundException;
import manager.task.exception.IntersectionException;

import java.time.Duration;
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

    private void updateEpicTimeFields(Epic epic) {
        List<Subtask> subs = getSubtasksByEpicId(epic.getId());
        if (subs.isEmpty()) {
            epic.setStartTime(null);
            epic.setDuration(null);
            epic.setEndTime(null);
            return;
        }
        LocalDateTime earliest = subs.getFirst().getStartTime();
        LocalDateTime latest = subs.getFirst().getEndTime();
        Duration total = Duration.ZERO;
        for (Subtask st : subs) {
            if (st.getStartTime() != null && st.getDuration() != null) {
                LocalDateTime s = st.getStartTime();
                LocalDateTime e = st.getEndTime();
                if (earliest == null || s.isBefore(earliest)) earliest = s;
                if (latest == null || e.isAfter(latest)) latest = e;
                total = total.plus(st.getDuration());
            }
        }
        epic.setStartTime(earliest);
        epic.setDuration(total);
        epic.setEndTime(latest);
    }

    private int generateId() {
        return nextId++;
    }

    @Override
    public void createTask(Task task) {
        task.setId(generateId());
        if (hasTimeIntersection(task)) {
            throw new IntersectionException();
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
            throw new NotFoundException("Эпик с id " + subtask.getEpicId() + " не найден");
        }
        subtask.setId(generateId());
        if (hasTimeIntersection(subtask)) {
            throw new IntersectionException();
        }
        subtasks.put(subtask.getId(), subtask);
        epic.addSubtaskId(subtask.getId());
        updateEpic(epic);
        prioritizedTasks.add(subtask);
    }

    @Override
    public void updateTask(Task task) {
        if (!tasks.containsKey(task.getId())) {
            throw new NotFoundException("Задача с id " + task.getId() + " не найдена");
        }
        Task old = tasks.get(task.getId());
        prioritizedTasks.remove(old);
        if (hasTimeIntersection(task)) {
            prioritizedTasks.add(old);
            throw new IntersectionException();
        }
        tasks.put(task.getId(), task);
        prioritizedTasks.add(task);
    }

    @Override
    public void updateEpic(Epic epic) {
        Epic stored = epics.get(epic.getId());
        if (stored == null) {
            throw new NotFoundException("Эпик с id " + epic.getId() + " не найден");
        }
        stored.setName(epic.getName());
        stored.setDescription(epic.getDescription());
        List<Subtask> subs = getSubtasksByEpicId(stored.getId());
        if (subs.isEmpty()) {
            stored.setStatus(Status.NEW);
        } else {
            boolean allNew = subs.stream().allMatch(st -> st.getStatus() == Status.NEW);
            boolean allDone = subs.stream().allMatch(st -> st.getStatus() == Status.DONE);
            stored.setStatus(allDone ? Status.DONE : allNew ? Status.NEW : Status.IN_PROGRESS);
        }
        updateEpicTimeFields(stored);
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (!subtasks.containsKey(subtask.getId())) {
            throw new NotFoundException("Подзадача с id " + subtask.getId() + " не найдена");
        }
        Subtask old = subtasks.get(subtask.getId());
        prioritizedTasks.remove(old);
        if (hasTimeIntersection(subtask)) {
            prioritizedTasks.add(old);
            throw new IntersectionException();
        }
        subtasks.put(subtask.getId(), subtask);
        prioritizedTasks.add(subtask);
        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null) updateEpic(epic);
    }

    @Override
    public void deleteTaskById(int id) {
        Task removed = tasks.remove(id);
        if (removed == null) {
            throw new NotFoundException("Задача с id " + id + " не найдена");
        }
        prioritizedTasks.remove(removed);
        historyManager.remove(id);
    }

    @Override
    public void deleteEpicById(int id) {
        Epic removed = epics.remove(id);
        if (removed == null) {
            throw new NotFoundException("Эпик с id " + id + " не найден");
        }
        for (int sid : removed.getSubtaskIds()) {
            Subtask st = subtasks.remove(sid);
            if (st != null) {
                prioritizedTasks.remove(st);
                historyManager.remove(sid);
            }
        }
        historyManager.remove(id);
    }

    @Override
    public void deleteSubtaskById(int id) {
        Subtask removed = subtasks.remove(id);
        if (removed == null) {
            throw new NotFoundException("Подзадача с id " + id + " не найдена");
        }
        prioritizedTasks.remove(removed);
        Epic epic = epics.get(removed.getEpicId());
        if (epic != null) {
            epic.removeSubtaskId(id);
            updateEpic(epic);
        }
        historyManager.remove(id);
    }

    @Override
    public void clearTasks() {
        tasks.keySet().forEach(historyManager::remove);
        prioritizedTasks.clear();
        tasks.clear();
    }

    @Override
    public void clearEpics() {
        epics.values().forEach(ep -> ep.getSubtaskIds().forEach(historyManager::remove));
        subtasks.keySet().forEach(historyManager::remove);
        prioritizedTasks.removeIf(t -> t instanceof Subtask);
        epics.clear();
        subtasks.clear();
    }

    @Override
    public void clearSubtasks() {
        subtasks.keySet().forEach(historyManager::remove);
        prioritizedTasks.removeIf(t -> t instanceof Subtask);
        subtasks.clear();
        epics.values().forEach(ep -> { ep.getSubtaskIds().clear(); updateEpic(ep); });
    }

    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task == null) {
            throw new NotFoundException("Задача с id " + id + " не найдена");
        }
        historyManager.add(task);
        return task;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic == null) {
            throw new NotFoundException("Эпик с id " + id + " не найден");
        }
        historyManager.add(epic);
        return epic;
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask sub = subtasks.get(id);
        if (sub == null) {
            throw new NotFoundException("Подзадача с id " + id + " не найдена");
        }
        historyManager.add(sub);
        return sub;
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
            throw new NotFoundException("Эпик с id " + epicId + " не найден");
        }
        List<Subtask> result = new ArrayList<>();
        for (Integer sid : epic.getSubtaskIds()) {
            Subtask st = subtasks.get(sid);
            if (st != null) result.add(st);
        }
        return result;
    }
}
