# Java HTTP/JSON 실습 가이드

## 📚 학습 로드맵

```
SUB1 (기초) → SUB2 (응용) → SUB3 (JSON/HTTP) → SUB4 (확장)
```

---

## 🎯 실습 환경 구성

### 1단계: 서버 실행하기

```bash
# SUB3 서버 실행 (단일 스레드)
cd SUB3
javac -cp ".:gson-2.10.1.jar" ProgramSingle.java
java -cp ".:gson-2.10.1.jar" ProgramSingle

# 또는 멀티 스레드 버전
java -cp ".:gson-2.10.1.jar" ProgramMulti
```

### 2단계: 클라이언트로 테스트

```bash
# 클라이언트 실행
javac HttpClientExample.java
java HttpClientExample
```

---

## 📖 핵심 개념 정리

### 1. JSON 처리 (Gson 라이브러리)

#### JSON 읽기 (파일 → Java 객체)
```java
// Map으로 변환
Gson gson = new Gson();
Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
Map<String, List<String>> data = gson.fromJson(jsonString, mapType);
```

#### JSON 파싱 (문자열 → JsonObject)
```java
JsonObject obj = JsonParser.parseString(jsonString).getAsJsonObject();
String value = obj.get("fieldName").getAsString();
int number = obj.get("count").getAsInt();
```

#### JSON 생성 (Java 객체 → JSON 문자열)
```java
Map<String, Integer> result = new HashMap<>();
result.put("correct", 10);
result.put("total", 15);
String json = gson.toJson(result);  // {"correct":10,"total":15}
```

---

### 2. HTTP 서버 (com.sun.net.httpserver)

#### 서버 생성 및 시작
```java
// 1. 서버 생성 (IP:포트)
HttpServer server = HttpServer.create(
    new InetSocketAddress("127.0.0.1", 8080), 0);

// 2. 엔드포인트 등록
server.createContext("/path", ClassName::handlerMethod);

// 3. 실행자 설정
server.setExecutor(null);  // 단일 스레드
// server.setExecutor(Executors.newCachedThreadPool());  // 멀티 스레드

// 4. 서버 시작
server.start();
```

#### 요청 처리 핸들러
```java
static void handleRequest(HttpExchange ctx) throws IOException {
    // 1. 요청 바디 읽기
    String body = new String(
        ctx.getRequestBody().readAllBytes(),
        StandardCharsets.UTF_8
    );

    // 2. JSON 파싱
    JsonObject json = JsonParser.parseString(body).getAsJsonObject();

    // 3. 비즈니스 로직 처리
    String result = processData(json);

    // 4. 응답 전송
    sendResponse(ctx, result);
}
```

#### 응답 전송
```java
static void sendResponse(HttpExchange ctx, String json) throws IOException {
    if (json != null) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ctx.getResponseHeaders().set("Content-Type", "application/json");
        ctx.sendResponseHeaders(200, bytes.length);
        ctx.getResponseBody().write(bytes);
    } else {
        ctx.sendResponseHeaders(200, -1);  // 바디 없음
    }
    ctx.close();
}
```

---

### 3. HTTP 클라이언트 (java.net.http)

#### POST 요청 (JSON 전송)
```java
// 1. 클라이언트 생성
HttpClient client = HttpClient.newHttpClient();

// 2. JSON 데이터 준비
String jsonBody = """
    {
        "field1": "value1",
        "field2": 123
    }
    """;

// 3. 요청 생성
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://127.0.0.1:8080/endpoint"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
    .build();

// 4. 요청 전송 및 응답 받기
HttpResponse<String> response = client.send(
    request,
    HttpResponse.BodyHandlers.ofString()
);

// 5. 응답 처리
int statusCode = response.statusCode();  // 200 = 성공
String responseBody = response.body();
```

#### GET 요청
```java
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://127.0.0.1:8080/endpoint"))
    .GET()
    .build();
```

---

## 🔥 실습 문제

### 난이도 1: 기본 (SUB3 기반)

**문제 1.1**: `/monitoring` 엔드포인트에 10개의 서로 다른 데이터 전송
- requestId: req001 ~ req010
- dataValue: 랜덤 (1~10)

**문제 1.2**: `/performance` 응답 JSON을 파싱하여 정확도 퍼센트 계산
- 예: correct=8, total=10 → "정확도: 80.0%"

**문제 1.3**: 새로운 엔드포인트 `/status` 추가 (GET)
- 서버 상태 정보를 JSON으로 반환
- 예: `{"status":"running","recordCount":15}`

---

### 난이도 2: 응용 (SUB4 기반)

**문제 2.1**: Latency 데이터 포함하여 전송 및 조회
- 모니터링 데이터에 latency 필드 추가
- 평균 latency가 100ms 이하인지 확인

**문제 2.2**: 여러 모델의 성능을 비교하는 클라이언트 작성
- model1, model2, model3의 성능을 조회
- 가장 정확도가 높은 모델 출력

**문제 2.3**: CSV 파일에서 데이터 읽어서 서버에 전송
- `data.csv` 형식: `requestId,timestamp,dataType,dataValue,latency`
- 각 줄을 JSON으로 변환하여 전송

---

### 난이도 3: 실전 (종합)

**문제 3.1**: 실시간 모니터링 대시보드
- 1초마다 랜덤 데이터를 서버에 전송
- 10초마다 성능 조회하여 콘솔에 출력
- Ctrl+C로 종료 시 최종 통계 출력

**문제 3.2**: 에러 처리 및 재시도 로직
- 서버 연결 실패 시 3회 재시도
- 각 재시도 사이에 1초 대기
- 3회 실패 시 에러 로그 파일 작성

**문제 3.3**: 멀티 스레드 클라이언트
- 10개 스레드가 동시에 요청 전송
- 모든 스레드 완료 후 총 소요 시간 측정
- Single vs Multi 서버 성능 비교

**문제 3.4**: RESTful API 확장
- PUT `/monitoring/{requestId}` : 데이터 수정
- DELETE `/monitoring/{requestId}` : 데이터 삭제
- GET `/monitoring/{requestId}` : 특정 데이터 조회

---

## 🛠️ 자주 사용하는 코드 패턴

### 패턴 1: 동적 JSON 생성
```java
String json = String.format("""
    {
        "requestId": "%s",
        "timestamp": "%s",
        "dataValue": %d
    }
    """, requestId, timestamp, value);
```

### 패턴 2: 여러 요청 순차 전송
```java
for (int i = 0; i < 10; i++) {
    String requestId = String.format("req%03d", i);
    int value = (int)(Math.random() * 10) + 1;

    String json = createJson(requestId, value);
    sendRequest(client, json);

    Thread.sleep(100);  // 0.1초 대기
}
```

### 패턴 3: 응답 에러 체크
```java
HttpResponse<String> response = client.send(request, ...);

if (response.statusCode() == 200) {
    System.out.println("성공: " + response.body());
} else {
    System.err.println("에러 " + response.statusCode());
}
```

### 패턴 4: Map → JSON → Map (라운드트립)
```java
// Java Map → JSON 문자열
Map<String, Object> data = new HashMap<>();
data.put("name", "test");
data.put("count", 5);
String json = gson.toJson(data);

// JSON 문자열 → Java Map
Type type = new TypeToken<Map<String, Object>>() {}.getType();
Map<String, Object> parsed = gson.fromJson(json, type);
```

---

## 📝 체크리스트

### 기본 개념 이해
- [ ] JSON 형식 이해 (객체, 배열, 문자열, 숫자)
- [ ] Gson으로 JSON 파싱/생성
- [ ] HTTP 메서드 이해 (GET, POST, PUT, DELETE)
- [ ] HTTP 상태 코드 이해 (200, 404, 500)

### 서버 구현
- [ ] HttpServer 생성 및 시작
- [ ] 엔드포인트 등록 (createContext)
- [ ] 요청 바디 읽기
- [ ] JSON 응답 전송

### 클라이언트 구현
- [ ] HttpClient로 POST 요청
- [ ] JSON 바디 전송
- [ ] 응답 받기 및 파싱
- [ ] 에러 처리

### 실전 응용
- [ ] 반복문으로 여러 요청 전송
- [ ] CSV/TXT 파일 읽어서 JSON 변환
- [ ] 멀티 스레드 환경에서 동시 요청
- [ ] 에러 처리 및 재시도 로직

---

## 🔍 디버깅 팁

### 1. JSON 형식 확인
```bash
# 온라인 JSON Validator 사용
https://jsonlint.com/

# 또는 jq 명령어 (Linux/Mac)
echo '{"key":"value"}' | jq
```

### 2. 서버 로그 추가
```java
System.out.println("[" + timestamp + "] 요청 받음: " + requestId);
```

### 3. 클라이언트 요청/응답 출력
```java
System.out.println(">>> 전송: " + jsonBody);
System.out.println("<<< 응답: " + response.body());
```

### 4. 서버 실행 확인
```bash
# 포트 사용 여부 확인
lsof -i :8080  # Mac/Linux
netstat -ano | findstr :8080  # Windows
```

---

## 📚 참고 자료

### 공식 문서
- [Gson 사용 가이드](https://github.com/google/gson/blob/master/UserGuide.md)
- [Java HttpServer API](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/com/sun/net/httpserver/package-summary.html)
- [Java HttpClient API](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/package-summary.html)

### 추천 학습 순서
1. SUB1, SUB2로 Java 기초 다지기
2. SUB3/ProgramSingle로 서버 구조 이해
3. HttpClientExample로 클라이언트 실습
4. 실습 문제 1.x 풀기
5. SUB3/ProgramMulti로 멀티 스레드 이해
6. SUB4로 확장된 기능 학습
7. 실습 문제 2.x, 3.x 도전

---

## ✨ 다음 단계

### 심화 학습
- Spring Boot로 REST API 개발
- 데이터베이스 연동 (JDBC, JPA)
- 비동기 처리 (CompletableFuture)
- 웹소켓 실시간 통신

### 실전 프로젝트 아이디어
- 날씨 API 클라이언트
- 간단한 채팅 서버/클라이언트
- 주식 시세 모니터링 시스템
- 로그 수집 및 분석 서버
