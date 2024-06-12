package com.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class util {

	public void LogReader() throws Exception {
		File file = new File("LOG.TXT");
		while( !file.exists() ) {
			Thread.sleep(100);
			continue;
		}
		
		BufferedReader br = new BufferedReader(new FileReader("LOG.TXT"));
		while(true) {
			String line = br.readLine();
			if( line == null ) {
				Thread.sleep(1);
				continue;
			}
			
			String[] words = line.split(" - ");
			String[] strNums = words[1].split(" ");
			int sum = Integer.parseInt(strNums[0]) + Integer.parseInt(strNums[1]);
			System.out.println(String.format("[%s] %d", LocalDateTime.now(), sum));
		}
	}
	
	public void getCurrentTime() {
		LocalDateTime now = LocalDateTime.now();
		String strDT = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
		
		long ct = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		String strCT = sdf.format(ct);
	}
	
	public void StrTime2Date() throws Exception {
		String strTime = "2022-03-31 21:40:15";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dt = sdf.parse(strTime);
	}
	
	public void getTimeGap() throws Exception {
		String start = "202203311142310";
		String end = "202203311142420";
		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
		
		Date d1 = sf.parse(start);
		Date d2 = sf.parse(end);
		long diff = d2.getTime() - d1.getDate();
		System.out.println( diff/1000 ); // SEC Diff
	}
	
	public void DisplayStr() {
		
		// 10진수 4자리로 
		int a = 14;
		System.out.println( String.format("%04d", a) ); // 0014
		
		// 16진수 
		int b = 14;
		System.out.println( String.format("%02X %02x", b, b) ); // 0E 0e
		
		// 소수점
		double c = 12.345678;
		System.out.println( String.format("%08.3f", c) ); // 0012.346
	}
	
		protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		System.out.println("Request :" + req.getRequestURI());
		
		////
		File destFolder = new File("OUT");
		if( !destFolder.exists() ) {
			destFolder.mkdirs();
		}
		
		LocalTime currentTime = LocalTime.now();
		String fileName = String.format("OUT/%02d%02d.JSON", currentTime.getHour(), currentTime.getMinute());
		PrintWriter printWriter = new PrintWriter(new FileWriter(new File(fileName)));
		
		BufferedReader input = new BufferedReader(new InputStreamReader(req.getInputStream()));
		String buffer;
		while( (buffer=input.readLine()) != null) {
			printWriter.print(buffer);
		}
		input.close();
		printWriter.close();
		////
		
		res.setStatus(200);
		res.getWriter().write(fileName + "saved");
	}
	
	public void ClientMain() throws Exception {
		String strFileList = getFileList();
		HttpClient httpClient = new HttpClient();
		httpClient.start();
		Request request = httpClient.newRequest("http://127.0.0.1:8080/listFile").method(HttpMethod.POST);
		request.header(HttpHeader.CONTENT_TYPE, "applicatin/json");
		request.content(new StringContentProvider(strFileList, "utf-8"));
		ContentResponse contentRes = request.send();
		System.out.println(contentRes.getContentAsString());
		httpClient.stop();
	}
	
	public String  getFileList() {
		Gson gson = new Gson();
		
		JsonObject jo = new JsonObject();
		File dir = new File("IN");
		jo.addProperty("Folder", "Input");
		
		JsonArray ja = new JsonArray();
		File[] fList = dir.listFiles();
		for(File file : fList) {
			ja.add(file.getName());
		}
		jo.add("FILES", ja);
		
		String res = jo.toString();
		return res;
	}
}
