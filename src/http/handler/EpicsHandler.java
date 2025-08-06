package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import manager.task.TaskManager;
import model.Epic;
import manager.task.exception.NotFoundException;
import manager.task.exception.IntersectionException;

import java.io.IOException;
import java.util.List;


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
        String body = readRequestBody(exchange);
        Epic epic = gson.fromJson(body, Epic.class);

        try {
            if (epic.getId() <= 0) {
                manager.createEpic(epic);
            } else {
                manager.getEpicById(epic.getId());
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
        if (query == null) {
            manager.clearEpics();
            sendCreated(exchange);
        } else {
            int id = parseId(query);
            try {
                manager.getEpicById(id);
                manager.deleteEpicById(id);
                sendCreated(exchange);
            } catch (NotFoundException e) {
                sendNotFound(exchange);
            }
        }
    }
}
