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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/**
 * ============================================================================
 * 고급: 메시지 타입별 라우팅 서버 (GET/POST 분기 처리)
 * ============================================================================
 *
 * [학습 목표]
 * - HTTP 메서드 구분 (GET vs POST)
 * - 메시지 타입별 라우팅 (messageType 필드 기반)
 * - 동적 핸들러 매핑 (Map<타입, 핸들러>)
 * - Query Parameter 파싱 (GET 요청)
 *
 * [메시지 타입 예시]
 * - "REGISTER": 새 에이전트 등록 (POST)
 * - "MONITORING": 모니터링 데이터 수집 (POST)
 * - "QUERY_PERFORMANCE": 성능 조회 (GET 또는 POST)
 * - "QUERY_STATUS": 상태 조회 (GET)
 * - "DELETE_RECORD": 데이터 삭제 (POST)
 *
 * [요청 형식]
 * POST /api
 * {
 *   "messageType": "MONITORING",
 *   "data": { ... }
 * }
 *
 * GET /api?messageType=QUERY_STATUS&agentId=agent001
 */
public class ProgramRouter {
    static Map<String, Set<String>> Models;
    static List<JsonObject> Records = new ArrayList<>();
    static final Gson GSON = new Gson();

    public static void main(String[] args) throws IOException {
        // MODELS.JSON 파일 읽기
        Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
        Map<String, List<String>> raw = GSON.fromJson(
                Files.readString(Path.of("MODELS.JSON")), mapType);
        Models = new HashMap<>();
        for (Map.Entry<String, List<String>> kv : raw.entrySet()) {
            Models.put(kv.getKey(), new HashSet<>(kv.getValue()));
        }

        // HTTP 서버 생성
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);

        // 단일 엔드포인트 /api로 모든 요청 처리
        server.createContext("/api", ProgramRouter::handleApi);

        server.setExecutor(null);
        server.start();

        System.out.println("=== 라우팅 서버 시작 ===");
        System.out.println("주소: http://127.0.0.1:8080/api");
        System.out.println("지원 메시지 타입:");
        System.out.println("  - REGISTER (POST)");
        System.out.println("  - MONITORING (POST)");
        System.out.println("  - QUERY_PERFORMANCE (GET/POST)");
        System.out.println("  - QUERY_STATUS (GET)");
        System.out.println("  - LIST_MODELS (GET)");
    }

    /**
     * 통합 API 핸들러
     * - HTTP 메서드 확인 (GET/POST)
     * - 메시지 타입 추출
     * - 적절한 핸들러로 라우팅
     */
    static void handleApi(HttpExchange ctx) throws IOException {
        // HTTP 메서드 확인
        String method = ctx.getRequestMethod();  // "GET" 또는 "POST"

        String messageType = null;
        JsonObject data = null;

        if (method.equals("GET")) {
            // GET 요청: Query Parameter에서 messageType 추출
            // 예: /api?messageType=QUERY_STATUS&agentId=agent001
            String query = ctx.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParams(query);

            messageType = params.get("messageType");

            // Query Parameter를 JsonObject로 변환
            data = new JsonObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!entry.getKey().equals("messageType")) {
                    data.addProperty(entry.getKey(), entry.getValue());
                }
            }

        } else if (method.equals("POST")) {
            // POST 요청: Body에서 JSON 파싱
            String body = new String(ctx.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            messageType = json.get("messageType").getAsString();
            data = json.has("data") ? json.getAsJsonObject("data") : json;
        }

        // 메시지 타입에 따라 라우팅
        String response = routeMessage(messageType, data, method);

        // 응답 전송
        reply(ctx, response);
    }

    /**
     * 메시지 타입별 라우팅 로직
     * - messageType에 따라 적절한 핸들러 호출
     */
    static String routeMessage(String messageType, JsonObject data, String method) {
        if (messageType == null) {
            return errorResponse("messageType이 없습니다");
        }

        System.out.println("[" + method + "] 메시지 타입: " + messageType);

        // 메시지 타입별 분기 처리
        switch (messageType) {
            case "REGISTER":
                return handleRegister(data);

            case "MONITORING":
                return handleMonitoring(data);

            case "QUERY_PERFORMANCE":
                return handleQueryPerformance(data);

            case "QUERY_STATUS":
                return handleQueryStatus(data);

            case "LIST_MODELS":
                return handleListModels();

            case "DELETE_RECORD":
                return handleDeleteRecord(data);

            default:
                return errorResponse("알 수 없는 메시지 타입: " + messageType);
        }
    }

    /**
     * 핸들러 1: 에이전트 등록 (POST)
     * 요청: {"messageType":"REGISTER", "data":{"agentId":"agent010","modelName":"model1"}}
     * 응답: {"status":"success","message":"agent010 등록 완료"}
     */
    static String handleRegister(JsonObject data) {
        String agentId = data.get("agentId").getAsString();
        String modelName = data.get("modelName").getAsString();

        // 모델에 에이전트 추가
        Models.computeIfAbsent(modelName, k -> new HashSet<>()).add(agentId);

        Map<String, String> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", agentId + " 등록 완료 (모델: " + modelName + ")");

        return GSON.toJson(result);
    }

    /**
     * 핸들러 2: 모니터링 데이터 수집 (POST)
     * 요청: {"messageType":"MONITORING", "data":{...}}
     * 응답: {"status":"success","recordCount":15}
     */
    static String handleMonitoring(JsonObject data) {
        Records.add(data);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("recordCount", Records.size());

        return GSON.toJson(result);
    }

    /**
     * 핸들러 3: 성능 조회 (GET/POST)
     * GET: /api?messageType=QUERY_PERFORMANCE&modelName=model1&timeWindow=2025042812
     * POST: {"messageType":"QUERY_PERFORMANCE","data":{"modelName":"model1","timeWindow":"2025042812"}}
     * 응답: {"correct":10,"total":15}
     */
    static String handleQueryPerformance(JsonObject data) {
        String modelName = data.get("modelName").getAsString();
        String timeWindow = data.get("timeWindow").getAsString();

        Set<String> agents = Models.get(modelName);
        if (agents == null) {
            return errorResponse("모델을 찾을 수 없음: " + modelName);
        }

        Map<String, Integer> pred = new HashMap<>();
        Map<String, String> pTs = new HashMap<>();
        Map<String, Integer> act = new HashMap<>();

        for (JsonObject r : Records) {
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
        return GSON.toJson(result);
    }

    /**
     * 핸들러 4: 상태 조회 (GET)
     * GET: /api?messageType=QUERY_STATUS
     * 응답: {"status":"running","recordCount":15,"modelCount":3}
     */
    static String handleQueryStatus(JsonObject data) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "running");
        result.put("recordCount", Records.size());
        result.put("modelCount", Models.size());

        // agentId가 주어지면 해당 에이전트의 레코드 수만 반환
        if (data.has("agentId")) {
            String agentId = data.get("agentId").getAsString();
            long count = Records.stream()
                    .filter(r -> r.get("agentId").getAsString().equals(agentId))
                    .count();
            result.put("agentRecordCount", count);
        }

        return GSON.toJson(result);
    }

    /**
     * 핸들러 5: 모델 목록 조회 (GET)
     * GET: /api?messageType=LIST_MODELS
     * 응답: {"models":["model1","model2","model3"]}
     */
    static String handleListModels() {
        Map<String, Object> result = new HashMap<>();
        result.put("models", Models.keySet());
        return GSON.toJson(result);
    }

    /**
     * 핸들러 6: 레코드 삭제 (POST)
     * 요청: {"messageType":"DELETE_RECORD","data":{"agentId":"agent001","requestId":"req001"}}
     * 응답: {"status":"success","deletedCount":2}
     */
    static String handleDeleteRecord(JsonObject data) {
        String agentId = data.get("agentId").getAsString();
        String requestId = data.get("requestId").getAsString();

        // 해당 agentId와 requestId를 가진 레코드 삭제
        int initialSize = Records.size();
        Records.removeIf(r ->
            r.get("agentId").getAsString().equals(agentId) &&
            r.get("requestId").getAsString().equals(requestId)
        );
        int deletedCount = initialSize - Records.size();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("deletedCount", deletedCount);

        return GSON.toJson(result);
    }

    /**
     * Query Parameter 파싱
     * 예: "messageType=QUERY_STATUS&agentId=agent001"
     *  → {"messageType": "QUERY_STATUS", "agentId": "agent001"}
     */
    static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }

        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            }
        }
        return params;
    }

    /**
     * 에러 응답 생성
     */
    static String errorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        return GSON.toJson(error);
    }

    /**
     * HTTP 응답 전송
     */
    static void reply(HttpExchange ctx, String json) throws IOException {
        byte[] buf = json.getBytes(StandardCharsets.UTF_8);
        ctx.getResponseHeaders().set("Content-Type", "application/json");
        ctx.sendResponseHeaders(200, buf.length);
        ctx.getResponseBody().write(buf);
        ctx.close();
    }
}
