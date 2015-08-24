package com.amap.bigdata.fuse.tools.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GDFAnalyzer {

	private static String[] keyArray = new String[] { "y", "M", "w", "d", 
		                                              "f","l", "t", "h", 
		                                              "m", "s", "z"};
	private static Map<String, Integer> indexMap = new HashMap<String, Integer>();
	static {
		indexMap.put("y", 0);
		indexMap.put("M", 1);
		indexMap.put("w", 2);
		indexMap.put("d", 3);

		indexMap.put("f", 4);
		indexMap.put("l", 5);
		indexMap.put("t", 6);
		indexMap.put("h", 7);

		indexMap.put("m", 8);
		indexMap.put("s", 9);
		indexMap.put("z", 10);
	}

	private static Map<Integer, String> modelMap = new HashMap<Integer, String>();
	static {
		modelMap.put(0, "%s年");
		modelMap.put(1, "%s月");
		modelMap.put(2, "%s周");
		modelMap.put(3, "%s日");

		modelMap.put(4, "");
		modelMap.put(5, "");
		modelMap.put(6, "周%s");
		modelMap.put(7, "%s");

		modelMap.put(8, "%s");
		modelMap.put(9, "%s");
		modelMap.put(10, "%s");
	}

	private static Map<Integer, String> zbeginMap = new HashMap<Integer, String>();
	static {
		zbeginMap.put(4, "节假日");
		zbeginMap.put(5, "春季");
		zbeginMap.put(6, "夏季");
		zbeginMap.put(7, "秋季");

		zbeginMap.put(8, "冬季");
		zbeginMap.put(13, "雨季、汛期");
		zbeginMap.put(14, "干季");
	}

	private static String[] symbol = new String[] { "+", "-", "*" };

//	private static final Pattern time = Pattern.compile("([0-1]{1}\\d|2[0-4]):([0-5]\\d)");
//	private static final Pattern buding = Pattern.compile("d\\d{1,}");

	/**
	 * 解析GDF格式
	 * 
	 * @param gdfStr
	 * @return -1:GDF格式不正确
	 */
	public static String decode(String gdfStr) {
		if (gdfStr == null || gdfStr.length() == 0) {
			return "-1";
		}
		String gdfReg = "^\\[(.*?)\\]$";
		if(!gdfStr.matches(gdfReg)) {
			return "-1";
		}
		try {
			Pattern p = Pattern.compile("\\[\\((.*?)\\).*?\\]");
			Matcher m = p.matcher(gdfStr);
			while (m.find()) {
				String unitValue = m.group(0);
				String key  = transferUnit(unitValue);
				String value = gdfUnitToDate(unitValue);
				gdfStr = gdfStr.replaceFirst(key, value); 
			}
			if(gdfStr.startsWith("\\[") && gdfStr.endsWith("\\]")) {
				gdfStr = gdfStr.substring(1, gdfStr.length() - 1);
			}
			return parseExpression(gdfStr);
		} catch (Exception e) {
			return "-1";
		}
	}
	
	private static String parseExpression(String gdfExp) {
		String oneOperReg = "^[^\\[\\+\\-\\*]+[\\+\\-\\*]{1}[^\\+\\-\\*\\]]+$"; //只包含一个运算符
		if(gdfExp.matches(oneOperReg)) {
			return cal(gdfExp);
		}
		
		String noBraReg = "^[^\\[\\]]+$";  //不包含中括号
		String mutliReg = "[^\\[\\+\\-\\*]+[\\*][^\\[\\+\\-\\*]+"; //优先级高的运算符 *
		String pmReg = "[^\\[\\+\\-\\*]+[\\+\\-][^\\[\\+\\-\\*]+"; //运算符 +,-
		if(gdfExp.matches(noBraReg)) {
			Pattern p = Pattern.compile(mutliReg);
			Matcher m = p.matcher(gdfExp);
			if(m.find()) {
				String subExp = m.group(0);
				gdfExp = gdfExp.replaceFirst(mutliReg, parseExpression(subExp));
				return parseExpression(gdfExp);
			}
			else {
				p = Pattern.compile(pmReg);
				m = p.matcher(gdfExp);
				if(m.find()) {
					String subExp = m.group(0);
					gdfExp = gdfExp.replaceFirst(pmReg, parseExpression(subExp));
					return parseExpression(gdfExp);
				}
			}
		}
		
		//带括号
		String bracketsReg = "\\[[^\\[\\]]+\\]";
		Pattern bracketsPatther = Pattern.compile(bracketsReg);
		Matcher bracketsMather = bracketsPatther.matcher(gdfExp);
		if(bracketsMather.find()) {
			String subExp = bracketsMather.group(0);
			gdfExp = gdfExp.replaceFirst(bracketsReg, parseExpression(subExp.substring(1, subExp.length() - 1)));
			return parseExpression(gdfExp);
		}
		return gdfExp;
	}
	
	/**
	 * 两两计算
	 */
	private static String cal(String exp) {
		if(exp.matches("^[^\\+\\-\\*]+\\+[^\\+\\-\\*]+$")) {
			return calByPlus(exp);
		}
		else if(exp.matches("^[^\\+\\-\\*]+\\*[^\\+\\-\\*]+$")) {
			return calByMulti(exp);
		}
		else if(exp.matches("^[^\\+\\-\\*]+\\-[^\\+\\-\\*]+$")) {
			return calByMinus(exp);
		}
		return null;
	}
	
	private static String calByPlus(String exp) {
		return exp.replaceFirst("\\+", "和");
	}
	
	private static String calByMinus(String exp) {
		int index = exp.indexOf("-");
		String part1 = exp.substring(0, index);
		String part2 = exp.substring(index + 1);
		return part1 + " " + part2 + "除外";
	}
	
	private static String calByMulti(String exp) {
		return exp.replaceFirst("\\*", " ");
	}

	private static String transferUnit(String unit) {
		return unit.replace("[", "\\[").replace("]", "\\]").replace("{", "\\{")
				.replace("}", "\\}").replace("(", "\\(").replace(")", "\\)");
	}
	
	private static String gdfUnitToDate(String gdfUnit) throws Exception {
		int start1 = 1;
		int end1 = gdfUnit.indexOf(")", start1);
		String date = gdfUnit.substring(start1 + 1, end1);
		int start2 = end1 + 1;
		String to = "";
		if ("{".equals(gdfUnit.substring(start2, start2 + 1))) {
			int end2 = gdfUnit.indexOf("}", start2);
			to = gdfUnit.substring(start2 + 1, end2);
		}
		return analyzeDate(date, to);
	}

	private static Map<String, String> cache = new HashMap<String, String>();
	static {
		cache.put("M1", "1月");
		cache.put("M2", "2月");
		cache.put("M3", "3月");
		cache.put("M4", "4月");
		cache.put("M5", "5月");
		cache.put("M6", "6月");
		cache.put("M7", "7月");
		cache.put("M8", "8月");
		cache.put("M9", "9月");
		cache.put("M10", "10月");
		cache.put("M11", "11月");
		cache.put("M12", "12月");

		cache.put("t1", "周日");
		cache.put("t2", "周一");
		cache.put("t3", "周二");
		cache.put("t4", "周三");
		cache.put("t5", "周四");
		cache.put("t6", "周五");
		cache.put("t7", "周六");

		cache.put("h0", "00");
		cache.put("h1", "01");
		cache.put("h2", "02");
		cache.put("h3", "03");
		cache.put("h4", "04");
		cache.put("h5", "05");
		cache.put("h6", "06");
		cache.put("h7", "07");
		cache.put("h8", "08");
		cache.put("h9", "09");
		cache.put("h10", "10");
		cache.put("h11", "11");
		cache.put("h12", "12");
		cache.put("h13", "13");
		cache.put("h14", "14");
		cache.put("h15", "15");
		cache.put("h16", "16");
		cache.put("h17", "17");
		cache.put("h18", "18");
		cache.put("h19", "19");
		cache.put("h20", "20");
		cache.put("h21", "21");
		cache.put("h22", "22");
		cache.put("h23", "23");
<<<<<<< HEAD
		
		cache.put("m00", "00");
		cache.put("m0", "00");
		cache.put("m05", "05");
		cache.put("m5", "05");
		cache.put("m10", "10");
		cache.put("m15", "15");
		cache.put("m20", "20");
		cache.put("m25", "25");
		cache.put("m30", "30");
		cache.put("m35", "35");
		cache.put("m40", "40");
		cache.put("m45", "45");
		cache.put("m50", "50");
		cache.put("m55", "55");
=======
>>>>>>> 98176acf88469ecea6b8ecc4b05761ff4c1a4a79
	}

	private static String analyzeDate(String date, String to) throws Exception {
		if (date == null || date.length() == 0) {
			throw new Exception("GDF格式不正确,日期为空！");
		}
		int length = keyArray.length;
		int[] startArray = new int[length];
		Pattern p = Pattern.compile("[yMwdflthmsz][0-9]+");
		Matcher m = p.matcher(date);
		while (m.find()) {
			String v = m.group(0);
			String key = v.substring(0, 1);
			int value = Integer.valueOf(v.substring(1, v.length()));
			startArray[indexMap.get(key)] = value;
		}
		int[] futherArray = new int[length];
		Matcher m1 = p.matcher(to);
		while (m1.find()) {
			String v = m1.group(0);
			String key = v.substring(0, 1);
			int value = Integer.valueOf(v.substring(1, v.length()));
			futherArray[indexMap.get(key)] = value;
		}

		int[] toArray = new int[length];
		int carry = 0;
		int s1 = startArray[10];
		int to1 = futherArray[10];
		toArray[10] = Integer.valueOf(s1 + "" + to1);
		s1 = startArray[9];
		to1 = futherArray[9];
		int v = s1 + to1 + carry;
		toArray[9] = v % 60;
		carry = v / 60;
		s1 = startArray[8];
		to1 = futherArray[8];
		v = s1 + to1 + carry;
		toArray[8] = v % 60;
		carry = v / 60;
		s1 = startArray[7];
		to1 = futherArray[7];
		v = s1 + to1 + carry;
		toArray[7] = v % 60;
		carry = 0;
		s1 = startArray[3];
		to1 = futherArray[3];
		if(to1 < 1) {
			to1 = 1;
		}
		if(s1 == 0) {
			s1 = startArray[6];
			v = s1 + (to1 - 1);
			toArray[6] = v % 7;
		}
		else {
			toArray[3] = s1 + to1;
		}
		s1 = startArray[1];
		to1 = futherArray[1];
		if(to1 < 1) {
			to1 = 1;
		}
		v = s1 + (to1 - 1) + carry;
		toArray[1] = v;
		s1 = startArray[0];
		to1 = futherArray[0];
		v = s1 + to1 + carry;
		toArray[0] = v;

		StringBuilder desc1 = new StringBuilder();
		StringBuilder hms1 = new StringBuilder();
		StringBuilder desc2 = new StringBuilder();
		StringBuilder hms2 = new StringBuilder();

		String vstr = decodey(startArray[0]);
		if (vstr != null && vstr.length() > 0) {
			desc1.append(vstr);
		}
		vstr = decodey(toArray[0]);
		if (vstr != null && vstr.length() > 0) {
			desc2.append(vstr);
		}
		vstr = decodeM(startArray[1]);
		if (vstr != null && vstr.length() > 0) {
			desc1.append(vstr);
		}
		vstr = decodeM(toArray[1]);
		if (vstr != null && vstr.length() > 0) {
			desc2.append(vstr);
		}
		vstr = decoded(startArray[3]);
		if (vstr != null && vstr.length() > 0) {
			desc1.append(vstr);
		}
		vstr = decoded(toArray[3]);
		if (vstr != null && vstr.length() > 0) {
			desc2.append(vstr);
		}
		vstr = decodet(startArray[6]);
		if (vstr != null && vstr.length() > 0) {
			desc1.append(vstr);
		}
		vstr = decodet(toArray[6]);
		if (vstr != null && vstr.length() > 0) {
			desc2.append(vstr);
		}

		vstr = decodeh(startArray[7]);
		if (vstr != null && vstr.length() > 0) {
			hms1.append(vstr).append(":");
		}
		vstr = decodeh(toArray[7]);
		if (vstr != null && vstr.length() > 0) {
			hms2.append(vstr).append(":");
		}
		vstr = decodem(startArray[8]);
		if (vstr != null && vstr.length() > 0) {
			hms1.append(vstr);
		}
		vstr = decodem(toArray[8]);
		if (vstr != null && vstr.length() > 0) {
			hms2.append(vstr);
		}

		String result = "";
		if (!desc1.toString().equals(desc2.toString())) {
			result += desc1.toString() + "到" + desc2.toString();
		} else {
			result += desc1.toString();
		}
		vstr = decodez(toArray[10]);
		if (vstr != null && vstr.length() > 0) {
			if (result.length() > 0) {
				result += ",";
			}
			result += vstr;
		}
		if (!hms1.toString().equals(hms2.toString())) {
			if (result.length() > 0) {
				result += " ";
			}
			result += hms1.toString() + "到" + hms2.toString();
		} else {
			if (!"00:00".equals(hms1.toString())) {
				if (result.length() > 0) {
					result += " ";
				}
				result += hms1.toString();
			}
		}

		return result;
	}

	private static String decodey(int value) {
		if (value <= 0) {
			return null;
		}
		String model = modelMap.get(0);
		String v = model.replaceFirst("%s", value + "");
		return v;
	}

	private static String decodeM(int value) {
		if (value <= 0) {
			return null;
		}
		String model = modelMap.get(1);
		String v = model.replaceFirst("%s", value + "");
		return v;
	}

	private static String decodew(int value) {
		String model = modelMap.get(2);
		String v = model.replaceFirst("%s", value + "");
		if (v.length() < 2) {
			v = "0" + v;
		}
		return v;
	}

	private static String decoded(int value) {
		if (value <= 0) {
			return null;
		}
		String model = modelMap.get(3);
		String v = model.replaceFirst("%s", value + "");
		if (v.length() < 2) {
			v = "0" + v;
		}
		return v;
	}

	private static String decodef(int value) {
		String model = modelMap.get(4);
		String v = model.replaceFirst("%s", value + "");
		if (v.length() < 2) {
			v = "0" + v;
		}
		return v;
	}

	private static String decodel(int value) {
		String model = modelMap.get(5);
		String v = model.replaceFirst("%s", value + "");
		if (v.length() < 2) {
			v = "0" + v;
		}
		return v;
	}

	private static String decodet(int value) {
		if (value <= 0) {
			return null;
		}
		String v = decodeDate(value, "t");
		if (v == null) {
			String model = modelMap.get(6);
			v = model.replaceFirst("%s", (value - 1) + "");
			if (v.length() < 2) {
				v = "0" + v;
			}
		}
		return v;
	}

	private static String decodeh(int value) {
<<<<<<< HEAD
		if (value < 0) {
			return null;
		}
		String v = decodeDate(value, "h");
		if(v == null) {
			String model = modelMap.get(7);
			v = model.replaceFirst("%s", value + "");
			if (v.length() < 2) {
				v = "0" + v;
			}
		}		
=======
		String model = modelMap.get(7);
		String v = model.replaceFirst("%s", value + "");
		if (v.length() < 2) {
			v = "0" + v;
		}
>>>>>>> 98176acf88469ecea6b8ecc4b05761ff4c1a4a79
		return v;
	}

	private static String decodem(int value) {
<<<<<<< HEAD
		if (value < 0) {
			return null;
		}
		String v = decodeDate(value, "m");
		if(v == null) {
			String model = modelMap.get(8);
			v = model.replaceFirst("%s", value + "");
			if (v.length() < 2) {
				v = "0" + v;
			}
=======
		String model = modelMap.get(8);
		String v = model.replaceFirst("%s", value + "");
		if (v.length() < 2) {
			v = "0" + v;
>>>>>>> 98176acf88469ecea6b8ecc4b05761ff4c1a4a79
		}
		return v;
	}

	private static String decodes(int value) {
		String model = modelMap.get(9);
		String v = model.replaceFirst("%s", value + "");
		if (v.length() < 2) {
			v = "0" + v;
		}
		return v;
	}

	private static String decodez(int value) {
		String valueStr = value + "";
		int length = valueStr.length();
		if (length < 3) {
			return "";
		}
		String c = valueStr.substring(length - 2);
		String t = valueStr.substring(0, length - 2);
		String v = zbeginMap.get(Integer.valueOf(t));
		return v;
	}

	private static String decodeDate(int value, String key) {
		return cache.get(key + value);
	}

	public static String encode(String timeDesc) {
		return null;
	}

	public static void main(String[] args) {
		try {
			System.out.println(decode("[(h12m40)]"));
			System.out.println(decode("[[(t7){d2}]*[[(h6m20){h3m30}]+[(h16m20){h3m30}]]]"));
			System.out.println(decode("[[[(h8){h14}]*[(t2){d5}]]-[(z4){z54}]]"));
			System.out.println(decode("[[(h7){h3m20}]+[(h11m30)]+[(h12m25)]+[(h13m25)]+[(h14m20)]+[(h15m20)]+[(h16)]+[(h17){h2}]]"));
			//System.out.println(parseExpression("06:20+19:50*34234"));
//			System.out.println(cal("06:20到09:50+16:20到19:50+2"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}