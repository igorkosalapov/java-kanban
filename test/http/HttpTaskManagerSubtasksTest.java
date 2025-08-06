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

public class HttpTaskManagerSubtasksTest {
    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private HttpClient client;
    private int epicId;

    @BeforeEach
    @DisplayName("Настройка сервера, клиента и создание эпика перед тестами Subtasks")
    public void setUp() throws IOException, InterruptedException {
        manager = new InMemoryTaskManager();
        manager.clearTasks();
        manager.clearEpics();
        manager.clearSubtasks();
        taskServer = new HttpTaskServer(manager);
        taskServer.start();
        gson = HttpTaskServer.getGson();
        client = HttpClient.newHttpClient();

        Epic epic = new Epic("Epic1", "Описание эпика");
        String epicJson = gson.toJson(epic);
        HttpRequest postEpic = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .POST(HttpRequest.BodyPublishers.ofString(epicJson))
                .build();
        HttpResponse<String> epicResp = client.send(postEpic, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, epicResp.statusCode(), "POST /epics должен возвращать 201");

        HttpResponse<String> allEpics = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/epics")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        Epic[] epics = gson.fromJson(allEpics.body(), Epic[].class);
        assertTrue(epics.length > 0, "Ожидается хотя бы один эпик");
        epicId = epics[0].getId();
    }

    @AfterEach
    @DisplayName("Остановка сервера после тестов Subtasks")
    public void tearDown() {
        taskServer.stop();
    }

    @Test
    @DisplayName("GET /subtasks возвращает пустой список, если подзадач нет")
    public void testGetAllSubtasksEmpty() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    @DisplayName("POST /subtasks возвращает 201 Created при создании подзадачи")
    public void testCreateSubtaskReturnsCreated() throws IOException, InterruptedException {
        Subtask subtask = new Subtask("Sub1", "Описание подзадачи", Status.NEW, epicId,
                LocalDateTime.now(), Duration.ofMinutes(20));
        String subJson = gson.toJson(subtask);
        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(subJson))
                .build();
        HttpResponse<String> response = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), "POST /subtasks должен возвращать 201");
    }

    @Test
    @DisplayName("GET /subtasks возвращает список с созданной подзадачей")
    public void testGetAllSubtasksAfterCreation() throws IOException, InterruptedException {
        Subtask subtask = new Subtask("Sub2", "Описание", Status.NEW, epicId, LocalDateTime.now(),
                Duration.ofMinutes(15));
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(subtask)))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpRequest getAll = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(getAll, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Subtask[] subs = gson.fromJson(response.body(), Subtask[].class);
        assertEquals(1, subs.length, "Ожидается одна подзадача после создания");
        assertEquals("Sub2", subs[0].getName());
    }

    @Test
    @DisplayName("GET /subtasks?id={id} возвращает подзадачу по её ID")
    public void testGetSubtaskByIdReturnsSubtask() throws IOException, InterruptedException {
        Subtask subtask = new Subtask("Sub3", "Desc", Status.NEW, epicId, LocalDateTime.now(),
                Duration.ofMinutes(10));
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(subtask)))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> allResp = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/subtasks")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        Subtask created = gson.fromJson(allResp.body(), Subtask[].class)[0];
        int id = created.getId();

        HttpRequest getOne = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks?id=" + id))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(getOne, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        Subtask fetched = gson.fromJson(resp.body(), Subtask.class);
        assertEquals(id, fetched.getId());
        assertEquals("Sub3", fetched.getName());
    }

    @Test
    @DisplayName("GET /subtasks?id=nonexistent возвращает 404 Not Found для несуществующей подзадачи")
    public void testGetSubtaskNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks?id=999"))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, resp.statusCode(), "GET несуществующей подзадачи должен возвращать 404");
    }

    @Test
    @DisplayName("DELETE /subtasks?id=nonexistent возвращает 404 Not Found при удалении несуществующей подзадачи")
    public void testDeleteSubtaskNotFound() throws IOException, InterruptedException {
        HttpRequest delete = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks?id=999"))
                .DELETE()
                .build();
        HttpResponse<String> resp = client.send(delete, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, resp.statusCode(), "DELETE несуществующей подзадачи должен возвращать" +
                " 404");
    }

    @Test
    @DisplayName("DELETE /subtasks удаляет все подзадачи и возвращает 201 Created")
    public void testDeleteAllSubtasks() throws IOException, InterruptedException {
        Subtask subtask = new Subtask("Sub4", "Desc", Status.NEW, epicId, LocalDateTime.now(),
                Duration.ofMinutes(12));
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(subtask)))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpRequest deleteAll = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .DELETE()
                .build();
        HttpResponse<String> resp = client.send(deleteAll, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, resp.statusCode(), "DELETE /subtasks должен возвращать 201");

        HttpResponse<String> after = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/subtasks")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, after.statusCode());
        assertEquals("[]", after.body());
    }
}
