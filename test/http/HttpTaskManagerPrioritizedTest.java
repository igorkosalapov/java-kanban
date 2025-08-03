package http;

import com.google.gson.Gson;
import manager.task.InMemoryTaskManager;
import manager.task.TaskManager;
import model.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class HttpTaskManagerPrioritizedTest {
    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private HttpClient client;
    private int epicId;

    @BeforeEach
    @DisplayName("Настройка сервера, клиента и создание эпика перед тестом приоритизации задач")
    public void setUp() throws IOException, InterruptedException {
        manager = new InMemoryTaskManager();
        manager.clearTasks();
        manager.clearEpics();
        manager.clearSubtasks();
        taskServer = new HttpTaskServer(manager);
        taskServer.start();
        gson = HttpTaskServer.getGson();
        client = HttpClient.newHttpClient();

        // Создаём эпик через HTTP
        Epic epic = new Epic("EpicPriority", "Эпик для приоритизации");
        String epicJson = gson.toJson(epic);
        HttpResponse<String> postEpic = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/epics"))
                        .POST(HttpRequest.BodyPublishers.ofString(epicJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postEpic.statusCode(), "POST /epics должен возвращать 201");

        HttpResponse<String> getEpics = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/epics"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        Epic[] epics = gson.fromJson(getEpics.body(), Epic[].class);
        assertTrue(epics.length >= 1, "Ожидается хотя бы один эпик");
        epicId = epics[0].getId();
    }

    @AfterEach
    @DisplayName("Остановка сервера после теста приоритизации задач")
    public void tearDown() {
        taskServer.stop();
    }

    @Test
    @DisplayName("GET /prioritized возвращает задачи и подзадачи в порядке возрастания стартового времени")
    public void testPrioritizedOrderNoDuplicates() throws IOException, InterruptedException {
        LocalDateTime now = LocalDateTime.now();

        // 1. Создаём задачу с ранним стартом (+5 мин)
        Task earlyTask = new Task("Early", "Ранняя задача", Status.NEW,
                now.plusMinutes(5), Duration.ofMinutes(5));
        String earlyJson = gson.toJson(earlyTask);
        HttpResponse<String> postEarly = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/tasks"))
                        .POST(HttpRequest.BodyPublishers.ofString(earlyJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postEarly.statusCode(), "POST /tasks для ранней задачи должен" +
                " возвращать 201");

        // 2. Создаём подзадачу со средним стартом (+10 мин)
        Subtask midSub = new Subtask("Mid", "Средняя подзадача", Status.NEW,
                epicId, now.plusMinutes(11), Duration.ofMinutes(5));
        String midJson = gson.toJson(midSub);
        HttpResponse<String> postMid = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/subtasks"))
                        .POST(HttpRequest.BodyPublishers.ofString(midJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postMid.statusCode(), "POST /subtasks должен возвращать 201");

        // 3. Создаём задачу с поздним стартом (+15 мин)
        Task lateTask = new Task("Late", "Поздняя задача", Status.NEW,
                now.plusMinutes(17), Duration.ofMinutes(5));
        String lateJson = gson.toJson(lateTask);
        HttpResponse<String> postLate = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/tasks"))
                        .POST(HttpRequest.BodyPublishers.ofString(lateJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postLate.statusCode(), "POST /tasks для поздней задачи должен" +
                " возвращать 201");

        // 4. Запрашиваем приоритизацию
        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/prioritized"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), "GET /prioritized должен возвращать 200");

        Task[] ordered = gson.fromJson(resp.body(), Task[].class);
        assertEquals(3, ordered.length, "Должно быть три элемента в списке приоритизации");
        // Проверяем порядок по startTime
        assertTrue(ordered[0].getStartTime().isBefore(ordered[1].getStartTime()), "Первый старт должен быть" +
                " раньше второго");
        assertTrue(ordered[1].getStartTime().isBefore(ordered[2].getStartTime()), "Второй старт должен быть" +
                " раньше третьего");
    }
}
