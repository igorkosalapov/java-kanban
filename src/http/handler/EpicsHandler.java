package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import manager.task.TaskManager;
import model.Epic;
import manager.task.exception.NotFoundException;
import manager.task.exception.IntersectionException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class EpicsHandler extends BaseHttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    public EpicsHandler(TaskManager manager, Gson gson) {
        this.manager = manager;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String query = exchange.getRequestURI().getQuery();

            switch (method) {
                case "GET":
                    handleGet(exchange, query);
                    break;
                case "POST":
                    handlePost(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange, query);
                    break;
                default:
                    sendServerError(exchange);
            }
        } catch (RuntimeException e) {
            sendServerError(exchange);
        }
    }

    private void handleGet(HttpExchange exchange, String query) throws IOException {
        if (query == null) {
            List<Epic> epics = manager.getAllEpics();
            sendText(exchange, gson.toJson(epics));
        } else {
            int id = parseId(query);
            try {
                Epic epic = manager.getEpicById(id);
                sendText(exchange, gson.toJson(epic));
            } catch (NotFoundException e) {
                sendNotFound(exchange);
            }
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = new BufferedReader(new InputStreamReader(
                exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining());
        Epic epic = gson.fromJson(body, Epic.class);

        try {
            if (epic.getId() <= 0) {
                manager.createEpic(epic);
            } else {
                manager.updateEpic(epic);
            }
            sendCreated(exchange);
        } catch (IntersectionException e) {
            sendConflict(exchange);
        } catch (NotFoundException e) {
            sendNotFound(exchange);
        }
    }

    private void handleDelete(HttpExchange exchange, String query) throws IOException {
        try {
            if (query == null) {
                manager.clearEpics();
            } else {
                int id = parseId(query);
                manager.deleteEpicById(id);
            }
            sendCreated(exchange);
        } catch (NotFoundException e) {
            sendNotFound(exchange);
        }
    }

    private int parseId(String query) {
        return Integer.parseInt(query.split("=")[1]);
    }
}