package com.hulk.util.common;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hulk on 18-3-19.
 */

public class ConvertUtil {

    /**
     * 读取输入流,返回读到的byte数组
     * 逐步把输入流读取到buffer中，写到　ByteArrayOutputStream　的　outSteam　对象中，
     * 读完之后, outSteam.toByteArray()得到需要的数组
     *
     * @param inStream
     * @param inStream
     * @return 字节数组
     * @throws IOException
     */
    public static byte[] readStream(InputStream inStream, boolean isCloseInput) throws IOException {
        ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            int len = -1;
            while ((len = inStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
            }
            return outSteam.toByteArray();
        } finally {
            if (outSteam != null) {
                outSteam.close();
            }
            if (isCloseInput && inStream != null) {
                inStream.close();
            }
        }
    }

    /**
     * 由输入流读取字符串．
     * 通过　BufferedReader　和　BufferedReader 一行一行的读，直到最后.
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static String readAsString(InputStream inputStream, boolean isCloseInput) throws IOException {
        BufferedReader reader = null;
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream);
            reader = new BufferedReader(inputStreamReader);
            String tempLine = null;
            StringBuffer resultBuffer = new StringBuffer();
            while ((tempLine = reader.readLine()) != null) {
                resultBuffer.append(tempLine);
            }
            return resultBuffer.toString();
        } finally {
            if (isCloseInput && inputStream != null) {
                inputStream.close();
            }
            if (reader != null) {
                reader.close();
            }

            if (inputStreamReader != null) {
                inputStreamReader.close();
            }
        }
    }

    /**
     * Convert byte[] to hex string.这里我们可以将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串。
     *
     * @param src byte[] data
     * @return hex string
     */
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * Convert hex string to byte[]
     *
     * @param hexString the hex string
     * @return byte[]
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase(Locale.getDefault());
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 去除字符串中的空格、回车、换行符、制表符
     * @param str
     * @return
     */
    public static String replaceBlank(String str) {
        String dest = "";
        if (str != null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }

    /**
     * 去掉每一行字符串首尾的空白，拼接为一行：
     * 回车、换行符、制表符
     * @param str
     * @return
     */
    public static String removeLineBlank(String str) {
        StringReader reader = new StringReader(str);
        BufferedReader bReader = new BufferedReader(reader);
        StringBuffer buff = new StringBuffer();
        try {
            String line;
            while ((line = bReader.readLine()) != null) {
                String trimLine = line.trim();
                if (trimLine != null && !"".equals(trimLine)) {
                    buff.append(trimLine);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buff.toString();
    }
    
    /**
     * 列出所有行,多行文本转换为行的List.
     * @param text
     * @return 多行文本转换为行的List.
     */
    public static List<String> listLines(String text) {
        return listLines(text, null);
    }
    
    /**
     * 列出包含"keywords"的所有行
     * @param text
     * @param keywords 目标行的关键词, 为空是列出所有行
     * @return
     */
    public static List<String> listLines(String text, String keywords) {
    	if(text == null) {
    		return null;
    	}
    	StringReader reader = new StringReader(text.trim());
        BufferedReader bReader = new BufferedReader(reader);
        List<String> lines = new ArrayList<String>();
        String line = null;
        try {
        	while ((line = bReader.readLine()) != null) {
        		String line2 = line.trim();
        		if(keywords != null && !keywords.equals("")) {
        			//存在目标就只列出包含destStr的行
        			if(line2.contains(keywords)) {
            			lines.add(line2);
            		}
        		} else {
        			//destStr为空就列出所有行
        			lines.add(line2);
        		}
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return lines;
    }
}
