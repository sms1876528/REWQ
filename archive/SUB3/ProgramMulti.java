import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/**
 * SUB3 multi-threaded version. Cached thread pool serves requests in parallel;
 * MonitoringStore guards records with an internal lock and exposes a snapshot
 * for safe iteration during performance calculation.
 */
public class ProgramMulti {
    static MonitoringStore store;
    static PerformanceCalculator calc;

    public static void main(String[] args) throws IOException {
        store = new MonitoringStore(Path.of("MODELS.JSON"));
        calc = new PerformanceCalculator(store);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        server.createContext("/monitoring", ProgramMulti::handleMonitoring);
        server.createContext("/performance", ProgramMulti::handlePerformance);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    static void handleMonitoring(HttpExchange ctx) throws IOException {
        String body = new String(ctx.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        store.add(JsonParser.parseString(body).getAsJsonObject());
        reply(ctx, null);
    }

    static void handlePerformance(HttpExchange ctx) throws IOException {
        String body = new String(ctx.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject q = JsonParser.parseString(body).getAsJsonObject();
        String json = calc.performance(
                q.get("modelName").getAsString(),
                q.get("timeWindow").getAsString());
        reply(ctx, json);
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

    static class MonitoringStore {
        private final Map<String, Set<String>> models;
        private final List<JsonObject> records = new ArrayList<>();
        private final Object lock = new Object();

        MonitoringStore(Path modelsFile) throws IOException {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> raw = gson.fromJson(Files.readString(modelsFile), mapType);
            Map<String, Set<String>> m = new HashMap<>();
            for (Map.Entry<String, List<String>> kv : raw.entrySet()) {
                m.put(kv.getKey(), new HashSet<>(kv.getValue()));
            }
            this.models = m;
        }

        void add(JsonObject record) {
            synchronized (lock) {
                records.add(record);
            }
        }

        List<JsonObject> snapshot() {
            synchronized (lock) {
                return new ArrayList<>(records);
            }
        }

        Set<String> agentsOf(String modelName) {
            return models.get(modelName);
        }
    }

    static class PerformanceCalculator {
        private final MonitoringStore store;
        private final Gson gson = new Gson();

        PerformanceCalculator(MonitoringStore store) {
            this.store = store;
        }

        String performance(String modelName, String timeWindow) {
            Set<String> agents = store.agentsOf(modelName);
            Map<String, Integer> pred = new HashMap<>();
            Map<String, String> pTs = new HashMap<>();
            Map<String, Integer> act = new HashMap<>();

            for (JsonObject r : store.snapshot()) {
                String aid = r.get("agentId").getAsString();
                if (!agents.contains(aid)) continue;
                String key = aid + "|" + r.get("requestId").getAsString();
                String type = r.get("dataType").getAsString();
                if (type.equals("P")) {
                    pred.put(key, r.get("dataValue").getAsInt());
                    pTs.put(key, r.get("timestamp").getAsString());
                } else {
                    act.put(key, r.get("dataValue").getAsInt());
                }
            }

            int correct = 0, total = 0;
            for (Map.Entry<String, Integer> kv : pred.entrySet()) {
                if (!pTs.get(kv.getKey()).startsWith(timeWindow)) continue;
                total++;
                Integer a = act.get(kv.getKey());
                if (a != null && a.equals(kv.getValue())) correct++;
            }

            Map<String, Integer> result = new HashMap<>();
            result.put("correct", correct);
            result.put("total", total);
            return gson.toJson(result);
        }
    }
}
