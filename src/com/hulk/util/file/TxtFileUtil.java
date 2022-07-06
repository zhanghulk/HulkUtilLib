package com.hulk.util.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import hulk.text.TextUtils;
import hulk.util.PrintUtil;

public class TxtFileUtil {
	
	private static final String TAG = "TxtFileUtil";
	
	public static final String ENCODING = "UTF-8";
	
	/**
	 * 读取Assets目录下的文本文件内容
	 * @param input
	 * @param fileName
	 * @param original  是否保持原来的文本格式(保持文件中原样的文文本字符串，保持有换行符和首尾空格)
	 * @return
	 */
	public static String readAssetsFile(InputStream input, String fileName, boolean original){
        if (!TextUtils.isEmpty(fileName)) {
			try {
				if (input != null) {
					if (original) {
						return readOriginal(input);
					} else {
						return readNoSpace(input);
					}
				} else {
					PrintUtil.e(TAG, "AssetsFile InputStream is null, fileName: " + fileName);
				}
			} catch (Exception e) {
				PrintUtil.e(TAG, "readAssetsFile failed: " + e + ", from " + fileName, e);
			} finally {
				if(input != null){
	                try{
	                    input.close();
	                }catch(IOException ioe){
	                }
	            }
			}
        }
        return "";
    }
	
	/**
	 * 读取普通文件目录下的文本文件内容(SD卡或者data/data/...)
	 * @param filePath
	 * @param original 是否保持原来的文本格式(保持文件中原样的文文本字符串，保持有换行符和首尾空格)
	 * @return
	 */
	public static String readFile(String filePath, boolean original){
        if (!TextUtils.isEmpty(filePath)) {
        	InputStream input = null;
			try {
				input = new FileInputStream(filePath);
				if (input != null) {
					if (original) {
						return readOriginal(input);
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
        }
        return "";
    }
	
	/**
	 * 读取文件中原样的文文本字符串,保持原来的文本格式
	 * @param input
	 * @return
	 */
	public static String readOriginal(InputStream input){
		if (input == null) {
			return "";
		}
        String text = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(input, ENCODING));
            int fileLen = input.available();
            char[] chars = new char[fileLen];
            int readCount = reader.read(chars);
            PrintUtil.i(TAG, "readOriginal: " + ", fileLen= " + fileLen + ", readCount= " + readCount);
            text = new String(chars);
        } catch (Exception e) {
            PrintUtil.e(TAG, "readOriginal Exception: " + e);
        } finally {
            if(reader != null){
                try{
                    reader.close();
                }catch(IOException ioe){
                }
            }
        }
        return text;
    }

	/**
	 * 读取文件中文本字符串, 去掉换行符和首尾空格
	 * @param input
	 * @return
	 */
	public static String readNoSpace(InputStream input){
		if (input == null) {
			return "";
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
        if (!TextUtils.isEmpty(filePath)) {
        	InputStream input = null;
			try {
				input = new FileInputStream(filePath);
				if (input != null) {
					return readLines(input);
				}
			} catch (IOException e) {
				PrintUtil.e(TAG, "reaFile: " + e + ", from " + filePath);
			} finally {
				if(input != null){
	                try{
	                    input.close();
	                }catch(IOException ioe){
	                }
	            }
			}
        }
        return null;
    }
	
	public static List<String> readLines(InputStream input){
		if (input == null) {
			return null;
		}
		List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(input, ENCODING));
            String line;
            while ((line = reader.readLine()) != null) {
            	PrintUtil.i(TAG, "read list line: " + line);
            	lines.add(line.trim());
			}
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
        if (text != null && text.length() > 0) {
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
        return false;
    }
	
	/**
     * format time as "yyyy-MM-dd HH:mm:ss"
     * @param timeMillis
     * @return
     */
	public static String formatTimeSecond(long timeMillis) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(timeMillis);
    }
	
	/**
     * get current time as "yyyy-MM-dd HH:mm:ss.SSS"
     * @return
     */
	public static String getCurrentMillisecond() {
		return formatTimeMillisecond(System.currentTimeMillis());
    }
	
	/**
     * format time as "yyyy-MM-dd HH:mm:ss.SSS"
     * @param timeMillis
     * @return
     */
	public static String formatTimeMillisecond(long timeMillis) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return df.format(timeMillis);
    }
    
    /**
     * format time as long value: yyyyMMdd_HHmmss
     * @return
     */
    public static String formatTimeSecondStr(long timeMillis) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		return sdf.format(timeMillis);
	}
    
    /**
     * format time as yyyyMMdd
     * @return
     */
    public static String formatDateStr(long timeMillis) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		return sdf.format(timeMillis);
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
    	return TxtFileUtil.formatDateStr(millis) + extension;
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
    	return TxtFileUtil.formatTimeSecondStr(millis) + extension;
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
}
