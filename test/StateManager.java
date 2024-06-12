package com.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.test.StateManager.StatesDTO.StateDTO;

public class StateManager {
	private static Map<String, State> states = new ConcurrentHashMap<>();
	
	class StatesDTO{
		public Map<String, StateDTO> stateDto;
		
		class StateDTO {
			public String type;
			public String url;
			public List<String> parameters;
			
		}
	}
	
	public static State get(String name) {
		return states.get(name);
	}
	
	public static void load() throws JsonSyntaxException, IOException {
		StatesDTO dto = new Gson().fromJson(new String(Files.readAllBytes(Paths.get("STATE.JSON"))), StatesDTO.class);
		
		for( Entry<String, StateDTO> entry : dto.stateDto.entrySet() ) {
			states.put( entry.getKey(), makeState(entry.getKey(), entry.getValue()) );
		}
	}
	
	private static State makeState(String stateName, StateDTO dto) {
		if( "action".equals(dto.type) ) {
			return new State(stateName, dto.url, dto.parameters);
		}
		return null;
	}
}
