package http;

import com.google.gson.Gson;
import manager.task.InMemoryTaskManager;
import manager.task.TaskManager;
import model.Epic;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class HttpTaskManagerEpicsTest {
    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private HttpClient client;

    @BeforeEach
    @DisplayName("Настройка сервера и клиента перед тестами Epics")
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
    @DisplayName("Остановка сервера после тестов Epics")
    public void tearDown() {
        taskServer.stop();
    }

    @Test
    @DisplayName("GET /epics возвращает пустой список, если эпиков нет")
    public void testGetAllEpicsEmpty() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    @DisplayName("POST /epics возвращает 201 Created при создании эпика")
    public void testCreateEpicReturnsCreated() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic1", "Описание эпика");
        String json = gson.toJson(epic);

        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), "POST /epics должен возвращать 201 Created");
    }

    @Test
    @DisplayName("GET /epics возвращает список с созданным эпиком")
    public void testGetAllEpicsAfterCreation() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic2", "Описание");
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(epic)))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpRequest getAll = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(getAll, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Epic[] epics = gson.fromJson(response.body(), Epic[].class);
        assertEquals(1, epics.length, "Ожидается ровно один эпик после создания");
        assertEquals("Epic2", epics[0].getName());
    }

    @Test
    @DisplayName("GET /epics?id={id} возвращает эпик по его ID")
    public void testGetEpicByIdReturnsEpic() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic3", "Описание3");
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(epic)))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> allResp = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/epics")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        Epic created = gson.fromJson(allResp.body(), Epic[].class)[0];
        int id = created.getId();

        HttpRequest getOne = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics?id=" + id))
                .GET()
                .build();
        HttpResponse<String> response = client.send(getOne, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Epic fetched = gson.fromJson(response.body(), Epic.class);
        assertEquals(id, fetched.getId());
        assertEquals("Epic3", fetched.getName());
    }

    @Test
    @DisplayName("GET /epics?id=nonexistent возвращает 404 Not Found для несуществующего эпика")
    public void testGetEpicNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics?id=999"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode(), "GET несуществующего эпика должен возвращать 404");
    }

    @Test
    @DisplayName("DELETE /epics?id=nonexistent возвращает 404 Not Found при удалении несуществующего эпика")
    public void testDeleteEpicNotFound() throws IOException, InterruptedException {
        HttpRequest delete = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics?id=999"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(delete, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode(), "DELETE несуществующего эпика должен возвращать" +
                " 404");
    }

    @Test
    @DisplayName("DELETE /epics удаляет все эпики и возвращает 201 Created")
    public void testDeleteAllEpics() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic4", "Desc4");
        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(epic)))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpRequest deleteAll = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(deleteAll, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), "DELETE /epics должен возвращать 201");

        HttpResponse<String> after = client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/epics")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, after.statusCode());
        assertEquals("[]", after.body());
    }
}
