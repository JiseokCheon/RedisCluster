package com.example.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Config {
	Util util = new Util();
	public Config() {
	}

	public void loadConfig() {
		String fileName = Main.workDir+ Main.clusterConfigFileName;
		File file = new File(fileName);
		int idx = 1;
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// Tab(9), Carriage return(13), Space(32), , #(35),  
				if (line.charAt(0)==9 || line.charAt(0)==13 || line.charAt(0)==32 || line.charAt(0)==35 ) continue;
				// space, tab이 여러 개 있으면 name.length=1, 바로 new-line이면 length=0
				String name[] = util.split(line);
				if (name.length <= 1) continue;	// 유효한 문자가 없으면 바로 다음 라인을 읽는다. 
				System.out.println(idx+":"+line);
				idx++;
				Main.serverList.add(name);
			}
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}