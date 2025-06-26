import manager.*;
import manager.history.*;
import manager.task.*;
import model.*;

import java.lang.reflect.Field;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        TaskManager manager = Managers.getDefault();

        if (manager instanceof InMemoryTaskManager) {
            try {
                Field historyField = InMemoryTaskManager.class.getDeclaredField("historyManager");
                historyField.setAccessible(true);

                Object history = historyField.get(manager);

                System.out.println("\n== Проверка HistoryManager ==");
                if (history instanceof HistoryManager) {
                    System.out.println("✅ Используется интерфейс HistoryManager");
                } else {
                    System.out.println("❌ НЕ используется интерфейс HistoryManager");
                }

                if (history.getClass().equals(InMemoryHistoryManager.class)) {
                    System.out.println("✅ Реализация — InMemoryHistoryManager из Managers.getDefaultHistory()");
                } else {
                    System.out.println("❌ Реализация НЕ является InMemoryHistoryManager");
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                System.out.println("⚠️ Не удалось проверить historyManager: " + e.getMessage());
            }
        }

        Task task1 = new Task("Переезд", "Упаковать и перевезти вещи", Status.NEW);
        Task task2 = new Task("Позвонить бабушке", "Уточнить, как дела", Status.IN_PROGRESS);
        manager.createTask(task1);
        manager.createTask(task2);

        System.out.println("== Все задачи ==");
        for (Task task : manager.getAllTasks()) {
            System.out.println(task);
        }

        System.out.println("\nИстория просмотров (должна быть пустая):");
        printHistory(manager);

        Task updatedTask = new Task(task2.getName(), task2.getDescription(), Status.DONE);
        updatedTask.setId(task2.getId());
        manager.updateTask(updatedTask);
        System.out.println("\n== После обновления второй задачи ==");
        System.out.println(manager.getTaskById(updatedTask.getId()));

        System.out.println("\nИстория просмотров (должна содержать просмотренную задачу):");
        printHistory(manager);

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

        System.out.println("\nИстория просмотров (должна содержать задачу и эпик):");
        printHistory(manager);

        Subtask updatedSubtask = new Subtask(subtask1.getName(), subtask1.getDescription(),
                Status.DONE, subtask1.getEpicId());
        updatedSubtask.setId(subtask1.getId());
        manager.updateSubtask(updatedSubtask);
        System.out.println("\n== Эпик после обновления первой подзадачи ==");
        System.out.println(manager.getEpicById(epic1.getId()));
        System.out.println(manager.getSubtaskById(subtask1.getId()));
        System.out.println("\nИстория просмотров (должна содержать задачу, эпик и подзадачу):");
        printHistory(manager);

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
        System.out.println("\nТестирование ограничения истории:");
        for (int i = 0; i < 15; i++) {
            manager.getTaskById(task1.getId());
        }
        System.out.println("История после 15 просмотров одной задачи:");
        printHistory(manager);

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

        System.out.println("\nИстория просмотров после удалений:");
        printHistory(manager);

        manager.clearTasks();
        manager.clearSubtasks();
        manager.clearEpics();

        System.out.println("\n== После полной очистки ==");
        System.out.println("Задачи: " + manager.getAllTasks());
        System.out.println("Эпики: " + manager.getAllEpics());
        System.out.println("Подзадачи: " + manager.getAllSubtasks());

        System.out.println("\nИстория просмотров после очистки:");
        printHistory(manager);

    }

    private static void printHistory(TaskManager manager) {
        List<Task> history = manager.getHistory();
        if (history.isEmpty()) {
            System.out.println("(пусто)");
        } else {
            for (Task task : history) {
                System.out.println(task);
            }
        }
    }
}