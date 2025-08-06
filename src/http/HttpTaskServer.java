package http;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import http.adapter.DurationAdapter;
import http.adapter.LocalDateTimeAdapter;
import manager.task.TaskManager;
import manager.Managers;
import http.handler.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;

public class HttpTaskServer {
    private static final int PORT = 8080;

    private final HttpServer server;
    private final TaskManager manager;
    private static Gson gson;

    public static synchronized Gson getGson() {
        if (gson != null) {
            return gson;
        }
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .create();
        return gson;
    }

    public HttpTaskServer() throws IOException {
        this(Managers.getDefault());
    }

    public HttpTaskServer(TaskManager manager) throws IOException {
        this.manager = manager;
        gson = getGson();
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);
        createContexts();
    }

    private void createContexts() {
        server.createContext("/tasks", new TasksHandler(manager, gson));
        server.createContext("/subtasks", new SubtasksHandler(manager, gson));
        server.createContext("/epics", new EpicsHandler(manager, gson));
        server.createContext("/history", new HistoryHandler(manager, gson));
        server.createContext("/prioritized", new PrioritizedHandler(manager, gson));
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен на порту " + PORT);
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) {
        try {
            HttpTaskServer taskServer = new HttpTaskServer();
            taskServer.start();
            System.out.println("Нажмите ENTER, если хотите остановить сервер...");
            System.in.read();
            taskServer.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
