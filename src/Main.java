import manager.task.FileBackedTaskManager;
import model.*;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        File file = new File("resources/data.csv");
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        Task task1 = new Task("Погладить кота", "Задача 1", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(30));
        manager.createTask(task1);

        Epic epic1 = new Epic("Подготовить проект", "Эпик 1");
        manager.createEpic(epic1);

        Subtask sub1 = new Subtask("Сделать слайды", "Подзадача 1", Status.NEW, epic1.getId(),
                LocalDateTime.of(2025, 7, 16, 11, 0), Duration.ofMinutes(60));
        Subtask sub2 = new Subtask("Подготовить речь", "Подзадача 2", Status.DONE, epic1.getId(),
                LocalDateTime.of(2025, 7, 16, 12, 30), Duration.ofMinutes(30));
        manager.createSubtask(sub1);
        manager.createSubtask(sub2);

        manager.getTaskById(task1.getId());
        manager.getEpicById(epic1.getId());
        manager.getSubtaskById(sub1.getId());

        System.out.println("До загрузки из файла:");
        System.out.println(manager.getAllTasks());
        System.out.println(manager.getAllEpics());
        System.out.println(manager.getAllSubtasks());
        System.out.println("История:");
        System.out.println(manager.getHistory());

        System.out.println("\nStartTime эпика: " + epic1.getStartTime());
        System.out.println("EndTime эпика: " + epic1.getEndTime());
        System.out.println("Duration эпика: " + epic1.getDuration());

        System.out.println("\nЗадачи по приоритету:");
        for (Task task : manager.getPrioritizedTasks()) {
            LocalDateTime endTime = task.getStartTime().plus(task.getDuration());
            System.out.printf("ID=%d, Name='%s', StartTime=%s, Duration=%s, EndTime=%s%n",
                    task.getId(), task.getName(), task.getStartTime(), task.getDuration(), endTime);
        }

        try {
            Task conflict = new Task("Конфликтная задача", "Пересекается", Status.NEW,
                    LocalDateTime.of(2025, 7, 16, 10, 15),
                    Duration.ofMinutes(30));
            manager.createTask(conflict);
        } catch (IllegalArgumentException e) {
            System.out.println("\nПоймано исключение при пересечении задач: " + e.getMessage());
        }

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(file);

        System.out.println("\nПосле загрузки из файла:");
        System.out.println(loaded.getAllTasks());
        System.out.println(loaded.getAllEpics());
        System.out.println(loaded.getAllSubtasks());
        System.out.println("История:");
        System.out.println(loaded.getHistory());
        System.out.println("\nPrioritized после загрузки:");
        loaded.getPrioritizedTasks().forEach(task -> {
            LocalDateTime endTime = task.getStartTime().plus(task.getDuration());
            System.out.printf("ID=%d, Name='%s', StartTime=%s, Duration=%s, EndTime=%s%n",
                    task.getId(), task.getName(), task.getStartTime(), task.getDuration(), endTime);
        });

        Task newTask = new Task("Новая задача", "После загрузки", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 14, 0), Duration.ofMinutes(20));
        loaded.createTask(newTask);
        System.out.println("\nСоздана новая задача после загрузки: ID = " + newTask.getId());
    }
}
