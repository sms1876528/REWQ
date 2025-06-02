import com.google.gson.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class Runmanager2 {

	private statoc final List<WorkEntry> entries = new ArrayList<>();
    private static final Map<String, String> dictionary = new HashMap<>();
    private static final Set<String> stopwords = new HashSet<>();
    private static final Map<String, ModelInfo> models = new HashMap<>();
    private static final Map<String, List<WorkEntry>> workMapByDate = new ConcurrentHashMap<>();
    private static final Set<String> processedMessages = ConcurrentHashMap.newKeySet();
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        loadDictionary("DICTIONARY.TXT");
        loadStopwords("STOPWORD.TXT");
        loadModels("MODELS.JSON");

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveProgressSummary();
            }
        }, 60000, 300000); // 1분 후 시작, 5분 주기

        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new InferenceServlet()), "/");
        context.addServlet(new ServletHolder(new IngestServlet()), "/ingest");
        context.addServlet(new ServletHolder(new SummaryServlet()), "/summary");
        context.addServlet(new ServletHolder(new ProgressServlet()), "/progress");

        server.setHandler(context);
        server.start();
        server.join();
    }

    static class WorkEntry {
        String msgId, type, time;
        WorkEntry(String msgId, String type, String time) {
            this.msgId = msgId;
            this.type = type;
            this.time = time;
        }
    }

	// List<WorkEntry> workEntries = loadWorkEntries("WORK.TXT");
	// Map<String, Object> result = calculateProcessingRateByDate("20250523", workEntries);
	// System.out.println(new Gson().toJson(result));
	private static List<WorkEntry> loadWorkEntries(String fileName) throws IOException {
		//List<WorkEntry> entries = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("MSG_ID")) continue; // skip header
				String[] parts = line.split("\\s+");
				if (parts.length == 3) {
					entries.add(new WorkEntry(parts[0], parts[1], parts[2]));
				}
			}
		}

		return entries;
	}

	private static Map<String, Object> calculateProcessingRateByDate(String date, List<WorkEntry> entries) {
		int total = 0;
		int completed = 0;
		Set<String> started = new HashSet<>();
		Set<String> ended = new HashSet<>();

		for (WorkEntry entry : entries) {
			if (entry.time.startsWith(date)) {
				total++;
				if ("S".equals(entry.type)) {
					started.add(entry.msgId);
				} else if ("E".equals(entry.type)) {
					ended.add(entry.msgId);
				}
			}
		}

		// 완료된 건은 시작과 종료가 모두 있는 경우로 계산
		for (String msgId : ended) {
			if (started.contains(msgId)) {
				completed++;
			}
		}

		Map<String, Object> result = new HashMap<>();
		result.put("date", date);
		result.put("total", total);
		result.put("completed", completed);
		result.put("rate", total == 0 ? 0 : ((completed * 100.0) / total));

		return result;
	}

    private static void loadDictionary(String path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("#", 2);
                if (parts.length == 2) dictionary.put(parts[0].toLowerCase(), parts[1].trim());
            }
        }
    }

    private static void loadStopwords(String path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) stopwords.add(line.trim());
        }
    }

    private static void loadModels(String path) throws IOException {
        try (Reader reader = new FileReader(path)) {
            JsonArray modelsArray = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("models");
            for (JsonElement modelElement : modelsArray) {
                JsonObject modelObj = modelElement.getAsJsonObject();
                String modelname = modelObj.get("modelname").getAsString();
                String url = modelObj.get("url").getAsString();
                Map<String, String> codeToValueMap = new HashMap<>();
                for (JsonElement cls : modelObj.getAsJsonArray("classes")) {
                    JsonObject clsObj = cls.getAsJsonObject();
                    codeToValueMap.put(clsObj.get("code").getAsString(), clsObj.get("value").getAsString());
                }
                models.put(modelname, new ModelInfo(modelname, url, codeToValueMap));
            }
        }
    }

    static class ModelInfo {
        String modelname;
        String url;
        Map<String, String> codeToValueMap;
        ModelInfo(String modelname, String url, Map<String, String> codeToValueMap) {
            this.modelname = modelname;
            this.url = url;
            this.codeToValueMap = codeToValueMap;
        }
        String getValue(String code) {
            return codeToValueMap.getOrDefault(code, "unknown");
        }
    }

    static class InferenceServlet extends HttpServlet {
        protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            res.setContentType("application/json;charset=UTF-8");
            JsonObject jsonRequest = JsonParser.parseReader(req.getReader()).getAsJsonObject();
            String modelName = jsonRequest.get("modelname").getAsString();
            JsonArray queries = jsonRequest.getAsJsonArray("queries");

            ModelInfo model = models.get(modelName);
            if (model == null) {
                res.setStatus(400);
                res.getWriter().write("{"error":"Unknown model"}");
                return;
            }

            JsonArray results = new JsonArray();
            for (JsonElement q : queries) {
                String query = q.getAsString();
                String processed = processInput(query);
                String code = callModelServer(model.url, processed);
                results.add(model.getValue(code));
            }

            JsonObject jsonResponse = new JsonObject();
            jsonResponse.add("results", results);
            res.getWriter().write(gson.toJson(jsonResponse));
        }
    }

    static class IngestServlet extends HttpServlet {
        protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            res.setContentType("application/json;charset=UTF-8");
            JsonObject requestJson = JsonParser.parseReader(req.getReader()).getAsJsonObject();
            JsonArray messages = requestJson.getAsJsonArray("messages");
            for (JsonElement el : messages) {
                JsonObject obj = el.getAsJsonObject();
                String msgId = obj.get("msg_id").getAsString();
                String type = obj.get("type").getAsString();
                String time = obj.get("time").getAsString();
                String date = time.substring(0, 8);
                String key = msgId + "|" + type + "|" + time;
                if (processedMessages.add(key)) {
                    workMapByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(new WorkEntry(msgId, type, time));
                }
            }
            res.getWriter().write("{"status":"stored"}");
        }
    }

    static class ProgressServlet extends HttpServlet {
        protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            JsonObject reqJson = JsonParser.parseReader(req.getReader()).getAsJsonObject();
            String date = reqJson.get("date").getAsString();
            JsonObject result = computeProgress(date);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write(gson.toJson(result));
        }
    }

    static class SummaryServlet extends HttpServlet {
        protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String date = req.getParameter("date");
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write(gson.toJson(computeProgress(date)));
        }
        protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            new ProgressServlet().doPost(req, res);
        }
    }

    private static String processInput(String input) {
        StringBuilder sb = new StringBuilder();
        for (String word : input.split("\s+")) {
            String key = word.toLowerCase();
            String vector = dictionary.get(key);
            if (vector != null && !stopwords.contains(vector)) {
                sb.append(vector.replace(",", "")).append("");
            }
        }
        return sb.toString();
    }

    private static String callModelServer(String url, String payload) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(("{"query":"" + payload + ""}").getBytes(StandardCharsets.UTF_8));
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            return JsonParser.parseReader(reader).getAsJsonObject().get("result").getAsString();
        }
    }

    private static JsonObject computeProgress(String date) {
        JsonObject json = new JsonObject();
        List<WorkEntry> list = workMapByDate.getOrDefault(date, Collections.emptyList());
        Map<String, int[]> stats = new HashMap<>();
        for (WorkEntry e : list) {
            stats.putIfAbsent(e.msgId, new int[2]);
            stats.get(e.msgId)[0]++;
            if ("E".equalsIgnoreCase(e.type)) stats.get(e.msgId)[1]++;
        }
        for (Map.Entry<String, int[]> e : stats.entrySet()) {
            JsonObject jo = new JsonObject();
            jo.addProperty("total", e.getValue()[0]);
            jo.addProperty("complete", e.getValue()[1]);
            json.add(e.getKey(), jo);
        }
        return json;
    }

    private static void saveProgressSummary() {
        for (String date : workMapByDate.keySet()) {
            JsonObject obj = computeProgress(date);
            try (Writer w = new FileWriter("progress/progress_" + date + ".json")) {
                gson.toJson(obj, w);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}