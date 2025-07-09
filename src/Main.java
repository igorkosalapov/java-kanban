import manager.task.FileBackedTaskManager;
import model.*;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        File file = new File("resources/data.csv");

        // создаём менеджер с сохранением
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        // создаём задачи
        Task task1 = new Task("Погладить кота", "Задача 1", Status.NEW);
        manager.createTask(task1);

        Epic epic1 = new Epic("Подготовить проект", "Эпик 1");
        manager.createEpic(epic1);

        Subtask sub1 = new Subtask("Сделать слайды", "Подзадача 1", Status.NEW, epic1.getId());
        Subtask sub2 = new Subtask("Подготовить речь", "Подзадача 2", Status.DONE, epic1.getId());
        manager.createSubtask(sub1);
        manager.createSubtask(sub2);

        // вызываем задачи, чтобы они попали в историю
        manager.getTaskById(task1.getId());
        manager.getEpicById(epic1.getId());
        manager.getSubtaskById(sub1.getId());

        System.out.println("До загрузки из файла:");
        System.out.println(manager.getAllTasks());
        System.out.println(manager.getAllEpics());
        System.out.println(manager.getAllSubtasks());
        System.out.println("История:");
        System.out.println(manager.getHistory());

        // загружаем из файла в новый объект
        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(file);

        System.out.println("\nПосле загрузки из файла:");
        System.out.println(loaded.getAllTasks());
        System.out.println(loaded.getAllEpics());
        System.out.println(loaded.getAllSubtasks());
        System.out.println("История:");
        System.out.println(loaded.getHistory());
    }
}
