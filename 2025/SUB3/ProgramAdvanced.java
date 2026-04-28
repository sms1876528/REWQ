import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/**
 * ============================================================================
 * 고급: 다양한 자료구조 활용 서버
 * ============================================================================
 *
 * [학습 목표]
 * - Map, Queue, Set, List 등 다양한 자료구조 실전 활용
 * - TreeMap, PriorityQueue, LinkedHashMap 등 특수 자료구조
 * - 자료구조 선택 기준 이해
 * - 실시간 데이터 스트림 처리
 *
 * [사용하는 자료구조]
 * 1. HashMap<K, V>         : 빠른 검색 (O(1))
 * 2. LinkedHashMap<K, V>   : 순서 유지 + 빠른 검색
 * 3. TreeMap<K, V>         : 정렬된 Map (O(log n))
 * 4. ConcurrentHashMap     : 스레드 안전 Map
 * 5. Queue (LinkedList)    : FIFO 큐
 * 6. PriorityQueue         : 우선순위 큐
 * 7. Deque (ArrayDeque)    : 양방향 큐
 * 8. HashSet               : 중복 제거
 * 9. TreeSet               : 정렬된 Set
 * 10. BlockingQueue        : 생산자-소비자 패턴
 */
public class ProgramAdvanced {

    // ========== 1. 기본 저장소 (HashMap) ==========
    // 모델 → 에이전트 매핑
    static Map<String, Set<String>> modelAgents = new HashMap<>();

    // ========== 2. 시계열 데이터 (TreeMap - 자동 정렬) ==========
    // 타임스탬프 → 데이터 리스트 (시간 순서대로 자동 정렬)
    static TreeMap<String, List<JsonObject>> timeSeriesData = new TreeMap<>();

    // ========== 3. 최근 요청 큐 (Queue - FIFO) ==========
    // 최근 100개 요청만 유지
    static Queue<JsonObject> recentRequests = new LinkedList<>();
    static final int MAX_RECENT = 100;

    // ========== 4. 우선순위 처리 (PriorityQueue) ==========
    // latency가 높은 것부터 처리 (내림차순)
    static PriorityQueue<JsonObject> highLatencyQueue = new PriorityQueue<>(
        (a, b) -> {
            int latencyA = a.has("latency") ? a.get("latency").getAsInt() : 0;
            int latencyB = b.has("latency") ? b.get("latency").getAsInt() : 0;
            return Integer.compare(latencyB, latencyA); // 내림차순
        }
    );

    // ========== 5. 중복 제거 (Set) ==========
    // 처리된 requestId 추적 (중복 요청 방지)
    static Set<String> processedRequestIds = new HashSet<>();

    // ========== 6. 순서 보장 캐시 (LinkedHashMap - LRU) ==========
    // 최근 조회된 성능 데이터 캐싱 (LRU: Least Recently Used)
    static Map<String, String> performanceCache = new LinkedHashMap<String, String>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 50; // 최대 50개만 유지
        }
    };

    // ========== 7. 동시성 처리 (ConcurrentHashMap) ==========
    // 멀티스레드 환경에서 안전한 카운터
    static ConcurrentHashMap<String, Integer> agentRequestCount = new ConcurrentHashMap<>();

    // ========== 8. 양방향 큐 (Deque) ==========
    // 처리 중인 작업 (앞에서 추가, 뒤에서 제거)
    static Deque<JsonObject> processingQueue = new ArrayDeque<>();

    // ========== 9. 에러 로그 (TreeSet - 정렬) ==========
    // 타임스탬프 순서로 에러 로그 저장
    static TreeSet<String> errorLogs = new TreeSet<>();

    // ========== 10. 블로킹 큐 (생산자-소비자) ==========
    // 비동기 처리를 위한 작업 큐
    static BlockingQueue<JsonObject> asyncTaskQueue = new LinkedBlockingQueue<>(1000);

    static final Gson GSON = new Gson();

    public static void main(String[] args) throws IOException {
        // MODELS.JSON 로드
        loadModels();

        // 비동기 작업 처리 스레드 시작
        startAsyncWorker();

        // HTTP 서버 시작
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);

        // 엔드포인트 등록
        server.createContext("/api", ProgramAdvanced::handleRequest);

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("=== 고급 자료구조 서버 시작 ===");
        System.out.println("엔드포인트: http://127.0.0.1:8080/api");
        printDataStructureInfo();
    }

    /**
     * 통합 요청 핸들러
     */
    static void handleRequest(HttpExchange ctx) throws IOException {
        String method = ctx.getRequestMethod();
        String body = new String(ctx.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        if (body.isEmpty()) {
            reply(ctx, errorResponse("요청 바디가 비어있습니다"));
            return;
        }

        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        String messageType = json.get("messageType").getAsString();

        String response = switch (messageType) {
            case "ADD_DATA" -> handleAddData(json);
            case "QUERY_TIME_RANGE" -> handleQueryTimeRange(json);
            case "GET_RECENT" -> handleGetRecent();
            case "GET_HIGH_LATENCY" -> handleGetHighLatency(json);
            case "CHECK_DUPLICATE" -> handleCheckDuplicate(json);
            case "GET_CACHE_STATS" -> handleGetCacheStats();
            case "GET_AGENT_STATS" -> handleGetAgentStats();
            case "PROCESS_ASYNC" -> handleProcessAsync(json);
            case "GET_ERRORS" -> handleGetErrors();
            case "DEMO_ALL" -> handleDemoAll();
            default -> errorResponse("알 수 없는 타입: " + messageType);
        };

        reply(ctx, response);
    }

    /**
     * 1. 데이터 추가 (여러 자료구조에 동시 저장)
     */
    static String handleAddData(JsonObject request) {
        JsonObject data = request.getAsJsonObject("data");

        String requestId = data.get("requestId").getAsString();
        String agentId = data.get("agentId").getAsString();
        String timestamp = data.get("timestamp").getAsString();

        // 1. 중복 체크 (Set 활용)
        if (processedRequestIds.contains(requestId)) {
            return errorResponse("중복된 requestId: " + requestId);
        }
        processedRequestIds.add(requestId);

        // 2. 시계열 데이터 저장 (TreeMap - 자동 정렬)
        timeSeriesData.computeIfAbsent(timestamp, k -> new ArrayList<>()).add(data);

        // 3. 최근 요청 큐 업데이트 (Queue - FIFO)
        recentRequests.offer(data);
        if (recentRequests.size() > MAX_RECENT) {
            recentRequests.poll(); // 가장 오래된 것 제거
        }

        // 4. 높은 latency면 우선순위 큐에 추가
        if (data.has("latency") && data.get("latency").getAsInt() > 100) {
            highLatencyQueue.offer(data);
        }

        // 5. 에이전트별 요청 수 카운트 (ConcurrentHashMap)
        agentRequestCount.merge(agentId, 1, Integer::sum);

        // 6. 처리 큐에 추가 (Deque)
        processingQueue.addLast(data);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("requestId", requestId);
        result.put("totalProcessed", processedRequestIds.size());

        return GSON.toJson(result);
    }

    /**
     * 2. 시간 범위 조회 (TreeMap 활용)
     * TreeMap.subMap()으로 특정 시간 범위만 효율적으로 추출
     */
    static String handleQueryTimeRange(JsonObject request) {
        JsonObject params = request.getAsJsonObject("data");
        String startTime = params.get("startTime").getAsString();
        String endTime = params.get("endTime").getAsString();

        // TreeMap.subMap: startTime <= key < endTime 범위 추출 (O(log n))
        Map<String, List<JsonObject>> rangeData = timeSeriesData.subMap(startTime, true, endTime, false);

        // 전체 데이터 수 계산
        int totalCount = rangeData.values().stream()
                .mapToInt(List::size)
                .sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("dataPoints", rangeData.size());
        result.put("totalRecords", totalCount);
        result.put("data", rangeData);

        return GSON.toJson(result);
    }

    /**
     * 3. 최근 N개 요청 조회 (Queue 활용)
     */
    static String handleGetRecent() {
        List<JsonObject> recent = new ArrayList<>(recentRequests);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", recent.size());
        result.put("maxSize", MAX_RECENT);
        result.put("requests", recent);

        return GSON.toJson(result);
    }

    /**
     * 4. 높은 latency 요청 조회 (PriorityQueue 활용)
     */
    static String handleGetHighLatency(JsonObject request) {
        JsonObject params = request.getAsJsonObject("data");
        int limit = params.has("limit") ? params.get("limit").getAsInt() : 10;

        // PriorityQueue에서 상위 N개 추출 (복사본 생성)
        List<JsonObject> highLatency = new ArrayList<>();
        PriorityQueue<JsonObject> copy = new PriorityQueue<>(highLatencyQueue);

        for (int i = 0; i < limit && !copy.isEmpty(); i++) {
            highLatency.add(copy.poll());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", highLatency.size());
        result.put("requests", highLatency);

        return GSON.toJson(result);
    }

    /**
     * 5. 중복 체크 (Set 활용)
     */
    static String handleCheckDuplicate(JsonObject request) {
        JsonObject params = request.getAsJsonObject("data");
        String requestId = params.get("requestId").getAsString();

        boolean isDuplicate = processedRequestIds.contains(requestId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", requestId);
        result.put("isDuplicate", isDuplicate);
        result.put("totalProcessed", processedRequestIds.size());

        return GSON.toJson(result);
    }

    /**
     * 6. 캐시 통계 (LinkedHashMap - LRU 활용)
     */
    static String handleGetCacheStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cacheSize", performanceCache.size());
        result.put("maxCacheSize", 50);
        result.put("cachedKeys", performanceCache.keySet());

        return GSON.toJson(result);
    }

    /**
     * 7. 에이전트별 통계 (ConcurrentHashMap 활용)
     */
    static String handleGetAgentStats() {
        // 요청 수 기준 내림차순 정렬
        List<Map.Entry<String, Integer>> sorted = agentRequestCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAgents", sorted.size());
        result.put("totalRequests", sorted.stream().mapToInt(Map.Entry::getValue).sum());
        result.put("topAgents", sorted.stream()
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new)));

        return GSON.toJson(result);
    }

    /**
     * 8. 비동기 작업 등록 (BlockingQueue 활용)
     */
    static String handleProcessAsync(JsonObject request) {
        JsonObject data = request.getAsJsonObject("data");

        try {
            asyncTaskQueue.put(data); // 큐에 추가 (블로킹)
        } catch (InterruptedException e) {
            return errorResponse("작업 큐 추가 실패");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "queued");
        result.put("queueSize", asyncTaskQueue.size());

        return GSON.toJson(result);
    }

    /**
     * 9. 에러 로그 조회 (TreeSet - 자동 정렬)
     */
    static String handleGetErrors() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("errorCount", errorLogs.size());
        result.put("errors", errorLogs);

        return GSON.toJson(result);
    }

    /**
     * 10. 모든 자료구조 상태 출력 (데모)
     */
    static String handleDemoAll() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("HashMap_modelAgents", modelAgents.size() + " models");
        result.put("TreeMap_timeSeriesData", timeSeriesData.size() + " timestamps");
        result.put("Queue_recentRequests", recentRequests.size() + "/" + MAX_RECENT);
        result.put("PriorityQueue_highLatency", highLatencyQueue.size() + " items");
        result.put("Set_processedIds", processedRequestIds.size() + " unique IDs");
        result.put("LinkedHashMap_cache", performanceCache.size() + "/50");
        result.put("ConcurrentHashMap_agentCount", agentRequestCount.size() + " agents");
        result.put("Deque_processingQueue", processingQueue.size() + " items");
        result.put("TreeSet_errorLogs", errorLogs.size() + " errors");
        result.put("BlockingQueue_asyncTasks", asyncTaskQueue.size() + "/1000");

        return GSON.toJson(result);
    }

    /**
     * 비동기 작업 처리 워커 스레드
     */
    static void startAsyncWorker() {
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    JsonObject task = asyncTaskQueue.take(); // 블로킹 대기
                    // 실제 작업 처리 시뮬레이션
                    Thread.sleep(100);
                    System.out.println("[ASYNC] 작업 완료: " + task.get("requestId").getAsString());
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * MODELS.JSON 로드
     */
    static void loadModels() throws IOException {
        Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
        Map<String, List<String>> raw = GSON.fromJson(
                Files.readString(Path.of("MODELS.JSON")), mapType);
        for (Map.Entry<String, List<String>> kv : raw.entrySet()) {
            modelAgents.put(kv.getKey(), new HashSet<>(kv.getValue()));
        }
    }

    /**
     * 자료구조 정보 출력
     */
    static void printDataStructureInfo() {
        System.out.println("\n[사용 중인 자료구조]");
        System.out.println("1. HashMap           : 모델-에이전트 매핑");
        System.out.println("2. TreeMap           : 시계열 데이터 (자동 정렬)");
        System.out.println("3. Queue             : 최근 요청 100개 (FIFO)");
        System.out.println("4. PriorityQueue     : 높은 latency 우선 처리");
        System.out.println("5. Set               : 중복 requestId 방지");
        System.out.println("6. LinkedHashMap     : LRU 캐시 (최대 50개)");
        System.out.println("7. ConcurrentHashMap : 에이전트별 요청 수");
        System.out.println("8. Deque             : 처리 큐 (양방향)");
        System.out.println("9. TreeSet           : 에러 로그 (정렬)");
        System.out.println("10. BlockingQueue    : 비동기 작업 큐\n");
    }

    static String errorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        return GSON.toJson(error);
    }

    static void reply(HttpExchange ctx, String json) throws IOException {
        byte[] buf = json.getBytes(StandardCharsets.UTF_8);
        ctx.getResponseHeaders().set("Content-Type", "application/json");
        ctx.sendResponseHeaders(200, buf.length);
        ctx.getResponseBody().write(buf);
        ctx.close();
    }
}
