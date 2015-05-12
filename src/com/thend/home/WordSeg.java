package com.thend.home;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apache.commons.io.FileUtils;

public class WordSeg {
	
	public static void main(String[] args) {
		if(args.length < 1) {
			System.out.println("Usage:\nstart file_dir");
			return;
		}
		File dir = new File(args[0]);
		if(!dir.exists()) {
			System.out.println("dir " + args[0] + " does not exist!");
			return;
		}
		for(File file : dir.listFiles()) {
			try {
				String content = FileUtils.readFileToString(file);
				List<Term> parse = ToAnalysis.parse(content);
				String fileName = file.getName();
				int idx = fileName.indexOf("\\.");
				if(idx != -1) {
					fileName = fileName.substring(0,idx) + "_seg" + fileName.substring(idx);
				} else {
					fileName += "_seg";
				}
				FileUtils.writeStringToFile(new File("seg_out/" + fileName), parse.toString());
				System.out.println(parse);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
