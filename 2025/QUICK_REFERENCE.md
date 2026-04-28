# 빠른 참조 가이드 (Quick Reference)

## 🚀 자주 사용하는 코드 스니펫

### JSON 처리

#### 1. JSON 파일 읽기
```java
import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;

String jsonString = Files.readString(Path.of("data.json"));
```

#### 2. JSON → Java 객체
```java
// Map으로 변환
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
Map<String, List<String>> map = gson.fromJson(jsonString, mapType);

// JsonObject로 파싱
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

JsonObject obj = JsonParser.parseString(jsonString).getAsJsonObject();
String name = obj.get("name").getAsString();
int age = obj.get("age").getAsInt();
```

#### 3. Java 객체 → JSON
```java
import com.google.gson.Gson;

Gson gson = new Gson();
Map<String, Integer> data = new HashMap<>();
data.put("count", 10);
String json = gson.toJson(data);  // {"count":10}
```

---

### HTTP 서버

#### 1. 서버 생성 및 시작
```java
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

HttpServer server = HttpServer.create(
    new InetSocketAddress("127.0.0.1", 8080), 0
);

server.createContext("/path", ClassName::handlerMethod);
server.setExecutor(null);  // 단일 스레드
server.start();
```

#### 2. POST 요청 바디 읽기
```java
import com.sun.net.httpserver.HttpExchange;
import java.nio.charset.StandardCharsets;

static void handler(HttpExchange ctx) throws IOException {
    String body = new String(
        ctx.getRequestBody().readAllBytes(),
        StandardCharsets.UTF_8
    );

    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
    // 데이터 처리...
}
```

#### 3. JSON 응답 전송
```java
static void reply(HttpExchange ctx, String json) throws IOException {
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    ctx.getResponseHeaders().set("Content-Type", "application/json");
    ctx.sendResponseHeaders(200, bytes.length);
    ctx.getResponseBody().write(bytes);
    ctx.close();
}
```

---

### HTTP 클라이언트

#### 1. POST 요청 (JSON 전송)
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();

String jsonBody = "{\"key\":\"value\"}";

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://127.0.0.1:8080/endpoint"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
    .build();

HttpResponse<String> response = client.send(
    request,
    HttpResponse.BodyHandlers.ofString()
);

System.out.println("상태: " + response.statusCode());
System.out.println("응답: " + response.body());
```

#### 2. GET 요청
```java
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://127.0.0.1:8080/endpoint"))
    .GET()
    .build();
```

---

### 자주 사용하는 패턴

#### 1. 파일 읽기 (라인별)
```java
import java.nio.file.*;

List<String> lines = Files.readAllLines(Path.of("file.txt"));
for (String line : lines) {
    // 처리...
}
```

#### 2. 문자열 분리
```java
String line = "a#b#c#d";
String[] parts = line.split("#", -1);  // -1: 빈 문자열 포함
```

#### 3. 반복문 with 포맷팅
```java
for (int i = 1; i <= 10; i++) {
    String id = String.format("req%03d", i);  // req001, req002, ...
}
```

#### 4. HashMap 순회
```java
Map<String, Integer> map = new HashMap<>();

for (Map.Entry<String, Integer> entry : map.entrySet()) {
    String key = entry.getKey();
    Integer value = entry.getValue();
}
```

#### 5. 멀티 스레드 실행자
```java
import java.util.concurrent.Executors;

server.setExecutor(Executors.newCachedThreadPool());
```

#### 6. synchronized (스레드 안전)
```java
private final Object lock = new Object();

synchronized (lock) {
    // 크리티컬 섹션 (한 번에 한 스레드만 접근)
    list.add(item);
}
```

---

## ⚡ 컴파일 & 실행

### Gson 라이브러리 포함
```bash
# 컴파일
javac -cp ".:gson-2.10.1.jar" Program.java

# 실행
java -cp ".:gson-2.10.1.jar" Program

# Windows
javac -cp ".;gson-2.10.1.jar" Program.java
java -cp ".;gson-2.10.1.jar" Program
```

### 여러 파일 컴파일
```bash
javac -cp ".:gson-2.10.1.jar" *.java
```

---

## 🐛 디버깅 체크리스트

### JSON 관련
- [ ] JSON 형식이 올바른가? (중괄호, 따옴표, 쉼표)
- [ ] 필드 이름이 정확한가?
- [ ] 데이터 타입이 맞는가? (문자열 vs 숫자)

### HTTP 관련
- [ ] 서버가 실행 중인가?
- [ ] URL이 정확한가? (http://127.0.0.1:8080)
- [ ] Content-Type 헤더가 설정되었는가?
- [ ] 상태 코드가 200인가?

### 멀티 스레드 관련
- [ ] 공유 변수에 synchronized 사용했는가?
- [ ] 스냅샷을 사용하여 반복 중인가?
- [ ] 불변 객체와 가변 객체를 구분했는가?

---

## 📋 문제 해결 패턴

### 패턴 1: 파일 읽기 → 파싱 → 저장
```java
// 1. 파일 읽기
List<String> lines = Files.readAllLines(Path.of("data.txt"));

// 2. 파싱 및 저장
Map<String, Integer> map = new HashMap<>();
for (String line : lines) {
    String[] parts = line.split("#");
    map.put(parts[0], Integer.parseInt(parts[1]));
}

// 3. 처리
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
```

### 패턴 2: JSON 수신 → 처리 → JSON 응답
```java
// 1. JSON 수신
String requestBody = new String(ctx.getRequestBody().readAllBytes(), UTF_8);
JsonObject req = JsonParser.parseString(requestBody).getAsJsonObject();

// 2. 처리
String modelName = req.get("modelName").getAsString();
int result = processData(modelName);

// 3. JSON 응답
Map<String, Integer> response = new HashMap<>();
response.put("result", result);
String json = gson.toJson(response);

reply(ctx, json);
```

### 패턴 3: 여러 요청 전송 → 결과 수집
```java
List<Integer> results = new ArrayList<>();

for (int i = 0; i < 10; i++) {
    String json = createRequest(i);
    HttpResponse<String> response = sendRequest(client, json);

    JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
    results.add(obj.get("value").getAsInt());
}

// 결과 집계
int sum = results.stream().mapToInt(Integer::intValue).sum();
double avg = sum / (double) results.size();
```

---

## 💡 실전 팁

### 1. JSON 디버깅
```java
// Pretty print
Gson gson = new GsonBuilder().setPrettyPrinting().create();
System.out.println(gson.toJson(object));
```

### 2. 시간 측정
```java
long start = System.currentTimeMillis();
// 작업 수행...
long elapsed = System.currentTimeMillis() - start;
System.out.println("소요 시간: " + elapsed + "ms");
```

### 3. 재시도 로직
```java
int maxRetries = 3;
for (int i = 0; i < maxRetries; i++) {
    try {
        HttpResponse<String> response = client.send(request, ...);
        if (response.statusCode() == 200) {
            break;  // 성공
        }
    } catch (IOException e) {
        if (i == maxRetries - 1) throw e;  // 마지막 시도 실패
        Thread.sleep(1000);  // 1초 대기
    }
}
```

### 4. 로그 파일 작성
```java
import java.io.FileWriter;
import java.time.LocalDateTime;

try (FileWriter writer = new FileWriter("log.txt", true)) {
    String log = LocalDateTime.now() + " - " + message + "\n";
    writer.write(log);
}
```

---

## 🔗 변형 문제 대응 가이드

### 요청 형식이 바뀐 경우
```java
// 기존: {"modelName":"model1","timeWindow":"2025042812"}
// 변경: {"model":"model1","window":"2025042812"}

// 수정 전
String modelName = req.get("modelName").getAsString();

// 수정 후
String modelName = req.get("model").getAsString();
```

### 응답 형식에 필드 추가
```java
// 기존: {"correct":10,"total":15}
// 변경: {"correct":10,"total":15,"accuracy":66.7}

Map<String, Object> result = new LinkedHashMap<>();
result.put("correct", correct);
result.put("total", total);
result.put("accuracy", (double)correct / total * 100);
```

### 새로운 엔드포인트 추가
```java
// 1. 서버에 핸들러 등록
server.createContext("/newEndpoint", ClassName::handleNew);

// 2. 핸들러 구현
static void handleNew(HttpExchange ctx) throws IOException {
    // 처리 로직...
    reply(ctx, jsonResponse);
}

// 3. 클라이언트에서 호출
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(BASE_URL + "/newEndpoint"))
    .POST(...)
    .build();
```

### CSV → JSON 변환
```java
// CSV: requestId,timestamp,dataValue
// JSON: {"requestId":"req001","timestamp":"2025...","dataValue":5}

List<String> lines = Files.readAllLines(Path.of("data.csv"));
for (String line : lines) {
    String[] parts = line.split(",");

    String json = String.format("""
        {
            "requestId": "%s",
            "timestamp": "%s",
            "dataValue": %s
        }
        """, parts[0], parts[1], parts[2]);

    sendRequest(client, json);
}
```

---

## 📞 자주 묻는 질문 (FAQ)

**Q: Gson 라이브러리를 어디서 다운로드하나요?**
A: [Maven Repository](https://mvnrepository.com/artifact/com.google.code.gson/gson)에서 다운로드

**Q: 서버가 이미 실행 중이라는 에러가 나요**
A: `lsof -i :8080`으로 프로세스 확인 후 종료하거나 다른 포트 사용

**Q: 멀티 스레드에서 데이터가 꼬여요**
A: 공유 변수 접근 시 `synchronized` 블록 사용 필수

**Q: JSON 파싱 시 null 에러가 나요**
A: 필드 존재 여부를 `obj.has("fieldName")`로 먼저 확인

**Q: 한글이 깨져요**
A: `StandardCharsets.UTF_8` 사용 확인

---

이 가이드를 활용하여 다양한 변형 문제를 해결해보세요! 🚀
