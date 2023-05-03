package com.example.demo;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Util {
	public DecimalFormat decFmt = new DecimalFormat("###,###");
	public Util () {
	}

	public int checkInt(String val) {
		try {
			int i = Integer.parseInt(val);
			return i;
		} catch (NumberFormatException e) {
			return Integer.MIN_VALUE;
		}
	}

	public String[] split(String str) {
		str += " "; // To detect last token when not quoted...
		ArrayList<String> strings = new ArrayList<String>();
		boolean inQuote = false;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '"' || c == ' ' && !inQuote) {
				if (c == '"')
					inQuote = !inQuote;
				if (!inQuote && sb.length() > 0) {
					strings.add(sb.toString());
					sb.delete(0, sb.length());
				}
			} else
				sb.append(c);
		}
		return strings.toArray(new String[strings.size()]);
	}

	public String elapse(long start, long end) {
		String elapse = "";
		int elapseTime = (int) (end-start);
		if (elapseTime >= 1000) {
			elapse = this.decFmt.format(elapseTime/1000.0)+"sec";
		} else {
			elapse = this.decFmt.format(elapseTime)+"ms";
		}
		return "[소요시간:"+elapse+"]";
	}

	public void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
	}
}