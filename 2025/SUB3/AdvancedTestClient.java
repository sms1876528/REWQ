import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

/**
 * ============================================================================
 * 고급 자료구조 서버 테스트 클라이언트
 * ============================================================================
 */
public class AdvancedTestClient {

    private static final String BASE_URL = "http://127.0.0.1:8080/api";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        System.out.println("=== 고급 자료구조 테스트 시작 ===\n");

        // 1. 데모: 모든 자료구조 상태
        testDemoAll();
        pause();

        // 2. 여러 데이터 추가
        testAddMultipleData();
        pause();

        // 3. 시간 범위 조회 (TreeMap)
        testTimeRangeQuery();
        pause();

        // 4. 최근 요청 조회 (Queue)
        testRecentRequests();
        pause();

        // 5. 높은 latency 조회 (PriorityQueue)
        testHighLatency();
        pause();

        // 6. 중복 체크 (Set)
        testDuplicateCheck();
        pause();

        // 7. 에이전트 통계 (ConcurrentHashMap)
        testAgentStats();
        pause();

        // 8. 비동기 작업 (BlockingQueue)
        testAsyncProcessing();
        pause();

        // 9. 최종 상태
        testDemoAll();

        System.out.println("\n=== 모든 테스트 완료 ===");
    }

    static void testDemoAll() throws Exception {
        System.out.println("[ 모든 자료구조 상태 ]");
        String json = """
            {
                "messageType": "DEMO_ALL"
            }
            """;
        String response = sendPost(json);
        System.out.println(response);
        System.out.println();
    }

    static void testAddMultipleData() throws Exception {
        System.out.println("[ 데이터 추가 (10건) ]");

        for (int i = 1; i <= 10; i++) {
            String requestId = String.format("req%03d", i);
            String timestamp = "2025042812" + String.format("%04d", i * 100);
            int latency = random.nextInt(200);  // 0~199ms

            String json = String.format("""
                {
                    "messageType": "ADD_DATA",
                    "data": {
                        "requestId": "%s",
                        "agentId": "agent00%d",
                        "timestamp": "%s",
                        "dataType": "P",
                        "dataValue": %d,
                        "latency": %d
                    }
                }
                """, requestId, (i % 3) + 1, timestamp, i * 10, latency);

            String response = sendPost(json);
            System.out.println(i + ". " + response);
            Thread.sleep(50);
        }
        System.out.println();
    }

    static void testTimeRangeQuery() throws Exception {
        System.out.println("[ 시간 범위 조회 (TreeMap) ]");
        String json = """
            {
                "messageType": "QUERY_TIME_RANGE",
                "data": {
                    "startTime": "20250428120000",
                    "endTime": "20250428120500"
                }
            }
            """;
        String response = sendPost(json);
        System.out.println(response);
        System.out.println();
    }

    static void testRecentRequests() throws Exception {
        System.out.println("[ 최근 요청 조회 (Queue) ]");
        String json = """
            {
                "messageType": "GET_RECENT"
            }
            """;
        String response = sendPost(json);
        System.out.println(response.substring(0, Math.min(500, response.length())) + "...");
        System.out.println();
    }

    static void testHighLatency() throws Exception {
        System.out.println("[ 높은 Latency 조회 (PriorityQueue) ]");
        String json = """
            {
                "messageType": "GET_HIGH_LATENCY",
                "data": {
                    "limit": 5
                }
            }
            """;
        String response = sendPost(json);
        System.out.println(response);
        System.out.println();
    }

    static void testDuplicateCheck() throws Exception {
        System.out.println("[ 중복 체크 (Set) ]");

        // 존재하는 ID
        String json1 = """
            {
                "messageType": "CHECK_DUPLICATE",
                "data": {
                    "requestId": "req001"
                }
            }
            """;
        System.out.println("1. " + sendPost(json1));

        // 존재하지 않는 ID
        String json2 = """
            {
                "messageType": "CHECK_DUPLICATE",
                "data": {
                    "requestId": "req999"
                }
            }
            """;
        System.out.println("2. " + sendPost(json2));
        System.out.println();
    }

    static void testAgentStats() throws Exception {
        System.out.println("[ 에이전트 통계 (ConcurrentHashMap) ]");
        String json = """
            {
                "messageType": "GET_AGENT_STATS"
            }
            """;
        String response = sendPost(json);
        System.out.println(response);
        System.out.println();
    }

    static void testAsyncProcessing() throws Exception {
        System.out.println("[ 비동기 작업 등록 (BlockingQueue) ]");

        for (int i = 1; i <= 5; i++) {
            String json = String.format("""
                {
                    "messageType": "PROCESS_ASYNC",
                    "data": {
                        "requestId": "async%03d",
                        "taskType": "heavy_computation"
                    }
                }
                """, i);
            String response = sendPost(json);
            System.out.println(i + ". " + response);
            Thread.sleep(50);
        }
        System.out.println();
    }

    static String sendPost(String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    static void pause() throws InterruptedException {
        Thread.sleep(1000);
    }
}
