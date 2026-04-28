# Java 자료구조 완벽 가이드

## 📚 목차
1. [자료구조 선택 가이드](#자료구조-선택-가이드)
2. [Map 계열](#map-계열)
3. [Queue 계열](#queue-계열)
4. [Set 계열](#set-계열)
5. [List 계열](#list-계열)
6. [실전 활용 패턴](#실전-활용-패턴)

---

## 🎯 자료구조 선택 가이드

### 언제 무엇을 사용할까?

| 상황 | 자료구조 | 이유 |
|------|---------|------|
| 빠른 검색 필요 | **HashMap** | O(1) 검색 |
| 순서 유지 + 검색 | **LinkedHashMap** | 삽입 순서 유지 + O(1) |
| 정렬된 순서 필요 | **TreeMap** | 자동 정렬 (O(log n)) |
| 멀티스레드 환경 | **ConcurrentHashMap** | 스레드 안전 |
| 최근 N개만 유지 | **Queue (FIFO)** | 오래된 것 자동 제거 |
| 우선순위 처리 | **PriorityQueue** | 자동 우선순위 정렬 |
| 중복 제거 | **HashSet** | 자동 중복 제거 |
| 정렬 + 중복 제거 | **TreeSet** | 정렬된 Set |
| 생산자-소비자 패턴 | **BlockingQueue** | 스레드 간 안전한 데이터 전달 |

---

## 🗺️ Map 계열

### 1. HashMap<K, V>

**특징**: 가장 기본적인 Map, 빠른 검색 (O(1))

```java
// 생성
Map<String, Integer> map = new HashMap<>();

// 추가
map.put("apple", 100);
map.put("banana", 200);

// 조회
int price = map.get("apple");  // 100
int defaultValue = map.getOrDefault("orange", 0);  // 0

// 존재 여부 확인
if (map.containsKey("apple")) { ... }

// 순회
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    String key = entry.getKey();
    Integer value = entry.getValue();
}

// Java 8+ forEach
map.forEach((k, v) -> System.out.println(k + ": " + v));

// 값이 없으면 추가 (computeIfAbsent)
map.computeIfAbsent("grape", k -> 150);

// 값 업데이트 (merge)
map.merge("apple", 50, Integer::sum);  // 100 + 50 = 150
```

**실전 예제**: 단어 빈도수 계산
```java
Map<String, Integer> wordCount = new HashMap<>();
for (String word : words) {
    wordCount.merge(word, 1, Integer::sum);
}
```

---

### 2. LinkedHashMap<K, V>

**특징**: 삽입 순서 유지 또는 접근 순서 유지 (LRU 캐시 구현 가능)

```java
// 삽입 순서 유지
Map<String, String> insertionOrder = new LinkedHashMap<>();

// 접근 순서 유지 (LRU 캐시)
Map<String, String> lruCache = new LinkedHashMap<String, String>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
        return size() > 100;  // 최대 100개만 유지
    }
};

// 사용
lruCache.put("key1", "value1");
lruCache.get("key1");  // 접근하면 맨 뒤로 이동
```

**실전 예제**: 최근 조회 캐시
```java
Map<String, PerformanceData> performanceCache = new LinkedHashMap<>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, PerformanceData> eldest) {
        return size() > 50;  // 최근 50개만 캐싱
    }
};
```

---

### 3. TreeMap<K, V>

**특징**: 키 기준 자동 정렬 (Red-Black Tree), O(log n) 검색

```java
// 생성 (자동 정렬)
TreeMap<String, Integer> treeMap = new TreeMap<>();

treeMap.put("2025-04-28", 100);
treeMap.put("2025-04-27", 90);
treeMap.put("2025-04-29", 110);

// 순서대로 출력: 2025-04-27, 2025-04-28, 2025-04-29
for (String key : treeMap.keySet()) {
    System.out.println(key + ": " + treeMap.get(key));
}

// 범위 조회 (subMap)
Map<String, Integer> range = treeMap.subMap("2025-04-27", true, "2025-04-29", false);
// 2025-04-27 <= key < 2025-04-29

// 첫 번째/마지막 키
String first = treeMap.firstKey();   // "2025-04-27"
String last = treeMap.lastKey();     // "2025-04-29"

// 특정 키보다 작은/큰 키
String lower = treeMap.lowerKey("2025-04-28");   // "2025-04-27"
String higher = treeMap.higherKey("2025-04-28"); // "2025-04-29"
```

**실전 예제**: 시계열 데이터 관리
```java
TreeMap<String, List<JsonObject>> timeSeriesData = new TreeMap<>();

// 데이터 추가
timeSeriesData.computeIfAbsent(timestamp, k -> new ArrayList<>()).add(data);

// 특정 시간 범위 조회
Map<String, List<JsonObject>> rangeData =
    timeSeriesData.subMap("2025-04-28T00:00", "2025-04-28T23:59");
```

---

### 4. ConcurrentHashMap<K, V>

**특징**: 멀티스레드 환경에서 안전, 락 분할로 높은 동시성

```java
// 생성
ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();

// 원자적 업데이트 (스레드 안전)
concurrentMap.put("counter", 0);
concurrentMap.merge("counter", 1, Integer::sum);  // 1 증가

// computeIfAbsent도 원자적
concurrentMap.computeIfAbsent("key", k -> expensiveOperation());

// putIfAbsent (없을 때만 추가)
Integer old = concurrentMap.putIfAbsent("key", 100);
```

**실전 예제**: 에이전트별 요청 수 카운팅 (멀티스레드)
```java
ConcurrentHashMap<String, Integer> agentCount = new ConcurrentHashMap<>();

// 여러 스레드에서 동시 호출 가능
agentCount.merge(agentId, 1, Integer::sum);
```

---

## 📦 Queue 계열

### 1. Queue (LinkedList 구현)

**특징**: FIFO (First In First Out)

```java
// 생성
Queue<String> queue = new LinkedList<>();

// 추가 (끝에 추가)
queue.offer("first");
queue.offer("second");
queue.offer("third");

// 조회 (제거 안 함)
String peek = queue.peek();  // "first"

// 제거 (앞에서 제거)
String poll = queue.poll();  // "first" (제거됨)

// 크기 확인
int size = queue.size();

// 비어있는지 확인
boolean isEmpty = queue.isEmpty();
```

**실전 예제**: 최근 100개 요청만 유지
```java
Queue<JsonObject> recentRequests = new LinkedList<>();
static final int MAX_RECENT = 100;

// 새 요청 추가
recentRequests.offer(newRequest);
if (recentRequests.size() > MAX_RECENT) {
    recentRequests.poll();  // 가장 오래된 것 제거
}
```

---

### 2. PriorityQueue

**특징**: 우선순위 큐 (자동 정렬), Heap 구조

```java
// 오름차순 (기본)
PriorityQueue<Integer> minHeap = new PriorityQueue<>();

// 내림차순
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());

// 커스텀 비교 (latency 높은 순)
PriorityQueue<JsonObject> highLatencyQueue = new PriorityQueue<>((a, b) -> {
    int latencyA = a.get("latency").getAsInt();
    int latencyB = b.get("latency").getAsInt();
    return Integer.compare(latencyB, latencyA);  // 내림차순
});

// 추가
highLatencyQueue.offer(data);

// 최우선 항목 조회 (제거 안 함)
JsonObject top = highLatencyQueue.peek();

// 최우선 항목 제거
JsonObject removed = highLatencyQueue.poll();
```

**실전 예제**: 높은 latency 요청 우선 처리
```java
PriorityQueue<Request> urgentQueue = new PriorityQueue<>(
    Comparator.comparingInt(r -> -r.latency)  // 음수로 내림차순
);

// 가장 긴급한 요청 처리
while (!urgentQueue.isEmpty()) {
    Request urgent = urgentQueue.poll();
    processUrgent(urgent);
}
```

---

### 3. Deque (ArrayDeque)

**특징**: 양방향 큐 (앞/뒤 모두 추가/제거 가능)

```java
// 생성
Deque<String> deque = new ArrayDeque<>();

// 앞에 추가
deque.addFirst("front");
deque.offerFirst("new-front");

// 뒤에 추가
deque.addLast("back");
deque.offerLast("new-back");

// 앞에서 제거
String first = deque.pollFirst();

// 뒤에서 제거
String last = deque.pollLast();

// Stack처럼 사용 (LIFO)
deque.push("item");  // addFirst
String item = deque.pop();  // removeFirst
```

**실전 예제**: 처리 중인 작업 관리
```java
Deque<Task> processingQueue = new ArrayDeque<>();

// 새 작업 추가 (뒤에)
processingQueue.addLast(newTask);

// 긴급 작업 추가 (앞에)
processingQueue.addFirst(urgentTask);

// 작업 처리 (앞에서)
Task task = processingQueue.pollFirst();
```

---

### 4. BlockingQueue

**특징**: 스레드 안전 큐, 생산자-소비자 패턴

```java
// 생성 (크기 제한)
BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);

// 생산자 스레드
try {
    queue.put("item");  // 큐가 가득 차면 대기
} catch (InterruptedException e) { }

// 소비자 스레드
try {
    String item = queue.take();  // 큐가 비면 대기
} catch (InterruptedException e) { }

// 타임아웃
boolean success = queue.offer("item", 1, TimeUnit.SECONDS);
String item = queue.poll(1, TimeUnit.SECONDS);
```

**실전 예제**: 비동기 작업 처리
```java
BlockingQueue<JsonObject> asyncTaskQueue = new LinkedBlockingQueue<>(1000);

// 워커 스레드
Thread worker = new Thread(() -> {
    while (true) {
        try {
            JsonObject task = asyncTaskQueue.take();
            processTask(task);
        } catch (InterruptedException e) {
            break;
        }
    }
});
worker.start();

// 작업 등록
asyncTaskQueue.put(newTask);
```

---

## 🎲 Set 계열

### 1. HashSet

**특징**: 중복 제거, O(1) 검색

```java
// 생성
Set<String> set = new HashSet<>();

// 추가
set.add("apple");
set.add("banana");
set.add("apple");  // 중복 무시

// 크기
int size = set.size();  // 2

// 포함 여부
boolean has = set.contains("apple");  // true

// 순회
for (String item : set) {
    System.out.println(item);
}
```

**실전 예제**: 중복 requestId 방지
```java
Set<String> processedIds = new HashSet<>();

if (processedIds.contains(requestId)) {
    return "중복 요청";
}
processedIds.add(requestId);
```

---

### 2. TreeSet

**특징**: 정렬된 Set, O(log n)

```java
// 생성
TreeSet<String> treeSet = new TreeSet<>();

treeSet.add("charlie");
treeSet.add("alice");
treeSet.add("bob");

// 자동 정렬: alice, bob, charlie
for (String name : treeSet) {
    System.out.println(name);
}

// 범위 조회
Set<String> range = treeSet.subSet("alice", "charlie");  // alice, bob

// 첫/마지막
String first = treeSet.first();
String last = treeSet.last();
```

**실전 예제**: 정렬된 에러 로그
```java
TreeSet<String> errorLogs = new TreeSet<>();

errorLogs.add("2025-04-28T12:00:00 - Error A");
errorLogs.add("2025-04-28T11:00:00 - Error B");

// 자동으로 시간 순 정렬됨
for (String log : errorLogs) {
    System.out.println(log);
}
```

---

## 📋 List 계열

### ArrayList vs LinkedList

| 특징 | ArrayList | LinkedList |
|------|-----------|------------|
| 랜덤 접근 | O(1) | O(n) |
| 삽입/삭제 (중간) | O(n) | O(1) |
| 메모리 | 연속적 | 분산적 |
| 사용 시점 | 조회 많음 | 삽입/삭제 많음 |

```java
// ArrayList (일반적으로 더 많이 사용)
List<String> arrayList = new ArrayList<>();
arrayList.add("item1");
String item = arrayList.get(0);  // O(1)

// LinkedList (Queue 구현에도 사용)
List<String> linkedList = new LinkedList<>();
linkedList.add(0, "first");  // 앞에 삽입 O(1)
```

---

## 🔥 실전 활용 패턴

### 패턴 1: Map + List (그룹화)

```java
// 타임스탬프별로 데이터 그룹화
Map<String, List<JsonObject>> groupedData = new HashMap<>();

for (JsonObject data : allData) {
    String timestamp = data.get("timestamp").getAsString();
    groupedData.computeIfAbsent(timestamp, k -> new ArrayList<>()).add(data);
}
```

### 패턴 2: Map + Set (다대다 관계)

```java
// 모델 → 에이전트 매핑 (한 모델에 여러 에이전트, 중복 없음)
Map<String, Set<String>> modelAgents = new HashMap<>();

// 에이전트 추가
modelAgents.computeIfAbsent("model1", k -> new HashSet<>()).add("agent001");

// 모델에 속한 에이전트인지 확인
boolean belongs = modelAgents.get("model1").contains("agent001");
```

### 패턴 3: TreeMap + 범위 조회

```java
// 시계열 데이터 범위 조회
TreeMap<String, Integer> timeData = new TreeMap<>();

// 특정 시간 범위의 데이터만 추출
String start = "2025-04-28T00:00";
String end = "2025-04-28T12:00";
Map<String, Integer> morning = timeData.subMap(start, true, end, false);

// 평균 계산
double avg = morning.values().stream()
    .mapToInt(Integer::intValue)
    .average()
    .orElse(0.0);
```

### 패턴 4: Queue + 크기 제한 (원형 버퍼)

```java
// 최근 N개만 유지
Queue<String> recentLogs = new LinkedList<>();
int MAX_SIZE = 100;

void addLog(String log) {
    recentLogs.offer(log);
    if (recentLogs.size() > MAX_SIZE) {
        recentLogs.poll();
    }
}
```

### 패턴 5: PriorityQueue + 상위 K개

```java
// 상위 10개 높은 값 유지 (Min Heap)
PriorityQueue<Integer> topK = new PriorityQueue<>();
int K = 10;

for (int value : values) {
    topK.offer(value);
    if (topK.size() > K) {
        topK.poll();  // 가장 작은 값 제거
    }
}
// 결과: 상위 10개만 남음
```

### 패턴 6: Stream + 자료구조

```java
// 리스트 → Map 변환
Map<String, Agent> agentMap = agentList.stream()
    .collect(Collectors.toMap(Agent::getId, a -> a));

// 필터링 + 수집
List<String> highLatencyIds = requests.stream()
    .filter(r -> r.latency > 100)
    .map(r -> r.requestId)
    .collect(Collectors.toList());

// 그룹화
Map<String, List<Request>> byAgent = requests.stream()
    .collect(Collectors.groupingBy(r -> r.agentId));

// 카운팅
Map<String, Long> countByAgent = requests.stream()
    .collect(Collectors.groupingBy(r -> r.agentId, Collectors.counting()));
```

---

## 🎓 실습 문제

### 문제 1: LRU 캐시 구현
- LinkedHashMap을 사용하여 LRU 캐시 구현
- 최대 100개 항목 유지
- 가장 오래 사용되지 않은 항목 자동 제거

### 문제 2: 시간별 통계
- TreeMap으로 시간별 데이터 저장
- 특정 시간 범위의 평균값 계산
- 피크 시간대 찾기

### 문제 3: 우선순위 작업 큐
- PriorityQueue로 작업 우선순위 관리
- latency 높은 순으로 처리
- 상위 10개 긴급 작업 추출

### 문제 4: 중복 제거 + 정렬
- HashSet으로 중복 제거
- TreeSet으로 정렬
- 두 자료구조 성능 비교

### 문제 5: 생산자-소비자 패턴
- BlockingQueue로 작업 큐 구현
- 여러 생산자 스레드
- 여러 소비자 스레드
- 처리 속도 측정

---

## 📊 성능 비교

| 자료구조 | 검색 | 삽입 | 삭제 | 메모리 |
|---------|------|------|------|--------|
| HashMap | O(1) | O(1) | O(1) | 높음 |
| TreeMap | O(log n) | O(log n) | O(log n) | 보통 |
| ArrayList | O(1) | O(n) | O(n) | 낮음 |
| LinkedList | O(n) | O(1) | O(1) | 높음 |
| HashSet | O(1) | O(1) | O(1) | 높음 |
| TreeSet | O(log n) | O(log n) | O(log n) | 보통 |

---

이제 `ProgramAdvanced.java`를 실행하며 다양한 자료구조를 실습해보세요! 🚀
