
public class Main {
    public static void main(String[] args) {
        try {
            TaskManager manager = new InMemoryTaskManager();

            System.out.println("=== ТЕСТИРОВАНИЕ ТРЕКЕРА ЗАДАЧ ===");
            System.out.println("\n1. СОЗДАНИЕ ЗАДАЧ И ЭПИКОВ");

            Task task1 = manager.createTask(new Task("Заказать пиццу", "С сыром и пепперони",
                    Status.NEW));
            Task task2 = manager.createTask(new Task("Позвонить маме", "Узнать про дачу", Status.NEW));

            Epic epic1 = manager.createEpic(new Epic("Переезд", "Организация переезда в новый офис"));
            Epic epic2 = manager.createEpic(new Epic("Ремонт", "Косметический ремонт квартиры"));

            printAllTasks(manager);

            System.out.println("\n2. СОЗДАНИЕ И ТЕСТИРОВАНИЕ ПОДЗАДАЧ");

            Subtask subtask1 = manager.createSubtask(new Subtask("Упаковка вещей",
                    "Упаковать компьютеры", Status.NEW, epic1.getId()));
            Subtask subtask2 = manager.createSubtask(new Subtask("Заказ грузчиков",
                    "Найти через сервис", Status.IN_PROGRESS, epic1.getId()));
            Subtask subtask3 = manager.createSubtask(new Subtask("Поклейка обоев", "В гостиной",
                    Status.NEW, epic2.getId()));

            System.out.println("\nСтатус эпика после создания подзадач:");
            System.out.println(manager.getEpicById(epic1.getId())); // Должен быть IN_PROGRESS

            subtask1.setStatus(Status.DONE);
            manager.updateSubtask(subtask1);
            System.out.println("\nПосле завершения первой подзадачи:");
            System.out.println(manager.getEpicById(epic1.getId())); // Все еще IN_PROGRESS

            subtask2.setStatus(Status.DONE);
            manager.updateSubtask(subtask2);
            System.out.println("\nПосле завершения всех подзадач:");
            System.out.println(manager.getEpicById(epic1.getId())); // Должен стать DONE

            System.out.println("\n3. ТЕСТИРОВАНИЕ ИСТОРИИ ПРОСМОТРОВ");

            manager.getTaskById(task1.getId());
            manager.getEpicById(epic1.getId());
            manager.getSubtaskById(subtask1.getId());
            manager.getTaskById(task2.getId());
            manager.getEpicById(epic1.getId()); // Дубликат - должен остаться один

            System.out.println("История просмотров (должны быть 4 уникальных задачи):");
            manager.getHistory().forEach(System.out::println);

            System.out.println("\n4. ТЕСТИРОВАНИЕ УДАЛЕНИЯ");

            manager.deleteTaskById(task1.getId());
            System.out.println("После удаления задачи история:");
            manager.getHistory().forEach(System.out::println); // task1 должна исчезнуть

            System.out.println("\nПодзадачи эпика перед удалением:");
            manager.getSubtasksByEpicId(epic1.getId()).forEach(System.out::println);

            manager.deleteEpicById(epic1.getId());
            System.out.println("\nПосле удаления эпика:");
            System.out.println("Эпики: " + manager.getAllEpics().size());
            System.out.println("Подзадачи: " + manager.getAllSubtasks().size()); // Должно быть меньше

            System.out.println("\n5. ТЕСТИРОВАНИЕ СОХРАНЕНИЯ В ФАЙЛ");
            manager.saveToFile("tasks_backup.csv");
            System.out.println("Данные сохранены в файл tasks_backup.csv");

            System.out.println("\n=== ТЕСТИРОВАНИЕ ЗАВЕРШЕНО ===");

        } catch (NotFoundException | ValidationException e) {
            System.err.println("Ошибка при выполнении: " + e.getMessage());
        }
    }

    private static void printAllTasks(TaskManager manager) {
        System.out.println("\nВсе задачи:");
        manager.getAllTasks().forEach(System.out::println);

        System.out.println("\nВсе эпики:");
        manager.getAllEpics().forEach(System.out::println);

        System.out.println("\nВсе подзадачи:");
        manager.getAllSubtasks().forEach(System.out::println);
    }
}