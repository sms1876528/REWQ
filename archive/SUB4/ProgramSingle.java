import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/**
 * SUB4 single-threaded version. HttpServer uses caller thread (setExecutor(null)).
 */
public class ProgramSingle {
    static Map<String, Set<String>> Models;
    static List<JsonObject> Records = new ArrayList<>();

    static final Gson GSON = new Gson();

    public static void main(String[] args) throws IOException {
        Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
        Map<String, List<String>> raw = GSON.fromJson(
                Files.readString(Path.of("MODELS.JSON")), mapType);
        Models = new HashMap<>();
        for (Map.Entry<String, List<String>> kv : raw.entrySet()) {
            Models.put(kv.getKey(), new HashSet<>(kv.getValue()));
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        server.createContext("/monitoring", ProgramSingle::handleMonitoring);
        server.createContext("/performance", ProgramSingle::handlePerformance);
        server.setExecutor(null);
        server.start();
    }

    static void handleMonitoring(HttpExchange ctx) throws IOException {
        String body = new String(ctx.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Records.add(JsonParser.parseString(body).getAsJsonObject());
        reply(ctx, null);
    }

    static void handlePerformance(HttpExchange ctx) throws IOException {
        String body = new String(ctx.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject q = JsonParser.parseString(body).getAsJsonObject();
        String json = performance(q.get("modelName").getAsString(), q.get("timeWindow").getAsString());
        reply(ctx, json);
    }

    static String performance(String modelName, String timeWindow) {
        Set<String> agents = Models.get(modelName);
        Map<String, Integer> predValue = new HashMap<>();
        Map<String, Integer> predLatency = new HashMap<>();
        Map<String, String> pTs = new HashMap<>();
        Map<String, Integer> act = new HashMap<>();

        for (JsonObject r : Records) {
            String aid = r.get("agentId").getAsString();
            if (!agents.contains(aid)) continue;
            String key = aid + "|" + r.get("requestId").getAsString();
            String type = r.get("dataType").getAsString();
            if (type.equals("P")) {
                predValue.put(key, r.get("dataValue").getAsInt());
                predLatency.put(key, r.get("latency").getAsInt());
                pTs.put(key, r.get("timestamp").getAsString());
            } else {
                act.put(key, r.get("dataValue").getAsInt());
            }
        }

        int correct = 0, total = 0;
        long latencySum = 0;
        for (Map.Entry<String, Integer> kv : predValue.entrySet()) {
            if (!pTs.get(kv.getKey()).startsWith(timeWindow)) continue;
            total++;
            latencySum += predLatency.get(kv.getKey());
            Integer a = act.get(kv.getKey());
            if (a != null && a.equals(kv.getValue())) correct++;
        }
        int latency = total > 0 ? (int) (latencySum / total) : 0;

        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("correct", correct);
        result.put("total", total);
        result.put("latency", latency);
        return GSON.toJson(result);
    }

    static void reply(HttpExchange ctx, String json) throws IOException {
        if (json != null) {
            byte[] buf = json.getBytes(StandardCharsets.UTF_8);
            ctx.getResponseHeaders().set("Content-Type", "application/json");
            ctx.sendResponseHeaders(200, buf.length);
            ctx.getResponseBody().write(buf);
        } else {
            ctx.sendResponseHeaders(200, -1);
        }
        ctx.close();
    }
}
