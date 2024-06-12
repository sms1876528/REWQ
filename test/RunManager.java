package com.test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.test.server.MyServer;

public class RunManager {

	public static void main(String[] args) throws Exception {
		
		VariableManager.load();
		StateManager.load();
		
		MyServer server = new MyServer();
		server.start();
	}
	
	/*
	//create#action#http://127.0.0.1:8011/create#id
	//add#action#http://127.0.0.1:8012/add#id,data
	//fetch#action#http://127.0.0.1:8013/fetch#
	
	public static void main2(String[] args) throws Exception {
		
		static Scanner scanner = new Scanner(System.in);
		Map<String, StateManager> states = new HashMap<>();
		for(String line : Files.readAllLines(Paths.get("STATE.TXT"))) {
			String[] elements = line.split("#");
			String name = elements[0];
			String type = elements[1];
			String url  = elements[2];
			String[] keys = null;
			if(elements.length > 3) {
				keys = elements[3].split(",");
			}
			
			states.put(name, new StateManager(type, url, keys) );
		}
		
		VariableManager.load();
		
		while(true) {
			String request = scanner.nextLine();
			
			StateManager state = states.get(request);
			String print = state.getType() + " " + state.getUrl();
			String[] keys = state.getKeys();
			if(keys != null) {
				for(int i=0; i<keys.length; i++) {
					if(i == 0) {
						print += "?";
					} else if(i > 0) {
						print += "&";
					}
					print += keys[i] + "=" + VariableManager.get(keys[i]);
				}
			}
			System.out.println(print);
		}
	}
	*/

}
