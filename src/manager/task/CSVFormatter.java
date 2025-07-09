package manager.task;

import manager.history.HistoryManager;
import model.*;

import java.util.*;
import java.util.stream.Collectors;

public class CSVFormatter {
    public static String getHeader() {
        return "id,type,name,status,description,epic";
    }

    public static String toString(Task task) {
        String epicId = "";
        if (task instanceof Subtask subtask) {
            epicId = String.valueOf(subtask.getEpicId());
        }

        return String.format("%d,%s,%s,%s,%s,%s",
                task.getId(),
                task.getType(),
                task.getName(),
                task.getStatus(),
                task.getDescription(),
                epicId);
    }

    public static String toString(HistoryManager historyManager) {
        List<Task> history = historyManager.getHistory();
        return history.stream()
                .map(task -> String.valueOf(task.getId()))
                .collect(Collectors.joining(","));
    }

    public static List<Integer> historyFromString(String line) {
        if (line == null || line.isBlank()) return Collections.emptyList();
        String[] ids = line.split(",");
        List<Integer> result = new ArrayList<>();
        for (String id : ids) {
            result.add(Integer.parseInt(id));
        }
        return result;
    }

    public static Task fromString(String value) {
        String[] fields = value.split(",", -1);

        if (fields.length < 5) {
            throw new IllegalArgumentException("Неверный формат CSV-строки: " + value);
        }

        int id = Integer.parseInt(fields[0]);
        TaskType type = TaskType.valueOf(fields[1]);
        String name = fields[2];
        Status status = Status.valueOf(fields[3]);
        String description = fields[4];

        return switch (type) {
            case TASK -> {
                Task task = new Task(name, description, status);
                task.setId(id);
                yield task;
            }
            case EPIC -> {
                Epic epic = new Epic(name, description);
                epic.setId(id);
                epic.setStatus(status);
                yield epic;
            }
            case SUBTASK -> {
                if (fields.length < 6 || fields[5].isBlank()) {
                    throw new IllegalArgumentException("SUBTASK требует указания epicId: " + value);
                }
                int epicId = Integer.parseInt(fields[5]);
                Subtask subtask = new Subtask(name, description, status, epicId);
                subtask.setId(id);
                yield subtask;
            }
        };
    }

}
