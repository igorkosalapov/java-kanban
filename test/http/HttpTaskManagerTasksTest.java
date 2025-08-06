package http;

import com.google.gson.Gson;
import manager.task.InMemoryTaskManager;
import manager.task.TaskManager;
import model.Task;
import model.Status;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class HttpTaskManagerTasksTest {
    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private HttpClient client;

    @BeforeEach
    @DisplayName("Настройка сервера и клиента перед каждым тестом")
    public void setUp() throws IOException {
        manager = new InMemoryTaskManager();
        manager.clearTasks();
        manager.clearEpics();
        manager.clearSubtasks();
        taskServer = new HttpTaskServer(manager);
        taskServer.start();
        gson = HttpTaskServer.getGson();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    @DisplayName("Остановка сервера после каждого теста")
    public void tearDown() {
        taskServer.stop();
    }

    @Test
    @DisplayName("GET /tasks возвращает пустой список, если задач нет")
    public void testGetAllTasksEmpty() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    @DisplayName("POST /tasks возвращает 201 Created при создании задачи")
    public void testCreateTaskReturnsCreated() throws IOException, InterruptedException {
        Task task = new Task("Test task", "Desc", Status.NEW,
                LocalDateTime.now(), Duration.ofMinutes(10));
        String json = gson.toJson(task);

        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), "POST /tasks должен возвращать 201 Created");
    }

    @Test
    @DisplayName("GET /tasks возвращает список с созданной задачей")
    public void testGetAllTasksAfterCreation() throws IOException, InterruptedException {
        Task task = new Task("Sample", "Desc", Status.NEW,
                LocalDateTime.now(), Duration.ofMinutes(5));
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(task)))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpRequest getAll = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(getAll, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Task[] tasks = gson.fromJson(response.body(), Task[].class);
        assertEquals(1, tasks.length, "Ожидается ровно одна задача после создания");
        assertEquals("Sample", tasks[0].getName());
    }

    @Test
    @DisplayName("GET /tasks?id={id} возвращает задачу по её ID")
    public void testGetTaskByIdReturnsTask() throws IOException, InterruptedException {
        Task task = new Task("Lookup", "Desc", Status.NEW,
                LocalDateTime.now(), Duration.ofMinutes(7));
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(task)))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> allResp = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/tasks")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        Task created = gson.fromJson(allResp.body(), Task[].class)[0];
        int id = created.getId();

        HttpRequest getOne = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks?id=" + id))
                .GET()
                .build();
        HttpResponse<String> response = client.send(getOne, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Task fetched = gson.fromJson(response.body(), Task.class);
        assertEquals(id, fetched.getId());
        assertEquals("Lookup", fetched.getName());
    }

    @Test
    @DisplayName("GET /tasks?id=nonexistent возвращает 404 Not Found для несуществующей задачи")
    public void testGetTaskNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks?id=999"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode(), "GET несуществующей задачи должен возвращать 404");
    }

    @Test
    @DisplayName("DELETE /tasks?id=nonexistent возвращает 404 Not Found при удалении несуществующей задачи")
    public void testDeleteTaskNotFound() throws IOException, InterruptedException {
        HttpRequest delete = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks?id=999"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(delete, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode(), "DELETE несуществующей задачи должен возвращать" +
                " 404");
    }

    @Test
    @DisplayName("DELETE /tasks удаляет все задачи и возвращает 201 Created")
    public void testDeleteAllTasks() throws IOException, InterruptedException {
        Task task = new Task("Temp", "Desc", Status.NEW,
                LocalDateTime.now(), Duration.ofMinutes(10));
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(task)))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpRequest deleteAll = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(deleteAll, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), "DELETE /tasks должен возвращать 201");

        HttpResponse<String> after = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/tasks")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, after.statusCode());
        assertEquals("[]", after.body());
    }
}
