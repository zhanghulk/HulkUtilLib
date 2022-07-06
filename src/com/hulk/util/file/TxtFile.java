package com.hulk.util.file;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hulk.text.TextUtils;
import hulk.util.PrintUtil;

public class TxtFile {
	private static final String TAG = "TxtFile";
	private static final String ENCODING = "UTF-8";

	private File mFile;
	private long length = 0;
	private Lock mLock = new ReentrantLock();
	private String mCharsetName = ENCODING;
	
	public TxtFile(String filePath) {
		mFile = new File(filePath);
		length = mFile.exists() ? mFile.length() : 0;
	}
	
	public TxtFile(File file) {
		mFile = file;
		length = file.exists() ? file.length() : 0;
	}
	
	public void setCharsetName(String charsetName) {
		mCharsetName = charsetName;
	}
	
	public boolean writeLines(List<String> list, boolean append) throws Exception {
		if (list == null || list.isEmpty()) {
			PrintUtil.w(TAG, "write canceled, list is null or empty !! ");
			return false;
		}
		StringBuffer buffer = new StringBuffer();
		for (String line : list) {
			if(line == null) continue;
			String line2 = line.trim();
			if (!TextUtils.isEmpty(line2)) {
				buffer.append(line2).append('\n');// new line
			}
		}
		return write(buffer.toString(), append);
	}

	/**
	 * write text into file, per line is a activity log
	 * 
	 * @param text
	 * @param append
	 *            whether append line depending primary text or not.
	 * @throws Exception 
	 */
	public boolean write(String text, boolean append) throws Exception {
		if (append && TextUtils.isEmpty(text)) {
			PrintUtil.w(TAG, "write canceled, can not append a empty text !!");
			return false;
		}
		synchronized (mLock) {
			BufferedOutputStream bos = null;
			try {
				createNewFile();//create file
				//FileOutputStream will create file if not exists
				bos = new BufferedOutputStream(new FileOutputStream(mFile, append));
				byte[] data = text.getBytes(mCharsetName);
				if(data != null) {
					bos.write(data);
					length += data.length;
					//PrintUtil.i(TAG, "write text length= " + data.length + " to file: " + mFile);
				} else {
					PrintUtil.e(TAG, "write failed: data is null for mCharsetName=" + mCharsetName + ", text=" + text);
				}
				return true;
			} catch (Exception e) {
				PrintUtil.e(TAG, "write: " + e + 
						", Please check your app system setting, and make sure the storage writable and readable permissin is available");
				PrintUtil.e(TAG, "Failed to write text to " + mFile);
				//throw e;
				return false;
			} finally {
				if (bos != null) {
					try {
						bos.close();
					} catch (IOException e) {
						// ignored
						PrintUtil.e(TAG, "Failed to close: " + e);
					}
				}
			}
		}
	}
	
	public boolean createNewFile() throws IOException {
		if(mFile.exists()) {
			//PrintUtil.i(TAG, "Exists file: " + mFile);
			return true;
		}
		if (!makeParentDir()) {
			PrintUtil.e(TAG, "Failed to write text beacause that can not make parent dir for file path: " + mFile
					+ ", please check Manifest whether has permissin to write storage !! ");
			return false;
		}
		return mFile.createNewFile();
	}

	/**
	 * read all line text list
	 * 
	 * @return text list
	 */
	public List<String> readLines() {
		synchronized (mLock) {
			List<String> list = new ArrayList<String>();
			FileInputStream fis = null;
			BufferedReader reader = null;
			try {
				if(!mFile.exists()) {
					PrintUtil.w(TAG, "readLines failed, Not existed file: "+ mFile);
					return list;
				}
				fis = new FileInputStream(mFile);
				reader = new BufferedReader(new InputStreamReader(fis, ENCODING));
				String line;
				while ((line = reader.readLine()) != null) {
					String fixLine = line.trim();
					if(TextUtils.isEmpty(fixLine)) {
						continue;
					}
					//PrintUtil.v(TAG, "read line: " + fixLine);
					list.add(fixLine);
				}
			} catch (Exception e) {
				PrintUtil.e(TAG, "readList Exception: " + e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						// ignored
					}
				}
				if (reader != null) {
					try {
						reader.close();
					} catch (Exception e2) {
						// TODO: handle exception
					}
				}
			}
			return list;
		}
	}
	
	/**
	 * read all text
	 * 
	 * @return text list
	 */
	public String readText() {
		synchronized (mLock) {
			FileInputStream fis = null;
			BufferedReader reader = null;
			try {
				if(!mFile.exists()) {
					PrintUtil.w(TAG, "readText failed, Not existed file: "+ mFile);
					return "";
				}
				fis = new FileInputStream(mFile);
				reader = new BufferedReader(new InputStreamReader(fis, ENCODING));
				int fileLen = fis.available();
	            char[] chars = new char[fileLen];
	            int readCount = reader.read(chars);
	            PrintUtil.i(TAG, "read from Asset File: " + ", fileLen= " + fileLen + ", readCount= " + readCount);
	            return new String(chars);
			} catch (Exception e) {
				PrintUtil.e(TAG, "readText: " + e, e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						// ignored
					}
				}
				if (reader != null) {
					try {
						reader.close();
					} catch (Exception e2) {
						// TODO: handle exception
					}
				}
			}
			return "";
		}
	}
	
	public boolean makeParentDir() {
		if(mFile == null) return false;
		File pDir = mFile.getParentFile();
		if (!pDir.exists()) {
			boolean mkdirs = pDir.mkdirs();
			if (!mkdirs) {
				PrintUtil.e(TAG, "Failed to make parent dir: " + pDir);
				throw new RuntimeException("Failed to make parent dir: " + pDir);
			}
		}
		return true;
	}
	
	public File getFile() {
		return mFile;
	}
	
	public long length() {
		return length;
	}
	
	public String getFilePath() {
		return mFile.getAbsolutePath();
	}
	
	public String getName() {
		return mFile.getName();
	}
	
	public String getParentDir() {
		return mFile.getParent();
	}
	
	public File getParentFile() {
		return mFile.getParentFile();
	}
	
	public boolean exists() {
		return mFile != null? mFile.exists() : false;
	}
	
	public boolean delete() {
		return mFile != null? mFile.delete() : false;
	}
	
	public boolean renameTo(String dest) {
		File file = new File(dest);
		return renameTo(file);
	}
	
	public boolean renameTo(File dest) {
		if(mFile != null) {
			return mFile.renameTo(dest);
		}
		return false;
	}
	
	public boolean clear() throws Exception {
		return write("", false);
	}
	
	@Override
    public String toString() {
		return "TxtFile[Path= " + mFile + ", length= " + length
				+ ", mCharsetName= " + mCharsetName + "]";
    	
    }
}
