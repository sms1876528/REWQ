package com.test.j;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

public class JsonEx {

	public static void main(String[] args) throws Exception {
		
		String filePath = "sample.json";
		{
			JsonObject jObj = new JsonObject();
			jObj.addProperty("name", "spiderman");
			jObj.addProperty("age", 45);
			jObj.addProperty("married", true);
			
			JsonArray  jArr = new JsonArray();
			jArr.add("martial art");
			jArr.add("gun");
			jObj.add("specialty", jArr);
			
			JsonObject jObj2 = new JsonObject();
			jObj2.addProperty("1st", "done");
			jObj2.addProperty("2nd", "expected");
			jObj2.add("3rd", null);
			jObj.add("vaccine", jObj2);
			
			JsonArray  jArr2 = new JsonArray();
			jObj2 = new JsonObject();
			jObj2.addProperty("name", "spiderboy");
			jObj2.addProperty("age", 10);
			jArr2.add(jObj2);
			
			jObj2 = new JsonObject();
			jObj2.addProperty("name", "spidergirl");
			jObj2.addProperty("age", 9);
			jArr2.add(jObj2);
			
			jObj.add("children", jArr2);
			jObj.add("address", null);
			
			try(Writer writer = new FileWriter("sample.json")) {
				Gson gson = new GsonBuilder().serializeNulls().create();
				gson.toJson(jObj, writer);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		
		try {
			Gson gson = new Gson();
			JsonReader reader = new JsonReader(new FileReader(filePath));
			JsonObject jObj = gson.fromJson(reader, JsonObject.class);
			
			String name = jObj.get("name").getAsString();
			int age = jObj.get("age").getAsInt();
			System.out.println("name:" + name + " age: " + age);
			
			JsonArray jArr = jObj.get("children").getAsJsonArray();
			JsonObject jObj2 = jArr.get(1).getAsJsonObject();
			name = jObj2.get("name").getAsString();
			age = jObj2.get("age").getAsInt();
			System.out.println("name:" + name + " age: " + age);
			
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonObject jo = new JsonObject();
		
		jo.addProperty("company", "BBB");
		jo.addProperty("address", "BBC");
		
		JsonArray jArr = new JsonArray();
		String[] name = {"Alice", "Brown", "Ami"};
		String[] age = {"20", "25", "30"};
		boolean[] isNew = {true, false, true};
		
		for(int i=0; i<3; i++) {
			JsonObject joTmp = new JsonObject();
			joTmp.addProperty("name", name[i]);
			joTmp.addProperty("age", age[i]);
			joTmp.addProperty("isNew", isNew[i]);
			
			jArr.add(joTmp);
		}
		jo.add("newEmployees", jArr);
		
		String jsonStr = gson.toJson(jo);
		System.out.println(jsonStr);
		
		FileWriter fw = new FileWriter("lecture2.json");
		gson.toJson(jo, fw);
		fw.flush();
		fw.close();
	}
}
