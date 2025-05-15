import manager.*;
import model.*;

public class Main {
    public static void main(String[] args) {
        TaskManager manager = new TaskManager();

        Task task1 = new Task("Переезд", "Упаковать и перевезти вещи", Status.NEW);
        Task task2 = new Task("Позвонить бабушке", "Уточнить, как дела", Status.IN_PROGRESS);
        manager.createTask(task1);
        manager.createTask(task2);

        System.out.println("== Все задачи ==");
        for (Task task : manager.getAllTasks()) {
            System.out.println(task);
        }

        Task updatedTask = new Task(task2.getName(), task2.getDescription(), Status.DONE);
        updatedTask.setId(task2.getId());
        manager.updateTask(updatedTask);
        System.out.println("\n== После обновления второй задачи ==");
        System.out.println(manager.getTaskById(updatedTask.getId()));

        Epic epic1 = new Epic("Организовать праздник", "Большой семейный праздник");
        manager.createEpic(epic1);

        Subtask subtask1 = new Subtask("Выбрать дату", "Узнать, когда удобно всем", Status.NEW,
                epic1.getId());
        Subtask subtask2 = new Subtask("Забронировать кафе", "Найти и зарезервировать место",
                Status.NEW, epic1.getId());
        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        System.out.println("\n== Эпик с подзадачами после создания ==");
        System.out.println(manager.getEpicById(epic1.getId()));
        for (Subtask sub : manager.getSubtasksByEpicId(epic1.getId())) {
            System.out.println(sub);
        }

        Subtask updatedSubtask = new Subtask(subtask1.getName(), subtask1.getDescription(),
                Status.DONE, subtask1.getEpicId());
        updatedSubtask.setId(subtask1.getId());
        manager.updateSubtask(updatedSubtask);
        System.out.println("\n== Эпик после обновления первой подзадачи ==");
        System.out.println(manager.getEpicById(epic1.getId()));

        Subtask updatedSubtask2 = new Subtask(subtask2.getName(), subtask2.getDescription(), Status.DONE,
                subtask2.getEpicId());
        updatedSubtask2.setId(subtask2.getId());
        manager.updateSubtask(updatedSubtask2);
        System.out.println("\n== Эпик после обновления всех подзадач ==");
        System.out.println(manager.getEpicById(epic1.getId()));

        Epic updatedEpic = new Epic("Новый праздник", "Описание изменено");
        updatedEpic.setId(epic1.getId());
        manager.updateEpic(updatedEpic);
        System.out.println("\n== Эпик после обновления имени и описания ==");
        System.out.println(manager.getEpicById(updatedEpic.getId()));

        Epic epic2 = new Epic("Купить квартиру", "Выбор и покупка жилья");
        manager.createEpic(epic2);

        Subtask subtask3 = new Subtask("Найти риэлтора", "Выбрать проверенного", Status.NEW,
                epic2.getId());
        manager.createSubtask(subtask3);

        System.out.println("\n== Второй эпик и подзадача ==");
        System.out.println(manager.getEpicById(epic2.getId()));
        System.out.println(manager.getSubtaskById(subtask3.getId()));

        manager.deleteSubtaskById(subtask3.getId());
        System.out.println("\n== После удаления подзадачи второго эпика ==");
        System.out.println(manager.getEpicById(epic2.getId()));
        System.out.println("Подзадачи: " + manager.getSubtasksByEpicId(epic2.getId()));

        manager.deleteTaskById(task1.getId());
        manager.deleteEpicById(epic1.getId());

        System.out.println("\n== После удаления задачи и первого эпика ==");
        System.out.println("Оставшиеся задачи:");
        for (Task task : manager.getAllTasks()) {
            System.out.println(task);
        }
        System.out.println("Оставшиеся эпики:");
        for (Epic epic : manager.getAllEpics()) {
            System.out.println(epic);
        }
        System.out.println("Оставшиеся подзадачи:");
        for (Subtask subtask : manager.getAllSubtasks()) {
            System.out.println(subtask);
        }

        manager.clearTasks();
        manager.clearSubtasks();
        manager.clearEpics();

        System.out.println("\n== После полной очистки ==");
        System.out.println("Задачи: " + manager.getAllTasks());
        System.out.println("Эпики: " + manager.getAllEpics());
        System.out.println("Подзадачи: " + manager.getAllSubtasks());
    }
}
