import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SUB1 - Reads MONITORING.TXT and prints accuracy as "&lt;correct&gt;/&lt;total&gt;".
 * File format per line: &lt;requestId&gt;#&lt;timestamp&gt;#&lt;dataType&gt;#&lt;dataValue&gt;
 *   dataType: 'P' (predicted) or 'A' (actual). Same requestId has 1 P and 1 A row.
 * Accuracy = (#requestIds where P value == A value) / (#requestIds)
 */
public class Program {
    public static void main(String[] args) throws IOException {
        final String monitoringFile = "MONITORING.TXT";

        Map<String, Integer> predicted = new HashMap<>();
        Map<String, Integer> actual = new HashMap<>();

        List<String> lines = Files.readAllLines(Path.of(monitoringFile));
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("#", -1);
            if (parts.length != 4) continue;

            String requestId = parts[0];
            String dataType = parts[2];
            int dataValue = Integer.parseInt(parts[3]);

            if (dataType.equals("P")) {
                predicted.put(requestId, dataValue);
            } else if (dataType.equals("A")) {
                actual.put(requestId, dataValue);
            }
        }

        int correct = 0;
        int total = predicted.size();
        for (Map.Entry<String, Integer> kv : predicted.entrySet()) {
            Integer actualValue = actual.get(kv.getKey());
            if (actualValue != null && kv.getValue().equals(actualValue)) {
                correct++;
            }
        }

        System.out.println(correct + "/" + total);
    }
}
