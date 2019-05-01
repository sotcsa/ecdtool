package io.bogar.ecdtool.bl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class NGComment {

	private HashMap<String, List<String>> data;

	public NGComment() {
		data = new HashMap<String, List<String>>();
	}

	public void addComment(String key, String value) {
		String keyUCase = key.toUpperCase();
		List<String> commentList;
		if (data.containsKey(keyUCase)) {
			commentList = data.get(keyUCase);
		} else {
			commentList = new ArrayList<String>();
			data.put(keyUCase, commentList);
		}
		commentList.add(value);
	}

	public String[] getComment(String key) {
		String uKey = key.toUpperCase();
		if (!data.containsKey(uKey))
			return null;
		return data.get(uKey).toArray(
				new String[data.get(uKey).size()]);
	}
	
	public String getCommentPlus(String name, String concatWith ) {
		StringBuilder sb = new StringBuilder();
		String[] cmt = getComment(name);
		if (cmt == null) {
			return null;
		}
		for (int i=0; i<cmt.length; i++) {
			if (i>0) {
				sb.append(concatWith);
			}
			sb.append(cmt[i]);
		}
		return sb.toString();
	}
	
	public void delComment(String key) {
		data.remove(key.toUpperCase());
	}
	
	public void dump(File file) throws Exception {
		if (file.exists()) {
			file.delete();
		}
		if (!file.createNewFile()) {
			throw new Exception("Can't create file");
		}
		FileOutputStream os = new FileOutputStream(file);
		OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);
		Set<String> keyset = data.keySet();
		String[] keyarray = keyset.toArray(new String[keyset.size()]);
		Arrays.sort(keyarray, 0, keyarray.length);
		for (String key : keyarray) {
			for (String value : data.get(key)) {
				bw.write(key+"="+value+"\n");
			}
		}
		bw.close();
	}

}
