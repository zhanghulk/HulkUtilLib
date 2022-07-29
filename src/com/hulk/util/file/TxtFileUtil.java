package com.hulk.util.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import hulk.text.TextUtils;
import hulk.util.HulkDateUtil;
import hulk.util.PrintUtil;

public class TxtFileUtil {
	
	private static final String TAG = "TxtFileUtil";
	
	public static final String ENCODING = "UTF-8";
	
	/**
	 * 换行符,默认"\n"
	 */
	public static String sNewlineCharacter = "\n";
	
	public static void setNewlineCharacter(String newlineCharacter) {
    	sNewlineCharacter = newlineCharacter;
    }
	
	/**
	 * 读取普通文件目录下的文本文件内容(SD卡或者data/data/...)
	 * @param filePath
	 * @param original 是否保持原来的文本格式(保持文件中原样的文文本字符串，保持有换行符和首尾空格)
	 * @return
	 */
	public static String readFile(String filePath, boolean original) {
		if (TextUtils.isEmpty(filePath)) {
        	throw new IllegalArgumentException("Empty filePath=" + filePath);
        }
		File file = new File(filePath);
		if (!file.exists()) {
			PrintUtil.w(TAG, "readFile: Not exists file=" + file);
			return null;
		}
		InputStream input = null;
		try {
			input = new FileInputStream(file);
			if (input != null) {
				if (original) {
					return readInputText(input);
				} else {
					return readNoSpace(input);
				}
			}
		} catch (IOException e) {
			PrintUtil.e(TAG, "reaFile: " + e + ", from " + filePath, e);
		} finally {
			if(input != null){
                try{
                    input.close();
                }catch(IOException ioe){
                }
            }
		}
        return "";
    }
	
	/**
	 * 读取普通文件目录下的文本文件内容(SD卡或者data/data/...)
	 * <p>保持原来的文本格式(保持文件中原样的文文本字符串，保持有换行符和首尾空格)
	 * @param filePath
	 * @return
	 */
	public static String readFileText(String filePath) {
		if (TextUtils.isEmpty(filePath)) {
        	throw new IllegalArgumentException("readFileText: Empty filePath=" + filePath);
        }
		File file = new File(filePath);
		if (!file.exists()) {
			PrintUtil.w(TAG, "readFileText: Not exists file=" + file);
			return null;
		}
		InputStream input = null;
		try {
			input = new FileInputStream(file);
			return readInputText(input);
		} catch (IOException e) {
			PrintUtil.e(TAG, "readFileText: " + e + ", from " + filePath, e);
		} finally {
			if(input != null){
                try{
                    input.close();
                }catch(IOException ioe){
                }
            }
		}
        return "";
    }
	
	/**
	 * 读取文件中文本数据,保持原来的文本格式
	 * <p>保持原来的文本格式(保持文件中原样的文文本字符串，保持有换行符和首尾空格)
	 * @param input
	 * @return
	 */
	public static String readInputText(InputStream input){
		if (input == null) {
			throw new IllegalArgumentException("readInputText: input == null");
		}
        BufferedReader reader = null;
        StringBuffer buff = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(input, ENCODING));
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
            	lineCount++;
            	if(lineCount > 1) {
            		//从第二行开始,每次在签名加上换行符
            		buff.append(sNewlineCharacter);
            	}
            	buff.append(line);
			}
            String text = buff.toString();
            PrintUtil.i(TAG, "readInputText: text length= " + buff.length() + ", lineCount=" + lineCount);
            return text;
        } catch (Exception e) {
            PrintUtil.e(TAG, "readInputText Exception: " + e);
        } finally {
            if(reader != null){
                try{
                    reader.close();
                }catch(IOException ioe){
                }
            }
        }
        return "";
    }

	/**
	 * 读取文件中文本字符串, 去掉换行符和首尾空格
	 * @param input
	 * @return
	 */
	public static String readNoSpace(InputStream input){
		if (input == null) {
			throw new IllegalArgumentException("readNoSpace: input == null");
		}
        BufferedReader reader = null;
        StringBuffer buff = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(input, ENCODING));
            String line;
            while ((line = reader.readLine()) != null) {
            	buff.append(line.trim());
			}
            PrintUtil.i(TAG, "read No Space text: " + buff);
        } catch (Exception e) {
            PrintUtil.e(TAG, "readNoSpace Exception: " + e);
        } finally {
            if(reader != null){
                try{
                    reader.close();
                }catch(IOException ioe){
                }
            }
        }
        return buff.toString();
    }
	
	public static List<String> readLineList(String filePath){
		if (TextUtils.isEmpty(filePath)) {
			throw new IllegalArgumentException("readLineList: InvalidfilePath=" + filePath);
		}
		File file = new File(filePath);
		if (!file.exists()) {
			PrintUtil.w(TAG, "readLineList: Not exists file=" + file);
			return null;
		}
		InputStream input = null;
		try {
			input = new FileInputStream(file);
			if (input != null) {
				return readLines(input);
			}
		} catch (IOException e) {
			PrintUtil.e(TAG, "readLineList: " + e + ", from " + filePath);
		} finally {
			if(input != null){
                try{
                    input.close();
                }catch(IOException ioe){
                }
            }
		}
        return null;
    }
	
	public static List<String> readLines(InputStream input){
		if (input == null) {
			throw new IllegalArgumentException("readLines: input == null");
		}
		List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(input, ENCODING));
            String line;
            while ((line = reader.readLine()) != null) {
            	lines.add(line);
			}
            PrintUtil.i(TAG, "readLineList lines size: " + lines.size());
        } catch (Exception e) {
            PrintUtil.e(TAG, "readLineList Exception: " + e);
        } finally {
            if(reader != null){
                try{
                    reader.close();
                }catch(IOException ioe){
                }
            }
        }
        return lines;
    }
	
	public static boolean writeLines(List<String> lines, String filePath) throws Exception {
		if (lines == null || lines.isEmpty()) {
			PrintUtil.w(TAG, "write canceled, list is null or empty !! ");
			return false;
		}
		StringBuffer buffer = new StringBuffer();
		for (String line : lines) {
			if(line == null) continue;
			String line2 = line.trim();
			if (!TextUtils.isEmpty(line2)) {
				buffer.append(line2).append('\n');// new line
			}
		}
		return write(buffer.toString(), filePath);
	}

	public static boolean write(String text, String filePath) throws Exception {
        if (text == null || text.length() < 0) {
        	PrintUtil.w(TAG, "write: Invalid text=" + text);
			return false;
        }
        FileWriter writer = null;
        try {
            File file = new File(filePath);
            if (file.exists()) {
                boolean del = file.delete();
                PrintUtil.w(TAG, "writeTextToFile old file deleted= " + del);
            }
            File pDir = file.getParentFile();
            if (!pDir.exists()) {
            	boolean mkdirs = pDir.mkdirs();
            	PrintUtil.w(TAG, "create parent file mkdirs= " + mkdirs);
                if (!mkdirs) {
                	String err = "Failed to create parent dir: " + pDir
                    		+ ", please check Manifest whether has permissin to write storage";
                    PrintUtil.e(TAG, err);
                    throw new RuntimeException(err);
                }
            }
            writer = new FileWriter(file);
            writer.write(text);
            PrintUtil.i(TAG, "write Text: " + text + " >>> file path: " + filePath);
            return true;
        } catch (Exception e) {
            PrintUtil.e(TAG, "writeTextToFile Exception: " + e + ", text: " + text, e);
            throw e;
        }finally{
            if(writer!=null){
                try{
                    writer.close();
                }catch(IOException ioe){
                    //ignored
                }
            }
        }
    }
	
    /**
     * 创建文件名文件名: yyyyMMdd.txt or yyyyMMdd_HHmmss.txt
     * @param dir
     * @param prefix 前缀
     * @param extension 扩展名
     * @param timeMode  文件是否为时间模式
     * @return
     * @throws IllegalArgumentException
     */
    public static String createFilePath(String dir, String prefix, String extension, boolean timeMode) {
    	String dirPath = dir;
        if (!dirPath.startsWith("/")) {
        	dirPath = "/" + dirPath;
        }
        if (!dirPath.endsWith("/")) {
        	dirPath = dirPath + "/";
        }
        String filePath = dirPath + createCurrentFileName(extension, timeMode);
        String pre = prefix != null ? prefix.trim() : null;
        String fileName;
        if(pre != null && !"".equals(pre)) {
        	fileName = pre + "-" + createCurrentFileName(extension, timeMode);
        } else {
        	fileName = createCurrentFileName(extension, timeMode);
        }
        filePath = dirPath + fileName;
    	return filePath;
    }
    
    /**
     * 获取文件名文件名: yyyyMMdd.txt or yyyyMMdd_HHmmss.txt
     * @param timeMode 是否为时间模式格式， 默认日期模式
     * @return
     */
    public static String createCurrentFileName(String extension, boolean timeMode) {
    	long now = System.currentTimeMillis();
    	String fileName = "";
    	if(timeMode) {
    		fileName = getTimeSecondFileName(extension, now);
    	} else {
    		fileName = getDateFileName(extension, now);
    	}
    	return fileName;
    }
    
    /**
     * 文件名：yyyyMMdd.txt
     * @param extension eg .txt
     * @param millis 时间戳
     * @return
     */
    public static String getDateFileName(String extension, long millis) {
    	if(millis <= 0) {
    		millis = System.currentTimeMillis();
    	}
    	return HulkDateUtil.formatDateStr(millis) + extension;
    }
    
    /**
     * 文件名：yyyyMMdd_HHmmss.txt
     * @param extension eg .txt
     * @param millis 时间戳
     * @return
     */
    public static String getTimeSecondFileName(String extension, long millis) {
    	if(millis <= 0) {
    		millis = System.currentTimeMillis();
    	}
    	return HulkDateUtil.formatTimeSecondStr(millis) + extension;
    }
    
    /**
     * 添加文件名前缀：eg:locked_zhanghao.txt
     * @param srcPath 文件路径
     * @param prefix  前缀
     * @return
     */
    public static String addFilenamePrefix(String srcPath, String prefix) {
    	if(srcPath != null && !srcPath.equals("")) {
    		File src = new File(srcPath);
    		String path = src.getParent() + "/"+ prefix + "_" + src.getName();
    		return path;
    	}
    	return srcPath;
    }
    
    public static String formatTimeMillisecond(long timeMillis) {
    	return HulkDateUtil.formatTimeMillisecond(timeMillis);
    }
}
