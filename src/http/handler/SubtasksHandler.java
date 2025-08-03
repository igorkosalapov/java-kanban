package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import manager.task.TaskManager;
import model.Subtask;
import manager.task.exception.NotFoundException;
import manager.task.exception.IntersectionException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class SubtasksHandler extends BaseHttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    public SubtasksHandler(TaskManager manager, Gson gson) {
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
            List<Subtask> subs = manager.getAllSubtasks();
            sendText(exchange, gson.toJson(subs));
        } else {
            int id = parseId(query);
            try {
                Subtask sub = manager.getSubtaskById(id);
                sendText(exchange, gson.toJson(sub));
            } catch (NotFoundException e) {
                sendNotFound(exchange);
            }
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = new BufferedReader(new InputStreamReader(
                exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining());
        Subtask subtask = gson.fromJson(body, Subtask.class);

        try {
            if (subtask.getId() <= 0) {
                manager.createSubtask(subtask);
            } else {
                // проверим существование перед обновлением
                manager.getSubtaskById(subtask.getId());
                manager.updateSubtask(subtask);
            }
            sendCreated(exchange);
        } catch (IntersectionException e) {
            sendConflict(exchange);
        } catch (NotFoundException e) {
            sendNotFound(exchange);
        }
    }

    private void handleDelete(HttpExchange exchange, String query) throws IOException {
        if (query == null) {
            // удаляем все подзадачи
            manager.clearSubtasks();
            sendCreated(exchange);
        } else {
            int id = parseId(query);
            try {
                // проверить существование и вернуть 404, если нет
                manager.getSubtaskById(id);
                manager.deleteSubtaskById(id);
                sendCreated(exchange);
            } catch (NotFoundException e) {
                sendNotFound(exchange);
            }
        }
    }

    private int parseId(String query) {
        return Integer.parseInt(query.split("=")[1]);
    }
}
