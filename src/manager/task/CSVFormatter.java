package manager.task;

import manager.history.HistoryManager;
import model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CSVFormatter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static String getHeader() {
        return "id,type,name,status,description,epic,startTime,duration,endTime";
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
        try {
            return Stream.of(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> parseIntField(s, "historyId"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка парсинга истории: '" + value + "'", e);
        }
    }

    public static String toString(Task task) {
        String epicId = "";
        if (task instanceof Subtask subtask) {
            epicId = String.valueOf(subtask.getEpicId());
        }

        String startTime = task.getStartTime() == null ? "" : task.getStartTime().format(DATE_TIME_FORMATTER);
        String duration = task.getDuration() == null ? "" : String.valueOf(task.getDuration().toMinutes());
        String endTime = task.getEndTime() == null ? "" : task.getEndTime().format(DATE_TIME_FORMATTER);

        return String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s",
                task.getId(),
                task.getType(),
                task.getName(),
                task.getStatus(),
                task.getDescription(),
                epicId,
                startTime,
                duration,
                endTime);
    }

    public static Task fromString(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Пустая строка");
            }

            String[] fields = value.split(",", -1);

            if (fields.length < 9) {
                throw new IllegalArgumentException(
                        String.format("Недостаточно полей (ожидалось 9, получили %d)", fields.length)
                );
            }


            int id = parseIntField(fields[0], "id");
            TaskType type = parseEnumField(fields[1], TaskType.class, "type");
            String name = fields[2].trim();
            Status status = parseEnumField(fields[3], Status.class, "status");
            String description = fields[4].trim();


            LocalDateTime startTime = parseDateTime(fields[6]);
            Duration duration = parseDuration(fields[7]);
            LocalDateTime endTime = parseDateTime(fields[8]);


            return switch (type) {
                case TASK -> {
                    Task task = new Task(name, description, status, startTime, duration);
                    task.setId(id);
                    task.setEndTime(endTime);
                    yield task;
                }
                case EPIC -> {
                    Epic epic = new Epic(name, description);
                    epic.setId(id);
                    epic.setStatus(status);
                    epic.setStartTime(startTime);
                    epic.setDuration(duration);
                    epic.setEndTime(endTime);
                    yield epic;
                }
                case SUBTASK -> {
                    int epicId = parseEpicId(fields[5]);
                    Subtask subtask = new Subtask(name, description, status, epicId, startTime, duration);
                    subtask.setId(id);
                    subtask.setEndTime(endTime);
                    yield subtask;
                }
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка парсинга строки: '" + value + "'", e);
        }
    }

    private static int parseEpicId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Для подзадачи не указан epicId");
        }
        return parseIntField(value, "epicId");
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    String.format("Некорректный формат даты: '%s' (ожидается yyyy-MM-ddTHH:mm)", value), e);
        }
    }

    private static Duration parseDuration(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long minutes = Long.parseLong(value.trim());
            return Duration.ofMinutes(minutes);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Некорректная продолжительность: '%s' (ожидается число минут)", value), e);
        }
    }

    private static int parseIntField(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Некорректное значение поля %s: '%s' (ожидается целое число)", fieldName, value), e);
        }
    }

    private static <E extends Enum<E>> E parseEnumField(String value, Class<E> enumClass, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Некорректное значение поля %s: '%s' (допустимые значения: %s)",
                            fieldName, value, List.of(enumClass.getEnumConstants())), e);
        }
    }
}