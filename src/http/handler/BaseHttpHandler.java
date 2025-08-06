package http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public abstract class BaseHttpHandler implements HttpHandler {
    protected void sendText(HttpExchange exchange, String text) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, resp.length);
        exchange.getResponseBody().write(resp);
        exchange.close();
    }

    protected void sendCreated(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(201, -1);
        exchange.close();
    }

    protected void sendNotFound(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    protected void sendConflict(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(406, -1);
        exchange.close();
    }

    protected void sendServerError(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(500, -1);
        exchange.close();
    }

    protected int parseId(String query) {
        return Integer.parseInt(query.split("=")[1]);
    }

    protected String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            return br.lines().collect(Collectors.joining());
        }
    }
}
