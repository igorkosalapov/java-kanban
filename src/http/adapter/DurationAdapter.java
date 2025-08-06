package http.adapter;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Duration;

public class DurationAdapter
        implements JsonSerializer<Duration>, JsonDeserializer<Duration> {

    @Override
    public JsonElement serialize(Duration src, Type typeOfSrc, JsonSerializationContext ctx) {
        return new JsonPrimitive(src.toString());
    }

    @Override
    public Duration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {
        return Duration.parse(json.getAsString());
    }
}
