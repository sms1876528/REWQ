// 버퍼링된 입력 스트림을 제공하는 클래스 (효율적인 텍스트 읽기)
import java.io.BufferedReader;
// 파일 입출력을 위한 예외 처리 클래스
import java.io.IOException;
// 시스템 입력을 읽기 위한 클래스 (키보드 입력)
import java.io.InputStreamReader;
// 파일 읽기 기능을 제공하는 클래스
import java.nio.file.Files;
// 파일 경로를 다루는 클래스
import java.nio.file.Path;
// 해시맵(키-값 쌍을 저장하는 자료구조)을 사용하기 위한 클래스
import java.util.HashMap;
// 리스트(순서가 있는 데이터 목록)를 사용하기 위한 클래스
import java.util.List;
// Map 인터페이스(키-값 쌍 자료구조의 기본 형태)를 사용하기 위한 클래스
import java.util.Map;

/**
 * ============================================================================
 * 단계 2: 사용자 입력 처리 + 데이터 필터링 (응용)
 * ============================================================================
 *
 * SUB2 - 특정 시간대의 예측 정확도를 계산하는 프로그램
 *
 * [학습 목표]
 * - 사용자 입력 처리 (BufferedReader, InputStreamReader)
 * - 데이터 필터링 (startsWith를 이용한 시간 윈도우 필터링)
 * - 내부 클래스 정의 (Predicted 클래스)
 * - 복합 데이터 저장 (타임스탬프 + 값)
 *
 * [SUB1과의 차이점]
 * - SUB1: 모든 데이터 처리 → SUB2: 사용자가 지정한 시간대 데이터만 처리
 * - SUB1: 값만 저장 → SUB2: 타임스탬프도 함께 저장 (Predicted 클래스 사용)
 *
 * [프로그램 목적]
 * - MONITORING.TXT 파일을 읽어서 처리합니다
 * - 사용자로부터 시간 윈도우(yyyyMMddHH 형식)를 입력받습니다
 * - 해당 시간대의 예측값(P)에 대한 정확도를 계산합니다
 * - 결과를 "<맞은 개수>/<전체 개수>" 형식으로 출력합니다
 *
 * [동작 방식]
 * - 예측값(P)의 타임스탬프가 입력한 시간 윈도우로 시작하는 경우만 계산합니다
 * - 예: 입력이 "2023010112"이면, "20230101120000"으로 시작하는 모든 예측을 포함
 *
 * [다음 단계로]
 * - SUB3: JSON 파일 처리 + HTTP API 서버 구현 (네트워크 통신)
 */
public class Program {
    /**
     * 예측 데이터를 저장하는 내부 클래스
     * - timestamp: 예측이 이루어진 시간
     * - value: 예측된 값
     */
    private static final class Predicted {
        final String timestamp;  // 타임스탬프 (불변)
        final int value;         // 예측값 (불변)

        // 생성자: 타임스탬프와 값을 받아서 객체 초기화
        Predicted(String timestamp, int value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    // 프로그램의 시작점 (main 메서드)
    // throws IOException: 파일 읽기/입력 중 오류가 발생할 수 있음을 나타냄
    public static void main(String[] args) throws IOException {
        // 읽을 파일의 이름을 상수로 정의
        final String monitoringFile = "MONITORING.TXT";

        // 예측값(P)을 저장할 맵
        // 키: 요청ID, 값: Predicted 객체(타임스탬프+예측값)
        Map<String, Predicted> predicted = new HashMap<>();
        // 실제값(A)을 저장할 맵
        // 키: 요청ID, 값: 실제 데이터값
        Map<String, Integer> actual = new HashMap<>();

        // 파일의 모든 줄을 읽어서 리스트로 저장
        List<String> lines = Files.readAllLines(Path.of(monitoringFile));

        // 파일의 각 줄을 순회하면서 데이터 파싱
        for (String raw : lines) {
            // 줄의 앞뒤 공백 제거
            String line = raw.trim();
            // 빈 줄은 건너뛰기
            if (line.isEmpty()) continue;

            // '#' 기호로 문자열 분리
            // 예: "REQ001#20230101120000#P#5" → ["REQ001", "20230101120000", "P", "5"]
            String[] parts = line.split("#", -1);
            // 분리된 부분이 4개가 아니면 잘못된 형식이므로 건너뛰기
            if (parts.length != 4) continue;

            // 각 부분을 의미 있는 변수명으로 저장
            String requestId = parts[0];   // 요청 ID
            String timestamp = parts[1];   // 타임스탬프 (yyyyMMddHHmmss 형식)
            String dataType = parts[2];    // 데이터 타입 ('P' 또는 'A')
            int dataValue = Integer.parseInt(parts[3]);  // 데이터값을 정수로 변환

            // 데이터 타입에 따라 적절한 맵에 저장
            if (dataType.equals("P")) {
                // 예측값(P)인 경우: 타임스탬프와 값을 함께 저장
                predicted.put(requestId, new Predicted(timestamp, dataValue));
            } else if (dataType.equals("A")) {
                // 실제값(A)인 경우: 값만 저장
                actual.put(requestId, dataValue);
            }
        }

        // 사용자로부터 시간 윈도우 입력 받기
        // BufferedReader: 효율적인 텍스트 입력을 위한 클래스
        // InputStreamReader: 바이트 스트림을 문자 스트림으로 변환
        // System.in: 표준 입력(키보드)
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String window = br.readLine();  // 한 줄 읽기
        // null 체크 (입력이 없는 경우 빈 문자열로 설정)
        if (window == null) window = "";
        // 앞뒤 공백 제거
        window = window.trim();

        // 정확도 계산을 위한 변수 초기화
        int correct = 0;  // 맞은 개수
        int total = 0;    // 시간 윈도우에 해당하는 전체 개수

        // predicted 맵의 모든 항목을 순회
        for (Map.Entry<String, Predicted> kv : predicted.entrySet()) {
            // 예측의 타임스탬프가 입력한 시간 윈도우로 시작하지 않으면 건너뛰기
            // 예: window="2023010112", timestamp="20230101120000" → 포함됨
            // 예: window="2023010112", timestamp="20230101130000" → 건너뜀
            if (!kv.getValue().timestamp.startsWith(window)) continue;

            // 시간 윈도우에 포함되는 예측이므로 total 증가
            total++;

            // 같은 요청ID의 실제값을 actual 맵에서 가져옴
            Integer actualValue = actual.get(kv.getKey());
            // 실제값이 존재하고, 예측값과 일치하면 correct 증가
            // == 연산자는 기본형(int)에 사용, equals는 Integer 객체 비교에 사용
            if (actualValue != null && actualValue == kv.getValue().value) {
                correct++;
            }
        }

        // 결과를 "맞은개수/전체개수" 형식으로 출력
        // 예: "15/20" (해당 시간대 20개 중 15개 맞음)
        System.out.println(correct + "/" + total);
    }
}
