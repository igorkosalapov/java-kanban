package manager.task;

import manager.history.HistoryManager;
import model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CSVFormatter {
    public static String getHeader() {
        return "id,type,name,status,description,epic,startTime,duration";
    }

    public static String historyToString(HistoryManager historyManager) {
        return historyManager.getHistory().stream()
                .map(Task::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public static List<Integer> historyFromString(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] parts = value.split(",");
        return java.util.Arrays.stream(parts)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public static String toString(Task task) {
        String epicId = "";
        if (task instanceof Subtask subtask) {
            epicId = String.valueOf(subtask.getEpicId());
        }
        String startTime = task.getStartTime() == null ? "" : task.getStartTime().toString();
        String duration = task.getDuration() == null ? "" : String.valueOf(task.getDuration().toMinutes());

        return String.format("%d,%s,%s,%s,%s,%s,%s,%s",
                task.getId(),
                task.getType(),
                task.getName(),
                task.getStatus(),
                task.getDescription(),
                epicId,
                startTime,
                duration);
    }

    public static Task fromString(String value) {
        String[] fields = value.split(",", -1);

        if (fields.length < 8) {
            throw new IllegalArgumentException("Неверный формат CSV-строки: " + value);
        }

        int id = Integer.parseInt(fields[0]);
        TaskType type = TaskType.valueOf(fields[1]);
        String name = fields[2];
        Status status = Status.valueOf(fields[3]);
        String description = fields[4];
        String epicIdStr = fields[5];
        String startTimeStr = fields[6];
        String durationStr = fields[7];

        LocalDateTime startTime = startTimeStr.isBlank() ? null : LocalDateTime.parse(startTimeStr);
        Duration duration = durationStr.isBlank() ? null : Duration.ofMinutes(Long.parseLong(durationStr));

        return switch (type) {
            case TASK -> {
                Task task = new Task(name, description, status, startTime, duration);
                task.setId(id);
                yield task;
            }
            case EPIC -> {
                Epic epic = new Epic(name, description);
                epic.setId(id);
                epic.setStatus(status);
                epic.setStartTime(startTime);
                epic.setDuration(duration);
                yield epic;
            }
            case SUBTASK -> {
                if (epicIdStr.isBlank()) {
                    throw new IllegalArgumentException("SUBTASK требует указания epicId: " + value);
                }
                int epicId = Integer.parseInt(epicIdStr);
                Subtask subtask = new Subtask(name, description, status, epicId, startTime, duration);
                subtask.setId(id);
                subtask.setStartTime(startTime);
                subtask.setDuration(duration);
                yield subtask;
            }
        };
    }
}
