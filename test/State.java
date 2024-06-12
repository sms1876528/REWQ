package com.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class State {
	private String name;
	private String url;
	private List<String> parameters;
	
	public State(String name, String url, List<String> parameters) {
		this.name = name;
		this.url = url;
		this.parameters = parameters;
	}

	public String getName() {
		return this.name;
	}
	
	public void run() throws Exception {
		HttpClient client = new HttpClient();
		client.start();
		
		try {
			String query = makeQuery();
			System.out.println("Send query : " + url + query);
			ContentResponse contentResponse = client.newRequest(url + query).method(HttpMethod.GET).send();
			JsonObject responsJObj = new Gson().fromJson(contentResponse.getContentAsString(), JsonObject.class);
			System.out.println("Response : " + responsJObj.toString());
			
			for( String key: responsJObj.keySet() ) {
				VariableManager.put(key, responsJObj.get(key).getAsString());
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private String makeQuery() throws UnsupportedEncodingException {
		String query = "";
		for(int i=0; i<parameters.size(); i++) {
			if( i==0 ) {
				query += "?";
			}
			query += URLEncoder.encode(parameters.get(i), "UTF-8") + "=" +
					 URLEncoder.encode(VariableManager.get(parameters.get(i)), "UTF-8");
			if( i<parameters.size()-1 ) {
				query += "&";
			}
		}
		
		return query;
	}

	public void diifWay() throws Exception {
		Map<String, String> commandMap = new HashMap<String, String>();
		
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader("INFO/SERVER_COMMAND.TXT"));
		while( (line=br.readLine()) != null) {
			String[] dataArr = line.split("#");
			commandMap.put(dataArr[0], dataArr[1]);
		}
		br.close();
		
		Scanner scanner = new Scanner(System.in);
		//while(scanner.hasNext()) {
			String[] inputArr = scanner.next().split("#"); // CMD_001#DEVICE_069#fe303156
			String cmd = inputArr[0];
			String[] deviceArr = inputArr[1].split(",");
			String param = inputArr[2];
			
			for(String device : deviceArr) {
				String filename = String.format("DEVICE/REQ_TO_%s.TXT", device);
				String content = String.format("%s#%s", commandMap.get(cmd), param);
				writeFile(filename, content);
				Thread.sleep(500);
				
				filename = String.format("DEVICE/RES_FROM_%s.TXT", device);
				readFile(filename);
			}
		//}
	}
	
	private void writeFile(String fname, String content) throws Exception {
		Files.write(Paths.get(fname), (content + "\n").getBytes(), StandardOpenOption.CREATE);
	}

	private String readFile(String fname) throws Exception {
		return Files.readAllLines(Paths.get(fname)).get(0);
	}
}
