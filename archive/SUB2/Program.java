import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SUB2 - Reads MONITORING.TXT, accepts a yyyyMMddHH time-window from console,
 * and prints accuracy "&lt;correct&gt;/&lt;total&gt;" for requests whose P timestamp
 * falls in that window (i.e. P.timestamp starts with the input).
 */
public class Program {
    private static final class Predicted {
        final String timestamp;
        final int value;
        Predicted(String timestamp, int value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    public static void main(String[] args) throws IOException {
        final String monitoringFile = "MONITORING.TXT";

        Map<String, Predicted> predicted = new HashMap<>();
        Map<String, Integer> actual = new HashMap<>();

        List<String> lines = Files.readAllLines(Path.of(monitoringFile));
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("#", -1);
            if (parts.length != 4) continue;

            String requestId = parts[0];
            String timestamp = parts[1];
            String dataType = parts[2];
            int dataValue = Integer.parseInt(parts[3]);

            if (dataType.equals("P")) {
                predicted.put(requestId, new Predicted(timestamp, dataValue));
            } else if (dataType.equals("A")) {
                actual.put(requestId, dataValue);
            }
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String window = br.readLine();
        if (window == null) window = "";
        window = window.trim();

        int correct = 0;
        int total = 0;
        for (Map.Entry<String, Predicted> kv : predicted.entrySet()) {
            if (!kv.getValue().timestamp.startsWith(window)) continue;
            total++;
            Integer actualValue = actual.get(kv.getKey());
            if (actualValue != null && actualValue == kv.getValue().value) {
                correct++;
            }
        }

        System.out.println(correct + "/" + total);
    }
}
