# 메시지 타입 라우팅 가이드

## 📌 개요

실무에서는 단일 엔드포인트로 여러 타입의 메시지를 받아서 처리하는 경우가 많습니다.
이 가이드는 **메시지 타입별로 GET/POST를 분기 처리**하는 방법을 설명합니다.

---

## 🎯 학습 목표

1. **HTTP 메서드 구분**: GET vs POST 요청 처리
2. **메시지 타입 라우팅**: `messageType` 필드로 핸들러 분기
3. **Query Parameter 파싱**: GET 요청에서 데이터 추출
4. **동적 핸들러 매핑**: switch-case 또는 Map 기반 라우팅

---

## 📂 파일 구조

```
SUB3/
├── ProgramRouter.java        # 라우팅 서버 (메인)
├── RouterTestClient.java     # 테스트 클라이언트
├── MODELS.JSON               # 샘플 데이터
└── HttpClientExample.java    # 기본 클라이언트 예제
```

---

## 🚀 실행 방법

### 1단계: 서버 실행
```bash
cd SUB3
javac -cp ".:gson-2.10.1.jar" ProgramRouter.java
java -cp ".:gson-2.10.1.jar" ProgramRouter
```

### 2단계: 테스트 클라이언트 실행
```bash
# 새 터미널에서
javac RouterTestClient.java
java RouterTestClient
```

---

## 📖 핵심 개념

### 1. 메시지 타입별 라우팅

#### 메시지 타입 정의
```java
// 지원하는 메시지 타입
- REGISTER           : 에이전트 등록
- MONITORING         : 모니터링 데이터 수집
- QUERY_PERFORMANCE  : 성능 조회
- QUERY_STATUS       : 상태 조회
- LIST_MODELS        : 모델 목록 조회
- DELETE_RECORD      : 레코드 삭제
```

#### 라우팅 로직
```java
static String routeMessage(String messageType, JsonObject data, String method) {
    switch (messageType) {
        case "REGISTER":
            return handleRegister(data);

        case "MONITORING":
            return handleMonitoring(data);

        case "QUERY_PERFORMANCE":
            return handleQueryPerformance(data);

        // ... 기타 타입

        default:
            return errorResponse("알 수 없는 타입: " + messageType);
    }
}
```

---

### 2. GET vs POST 처리

#### POST 요청 (JSON Body)
```java
// 클라이언트
String json = """
    {
        "messageType": "MONITORING",
        "data": {
            "agentId": "agent001",
            "dataValue": 10
        }
    }
    """;

// 서버
String body = new String(ctx.getRequestBody().readAllBytes(), UTF_8);
JsonObject json = JsonParser.parseString(body).getAsJsonObject();
String messageType = json.get("messageType").getAsString();
JsonObject data = json.getAsJsonObject("data");
```

#### GET 요청 (Query Parameters)
```java
// 클라이언트
String url = "http://127.0.0.1:8080/api?messageType=QUERY_STATUS&agentId=agent001";

// 서버
String query = ctx.getRequestURI().getQuery();
// query = "messageType=QUERY_STATUS&agentId=agent001"

Map<String, String> params = parseQueryParams(query);
String messageType = params.get("messageType");
String agentId = params.get("agentId");
```

#### Query Parameter 파싱 구현
```java
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
```

---

### 3. 핸들러 구현 패턴

#### 패턴 1: 데이터 등록/수정 (POST)
```java
static String handleRegister(JsonObject data) {
    // 1. 데이터 추출
    String agentId = data.get("agentId").getAsString();
    String modelName = data.get("modelName").getAsString();

    // 2. 비즈니스 로직
    Models.computeIfAbsent(modelName, k -> new HashSet<>()).add(agentId);

    // 3. 응답 생성
    Map<String, String> result = new HashMap<>();
    result.put("status", "success");
    result.put("message", agentId + " 등록 완료");

    return GSON.toJson(result);
}
```

#### 패턴 2: 데이터 조회 (GET/POST)
```java
static String handleQueryStatus(JsonObject data) {
    // 1. 기본 상태 정보
    Map<String, Object> result = new HashMap<>();
    result.put("status", "running");
    result.put("recordCount", Records.size());

    // 2. 옵셔널 필터링
    if (data.has("agentId")) {
        String agentId = data.get("agentId").getAsString();
        long count = Records.stream()
            .filter(r -> r.get("agentId").getAsString().equals(agentId))
            .count();
        result.put("agentRecordCount", count);
    }

    return GSON.toJson(result);
}
```

#### 패턴 3: 데이터 삭제 (POST)
```java
static String handleDeleteRecord(JsonObject data) {
    String agentId = data.get("agentId").getAsString();
    String requestId = data.get("requestId").getAsString();

    // removeIf로 조건에 맞는 항목 삭제
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
```

---

## 🔥 실습 문제

### 난이도 1: 기본

**문제 1.1**: 새로운 메시지 타입 추가
- 메시지 타입: `COUNT_RECORDS`
- 기능: 전체 레코드 수를 반환
- GET 요청으로 구현
- 응답: `{"count": 15}`

**문제 1.2**: UPDATE 기능 추가
- 메시지 타입: `UPDATE_RECORD`
- 기능: 특정 레코드의 dataValue 수정
- POST 요청으로 구현
- 요청: `{"agentId":"agent001","requestId":"req001","newValue":99}`

**문제 1.3**: 에러 처리 강화
- 필수 필드 누락 시 명확한 에러 메시지
- 예: `{"status":"error","message":"agentId 필드가 없습니다"}`

---

### 난이도 2: 응용

**문제 2.1**: 조건부 조회 (GET)
- 메시지 타입: `QUERY_RECORDS`
- Query Parameters:
  - `agentId`: 특정 에이전트의 레코드만 조회
  - `dataType`: P 또는 A 필터링
  - `limit`: 최대 개수 제한
- 예: `/api?messageType=QUERY_RECORDS&agentId=agent001&dataType=P&limit=10`

**문제 2.2**: 일괄 등록 (POST)
- 메시지 타입: `BATCH_MONITORING`
- 요청: 여러 개의 모니터링 데이터를 배열로 전송
```json
{
    "messageType": "BATCH_MONITORING",
    "data": [
        {"agentId":"agent001", "requestId":"req001", ...},
        {"agentId":"agent002", "requestId":"req002", ...}
    ]
}
```

**문제 2.3**: 통계 조회 (GET/POST)
- 메시지 타입: `QUERY_STATISTICS`
- 기능: 모델별 평균 정확도 계산
- 응답:
```json
{
    "model1": {"avgAccuracy": 85.5, "totalRecords": 100},
    "model2": {"avgAccuracy": 90.2, "totalRecords": 50}
}
```

---

### 난이도 3: 실전

**문제 3.1**: Map 기반 핸들러 등록
- switch-case 대신 Map으로 핸들러 관리
- 동적으로 핸들러 추가/제거 가능
```java
Map<String, MessageHandler> handlers = new HashMap<>();
handlers.put("REGISTER", this::handleRegister);
handlers.put("MONITORING", this::handleMonitoring);

// 라우팅
MessageHandler handler = handlers.get(messageType);
if (handler != null) {
    return handler.handle(data);
}
```

**문제 3.2**: 인증/권한 체크
- 특정 메시지 타입은 인증 필요
- HTTP 헤더에서 Authorization 토큰 확인
- 권한 없으면 403 에러 반환

**문제 3.3**: 로깅 미들웨어
- 모든 요청/응답을 파일에 로깅
- 형식: `[timestamp] [method] [messageType] → [status]`
- 예: `[2025-04-28 12:00:00] [POST] [MONITORING] → success`

**문제 3.4**: 비동기 처리
- 시간이 오래 걸리는 작업은 백그라운드에서 처리
- 즉시 jobId 반환
- 별도 엔드포인트로 진행 상황 조회
```json
// 즉시 응답
{"status":"processing","jobId":"job-12345"}

// 나중에 조회
GET /api?messageType=CHECK_JOB&jobId=job-12345
→ {"status":"completed","result":{...}}
```

---

## 💡 실전 팁

### 1. 메시지 타입 관리
```java
// Enum으로 관리하면 오타 방지
enum MessageType {
    REGISTER,
    MONITORING,
    QUERY_PERFORMANCE,
    QUERY_STATUS,
    LIST_MODELS,
    DELETE_RECORD;

    public static MessageType fromString(String s) {
        try {
            return valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
```

### 2. 응답 포맷 통일
```java
// 성공 응답
{
    "status": "success",
    "data": { ... },
    "timestamp": "2025-04-28T12:00:00"
}

// 에러 응답
{
    "status": "error",
    "code": "INVALID_PARAM",
    "message": "agentId가 없습니다",
    "timestamp": "2025-04-28T12:00:00"
}
```

### 3. Validation 유틸
```java
static void requireField(JsonObject obj, String fieldName) {
    if (!obj.has(fieldName)) {
        throw new IllegalArgumentException(fieldName + " 필드가 없습니다");
    }
}

// 사용
try {
    requireField(data, "agentId");
    requireField(data, "requestId");
    // 처리...
} catch (IllegalArgumentException e) {
    return errorResponse(e.getMessage());
}
```

### 4. 로깅
```java
static void log(String method, String messageType, String status) {
    String timestamp = LocalDateTime.now().toString();
    System.out.printf("[%s] [%s] %s → %s%n",
        timestamp, method, messageType, status);
}

// 사용
log("POST", "MONITORING", "success");
```

---

## 🔍 디버깅 체크리스트

### GET 요청이 안 될 때
- [ ] Query Parameter 형식이 올바른가? (`key=value&key2=value2`)
- [ ] URL 인코딩이 필요한가? (공백, 특수문자)
- [ ] 서버에서 `ctx.getRequestURI().getQuery()` 값 확인

### POST 요청이 안 될 때
- [ ] JSON 형식이 올바른가?
- [ ] Content-Type 헤더가 `application/json`인가?
- [ ] 서버에서 Body를 제대로 읽었는가?

### 라우팅이 안 될 때
- [ ] messageType 필드가 존재하는가?
- [ ] messageType 값의 대소문자가 정확한가?
- [ ] switch-case에 해당 타입이 있는가?

---

## 📚 다음 단계

### 1. 미들웨어 패턴
- 요청 전처리 (로깅, 인증)
- 응답 후처리 (압축, 암호화)

### 2. 데이터 검증
- JSON Schema 검증
- 필수 필드 체크
- 타입 변환 오류 처리

### 3. 성능 최적화
- 응답 캐싱
- Connection Pool
- 비동기 처리

### 4. 실전 프로젝트
- RESTful API 서버 구축
- WebSocket 실시간 통신
- 마이크로서비스 아키텍처

---

## ✅ 요약

| 항목 | GET | POST |
|------|-----|------|
| 데이터 전달 | Query Parameters | JSON Body |
| 용도 | 조회 | 등록/수정/삭제 |
| 캐싱 | 가능 | 불가능 |
| 길이 제한 | 있음 (~2KB) | 없음 |

**핵심**: 메시지 타입으로 라우팅 → HTTP 메서드로 의도 구분 → 핸들러에서 처리

이제 `ProgramRouter.java`와 `RouterTestClient.java`를 실행하며 실습해보세요! 🚀
