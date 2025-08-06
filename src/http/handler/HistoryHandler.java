package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import manager.task.TaskManager;
import model.Task;

import java.io.IOException;
import java.util.List;

public class HistoryHandler extends BaseHttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    public HistoryHandler(TaskManager manager, Gson gson) {
        this.manager = manager;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                List<Task> history = manager.getHistory();
                String json = gson.toJson(history);
                sendText(exchange, json);
            } else {
                sendServerError(exchange);
            }
        } catch (Exception e) {
            sendServerError(exchange);
        }
    }
}
