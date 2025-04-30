import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Runmanager2 {

	public static void main(String[] args) throws Exception {

		Map<String, String> dictionary = loadDictionary("DICTIONARY.TXT");
		Set<String> stopWords = loadStopwords("STOPWORD.TXT");
		Map<String, ModelInfo> models = loadModels("MODELS.JSON");
		
		Server server = new Server(8080);
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.addServlet(new ServletHolder(new InferenceServlet(dictionary, stopWords, models)), "/");

		server.setHandler(context);
		server.start();
		server.join();
		
	}
	
	private static Map<String, String> loadDictionary(String fileName) throws IOException {
		Map<String, String> dict = new HashMap<>();
		
		try( BufferedReader reader = new BufferedReader( new FileReader(fileName) ) ) {
			String line;
			while( (line=reader.readLine()) != null ) {
				String[] parts = line.split("#", 2);
				if( parts.length == 2 ) {
					String word = parts[0].trim();
					String emneddingVector = parts[1].trim();
					dict.put(word, emneddingVector);
				}
			}
		}
		
		return dict;
	}
	
	private static Set<String> loadStopwords(String fileName) throws IOException {
		Set<String> stopWords = new HashSet<>();
		
		try( BufferedReader reader = new BufferedReader( new FileReader(fileName) ) ) {
			String line;
			while( (line=reader.readLine()) != null ) {
				stopWords.add( line.trim() );
			}
		}
		
		return stopWords;
	}
	
	private static Map<String, ModelInfo> loadModels(String fileName) throws IOException {
		
		Map<String, ModelInfo> models = new HashMap<>();
		try( BufferedReader reader = new BufferedReader(new FileReader(fileName)) ) {
			StringBuilder content = new StringBuilder();
			String line;
			
			while( (line = reader.readLine()) != null ) {
				content.append(line);
			}
			
			JsonObject jsonObject = JsonParser.parseString(content.toString()).getAsJsonObject();
			JsonArray modelsArray = jsonObject.getAsJsonArray("models");
			
			for(JsonElement modelElement : modelsArray) {
				JsonObject modelObj = modelElement.getAsJsonObject();
				String modelname = modelObj.get("modelname").getAsString();
				String url = modelObj.get("url").getAsString();
				
				List<ClassInfo> classes = new ArrayList<>();
				Map<String, String> codeToValueMap = new HashMap<>();
				JsonArray classesArray = modelObj.getAsJsonArray("classes");
				for(JsonElement classElement : classesArray) {
					JsonObject classObj = classElement.getAsJsonObject();
					String code = classObj.get("code").getAsString();
					String value = classObj.get("value").getAsString();
					
					classes.add( new ClassInfo(code, value) );
					codeToValueMap.put(code, value);
				}
				models.put(modelname, new ModelInfo(modelname, url, classes, codeToValueMap) );
			}
		}
		
		return models;
	}
	
	static class ModelInfo {
		private final String modelName;
		private final String url;
		private final List<ClassInfo> classes;
		private final Map<String, String> codeToValueMap;
		
		public ModelInfo(String modelname, String url, List<ClassInfo> classes, Map<String, String> codeToValueMap) {
			this.modelName = modelname;
			this.url = url;
			this.classes = classes;
			this.codeToValueMap = codeToValueMap;
		}
		
		public String getModelName() {
			return modelName;
		}
		
		public String getUrl() {
			return url;
		}
		
		public List<ClassInfo> getClasses() {
			return classes;
		}
		
		public String getValueForCode(String code) {
			return codeToValueMap.getOrDefault(code, code);
		}
	}
	
	static  class ClassInfo {
		private final String code;
		private final String value;
		
		public ClassInfo(String code, String value) {
			this.code = code;
			this.value = value;
		}
		
		public String getCode() {
			return code;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	private static String processInput(String input, Map<String, String> dictionary, Set<String> stopwords) {
		String[] words = input.split("\\s+");
		StringBuilder result = new StringBuilder();
		
		for(String word : words) {
			String lowercaseWord = word.toLowerCase();
			
			if( !lowercaseWord.isEmpty() ) {
				String embeddongVector = dictionary.get(lowercaseWord);
				if( embeddongVector != null && !stopwords.contains(embeddongVector) ) {
					result.append(embeddongVector).append(" ");
				}
			}
		}
		
		return result.toString().trim();
	}

	private static String callModelService(String modelUrl, String query) throws IOException {
		URL url = new URL(modelUrl);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);
		
		JsonObject requestJson = new JsonObject();
		requestJson.addProperty("query", query);
		
		try( OutputStream os = conn.getOutputStream() ) {
			byte[] input = new Gson().toJson(requestJson).getBytes("UTF-8");
			os.write(input, 0, input.length);
		}
		
		StringBuffer response = new StringBuffer();
		try( BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8")) ) {
			String line;
			while( (line = br.readLine()) != null ) {
				response.append(line);
			}
		}
		
		conn.disconnect();
		return response.toString();
	}
	
	static class InferenceServlet extends HttpServlet {
		private final Map<String, String> dictionary;
		private final Set<String> stopwords;
		private final Map<String, ModelInfo> models;
		private final Gson gson = new Gson();
		
		public InferenceServlet(Map<String, String> dictionary, Set<String> stopwords, Map<String, ModelInfo> models ) {
			this.dictionary = dictionary;
			this.stopwords = stopwords;
			this.models = models;
		}
		
		protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
			handleRequest(req, res, false);
		}
		
		protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
			handleRequest(req, res, true);
		}
		
		private void handleRequest(HttpServletRequest req, HttpServletResponse res, boolean isPost) throws IOException {
			res.setContentType("application/json;charset=UTF-8");
			
			try {
				String modelName;
				String[] queries;
				
				if (isPost) {
					StringBuilder sb = new StringBuilder();
					try( BufferedReader reader = req.getReader() ){
						String line;
						while( (line=reader.readLine()) != null ) {
							sb.append(line);
						}
						
						JsonObject jsonRequest = JsonParser.parseString(sb.toString()).getAsJsonObject();
						modelName = jsonRequest.get("modelname").getAsString();
						
						JsonArray queriesArray = jsonRequest.getAsJsonArray("queries");
						queries = new String[queriesArray.size()];
						
						for(int i=0; i<queriesArray.size(); i++) {
							queries[i] = queriesArray.get(i).getAsString();
						}
					}
				} else {
					modelName = req.getParameter("modelname");
					queries = req.getParameterValues("query");
				}
				
				if (modelName == null || queries == null || queries.length == 0) {
					res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					res.getWriter().println(gson.toJson(Map.of("error", "Missing modelname or query parameters")));
					return;
				}

				ModelInfo model = models.get(modelName);
				if( model == null ) {
					res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					HashMap<String, String> errMap = new HashMap<>();
					errMap.put("error", "Model not found: " + modelName);
					res.getWriter().println(gson.toJson(errMap));
					return;
				}
				
				JsonArray resultsArray = new JsonArray();
				for(String query : queries) {
					
					try {
						String processedQuery = processInput(query, dictionary, stopwords);
						String modelResponse = callModelService(model.getUrl(), processedQuery);
						
						JsonObject modelResponseJson = JsonParser.parseString(modelResponse).getAsJsonObject();
						String resultCode = modelResponseJson.get("result").getAsString(); // Get classification code
						String resultValue = model.getValueForCode(resultCode);
						resultsArray.add(resultValue);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				JsonObject jsonResponse = new JsonObject();
				jsonResponse.add("results", resultsArray);
				
				res.setStatus(HttpServletResponse.SC_OK);
				res.getWriter().println(gson.toJson(jsonResponse));
				
			} catch (Exception e) {
				res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				res.getWriter().println(gson.toJson(Map.of("error", "Internal server error")));
				e.printStackTrace();
			}
		}
	}
/*	
	static class InferencdHandler extends AbstractHandler {

		private final Map<String, String> dictionary;
		private final Set<String> stopwords;
		private final Map<String, ModelInfo> models;
		private final Gson gson = new Gson();
		
		public InferencdHandler(Map<String, String>dictionary, Set<String>stopwords, Map<String, ModelInfo>models) {
			this.dictionary = dictionary;
			this.stopwords = stopwords;
			this.models = models;
		}
		
		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			
			response.setContentType("application/json;charset=UTF-8");
			baseRequest.setHandled(true);
			
			if( !request.getMethod().equals("POST") ) {
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				HashMap<String, String> errorMap = new HashMap<>();
				errorMap.put("error", "Method not allowed");
				response.getWriter().println(gson.toJson(errorMap));
				return;
			}
			
			try {
				// READ JSON Request
				StringBuffer requestBody = new StringBuffer();
				String line;
				BufferedReader reader = request.getReader();
				while( (line=reader.readLine()) != null) {
					requestBody.append(line);
				}
				
				// Parse Request
				JsonObject jsonRequest = JsonParser.parseString( requestBody.toString() ).getAsJsonObject();
				String modelName = jsonRequest.get("modelname").getAsString();
				JsonArray queries = jsonRequest.getAsJsonArray("queries");
				
				ModelInfo model = models.get(modelName);
				if( model == null ) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					HashMap<String, String> errMap = new HashMap<>();
					errMap.put("error", "Model not found: " + modelName);
					response.getWriter().println(gson.toJson(errMap));
					return;
				}
				
				JsonArray resultsArray = new JsonArray();
				for(int i=0; i<queries.size(); i++) {
					String query = queries.get(i).getAsString();
					
					String processedQuery = processInput(query, dictionary, stopwords);
					String modelResponse = callModelService(model.getUrl(), processedQuery);
					
					JsonObject modelResponseJson = JsonParser.parseString(modelResponse).getAsJsonObject();
					String resultCode = modelResponseJson.get("result").getAsString(); // Get classification code
					String resultValue = model.getValueForCode(resultCode);
					
					resultsArray.add(resultValue);
				}
				
				JsonObject jsonResponse = new JsonObject();
				jsonResponse.add("results", resultsArray);
				
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println(gson.toJson(jsonResponse));
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	*/
}
