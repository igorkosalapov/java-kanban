package manager.task;


import model.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = file;
    }

    public static FileBackedTaskManager loadFromFile(File file) {
        FileBackedTaskManager manager = new FileBackedTaskManager(file);
        List<String> lines;

        try {
            lines = Files.readAllLines(file.toPath());
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка чтения файла", e);
        }

        if (lines.isEmpty()) return manager;

        int maxId = 0;
        int i = 1;
        for (; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) break;

            Task task = CSVFormatter.fromString(line);

            int id = task.getId();
            maxId = Math.max(maxId, id);

            if (task instanceof Epic epic) {
                manager.epics.put(id, epic);
            } else if (task instanceof Subtask subtask) {
                manager.subtasks.put(id, subtask);
            } else {
                manager.tasks.put(id, task);
            }
        }

        manager.nextId = maxId + 1;

        if (i + 1 < lines.size()) {
            String historyLine = lines.get(i + 1);
            List<Integer> historyIds = CSVFormatter.historyFromString(historyLine);

            for (int id : historyIds) {
                Task task = manager.tasks.get(id);
                if (task == null) task = manager.epics.get(id);
                if (task == null) task = manager.subtasks.get(id);
                if (task != null) {
                    manager.historyManager.add(task);
                }
            }
        }

        for (Subtask subtask : manager.subtasks.values()) {
            Epic epic = manager.epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.addSubtaskId(subtask.getId());
            }
        }

        return manager;
    }

    @Override
    public void createTask(Task task) {
        super.createTask(task);
        save();
    }

    @Override
    public void createSubtask(Subtask subtask) {
        super.createSubtask(subtask);
        save();
    }

    @Override
    public void createEpic(Epic epic) {
        super.createEpic(epic);
        save();
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void deleteTaskById(int id) {
        super.deleteTaskById(id);
        save();
    }

    @Override
    public void deleteSubtaskById(int id) {
        super.deleteSubtaskById(id);
        save();
    }

    @Override
    public void deleteEpicById(int id) {
        super.deleteEpicById(id);
        save();
    }

    @Override
    public void clearTasks() {
        super.clearTasks();
        save();
    }

    @Override
    public void clearSubtasks() {
        super.clearSubtasks();
        save();
    }

    @Override
    public void clearEpics() {
        super.clearEpics();
        save();
    }

    @Override
    public Task getTaskById(int id) {
        Task task = super.getTaskById(id);
        save();
        return task;
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = super.getSubtaskById(id);
        save();
        return subtask;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = super.getEpicById(id);
        save();
        return epic;
    }

    private void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(CSVFormatter.getHeader());
            writer.newLine();

            for (Task task : getAllTasks()) {
                writer.write(CSVFormatter.toString(task));
                writer.newLine();
            }
            for (Epic epic : getAllEpics()) {
                writer.write(CSVFormatter.toString(epic));
                writer.newLine();
            }
            for (Subtask subtask : getAllSubtasks()) {
                writer.write(CSVFormatter.toString(subtask));
                writer.newLine();
            }

            writer.newLine();
            writer.write(CSVFormatter.toString(historyManager));
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка сохранения файла", e);
        }
    }
}
