import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * ============================================================================
 * 라우팅 서버 테스트 클라이언트
 * ============================================================================
 *
 * [학습 목표]
 * - 다양한 메시지 타입으로 요청 전송
 * - GET과 POST 요청 혼합 사용
 * - 응답 처리 및 검증
 *
 * [실행 순서]
 * 1. ProgramRouter 서버 실행
 * 2. 이 클라이언트 실행
 * 3. 각 메시지 타입별 응답 확인
 */
public class RouterTestClient {

    private static final String BASE_URL = "http://127.0.0.1:8080/api";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        System.out.println("=== 라우팅 서버 테스트 시작 ===\n");

        // 0. 초기 상태 조회 (GET)
        testQueryStatus();
        Thread.sleep(500);

        // 1. 에이전트 등록 (POST)
        testRegister();
        Thread.sleep(500);

        // 2. 모델 목록 조회 (GET)
        testListModels();
        Thread.sleep(500);

        // 3. 모니터링 데이터 전송 (POST) - 여러 건
        testMultipleMonitoring();
        Thread.sleep(500);

        // 4. 성능 조회 (POST)
        testQueryPerformancePost();
        Thread.sleep(500);

        // 5. 성능 조회 (GET)
        testQueryPerformanceGet();
        Thread.sleep(500);

        // 6. 특정 에이전트 상태 조회 (GET)
        testQueryAgentStatus();
        Thread.sleep(500);

        // 7. 레코드 삭제 (POST)
        testDeleteRecord();
        Thread.sleep(500);

        // 8. 최종 상태 확인 (GET)
        testQueryStatus();

        System.out.println("\n=== 모든 테스트 완료 ===");
    }

    /**
     * 테스트 1: 에이전트 등록 (POST)
     */
    static void testRegister() throws Exception {
        System.out.println("[ 테스트 1: 에이전트 등록 ]");

        String json = """
            {
                "messageType": "REGISTER",
                "data": {
                    "agentId": "agent010",
                    "modelName": "model1"
                }
            }
            """;

        String response = sendPost(json);
        System.out.println("응답: " + response);
        System.out.println();
    }

    /**
     * 테스트 2: 모델 목록 조회 (GET)
     */
    static void testListModels() throws Exception {
        System.out.println("[ 테스트 2: 모델 목록 조회 ]");

        String url = BASE_URL + "?messageType=LIST_MODELS";
        String response = sendGet(url);
        System.out.println("응답: " + response);
        System.out.println();
    }

    /**
     * 테스트 3: 여러 모니터링 데이터 전송 (POST)
     */
    static void testMultipleMonitoring() throws Exception {
        System.out.println("[ 테스트 3: 모니터링 데이터 전송 (5건) ]");

        // 예측 데이터 3건
        for (int i = 1; i <= 3; i++) {
            String json = String.format("""
                {
                    "messageType": "MONITORING",
                    "data": {
                        "agentId": "agent001",
                        "requestId": "req%03d",
                        "timestamp": "20250428120000",
                        "dataType": "P",
                        "dataValue": %d
                    }
                }
                """, i, i * 2);

            String response = sendPost(json);
            System.out.println("전송 " + i + ": " + response);
            Thread.sleep(100);
        }

        // 실제 데이터 2건
        for (int i = 1; i <= 2; i++) {
            String json = String.format("""
                {
                    "messageType": "MONITORING",
                    "data": {
                        "agentId": "agent001",
                        "requestId": "req%03d",
                        "timestamp": "20250428120100",
                        "dataType": "A",
                        "dataValue": %d
                    }
                }
                """, i, i * 2);

            String response = sendPost(json);
            System.out.println("전송 " + (i + 3) + ": " + response);
            Thread.sleep(100);
        }

        System.out.println();
    }

    /**
     * 테스트 4: 성능 조회 (POST)
     */
    static void testQueryPerformancePost() throws Exception {
        System.out.println("[ 테스트 4: 성능 조회 (POST) ]");

        String json = """
            {
                "messageType": "QUERY_PERFORMANCE",
                "data": {
                    "modelName": "model1",
                    "timeWindow": "2025042812"
                }
            }
            """;

        String response = sendPost(json);
        System.out.println("응답: " + response);
        System.out.println();
    }

    /**
     * 테스트 5: 성능 조회 (GET)
     */
    static void testQueryPerformanceGet() throws Exception {
        System.out.println("[ 테스트 5: 성능 조회 (GET) ]");

        String url = BASE_URL + "?messageType=QUERY_PERFORMANCE&modelName=model1&timeWindow=2025042812";
        String response = sendGet(url);
        System.out.println("응답: " + response);
        System.out.println();
    }

    /**
     * 테스트 6: 상태 조회 (GET)
     */
    static void testQueryStatus() throws Exception {
        System.out.println("[ 테스트 6: 전체 상태 조회 ]");

        String url = BASE_URL + "?messageType=QUERY_STATUS";
        String response = sendGet(url);
        System.out.println("응답: " + response);
        System.out.println();
    }

    /**
     * 테스트 7: 특정 에이전트 상태 조회 (GET)
     */
    static void testQueryAgentStatus() throws Exception {
        System.out.println("[ 테스트 7: 에이전트별 상태 조회 ]");

        String url = BASE_URL + "?messageType=QUERY_STATUS&agentId=agent001";
        String response = sendGet(url);
        System.out.println("응답: " + response);
        System.out.println();
    }

    /**
     * 테스트 8: 레코드 삭제 (POST)
     */
    static void testDeleteRecord() throws Exception {
        System.out.println("[ 테스트 8: 레코드 삭제 ]");

        String json = """
            {
                "messageType": "DELETE_RECORD",
                "data": {
                    "agentId": "agent001",
                    "requestId": "req001"
                }
            }
            """;

        String response = sendPost(json);
        System.out.println("응답: " + response);
        System.out.println();
    }

    /**
     * POST 요청 전송
     */
    static String sendPost(String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * GET 요청 전송
     */
    static String sendGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * ============================================================================
     * 추가 실습 과제
     * ============================================================================
     */

    /**
     * 실습 1: 에러 케이스 테스트
     * - 존재하지 않는 messageType 전송
     * - 필수 필드 누락
     * - 잘못된 JSON 형식
     */
    @SuppressWarnings("unused")
    static void practiceErrorCases() {
        System.out.println("[ 실습 1: 에러 케이스 테스트 ]");
        // TODO: 구현해보세요!
    }

    /**
     * 실습 2: 벤치마크 테스트
     * - 100개의 요청을 연속으로 전송
     * - 전체 소요 시간 측정
     * - 평균 응답 시간 계산
     */
    @SuppressWarnings("unused")
    static void practiceBenchmark() {
        System.out.println("[ 실습 2: 벤치마크 테스트 ]");
        // TODO: 구현해보세요!
        // 힌트: long start = System.currentTimeMillis();
    }

    /**
     * 실습 3: CSV 파일에서 데이터 읽어서 전송
     * - test_data.csv 파일 생성
     * - 각 줄을 읽어서 JSON으로 변환 후 전송
     */
    @SuppressWarnings("unused")
    static void practiceCsvImport() {
        System.out.println("[ 실습 3: CSV 데이터 임포트 ]");
        // TODO: 구현해보세요!
    }
}
