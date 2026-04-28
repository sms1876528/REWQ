// 파일 입출력을 위한 예외 처리 클래스
import java.io.IOException;
// 제네릭 타입 정보를 런타임에 유지하기 위한 클래스
import java.lang.reflect.Type;
// 소켓 주소(IP + 포트)를 표현하는 클래스
import java.net.InetSocketAddress;
// UTF-8 등 문자 인코딩을 위한 클래스
import java.nio.charset.StandardCharsets;
// 파일 읽기 기능을 제공하는 클래스
import java.nio.file.Files;
// 파일 경로를 다루는 클래스
import java.nio.file.Path;
// 동적 배열(크기가 가변적인 리스트)을 위한 클래스
import java.util.ArrayList;
// 해시맵(키-값 쌍 저장)을 위한 클래스
import java.util.HashMap;
// 중복을 허용하지 않는 집합 자료구조를 위한 클래스
import java.util.HashSet;
// 리스트 인터페이스
import java.util.List;
// Map 인터페이스
import java.util.Map;
// Set 인터페이스
import java.util.Set;

// HTTP 요청/응답을 처리하는 클래스 (Java 내장 HTTP 서버)
import com.sun.net.httpserver.HttpExchange;
// HTTP 서버를 생성하고 관리하는 클래스
import com.sun.net.httpserver.HttpServer;

// JSON 직렬화/역직렬화를 위한 Google Gson 라이브러리
import com.google.gson.Gson;
// JSON 객체를 표현하는 클래스
import com.google.gson.JsonObject;
// JSON 문자열을 파싱하는 클래스
import com.google.gson.JsonParser;
// 제네릭 타입 정보를 Gson에 전달하기 위한 클래스
import com.google.gson.reflect.TypeToken;

/**
 * ============================================================================
 * 단계 3: JSON 처리 + HTTP API 서버 (네트워크 통신) - 단일 스레드
 * ============================================================================
 *
 * SUB3 ProgramSingle - HTTP API 서버 (단일 스레드 버전)
 *
 * [학습 목표]
 * - JSON 파일 읽기 (MODELS.JSON, Gson 라이브러리)
 * - JSON 데이터 파싱 (TypeToken, JsonObject)
 * - HTTP 서버 구현 (com.sun.net.httpserver.HttpServer)
 * - RESTful API 엔드포인트 설계
 * - 메서드 참조 문법 (::)
 *
 * [SUB2와의 차이점]
 * - SUB2: TXT 파일 읽기 → SUB3: JSON 파일 + HTTP 요청으로 데이터 수신
 * - SUB2: 콘솔 입출력 → SUB3: HTTP 요청/응답 (네트워크 통신)
 * - SUB2: 단일 실행 → SUB3: 서버로 계속 실행되며 여러 요청 처리
 *
 * [프로그램 목적]
 * - 모니터링 데이터를 수집하고 모델별 성능을 조회하는 HTTP API 서버
 * - 단일 스레드로 동작하여 동시성 문제가 발생하지 않음
 *
 * [제공 API]
 * 1. POST /monitoring: 모니터링 데이터 수집 (JSON 요청 바디)
 * 2. POST /performance: 모델별 성능 조회 (정확도 반환)
 *
 * [동작 방식]
 * - MODELS.JSON에서 모델-에이전트 매핑 정보를 읽어옴
 * - HttpServer를 127.0.0.1:8080에서 실행
 * - setExecutor(null): 호출 스레드에서 직접 처리 (단일 스레드)
 *
 * [다음 단계로]
 * - ProgramMulti: 멀티 스레드 처리 (동시 요청 처리 + 스레드 안전성)
 * - SUB4: 성능 메트릭 추가 (latency 측정)
 */
public class ProgramSingle {
    // 모델 이름 → 해당 모델의 에이전트 ID 집합을 매핑
    // 예: {"ModelA" → {"Agent1", "Agent2"}, "ModelB" → {"Agent3"}}
    static Map<String, Set<String>> Models;

    // 모든 모니터링 레코드를 저장하는 리스트
    // 각 레코드는 JSON 객체로 저장됨
    static List<JsonObject> Records = new ArrayList<>();

    // JSON 처리를 위한 Gson 객체 (재사용)
    static final Gson GSON = new Gson();

    // 프로그램의 시작점
    public static void main(String[] args) throws IOException {
        // MODELS.JSON 파일 읽기 및 파싱
        // TypeToken: 제네릭 타입 정보를 런타임에 전달
        // Map<String, List<String>> 타입 정보를 Gson에 알려줌
        Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();

        // JSON 파일을 읽어서 Map으로 변환
        // Files.readString(): 파일 내용 전체를 문자열로 읽기
        // GSON.fromJson(): JSON 문자열을 Java 객체로 변환
        Map<String, List<String>> raw = GSON.fromJson(
                Files.readString(Path.of("MODELS.JSON")), mapType);

        // List를 Set으로 변환 (빠른 검색을 위해)
        // List는 순차 검색 O(n), Set은 해시 검색 O(1)
        Models = new HashMap<>();
        for (Map.Entry<String, List<String>> kv : raw.entrySet()) {
            // List<String>을 HashSet<String>으로 변환
            Models.put(kv.getKey(), new HashSet<>(kv.getValue()));
        }

        // HTTP 서버 생성
        // InetSocketAddress("127.0.0.1", 8080): 로컬호스트 8080 포트
        // 두 번째 인자 0: 기본 백로그(대기 큐) 크기 사용
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);

        // 엔드포인트 등록
        // "/monitoring" 경로로 들어오는 요청은 handleMonitoring 메서드로 처리
        server.createContext("/monitoring", ProgramSingle::handleMonitoring);
        // "/performance" 경로로 들어오는 요청은 handlePerformance 메서드로 처리
        server.createContext("/performance", ProgramSingle::handlePerformance);

        // null executor 설정: 요청을 별도 스레드가 아닌 호출 스레드에서 처리
        // → 단일 스레드로 동작하므로 동기화(synchronized) 불필요
        server.setExecutor(null);

        // 서버 시작 (블로킹되지 않고 백그라운드에서 실행됨)
        server.start();
    }

    /**
     * /monitoring 엔드포인트 핸들러
     * - 모니터링 데이터를 수집하여 Records 리스트에 저장
     *
     * @param ctx HTTP 요청/응답 컨텍스트
     */
    static void handleMonitoring(HttpExchange ctx) throws IOException {
        // 요청 바디를 바이트 배열로 읽고 UTF-8 문자열로 변환
        String body = new String(ctx.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        // JSON 문자열을 JsonObject로 파싱하고 Records 리스트에 추가
        Records.add(JsonParser.parseString(body).getAsJsonObject());

        // 응답 전송 (바디 없음)
        reply(ctx, null);
    }

    /**
     * /performance 엔드포인트 핸들러
     * - 특정 모델과 시간 윈도우에 대한 성능 통계를 반환
     *
     * @param ctx HTTP 요청/응답 컨텍스트
     */
    static void handlePerformance(HttpExchange ctx) throws IOException {
        // 요청 바디를 JSON으로 파싱
        String body = new String(ctx.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject q = JsonParser.parseString(body).getAsJsonObject();

        // 요청에서 modelName과 timeWindow 추출하여 성능 계산
        String json = performance(q.get("modelName").getAsString(), q.get("timeWindow").getAsString());

        // 계산 결과를 JSON으로 응답
        reply(ctx, json);
    }

    /**
     * 성능 계산 메서드
     * - 특정 모델과 시간 윈도우에 대한 정확도를 계산
     *
     * @param modelName 모델 이름
     * @param timeWindow 시간 윈도우 (yyyyMMddHH 형식)
     * @return JSON 형식의 결과 {"correct": 맞은개수, "total": 전체개수}
     */
    static String performance(String modelName, String timeWindow) {
        // 해당 모델에 속한 에이전트 ID 집합 가져오기
        Set<String> agents = Models.get(modelName);

        // 예측값, 타임스탬프, 실제값을 저장할 맵
        // 키: "에이전트ID|요청ID" 형식의 복합 키
        Map<String, Integer> pred = new HashMap<>();  // 예측값 저장
        Map<String, String> pTs = new HashMap<>();    // 예측 타임스탬프 저장
        Map<String, Integer> act = new HashMap<>();   // 실제값 저장

        // 모든 레코드를 순회하면서 해당 모델의 데이터만 필터링
        for (JsonObject r : Records) {
            // 레코드의 에이전트 ID 가져오기
            String aid = r.get("agentId").getAsString();
            // 이 에이전트가 현재 모델에 속하지 않으면 건너뛰기
            if (!agents.contains(aid)) continue;

            // 복합 키 생성: "에이전트ID|요청ID"
            // 동일한 요청ID라도 다른 에이전트는 구분됨
            String key = aid + "|" + r.get("requestId").getAsString();

            // 데이터 타입 확인
            String type = r.get("dataType").getAsString();
            if (type.equals("P")) {
                // 예측(P) 데이터인 경우
                pred.put(key, r.get("dataValue").getAsInt());
                pTs.put(key, r.get("timestamp").getAsString());
            } else {
                // 실제(A) 데이터인 경우
                act.put(key, r.get("dataValue").getAsInt());
            }
        }

        // 정확도 계산
        int correct = 0;  // 맞은 개수
        int total = 0;    // 시간 윈도우에 속하는 전체 개수

        // 모든 예측값을 순회
        for (Map.Entry<String, Integer> kv : pred.entrySet()) {
            // 예측의 타임스탬프가 시간 윈도우에 포함되지 않으면 건너뛰기
            if (!pTs.get(kv.getKey()).startsWith(timeWindow)) continue;

            // 시간 윈도우에 포함되므로 total 증가
            total++;

            // 같은 키의 실제값 가져오기
            Integer a = act.get(kv.getKey());
            // 실제값이 존재하고 예측값과 일치하면 correct 증가
            if (a != null && a.equals(kv.getValue())) correct++;
        }

        // 결과를 Map으로 구성
        Map<String, Integer> result = new HashMap<>();
        result.put("correct", correct);
        result.put("total", total);

        // Map을 JSON 문자열로 변환하여 반환
        // 예: {"correct":15,"total":20}
        return GSON.toJson(result);
    }

    /**
     * HTTP 응답 전송 메서드
     *
     * @param ctx HTTP 컨텍스트
     * @param json 응답 바디로 전송할 JSON 문자열 (null이면 바디 없음)
     */
    static void reply(HttpExchange ctx, String json) throws IOException {
        if (json != null) {
            // JSON 응답이 있는 경우
            // 문자열을 UTF-8 바이트 배열로 변환
            byte[] buf = json.getBytes(StandardCharsets.UTF_8);

            // Content-Type 헤더 설정
            ctx.getResponseHeaders().set("Content-Type", "application/json");

            // 응답 헤더 전송 (200 OK, 바디 길이 지정)
            ctx.sendResponseHeaders(200, buf.length);

            // 응답 바디 전송
            ctx.getResponseBody().write(buf);
        } else {
            // JSON 응답이 없는 경우
            // -1: 응답 바디 없음
            ctx.sendResponseHeaders(200, -1);
        }

        // HTTP 연결 종료
        ctx.close();
    }
}
