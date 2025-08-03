package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import model.Task;
import manager.task.TaskManager;

import java.io.IOException;
import java.util.List;

public class PrioritizedHandler extends BaseHttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    public PrioritizedHandler(TaskManager manager, Gson gson) {
        this.manager = manager;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                List<Task> prioritized = manager.getPrioritizedTasks();
                String json = gson.toJson(prioritized);
                sendText(exchange, json);
            } else {
                sendServerError(exchange);
            }
        } catch (Exception e) {
            sendServerError(exchange);
        }
    }
}
