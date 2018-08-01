package com.yan.common.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class FileUtil {

	/**
	 * 将内容写入到一个文件
	 * @param filePath 文件的全路径
	 * @param content 文件内容
	 * @param chartset 字符集
	 * @throws Exception 
	 */
	public static void writeToFile(String filePath, String content, String chartset) throws Exception {
		if(chartset == null || "".equals(chartset.trim())) {
			chartset = "UTF-8";
		}
		File file = new File(filePath);
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), chartset));
		writer.write(content);
		writer.flush();
		writer.close();
	}
	
	public static String readFromFile(String filePath, String chartset) throws Exception {
		if(chartset == null || "".equals(chartset.trim())) {
			chartset = "UTF-8";
		}
		File file = new File(filePath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), chartset));
		StringBuilder contentBuilder = new StringBuilder();
		String line = null;
		while((line = reader.readLine())!= null) {
			contentBuilder.append(line);
			contentBuilder.append("\n");
		}
		reader.close();
		return contentBuilder.toString();
	}
}
