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

public class HttpTaskManagerHistoryTest {
    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private HttpClient client;
    private int taskId;
    private int epicId;
    private int subtaskId;

    @BeforeEach
    @DisplayName("Настройка сервера и клиента перед тестами History")
    public void setUp() throws IOException, InterruptedException {
        manager = new InMemoryTaskManager();
        manager.clearTasks();
        manager.clearEpics();
        manager.clearSubtasks();
        taskServer = new HttpTaskServer(manager);
        taskServer.start();
        gson = HttpTaskServer.getGson();
        client = HttpClient.newHttpClient();

        // Создать Task
        Task task = new Task("TaskH", "DescH", Status.NEW,
                LocalDateTime.now(), Duration.ofMinutes(10));
        String taskJson = gson.toJson(task);
        HttpResponse<String> respTask = client.send(
                HttpRequest.newBuilder().uri(URI.create
                        ("http://localhost:8080/tasks")).POST(HttpRequest.BodyPublishers.ofString(taskJson)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respTask.statusCode());
        Task createdTask = gson.fromJson(
                client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/tasks")).GET().build(),
                        HttpResponse.BodyHandlers.ofString()).body(), Task[].class)[0];
        taskId = createdTask.getId();

        // Создать Epic
        Epic epic = new Epic("EpicH", "DescEpicH");
        String epicJson = gson.toJson(epic);
        HttpResponse<String> respEpic = client.send(
                HttpRequest.newBuilder().uri(URI.create
                        ("http://localhost:8080/epics")).POST(HttpRequest.BodyPublishers.ofString(epicJson)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respEpic.statusCode());
        Epic createdEpic = gson.fromJson(
                client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/epics")).GET().build(),
                        HttpResponse.BodyHandlers.ofString()).body(), Epic[].class)[0];
        epicId = createdEpic.getId();

        // Создать Subtask
        Subtask sub = new Subtask("SubH", "DescSubH", Status.NEW, epicId,
                LocalDateTime.now().plusMinutes(15), Duration.ofMinutes(5));
        String subJson = gson.toJson(sub);
        HttpResponse<String> respSub = client.send(
                HttpRequest.newBuilder().uri(URI.create
                        ("http://localhost:8080/subtasks")).POST(HttpRequest.BodyPublishers.ofString(subJson)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respSub.statusCode());
        Subtask createdSub = gson.fromJson(
                client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/subtasks")).GET().build(),
                        HttpResponse.BodyHandlers.ofString()).body(), Subtask[].class)[0];
        subtaskId = createdSub.getId();
    }

    @AfterEach
    @DisplayName("Остановка сервера после тестов History")
    public void tearDown() {
        taskServer.stop();
    }

    @Test
    @DisplayName("GET /history возвращает пустой список при отсутствии просмотров")
    public void testGetHistoryEmpty() throws IOException, InterruptedException {
        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/history")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals("[]", resp.body());
    }

    @Test
    @DisplayName("GET /history возвращает историю просмотров в порядке вызовов")
    public void testGetHistoryOrder() throws IOException, InterruptedException {
        // Просмотреть Task
        client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/tasks?id=" + taskId)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        // Просмотреть Epic
        client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/epics?id=" + epicId)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        // Просмотреть Subtask
        client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/subtasks?id=" +
                subtaskId)).GET().build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/history")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        Task[] history = gson.fromJson(resp.body(), Task[].class);
        assertEquals(3, history.length);
        assertEquals(taskId, history[0].getId());
        assertEquals(epicId, history[1].getId());
        assertEquals(subtaskId, history[2].getId());
    }
}
