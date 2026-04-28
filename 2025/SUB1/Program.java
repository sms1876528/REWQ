// 파일 입출력을 위한 예외 처리 클래스
import java.io.IOException;
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
 * 단계 1: TXT 파일 기반 데이터 처리 (기초)
 * ============================================================================
 *
 * SUB1 - MONITORING.TXT 파일을 읽어서 예측 정확도를 계산하는 프로그램
 *
 * [학습 목표]
 * - 파일 입출력 (Files.readAllLines)
 * - 문자열 파싱 (split, trim)
 * - 자료구조 활용 (HashMap)
 * - 반복문과 조건문
 *
 * [프로그램 목적]
 * - 예측값(P)과 실제값(A)을 비교하여 정확도를 계산합니다
 * - 결과를 "<맞은 개수>/<전체 개수>" 형식으로 화면에 출력합니다
 *
 * [파일 형식]
 * - 각 줄의 형식: <요청ID>#<타임스탬프>#<데이터타입>#<데이터값>
 * - 데이터타입: 'P' (예측값) 또는 'A' (실제값)
 * - 동일한 요청ID는 P 행 1개와 A 행 1개를 가집니다
 *
 * [정확도 계산]
 * - 정확도 = (예측값과 실제값이 일치하는 요청ID 개수) / (전체 요청ID 개수)
 *
 * [다음 단계로]
 * - SUB2: 사용자 입력 처리 (BufferedReader) + 시간 필터링 추가
 */
public class Program {
    // 프로그램의 시작점 (main 메서드)
    // throws IOException: 파일 읽기 중 오류가 발생할 수 있음을 나타냄
    public static void main(String[] args) throws IOException {
        // 읽을 파일의 이름을 상수로 정의 (final: 값 변경 불가)
        final String monitoringFile = "MONITORING.TXT";

        // 예측값(P)을 저장할 맵 (키: 요청ID, 값: 예측된 데이터값)
        Map<String, Integer> predicted = new HashMap<>();
        // 실제값(A)을 저장할 맵 (키: 요청ID, 값: 실제 데이터값)
        Map<String, Integer> actual = new HashMap<>();

        // 파일의 모든 줄을 읽어서 리스트로 저장
        // Path.of(): 파일 경로 객체 생성
        // Files.readAllLines(): 파일의 모든 줄을 한 번에 읽음
        List<String> lines = Files.readAllLines(Path.of(monitoringFile));

        // 파일의 각 줄을 순회하면서 처리
        for (String raw : lines) {
            // 줄의 앞뒤 공백 제거
            String line = raw.trim();
            // 빈 줄은 건너뛰기
            if (line.isEmpty()) continue;

            // '#' 기호로 문자열 분리 (-1: 빈 문자열도 포함)
            // 예: "REQ001#20230101#P#5" → ["REQ001", "20230101", "P", "5"]
            String[] parts = line.split("#", -1);
            // 분리된 부분이 4개가 아니면 잘못된 형식이므로 건너뛰기
            if (parts.length != 4) continue;

            // 각 부분을 의미 있는 변수명으로 저장
            String requestId = parts[0];   // 요청 ID (예: "REQ001")
            String dataType = parts[2];    // 데이터 타입 ('P' 또는 'A')
            int dataValue = Integer.parseInt(parts[3]);  // 데이터값을 정수로 변환

            // 데이터 타입에 따라 적절한 맵에 저장
            if (dataType.equals("P")) {
                // 예측값(P)인 경우 predicted 맵에 저장
                predicted.put(requestId, dataValue);
            } else if (dataType.equals("A")) {
                // 실제값(A)인 경우 actual 맵에 저장
                actual.put(requestId, dataValue);
            }
        }

        // 정확도 계산을 위한 변수 초기화
        int correct = 0;  // 맞은 개수
        int total = predicted.size();  // 전체 예측 개수

        // predicted 맵의 모든 항목을 순회
        // kv는 키-값 쌍(Map.Entry)을 나타냄
        for (Map.Entry<String, Integer> kv : predicted.entrySet()) {
            // 같은 요청ID의 실제값을 actual 맵에서 가져옴
            Integer actualValue = actual.get(kv.getKey());
            // 실제값이 존재하고, 예측값과 일치하면 correct 증가
            // actualValue != null: 실제값이 있는지 확인
            // kv.getValue().equals(actualValue): 예측값 == 실제값 확인
            if (actualValue != null && kv.getValue().equals(actualValue)) {
                correct++;
            }
        }

        // 결과를 "맞은개수/전체개수" 형식으로 출력
        // 예: "8/10" (10개 중 8개 맞음)
        System.out.println(correct + "/" + total);
    }
}
