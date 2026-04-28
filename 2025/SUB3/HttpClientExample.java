import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * ============================================================================
 * HTTP 클라이언트 예제 - SUB3/SUB4 서버와 통신하기
 * ============================================================================
 *
 * [학습 목표]
 * - HTTP 클라이언트 생성 및 요청 전송
 * - JSON 데이터를 HTTP 바디로 전송
 * - 응답 받아서 처리하기
 *
 * [사용 방법]
 * 1. SUB3/ProgramSingle.java 또는 ProgramMulti.java 서버 실행
 * 2. 이 클라이언트 실행
 * 3. 서버 응답 확인
 *
 * [실습 과제]
 * - 다양한 JSON 데이터로 요청 보내보기
 * - 응답 JSON을 파싱하여 특정 필드만 출력하기
 * - 반복문으로 여러 요청 보내기
 */
public class HttpClientExample {

    // 서버 주소 (SUB3/SUB4 서버)
    private static final String BASE_URL = "http://127.0.0.1:8080";

    public static void main(String[] args) throws IOException, InterruptedException {
        // HTTP 클라이언트 생성 (재사용 가능)
        HttpClient client = HttpClient.newHttpClient();

        System.out.println("=== HTTP 클라이언트 예제 시작 ===\n");

        // 예제 1: 모니터링 데이터 전송 (POST /monitoring)
        sendMonitoringData(client);

        // 예제 2: 성능 조회 (POST /performance)
        queryPerformance(client);

        System.out.println("\n=== 모든 예제 완료 ===");
    }

    /**
     * 예제 1: 모니터링 데이터를 서버에 전송
     * - JSON 형식으로 데이터 전송
     * - 응답 상태 코드 확인
     */
    private static void sendMonitoringData(HttpClient client) throws IOException, InterruptedException {
        System.out.println("[ 예제 1: 모니터링 데이터 전송 ]");

        // JSON 데이터 준비 (실제로는 동적으로 생성)
        // SUB3용 JSON 형식
        String jsonBody = """
            {
                "agentId": "agent001",
                "requestId": "req001",
                "timestamp": "20250428120000",
                "dataType": "P",
                "dataValue": 5
            }
            """;

        // 주석: SUB4용은 "latency" 필드 추가 필요
        // "latency": 100

        // HTTP POST 요청 생성
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/monitoring"))  // 엔드포인트 URL
                .header("Content-Type", "application/json") // JSON 타입 명시
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)) // 바디 설정
                .build();

        // 요청 전송 및 응답 받기
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 응답 정보 출력
        System.out.println("전송한 데이터:");
        System.out.println(jsonBody);
        System.out.println("응답 상태 코드: " + response.statusCode()); // 200이면 성공
        System.out.println("응답 바디: " + response.body());
        System.out.println();
    }

    /**
     * 예제 2: 성능 데이터를 서버에 조회
     * - JSON 요청을 보내고 JSON 응답 받기
     * - 응답 데이터 출력
     */
    private static void queryPerformance(HttpClient client) throws IOException, InterruptedException {
        System.out.println("[ 예제 2: 성능 데이터 조회 ]");

        // 조회 요청 JSON (modelName과 timeWindow 지정)
        String jsonBody = """
            {
                "modelName": "model1",
                "timeWindow": "2025042812"
            }
            """;

        // HTTP POST 요청 생성
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/performance"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // 요청 전송 및 응답 받기
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 응답 정보 출력
        System.out.println("조회 요청:");
        System.out.println(jsonBody);
        System.out.println("응답 상태 코드: " + response.statusCode());
        System.out.println("응답 JSON: " + response.body());

        // 실습: Gson을 사용하여 응답 JSON 파싱하기
        // 예: {"correct":10,"total":15,"latency":120}
        System.out.println("\n[실습 과제] 위 응답 JSON을 파싱하여 각 필드를 출력해보세요!");
        System.out.println();
    }

    /**
     * ============================================================================
     * 추가 실습 예제 (직접 구현해보세요!)
     * ============================================================================
     */

    /**
     * 실습 1: 여러 모니터링 데이터를 반복문으로 전송
     * - for문을 사용하여 10개의 데이터를 전송
     * - 각 데이터는 requestId를 다르게 설정 (req001, req002, ...)
     * - dataValue는 랜덤하게 생성
     */
    @SuppressWarnings("unused")
    private static void practiceMultipleSend(HttpClient client) {
        System.out.println("[ 실습 1: 여러 데이터 전송 ]");
        // TODO: 구현해보세요!
        // 힌트: for (int i = 0; i < 10; i++) { ... }
        // 힌트: String.format("req%03d", i) → "req001", "req002", ...
    }

    /**
     * 실습 2: 응답 JSON을 Gson으로 파싱하기
     * - Gson 라이브러리를 사용하여 응답 파싱
     * - correct, total, latency 필드를 각각 출력
     */
    @SuppressWarnings("unused")
    private static void practiceJsonParsing(String jsonResponse) {
        System.out.println("[ 실습 2: JSON 파싱 ]");
        // TODO: 구현해보세요!
        // 힌트: Gson gson = new Gson();
        // 힌트: JsonObject obj = JsonParser.parseString(jsonResponse).getAsJsonObject();
        // 힌트: int correct = obj.get("correct").getAsInt();
    }

    /**
     * 실습 3: 에러 처리 추가
     * - 서버가 실행되지 않은 경우 에러 처리
     * - HTTP 상태 코드가 200이 아닌 경우 에러 메시지 출력
     */
    @SuppressWarnings("unused")
    private static void practiceErrorHandling(HttpClient client) {
        System.out.println("[ 실습 3: 에러 처리 ]");
        // TODO: 구현해보세요!
        // 힌트: try-catch로 예외 처리
        // 힌트: if (response.statusCode() != 200) { ... }
    }

    /**
     * 실습 4: GET 요청 구현 (서버 수정 필요)
     * - 서버에 GET /status 엔드포인트 추가
     * - 클라이언트에서 GET 요청 전송
     */
    @SuppressWarnings("unused")
    private static void practiceGetRequest(HttpClient client) {
        System.out.println("[ 실습 4: GET 요청 ]");
        // TODO: 구현해보세요!
        // 힌트: .GET() 메서드 사용
        // 힌트: 서버에도 GET 핸들러 추가 필요
    }
}
