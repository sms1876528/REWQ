package com.test.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.test.StateManager;

public class MyServlet extends HttpServlet {

	private static final long serialVersionUID = 8572241974921679005L;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String requestURL = req.getRequestURI().toString();
		String path = requestURL.substring(requestURL.lastIndexOf("/") + 1);
		
		///
		System.out.println("Request : "+ req.getRequestURL());
		String [] words = req.getPathInfo().toString().split("/"); 
		String command = words[1];
		
		if (command.equals("REPORT")) {
		
		}
		///
		
		System.out.println("Path : " + path);
		
		try {
			StateManager.get(path).run();
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String requestStr = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		CommandRequest requestData = this.gson.fromJson(requestStr, CommandRequest.class);
		
		switch( request.getPathInfo() ) {
		case "/fromServer":
			List<String> result = new ArrayList<>();
			for(String device : requestData.getTargetDevice()) {
				DeviceInfo deviceInfo = this.deviceInfoMap.get(device);
				String forwardCmd = this.serverCmdInfoMap.get(requestData.getCommand()).getForwardCmd();
				
				String cmdResponse = null;
				try {
					cmdResponse = sendPostRequest(
							String.format("http://%s:%d/fromEdge", deviceInfo.getHostname(), deviceInfo.getPort()),
							new CommandRequest(forwardCmd, null, requestData.getParam()).toJson(gson));
				} catch(Exception e) {
					e.printStackTrace();
				}
				
				CommandResponse responseFromDevice = gson.fromJson(cmdResponse, CommandResponse.class);
				result.addAll(responseFromDevice.getResult());
			}
			response.setStatus(200);
			response.getWriter().write(new CommandResponse(result).toJson(gson));
			break;
		}
	}
	
	private String sendPostRequest(String url, String content) throws Exception {
		HttpClient httpClient = new HttpClient();
		httpClient.start();
		try {
			org.eclipse.jetty.client.api.Request request = httpClient.POST(url);
			request.header(HttpHeader.CONTENT_TYPE, "application/json");
			request.content(new StringContentProvider(content), "utf-8");
			ContentResponse response = request.send();
			return new String(response.getContent());
		} catch(ExecutionException e) {
			e.printStackTrace();
		} finally {
			httpClient.stop();
		}
		return null;
	}
	
	protected void doPost2(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		System.out.println("Request : "+ req.getRequestURL());
		
		Gson gson = new Gson();
		JsonObject resJson = new JsonObject();		

		// read body /////////////////////////////////////////////////////////////////////////////////
		BufferedReader input = new BufferedReader(new InputStreamReader(req.getInputStream()));
		String buffer;
		StringBuilder sb = new StringBuilder();
		while ((buffer = input.readLine()) != null) {
			sb.append(buffer + "\n");
		}
		String strBody = sb.toString();
		input.close();
		
		JsonObject jsonBody = gson.fromJson(strBody, JsonObject.class);
		String managerId = jsonBody.get("ManagerID").getAsString();
		String reportId = jsonBody.get("ReportID").getAsString();		
		///////////////////////////////////////////////////////////////////////////////////////////////
		
		String [] words = req.getPathInfo().toString().split("/"); 
		String command = words[1];
		
		switch(command) {
		case "FINISH":
		{
				//lockEnd.lock();
				
				resJson.addProperty("Result", "Ok");
				//ValidatorReport.cancelTimer(Integer.parseInt(reportId));
				//ValidatorReport.saveReportFile(reportId, command);
				//ValidatorReport.removeReport(reportId);
				//Logger.WriteLog(managerId, command, reportId);
				//lockEnd.unlock();
			}
			break;
		case "FAIL":
		{
				//lockEnd.lock();

				resJson.addProperty("Result", "Ok");
				//ValidatorReport.cancelTimer(Integer.parseInt(reportId));
				//ValidatorReport.saveReportFile(reportId, command);
				//ValidatorReport.removeReport(reportId);
				//Logger.WriteLog(managerId, command, reportId);
				//lockEnd.unlock();				
			}
			break;
		}		
		
		res.setStatus(200);
		res.setContentType("application/json");
		res.getWriter().print(resJson.toString());
		res.getWriter().flush();
	}
	
}
