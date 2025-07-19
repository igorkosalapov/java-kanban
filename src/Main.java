import manager.task.FileBackedTaskManager;
import model.*;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        File file = new File("tasks.csv");
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        Task task = new Task("Погладить кота", "Задача 1", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 10, 0), Duration.ofMinutes(30));
        manager.createTask(task);

        Epic epic = new Epic("Подготовить проект", "Эпик 1");
        manager.createEpic(epic);

        Subtask sub1 = new Subtask("Сделать слайды", "Подзадача 1", Status.NEW, epic.getId(),
                LocalDateTime.of(2025, 7, 16, 11, 0), Duration.ofMinutes(60));
        Subtask sub2 = new Subtask("Подготовить речь", "Подзадача 2", Status.DONE, epic.getId(),
                LocalDateTime.of(2025, 7, 16, 12, 30), Duration.ofMinutes(30));
        manager.createSubtask(sub1);
        manager.createSubtask(sub2);

        manager.getTaskById(task.getId());
        manager.getEpicById(epic.getId());
        manager.getSubtaskById(sub1.getId());

        System.out.println("До загрузки из файла:\n");

        System.out.println("Задачи:");
        for (Task t : manager.getAllTasks()) {
            System.out.println(t);
        }

        System.out.println("\nЭпики:");
        for (Epic e : manager.getAllEpics()) {
            System.out.println(e);
        }

        System.out.println("\nПодзадачи:");
        for (Subtask s : manager.getAllSubtasks()) {
            System.out.println(s);
        }

        System.out.println("\nИстория:");
        for (Task h : manager.getHistory()) {
            System.out.println(h);
        }

        System.out.println("\nStartTime эпика: " + epic.getStartTime());
        System.out.println("EndTime эпика: " + epic.getEndTime());
        System.out.println("Duration эпика: " + epic.getDuration());

        System.out.println("\nЗадачи по приоритету:");
        for (Task p : manager.getPrioritizedTasks()) {
            System.out.printf("ID=%d, Name='%s', StartTime=%s, Duration=%s, EndTime=%s%n",
                    p.getId(), p.getName(), p.getStartTime(), p.getDuration(), p.getEndTime());
        }

        System.out.println("\nПосле загрузки из файла:\n");
        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(file);

        System.out.println("Задачи:");
        for (Task t : loaded.getAllTasks()) {
            System.out.println(t);
        }

        System.out.println("\nЭпики:");
        for (Epic e : loaded.getAllEpics()) {
            System.out.println(e);
        }

        System.out.println("\nПодзадачи:");
        for (Subtask s : loaded.getAllSubtasks()) {
            System.out.println(s);
        }

        System.out.println("\nИстория:");
        for (Task h : loaded.getHistory()) {
            System.out.println(h);
        }

        System.out.println("\nPrioritized после загрузки:");
        for (Task p : loaded.getPrioritizedTasks()) {
            System.out.printf("ID=%d, Name='%s', StartTime=%s, Duration=%s, EndTime=%s%n",
                    p.getId(), p.getName(), p.getStartTime(), p.getDuration(), p.getEndTime());
        }

        Task newTask = new Task("Новая задача", "После загрузки", Status.NEW,
                LocalDateTime.of(2025, 7, 16, 14, 0), Duration.ofMinutes(20));
        loaded.createTask(newTask);

        System.out.println("\nСоздана новая задача после загрузки: ID = " + newTask.getId());
    }
}
