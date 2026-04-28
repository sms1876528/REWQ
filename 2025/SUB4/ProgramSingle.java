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
// 삽입 순서를 유지하는 LinkedHashMap (JSON 출력 순서 보장)
import java.util.LinkedHashMap;
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
 * 단계 4: 성능 메트릭 확장 (지연시간 측정) - 단일 스레드
 * ============================================================================
 *
 * SUB4 ProgramSingle - HTTP API 서버 (단일 스레드 버전 + 지연시간)
 *
 * [학습 목표]
 * - 집계 연산 (평균값 계산)
 * - 다중 메트릭 관리 (정확도 + 지연시간)
 * - 순서 보장 자료구조 (LinkedHashMap)
 * - 데이터 타입 변환 (long → int)
 *
 * [SUB3과의 차이점]
 * - SUB3: 정확도만 계산 → SUB4: 정확도 + 평균 지연시간
 * - SUB3: HashMap 1개 → SUB4: 예측값/지연시간 분리 저장 (predValue, predLatency)
 * - SUB3: HashMap → SUB4: LinkedHashMap (JSON 출력 순서 보장)
 *
 * [프로그램 목적]
 * - 모니터링 데이터를 수집하고 모델별 성능을 조회하는 HTTP API 서버
 * - SUB3와 유사하지만 지연시간(latency) 정보를 추가로 수집하고 반환
 * - 단일 스레드로 동작하여 동시성 문제가 발생하지 않음
 *
 * [제공 API]
 * 1. POST /monitoring: 모니터링 데이터 수집 (latency 포함)
 * 2. POST /performance: 모델별 성능 조회 (정확도 + 평균 지연시간)
 *
 * [동작 방식]
 * - MODELS.JSON에서 모델-에이전트 매핑 정보를 읽어옴
 * - HttpServer를 127.0.0.1:8080에서 실행
 * - setExecutor(null): 호출 스레드에서 직접 처리 (단일 스레드)
 *
 * [다음 단계로]
 * - ProgramMulti: 동일한 기능을 멀티 스레드 환경에서 안전하게 구현
 */
public class ProgramSingle {
    // 모델 이름 → 해당 모델의 에이전트 ID 집합을 매핑
    static Map<String, Set<String>> Models;

    // 모든 모니터링 레코드를 저장하는 리스트
    static List<JsonObject> Records = new ArrayList<>();

    // JSON 처리를 위한 Gson 객체
    static final Gson GSON = new Gson();

    // 프로그램의 시작점
    public static void main(String[] args) throws IOException {
        // MODELS.JSON 파일 읽기 및 파싱
        Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
        Map<String, List<String>> raw = GSON.fromJson(
                Files.readString(Path.of("MODELS.JSON")), mapType);

        // List를 Set으로 변환 (빠른 검색을 위해)
        Models = new HashMap<>();
        for (Map.Entry<String, List<String>> kv : raw.entrySet()) {
            Models.put(kv.getKey(), new HashSet<>(kv.getValue()));
        }

        // HTTP 서버 생성 (127.0.0.1:8080)
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);

        // 엔드포인트 등록
        server.createContext("/monitoring", ProgramSingle::handleMonitoring);
        server.createContext("/performance", ProgramSingle::handlePerformance);

        // null executor 설정: 단일 스레드로 동작
        server.setExecutor(null);

        // 서버 시작
        server.start();
    }

    /**
     * /monitoring 엔드포인트 핸들러
     * - 모니터링 데이터를 수집하여 Records 리스트에 저장
     *
     * @param ctx HTTP 요청/응답 컨텍스트
     */
    static void handleMonitoring(HttpExchange ctx) throws IOException {
        // 요청 바디를 UTF-8 문자열로 읽기
        String body = new String(ctx.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        // JSON을 파싱하고 Records 리스트에 추가
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

        // 성능 계산 수행
        String json = performance(q.get("modelName").getAsString(), q.get("timeWindow").getAsString());

        // 계산 결과를 JSON으로 응답
        reply(ctx, json);
    }

    /**
     * 성능 계산 메서드 (정확도 + 평균 지연시간)
     * - 특정 모델과 시간 윈도우에 대한 정확도와 평균 지연시간을 계산
     *
     * @param modelName 모델 이름
     * @param timeWindow 시간 윈도우 (yyyyMMddHH 형식)
     * @return JSON 형식의 결과 {"correct": 맞은개수, "total": 전체개수, "latency": 평균지연시간}
     */
    static String performance(String modelName, String timeWindow) {
        // 해당 모델에 속한 에이전트 ID 집합 가져오기
        Set<String> agents = Models.get(modelName);

        // 예측값, 지연시간, 타임스탬프, 실제값을 저장할 맵
        // 키: "에이전트ID|요청ID" 형식의 복합 키
        Map<String, Integer> predValue = new HashMap<>();    // 예측값
        Map<String, Integer> predLatency = new HashMap<>();  // 지연시간 (밀리초)
        Map<String, String> pTs = new HashMap<>();           // 예측 타임스탬프
        Map<String, Integer> act = new HashMap<>();          // 실제값

        // 모든 레코드를 순회하면서 해당 모델의 데이터만 필터링
        for (JsonObject r : Records) {
            // 레코드의 에이전트 ID 가져오기
            String aid = r.get("agentId").getAsString();
            // 이 에이전트가 현재 모델에 속하지 않으면 건너뛰기
            if (!agents.contains(aid)) continue;

            // 복합 키 생성: "에이전트ID|요청ID"
            String key = aid + "|" + r.get("requestId").getAsString();

            // 데이터 타입 확인
            String type = r.get("dataType").getAsString();
            if (type.equals("P")) {
                // 예측(P) 데이터인 경우
                predValue.put(key, r.get("dataValue").getAsInt());
                predLatency.put(key, r.get("latency").getAsInt());  // 지연시간 저장
                pTs.put(key, r.get("timestamp").getAsString());
            } else {
                // 실제(A) 데이터인 경우
                act.put(key, r.get("dataValue").getAsInt());
            }
        }

        // 정확도 및 평균 지연시간 계산
        int correct = 0;      // 맞은 개수
        int total = 0;        // 시간 윈도우에 속하는 전체 개수
        long latencySum = 0;  // 지연시간 합계 (평균 계산을 위해)

        // 모든 예측값을 순회
        for (Map.Entry<String, Integer> kv : predValue.entrySet()) {
            // 예측의 타임스탬프가 시간 윈도우에 포함되지 않으면 건너뛰기
            if (!pTs.get(kv.getKey()).startsWith(timeWindow)) continue;

            // 시간 윈도우에 포함되므로 total 증가
            total++;

            // 지연시간 누적 (평균 계산을 위해)
            latencySum += predLatency.get(kv.getKey());

            // 같은 키의 실제값 가져오기
            Integer a = act.get(kv.getKey());
            // 실제값이 존재하고 예측값과 일치하면 correct 증가
            if (a != null && a.equals(kv.getValue())) correct++;
        }

        // 평균 지연시간 계산
        // total > 0: 0으로 나누는 것 방지 (데이터가 없으면 0)
        // (int) 캐스팅: long을 int로 변환
        int latency = total > 0 ? (int) (latencySum / total) : 0;

        // 결과를 LinkedHashMap으로 구성 (순서 보장: correct, total, latency)
        // HashMap은 순서를 보장하지 않지만, LinkedHashMap은 삽입 순서를 유지
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("correct", correct);
        result.put("total", total);
        result.put("latency", latency);  // 평균 지연시간 추가

        // Map을 JSON 문자열로 변환하여 반환
        // 예: {"correct":15,"total":20,"latency":125}
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
            byte[] buf = json.getBytes(StandardCharsets.UTF_8);
            ctx.getResponseHeaders().set("Content-Type", "application/json");
            ctx.sendResponseHeaders(200, buf.length);
            ctx.getResponseBody().write(buf);
        } else {
            // JSON 응답이 없는 경우
            ctx.sendResponseHeaders(200, -1);
        }
        ctx.close();
    }
}
