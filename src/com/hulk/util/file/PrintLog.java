package com.hulk.util.file;

import com.hulk.util.common.FileUtils;
import com.hulk.util.file.TxtFile;

import hulk.util.HulkDateUtil;
import hulk.util.PrintUtil;

import com.hulk.model.pc.core.OnWarehouseListener;
import com.hulk.model.pc.core.SysLog;
import com.hulk.model.pc.test.HulkTestConsumer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;

/**
 * 打印log信息到txt文件中. 日志文件名称根据timeMode分为两种模式：
 * <p>1. false：日期模式 yyyyMMdd.txt 默认模式， eg： 20180118.txt.
 * <p>2. true： 时间模式： yyyyMMdd_HHmmss.txt， eg： 20180118_080324.txt.
 * <p> 可以给日志文件夹前缀,eg: vpn, 日志文件为vpn-20180118.txt or vpn-20180118_080324.txt.
 * Created by zhanghao on 2018-1-17, modify 2020-02-19
 */

public class PrintLog {
	/**
     * log level
     */
    public enum LogLevel {
        V,
        D,
        I,
        W,
        E
    }
    
    public static String TAG = "PrintLog";
    /**默认每个文件最大5M  */
    public static final long FILE_LENGTH_LIMIT = 1024 * 1024 * 5;
    /**日志文件个数最大数量*/
    public static final int LOG_MAX_FILE_COUNT = 5;
    
    /**
     * 默认缓存大小 ，超过之后立刻写入，提高运行效率
     */
    public static final int DEFAULT_BUFFER_LENGTH = 1024 * 4;
    
    /**
     * 最大缓存大小 ，超过之后立刻写入，必须清空缓存，避免OOM
     */
    public static final int MAX_BUFFER_LENGTH = 1024 * 1024;
    
    public static final String FILE_EXTENSION = ".txt";
    
    /**locked标记的log文件不会被自动删除*/
    public static final String LOCKED_FLAG = "locked";
    
    File mDir;
    TxtFile mTxtFile;
  //是否为缓冲区模式，可以减少IO此时，自己控制缓冲区，可以提高效率
    boolean bufferMode = false;
    int bufferLength = DEFAULT_BUFFER_LENGTH;
    StringBuffer buffer;
    Object mBufferLock = new Object();
    //是否每次新起一行
    boolean lineMode = true;
    
    //默认使用日期模式: yyyyMMdd_HHmmss.txt, false: yyyyMMdd.txt
    boolean fileNameTimeMode = false;
    String fileExtension = FILE_EXTENSION;
    String logPrefix;//log文件前缀
    String customFilename;//log文件名
    
    //limit max file number in dir, but 0 is not to limit
    int maxFileCount = LOG_MAX_FILE_COUNT;
    int logFileCount;//log文件数量
    String[] logFilenames;//单签目录下文件数组
    
    //限制文件大小,最近文件进行判断穿件文件
    long maxFileLength = FILE_LENGTH_LIMIT;
    
    Object mWriteFileLock = new Object();
    private volatile boolean mLogSyncWriting = false;
    
    /**
     * 异步写入模式
     * <p> 利用生产者-消费者内模式实现
     */
    private boolean mWriteLogAsyncMode = true;
    
    private LogConsumer mLogConsumer = null;
    private LogWarehouse mLogWarehouse = null;

    private int mWarehouseCapacity = 10;
    
    /**
     * 是否初始化成功的
     */
    private boolean mInited = false;
    
    private boolean mDebugMode = true;
    
    public PrintLog(String dir) {
    	this.mDir = new File(dir);
    	init();
    }

    public PrintLog(String dir, boolean bufferMode, int bufferLength) {
    	this.mDir = new File(dir);
        this.bufferMode = bufferMode;
        this.bufferLength = bufferLength;
        init();
    }
    
    public PrintLog(TxtFile file, boolean bufferMode, boolean lineMode) {
    	this.mTxtFile = file;
    	this.mDir = getParentDirFile();
        this.bufferMode = bufferMode;
        this.lineMode = lineMode;
        init();
    }
    
    public void setMaxFileCount(int maxFileCount) {
    	this.maxFileCount = maxFileCount;
    }
    
    public int getMaxFileCount() {
    	return this.maxFileCount;
    }
    
    public int getLogFileCount() {
    	return this.logFileCount;
    }
    
    public String[] getLogFilenames() {
    	return this.logFilenames;
    }
    
    public void setMaxFileLength(long maxFileLength) {
    	this.maxFileLength = maxFileLength;
    }
    
    public long getMaxFileLength() {
    	return this.maxFileLength;
    }
    
    public void setLogPrefix(String logPrefix) {
    	this.logPrefix = logPrefix;
    }
    
    public String getLogPrefix() {
    	return this.logPrefix;
    }
    
    public void setLogFilemane(String logFilename) {
    	this.customFilename = logFilename;
    }
    
    public void setCustomLogFilemane(String filename) {
    	this.customFilename = filename;
    }
    
    public void setCharsetName(String charsetName) {
    	if(mTxtFile != null) {
    		mTxtFile.setCharsetName(charsetName);
    	}
	}
    
    public void setFileExtension(String extension) {
    	fileExtension = extension;
	}
    
    public void setFileNameTimeMode(boolean timeMode) {
    	fileNameTimeMode = timeMode;
    }
    
    public void setBufferMode(boolean bufferMode) {
    	this.bufferMode = bufferMode;
    	ensureBuffer();
    }
    
    public void setBufferLength(int bufferLength) {
    	this.bufferLength = bufferLength;
    }
    
    public void setLineMode(boolean lineMode) {
    	this.lineMode = lineMode;
    }

    /**
     * 打印log到文件中:
     * <p>文件超限制，重新创建一个文件
     * <p>文件数量过多，删除旧的文件
     * @param text 日志信息
     * @return 是否打印成功
     * @return
     */
    public boolean printLog(String text) throws Exception {
        return printLog(LogLevel.I, TAG, text);
    }

    /**
     * 打印log到文件中:
     * <p>文件超限制，重新创建一个文件
     * <p>文件数量过多，删除旧的文件
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @return 是否打印成功
     * @return
     */
    public boolean printLog(LogLevel level, String tag, String text){
        return printLog(level, tag, text, null, null);
    }
    
    /**
     * 打印log到文件中:
     * <p>文件超限制，重新创建一个文件
     * <p>文件数量过多，删除旧的文件
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param threadInfo  线程信息，可包含进程号等等,建议格式pid/tid 如果为空默认:tName-tid(java不方便获取进程号)
     * @return 是否打印成功
     * @return
     */
    public boolean printLog(LogLevel level, String tag, String text, String threadInfo){
        return printLog(level, tag, text, threadInfo, null);
    }
    
    /**
     * 打印log到文件中:
     * <p>文件超限制，重新创建一个文件
     * <p>文件数量过多，删除旧的文件
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param e 异常堆栈
     * @return 是否打印成功
     * @return
     */
    public boolean printLog(LogLevel level, String tag, String text, Throwable e){
        return printLog(level, tag, text, null, e);
    }
    
    /**
     * 打印log到文件中:
     * <p>文件超限制，重新创建一个文件
     * <p>文件数量过多，删除旧的文件
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param threadInfo  线程信息，可包含进程号等等,建议格式pid/tid 如果为空默认:tName-tid(java不方便获取进程号)
     * @param e 异常堆栈
     * @return 是否打印成功
     * @return
     */
    public boolean printLog(LogLevel level, String tag, String text, String threadInfo, Throwable e){
        try {
            String levelStr = String.valueOf(level);
            printLog(levelStr, tag, text, threadInfo, e);
            return true;
        } catch (Exception ex) {
            PrintUtil.e(TAG, "printLog failed: " + ex, ex);
        }
        return false;
    }
    
    /**
     * 打印日志到文件中
     * <p>文件超限制，重新创建一个文件
     * <p>文件数量过多，删除旧的文件
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param threadInfo  线程信息，可包含进程号等等,建议格式pid/tid 如果为空默认:tName-tid(java不方便获取进程号)
     * @return 是否打印成功
     * @throws Exception
     */
    public boolean printLog(String level, String tag, String text, String threadInfo) throws Exception {
    	return printLog(level, tag, text, threadInfo, null);
    }
    
    /**
     * 打印日志到文件中
     * <p>文件超限制，重新创建一个文件
     * <p>文件数量过多，删除旧的文件
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param e 异常堆栈
     * @return 是否打印成功
     * @throws Exception
     */
    public boolean printLog(String level, String tag, String text, Throwable e) throws Exception {
    	String str = formatLogStr(level, tag, text, null, e);
    	return write(str);
    }
    
    /**
     * 打印日志到文件中
     * <p>文件超限制，重新创建一个文件
     * <p>文件数量过多，删除旧的文件
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param threadInfo  线程信息，可包含进程号等等,建议格式pid/tid 如果为空默认:tName-tid(java不方便获取进程号)
     * @param e 异常堆栈
     * @return 是否打印成功
     * @throws Exception
     */
    public boolean printLog(String level, String tag, String text, String threadInfo, Throwable e) throws Exception {
    	String str = formatLogStr(level, tag, text, threadInfo, e);
    	return write(str);
    }
    
    /**
     * * 格式化日志信息
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param e 异常堆栈
     * @param level
     * @param tag
     * @param text
     * @param e
     * @return
     */
    public String formatLogStr(LogLevel level, String tag, String text, Throwable e) {
    	return formatLogStr(String.valueOf(level), tag, text, null, e);
    }
    
    /**
     * 格式化日志信息
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param threadInfo  线程信息，可包含进程号等等,建议格式pid/tid 如果为空默认:tName-tid(java不方便获取进程号)
     * @param e 异常堆栈
     * @return
     */
    public String formatLogStr(String level, String tag, String text, String threadInfo, Throwable e) {
    	String str = PrintUtil.formatLogStr(level, tag, text, threadInfo, e);
    	if (lineMode) {
    		return "\n" + str;
    	}
    	return str;
    }
    
    /**
     * 写入日志文件
     * @param str
     * @return
     * @throws Exception
     */
    public boolean write(String str) throws Exception {
        boolean written = write(str, true);
        return written;
    }
    
    /**
     * flush buffer and then append it
     * @param str
     * @return
     * @throws Exception
     */
    public PrintLog appendStr(String str) throws Exception {
    	appendToBuffer(str);
		return this;
    }
    
    /**
     * 追加日志（bufferMode）
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param e 异常堆栈
     * @return
     * @throws Exception
     */
    public PrintLog appendLog(LogLevel level, String tag, String text) throws Exception {
		return appendLog(level, tag, text, null, null);
    }
    
    /**
     * 追加日志（bufferMode）
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param threadInfo  线程信息，可包含进程号等等,建议格式pid/tid 如果为空默认:tName-tid(java不方便获取进程号)
     * @return
     * @throws Exception
     */
    public PrintLog appendLog(LogLevel level, String tag, String text, String threadInfo) throws Exception {
		return appendLog(level, tag, text, threadInfo, null);
    }
    
    /**
     * 追加日志（bufferMode）
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param threadInfo  线程信息，可包含进程号等等,建议格式pid/tid 如果为空默认:tName-tid(java不方便获取进程号)
     * @param e 异常堆栈
     * @return
     * @throws Exception
     */
    public PrintLog appendLog(LogLevel level, String tag, String text, String threadInfo, Throwable e) throws Exception {
		return appendLog(String.valueOf(level), tag, text, threadInfo, e);
    }
    
    /**
     * 追加日志（bufferMode）
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param e 异常堆栈
     * @return
     * @throws Exception
     */
    public PrintLog appendLog(String level, String tag, String text, Throwable e) throws Exception {
		return appendLog(level, tag, text, null, e);
    }
    
    /**
     * 追加日志（bufferMode）
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param threadInfo  线程信息，可包含进程号等等,建议格式pid/tid 如果为空默认:tName-tid(java不方便获取进程号)
     * @param e 异常堆栈
     * @return
     * @throws Exception
     */
    public PrintLog appendLog(String level, String tag, String text, String threadInfo, Throwable e) throws Exception {
    	String str = formatLogStr(level, tag, text, threadInfo, null);
		return appendToBuffer(str);
    }
    
    /**
     * 追加日志
     * <p>如果超过缓存大小，立刻写入文件
     * @param str
     * @return
     * @throws Exception
     */
    public PrintLog appendToBuffer(String str) throws Exception {
    	ensureBuffer();
		buffer.append(str);
		flushBuffer();
		return this;
    }
    
    /**
     * Flush buffer if current buffer length extend max length
     * @return The length of written log string. If failed to write file , return -1.
     */
    public int flushBuffer() {
    	if(isBufferEmpty()) {
    		//Ignored invalid buffer
    		return 0;
    	}
    	if(isBufferExceeding()) {
    		try {
    			int len = doFlush();
    			return len;
    		} catch (Throwable e) {
    			PrintUtil.e(TAG, "flushBuffer failed: " + e, e);
    		}
    	}
    	if(isBufferMaxExceeding()) {
			//Force clear buffer avoid to OOM
			int len = clearBuffer();
			PrintUtil.w(TAG, "##flush: Force clear buffer avoid to OOM， length: " + len);
		}
    	return 0;
    }
    
    /**
     * Flush buffer and return written log string length.
     * @return flush writen result.
     * @throws Exception
     */
    public boolean flush() throws Exception {
    	int flushed = doFlush();
    	return flushed > 0;
    }
    
    /**
     * Flush buffer and return written log string length.
     * @return The length of written log string. If failed to write file , return -1.
     * @throws Exception
     */
    public int doFlush() throws Exception {
    	//及时清空buffer，避免缓存越来越大，占用资源
		String str = getBufferStr();
    	int len = clearBuffer();
    	if(str == null || str.equals("")) {
    		return 0;
    	}
    	if(!isWriteLogAsyncMode() && isLogSyncWriting()) {
    		PrintUtil.w(TAG, "##doFlush: Not Async ModeAnd current wirting. next time to write.");
    		return 0;
    	}
    	// write text and clear buffer
    	boolean written = writeToFile(str, true);
    	if(!written) {
    		PrintUtil.w(TAG, "##doFlush: Failed to write buffer to file");
     		return -1;
    	}
    	PrintUtil.i(TAG, "doFlush text len= " + len);
        return len;
    }
    
    /**
     * 缓存是否超过长度限制
     * @return
     */
    public boolean isBufferExceeding() {
    	if(isBufferEmpty()) {
    		//Ignored invalid buffer
    		return false;
    	}
    	int length = buffer.length();
    	if(length > bufferLength) {
    		return true;
    	}
    	if(isBufferMaxExceeding()) {
    		//超最大限制必须写入
    		return true;
    	}
    	return false;
    }
    
    /**
     * 缓存是否超过最大长度限制
     * <p> 超最大限制必须写入，或者清除缓存避免OOM
     * @return
     */
    public boolean isBufferMaxExceeding() {
    	if(isBufferEmpty()) {
    		//Ignored invalid buffer
    		return false;
    	}
    	int length = buffer.length();
    	if(length > MAX_BUFFER_LENGTH) {
    		//超最大限制必须写入，或者清除缓存避免OOM
    		return true;
    	}
    	return false;
    }
    
    
    /**
     * 获取缓冲区的数据
     * @return
     */
    public String getBufferStr() {
    	if(buffer == null) {
    		return "";
    	}
    	return buffer.toString();
    }
    
    /**
     * 获取缓冲区的数据
     * @param isClear 是否清空 （及时清空buffer，避免缓存越来越大，占用资源）
     * @return
     */
    public String getBufferStr(boolean isClear) {
    	String str = getBufferStr();
		//及时清空buffer，避免缓存越来越大，占用资源
    	clearBuffer();
    	return str;
    }
    
    /**
     * 清空缓存数据
     * @return cleared string length
     */
    public int clearBuffer() {
    	if(buffer == null) {
    		return -1;
    	}
    	if(isBufferEmpty()) {
    		//Ignored invalid buffer
    		return 0;
    	}
    	int len = buffer.length();
    	//delete包前不包后
		//buffer.delete(0, len);
    	//setLength效率不delete高
		buffer.setLength(0);
        return len;
    }
    
    /**
     * 缓存是否为空
     * @return
     */
    public boolean isBufferEmpty() {
    	if(buffer == null || buffer.length() <= 0) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * 写入文本
     * @param str
     * @param append
     * @return
     * @throws Exception 
     */
    private boolean write(String text, boolean append) throws Exception {
    	if(bufferMode) {
        	appendToBuffer(text);
        	return true;
    	}
    	return writeToFileSync(text, append);
    }
    
    public void setWriteLogAsyncMode(boolean writeLogAsyncMode) {
    	this.mWriteLogAsyncMode = false;
    }
    
    public boolean isWriteLogAsyncMode() {
    	return this.mWriteLogAsyncMode;
    }
    
    /**
     * 写入文本
     * @param str
     * @param append
     * @return
     * @throws Exception 
     */
    private boolean writeToFile(String text, boolean append) throws Exception {
    	if(!isInited()) {
    		if(isDebugMode()) {
    			SysLog.e(TAG, "writeToFileSync: Not inited");
    		}
    		return false;
    	}
    	if(isWriteLogAsyncMode()) {
    		return writeToFileAsync(text, append);
    	} else {
    		//默认写法：直接在当前线程写入文件，可能或出现线程等待时间较长
    		return writeToFileSync(text, append);
    	}
    }
    
    /**
     * 异步写入日志信息到文件
     * <p> 利用生产者-消费者内模式实现
     * @param text
     * @param append
     * @return
     * @throws Exception
     */
    private boolean writeToFileAsync(String text, boolean append) throws Exception {
    	if(queueLog(text, append)) {
    		return true;
    	} else {
    		SysLog.e(TAG, "writeToFileAsync: mLogConsumer is null");
    		return false;
    	}
    }
    
    /**
     * 把日志放入日志仓库,排队等待消费者(mLogConsumer)写入文件.
     * @param text
     * @param append
     * @return
     */
    public boolean queueLog(String text, boolean append) {
    	boolean started = startFileConsumer();
    	if(!started) {
    		SysLog.e(TAG, "writeToFileAsync: Log consumer is not starting");
    		return false;
    	}
    	//没必要每次都修改一次浪费资源
    	//setLogConsumerAppend(append);
    	if(mLogWarehouse != null) {
    		//日志放入仓库,排队等待消费者(mLogConsumer)写入文件.
    		mLogWarehouse.put(text);
    		return true;
    	}
    	return false;
    }
    
    /**
     * 当前线程写入文件
     * <p>老版本默认写法：直接在当前线程写入文件，可能或出现线程等待时间较长,有些设备会耗时过长，出现ANR.
     * @param text
     * @param append
     * @return
     * @throws Exception
     */
    private boolean writeToFileSync(String text, boolean append) throws Exception {
    	//此处需要使用同步锁，避免文件内容乱序
    	synchronized (mWriteFileLock) {
    		mLogSyncWriting = true;
    		if(mTxtFile == null) {
        		//为空说明之前初始化时已经失败，此处没不要重复创建，有些设备会耗时过长，出现ANR
    			if(isDebugMode()) {
        			SysLog.e(TAG, "writeToFileAsync: mLogConsumer is null");
        		}
        		return false;
        	}
    		boolean written = false;
    		try {
    			written = mTxtFile.write(text, append);
    		} catch (Throwable e) {
    			SysLog.e(TAG, "writeToFileSync Failed:" + e);
    		}
    		mLogSyncWriting = false;
            return written;
    	}
    }
    
    public boolean isLogSyncWriting() {
    	return mLogSyncWriting;
    }
    
    /**
     * 是否初始化成功的
     * @return
     */
    public boolean isInited() {
    	return mInited;
    }
    
    /**
     * 初始化日志文件
     * @throws IOException 
     */
    public void init() {
		try {
			PrintUtil.i(TAG, "init： Starting.");
			if(mWriteLogAsyncMode) {
				PrintUtil.i(TAG, "init： init Log Consumer");
				initLogConsumer();
			}
			//确保日志文件存在
			initLogFile();
			renderFiles();
			mInited = true;
			PrintUtil.i(TAG, "init： Finsihed.");
		} catch (Throwable e) {
			PrintUtil.e(TAG, "init failed: " + e, e);
		}
    }
    
    /**
     * 强制初始化文件消费者
     */
    public boolean doInitFileConsumer() {
    	mLogWarehouse = new LogWarehouse(mWarehouseCapacity, new LinkedList<String>());
    	mLogConsumer = new LogConsumer(mLogWarehouse);
    	setLogWarehouseCapacityAuto(true);
    	boolean debug = isDebugMode();
    	setLogConsumerDebug(debug);
    	ensureLogFile();
    	if(mTxtFile != null) {
    		mLogConsumer.setTxtFile(mTxtFile);
    		return true;
    	} else {
    		//为空说明之前初始化时已经失败，此处没不要重复创建，有些设备会耗时过长，出现ANR
    		if(isDebugMode()) {
    			SysLog.e(TAG, "doInitFileConsumer: mLogConsumer is null");
    		}
    		return false;
    	}
    }
    
    public void setLogConsumerDebug(boolean debugMode) {
    	if(mLogConsumer != null) {
    		mLogConsumer.setDebugMode(debugMode);
    	}
    	if(mLogWarehouse != null) {
    		mLogWarehouse.setDebugMode(debugMode);
    	}
    }
    
    public void setLogConsumer(LogConsumer logConsumer) {
    	mLogConsumer = logConsumer;
    	if(mLogConsumer != null && !mLogConsumer.hasTxtFile()) {
    		setLogConsumerFile(mTxtFile);
    	}
    }
    
    public LogConsumer getLogConsumer() {
    	return mLogConsumer;
    }
    
    public LogWarehouse getLogWarehouse() {
    	return mLogWarehouse;
    }
    
    public void setLogConsumerFile(TxtFile txtFile) {
    	setLogConsumerFile(txtFile, true);
    }
    
    public void setLogConsumerFile(TxtFile txtFile, boolean force) {
    	SysLog.i(TAG, "setLogConsumerFile: txtFile=" + txtFile + ", force=" + force);
    	if(mLogConsumer != null) {
    		if(force) {
    			mLogConsumer.setTxtFile(txtFile);
    			return;
    		}
    		if(!mLogConsumer.hasTxtFile()) {
    			mLogConsumer.setTxtFile(txtFile);
    		} 
    	}
    }
    
    public void setLogConsumerAppend(boolean append) {
    	if(mLogConsumer != null) {
    		mLogConsumer.setAppend(append);
    	}
    }
    
    public boolean isLogConsumerAppendMode() {
    	if(mLogConsumer != null) {
    		return mLogConsumer.isAppendMode();
    	}
    	return false;
    }
    
    /**
     * 初始化文件消费者
     */
    public boolean initLogConsumer() {
    	if(mLogConsumer == null) {
    		SysLog.i(TAG, "initLogConsumer: do init.");
    		return doInitFileConsumer();
    	}
    	return true;
    }
    
    /**
     * 日志文件消费者是否正在运行
     */
    public boolean isFileConsumerRunning() {
    	return mLogConsumer != null && mLogConsumer.isRunning();
    }
    
    /**
     * 启动日志文件消费者
     */
    public boolean startFileConsumer() {
    	if(!isFileConsumerRunning()) {
    		boolean isInit = initLogConsumer();
    		if(!isInit) {
    			SysLog.w(TAG, "startFileConsumer: Failed to init Log Consumer.");
    			return false;
    		}
    		SysLog.w(TAG, "startFileConsumer: starting.");
    		mLogConsumer.start();
    		return true;
    	} else {
    		//SysLog.i(TAG, "startFileConsumer: Alreay running.");
    		return true;
    	}
    }
    
    /**
     * 停止日志文件消费者
     */
    public void stopFileConsumer() {
    	if(isFileConsumerRunning()) {
    		mLogConsumer.stop();
    		SysLog.w(TAG, "stopFileConsumer: stopped.");
    	} else {
    		SysLog.w(TAG, "stopFileConsumer: Ignore Not running.");
    	}
    }
    
    /**
     * 设置日志仓库容量
     * @param capacity
     */
    public void setLogWarehouseCapacity(int capacity) {
    	mWarehouseCapacity = capacity;
    	if(mLogWarehouse != null) {
    		mLogWarehouse.setCapacity(capacity);
    	}
    }
    
    /**
     * 设置日志仓库最大容量
     * @param maxCapacity
     */
    public void setLogWarehouseMaxCapacity(int maxCapacity) {
    	if(mLogWarehouse != null) {
    		mLogWarehouse.setMaxCapacity(maxCapacity);
    	}
    }
    
    /**
     * 设置存放缓存等待是否禁用: 在缓冲区满,如果线程不能等待,就把缓存轻质清除掉,避免等待.
     * @param putFullWaitingDisabled
     */
    public void setLogWarehousePutFullWaitingDisabled(boolean putFullWaitingDisabled) {
    	if(mLogWarehouse != null) {
    		mLogWarehouse.setPutFullWaitingDisabled(putFullWaitingDisabled);
    	}
    }
    
    /**
     * 禁用存放缓存等待: 在缓冲区满,如果线程不能等待,就把缓存轻质清除掉,避免等待.
     */
    public void disableLogWarehousePutFullWaiting() {
    	if(mLogWarehouse != null) {
    		mLogWarehouse.disablePutFullWaiting();
    	}
    }
    
    /**
     * 设置日志仓库监听器
     * @param listener
     */
    public void setLogWarehouseListener(OnWarehouseListener<?> listener) {
    	if(mLogWarehouse != null) {
    		mLogWarehouse.setListener(listener);
    	}
    }
    
    /**
     * 设置日志仓库容量自动模式
     * @param capacityAuto
     */
    public void setLogWarehouseCapacityAuto(boolean capacityAuto) {
    	if(mLogWarehouse != null) {
    		mLogWarehouse.setCapacityAuto(capacityAuto);
    	}
    }
    
    public void setFileConsumer(LogConsumer fileConsumer) {
    	mLogConsumer = fileConsumer;
    }
    
    public LogConsumer getFileConsumer() {
    	return mLogConsumer;
    }
    
    public int getFileCount() {
    	String[] paths = null;
    	if(mDir != null) {
    		paths = mDir.list();
    	}
    	return paths != null ? paths.length : 0;
    }
    
    /**
     * 列出文件,文件数量过多，删除旧的文件
     */
    public void renderFiles() {
    	renderFiles(true);
    }
    
    /**
     * 列出文件,文件数量过多，删除旧的文件
     * @param asyncMode
     */
    public void renderFiles(boolean asyncMode) {
    	if(mDir != null && mDir.exists()) {
    		if(asyncMode) {
    			doRenderFilesAsync();
    		} else {
    			doRenderFiles();
    		}
    	} else {
    		PrintUtil.w(TAG, "renderFiles： Not existed dir: " + mDir);
    	}
    }
    
    /**
     * 列出文件,文件数量过多，删除旧的文件
     */
    public void doRenderFiles() {
    	logFileCount = 0;
    	renderDirFilenames();
		deleteOldFiles();
    }
    
    public void doRenderFilesAsync() {
    	Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				PrintUtil.w(TAG, "doRenderFilesAsync.run: do render files");
				doRenderFiles();
			}
    	});
    	t.start();
    }
    
    public void renderDirFilenames() {
    	if(mDir == null || !mDir.exists()) {
    		doRenderFilesAsync();
    		PrintUtil.w(TAG, "renderDirFilenames： Not existed dir: " + mDir);
    		return;
    	}
    	//列出dir下的文件列表，仅文件名不含签名路径,如果需要文件请使用dir.listFIles();
		logFilenames = mDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
            	if(dir != null && name != null) {
            		//刨开忽略文件和文件夹
            		File file = new File(dir, name);
            		if(file.isDirectory()) {
            			return false;
            		}
            		if(name.contains(LOCKED_FLAG)) {
            			return false;
            		}
            	}
                return true;
            }
        });
		if(logFilenames != null) {
			logFileCount = logFilenames.length;
		}
		PrintUtil.w(TAG, "renderDirFilenames： logFileCount= " + logFileCount);
    }
    
    /**
     * 如果文件个数超限制，删除最早的一个
     * @return
     */
    public int deleteOldFiles() {
    	if(mDir == null || !mDir.exists()) {
    		PrintUtil.w(TAG, "deleteOldFiles： Not existed dir: " + mDir);
    		return 0;
    	}
    	if(maxFileCount > 0 && logFileCount > maxFileCount) {
    		int deletedCount = FileUtils.deleteOldFiles(mDir, maxFileCount, LOCKED_FLAG);
    		PrintUtil.w(TAG, "deleteOldFiles： count= " + deletedCount + ", maxFileCount= " + maxFileCount);
    		return deletedCount;
    	}
    	return 0;
    }
    
    /**
     * 当前文件大小超过限额之后,把文件名改成时间格式,避免重复.
     * @return
     */
    public boolean renameExceedLengthFile() {
    	if(maxFileLength <= 0) {
    		return false;
    	}
    	if(mTxtFile == null || !mTxtFile.exists()) {
    		return false;
		}
    	long length = mTxtFile.length();
		if(length > maxFileLength) {
			String filename = TxtFileUtil.createCurrentFileName(fileExtension, true);
			File dest = new File(mDir, filename);
			boolean renamed = renameTo(dest);
			if(renamed) {
				PrintUtil.i(TAG, "renamed exceed length File: " + mTxtFile + " to " + dest
						+ ", length= " + length + ", maxFileLength= " + maxFileLength);
			} else {
				PrintUtil.w(TAG, "rename exceed length File failed: " + mTxtFile + " to " + dest
						+ ", length= " + length + ", maxFileLength= " + maxFileLength);
			}
			return renamed;
		}
    	return false;
    }
    
    public boolean renameTo(String destFilePath) {
    	return renameTo(new File(destFilePath));
    }
    
    public boolean renameTo(File dest) {
    	if(mTxtFile != null) {
    		if(mTxtFile.exists()) {
    			boolean renamed = mTxtFile.renameTo(dest);
    			if(!renamed) {
    				PrintUtil.w(TAG, "rename failed: " + mTxtFile + " to " + dest);
    			}
    			return renamed;
    		} else {
    			PrintUtil.w(TAG, "renameTo: Not existed file: " + mTxtFile);
    		}
    	}
    	return false;
    }
    
    /**
     * 锁定当前的日志文件(在文件加上"locked"标识，不会自动删除)
     * @return
     */
    public boolean lockFile() {
    	if(mTxtFile != null) {
    		String path = getLockFailedPath();
    		boolean locked = renameTo(path);
    		PrintUtil.w(TAG, "lockFile: locked= " + locked + ", new path= " + path);
    		return locked;
    	}
		return false;
    }
    
    public String getLockFailedPath() {
    	if(mTxtFile != null) {
    		return TxtFileUtil.addFilenamePrefix(mTxtFile.getFilePath(), LOCKED_FLAG);
    	}
    	return null;
    }
    
    /**
     * 创建日志文本文件对象: yyyyMMdd.txt or yyyyMMdd_HHmmss.txt
     * @return
     * @throws IOException 
     */
    public TxtFile createNewFile(boolean timeMode) throws IOException {
    	ensureFileExtension();
        String filePath = createCurrentFilePath(timeMode);
    	TxtFile logFile = new TxtFile(filePath);
    	boolean created = true;
    	if(!logFile.exists()) {
    		created = logFile.createNewFile();
    		PrintUtil.w(TAG, "create log new file: " + logFile + "" + created);
    	} else {
    		PrintUtil.w(TAG, "Existed log file: " + logFile);
    	}
    	return logFile;
    }
    
    public String createCurrentFilePath(boolean timeMode) {
    	String dir = getDirPath();
    	String filePath = TxtFileUtil.createFilePath(dir, logPrefix, fileExtension, timeMode);
    	return filePath;
    }
    
    public String getDirPath() {
    	try {
			ensureDir();
			return mDir.getAbsolutePath();
		} catch (IOException e) {
			PrintUtil.e(TAG, "getDirPath error: " + e, e);
		}
    	return "";
    }
    
    public String getFilePath() {
    	String filePath = "";
    	try {
    		if(mTxtFile != null) {
    			filePath = mTxtFile.getFilePath();
    		}
    	} catch (IllegalArgumentException e) {
    		PrintUtil.e(TAG, "getFilePath error: " + e, e);
    	}
    	return filePath;
    }
    
    /**
     * 优先使用上一个没有存满的文件, 否则重新创建一个
     * @return
     * @throws IOException 
     */
    public TxtFile createLogFile() throws IOException {
    	TxtFile file = null;
    	if(fileNameTimeMode) {
    		file = createNewFile(true);
    	} else {
    		file = createNewFile(false);
    	}
    	return file;
    }
    
    /**
     * 确保日志w文件可用
     * @throws IOException
     */
    public void ensureLogFile() {
    	try {
    		ensureDir();
        	ensoureTxtFile();
    	} catch (Exception e) {
    		PrintUtil.e(TAG, "ensureLogFile failed: " + e);
    	}
    }
    
    private void ensureDir() throws IOException {
    	if(mDir == null) {
    		if(mTxtFile != null) {
    			//用于自定义mTxtFile文件路径的情况(很少用)
        		mDir = mTxtFile.getParentFile();
        	} else {
        		PrintUtil.e(TAG, "ensureDir: The dir is null");
        		throw new NullPointerException("The dir is null");
        	}
    	}
    	if(!mDir.exists()) {
    		boolean mkDir = mDir.mkdirs();
    		if(mkDir) {
    			PrintUtil.i(TAG, "created dir: " + mDir);
    		} else {
    			String tips = "Please check dir path, it must be whole dir path, eg: /storage/emulated/0/tj/logs/myapp/main";
    			String msg = "Failed create dir: " + mDir + ". " + tips;
    			PrintUtil.e(TAG, "ensureLogFile: " + msg);
    			throw new IOException(msg);
    		}
    	}
    }
    
    /**
     * 初始化日志文件
     * @throws IOException
     */
    public void initLogFile() throws IOException {
    	initTxtFile();
    }
    
    private void ensoureTxtFile() throws IOException {
    	if(mTxtFile == null) {
    		createTxtFile();
    	}
    	recreateTxtFileIfNeed();
    }
    
    /**
     * 初始化log文件
     * @throws IOException
     */
    public void initTxtFile() throws IOException {
    	createTxtFile();
    	recreateTxtFileIfNeed();
    }
    
    /**
     * 重新创建文件
     * @throws IOException
     */
    public TxtFile createTxtFile() throws IOException {
    	ensureDir();
    	if(customFilename != null && !customFilename.equals("")) {
    		//用户自定义的文件名
    		File file= new File(mDir, customFilename);
    		mTxtFile = new TxtFile(file);
    	} else {
    		//自动创建文件名,安札当前日期时间创建
    		mTxtFile = createLogFile();
    	}
    	setLogConsumerFile(mTxtFile, false);
    	return mTxtFile;
    }
    
    /**
     * 
     * @return
     * @throws IOException
     */
    public boolean recreateTxtFileIfNeed() throws IOException {
    	boolean recreated = recreateTxtFileIfNotToday();
    	if(!recreated) {
    		recreated = recreateTxtFileIfExtendMaxLength();
    	}
    	return recreated;
    }
    
    public boolean simplifyLogFileAsync() {
    	return simplifyLogFile(true);
    }
    
    public boolean simplifyLogFile(boolean asyncMode) {
    	try {
    		renderFiles(asyncMode);
    		return recreateTxtFileIfNeed();
    	} catch (Exception e) {
    		PrintUtil.e(TAG, "simplifyLogFile: " + e, e);
    	}
    	return false;
    }
    
    /**
     * 若果按照日期命名的文件超过了最大长度，就使用时间秒来命名，避免同一个文件过大
     * @return
     * @throws IOException
     */
    public boolean recreateTxtFileIfExtendMaxLength() throws IOException {
    	//若果按照日期命名的文件超过了最大长度，就使用时间秒来命名，避免同一个文件过大
    	if(mTxtFile != null && mTxtFile.exists()) {
    		long length = mTxtFile.length();
			if(length > maxFileLength || length > FILE_LENGTH_LIMIT) {
				mTxtFile = createNewFile(true);
				return true;
			}
    	}
    	return false;
    }

    /**
     * 若果按照日期命名的文件不是今天的，就使用重新创建文件
     * @return
     * @throws IOException
     */
    public boolean recreateTxtFileIfNotToday() throws IOException {
    	//若果按照日期命名的文件不是今天的，就使用重新创建文件
    	if(mTxtFile != null && mTxtFile.exists()) {
    		long now = System.currentTimeMillis();
    		String todayStr = HulkDateUtil.formatDateStr(now);
    		String filepath = mTxtFile.getFilePath();
			if(!filepath.contains(todayStr)) {
				mTxtFile = createNewFile(false);
				return true;
			}
    	}
    	return false;
    }
    
    public File getParentDirFile() {
    	if(mTxtFile != null) {
    		return mTxtFile.getParentFile();
    	}
    	return null;
    }
    
    public TxtFile getTxtFile() {
    	return mTxtFile;
    }
    
    /**
     * 创建以日期命名的文件
     * @return
     * @throws IOException 
     */
    public TxtFile createDateLogFile() throws IOException {
    	TxtFile file = createNewFile(false);
    	return file;
    }
    
    private void ensureFileExtension() {
    	if(fileExtension == null || fileExtension.equals("")) {
    		fileExtension = FILE_EXTENSION;
    	}
    }
    
    private void ensureBuffer() {
    	if(buffer == null) {
    		this.buffer = new StringBuffer();
    	}
    }
    
    public void setDebugMode(boolean debugMode) {
    	this.mDebugMode = debugMode;
    	setLogConsumerDebug(debugMode);
    }
    
    public boolean isDebugMode() {
    	return this.mDebugMode;
    }
    
    @Override
    public String toString() {
		return "PrintLog[mTxtFile= "+ mTxtFile
				+ ", logPrefix= " + logPrefix
				+ ", bufferMode= " + bufferMode
				+ ", logFileCount= " + logFileCount
				+ ", maxFileCount= " + maxFileCount
				+ ", maxFileLength= " + maxFileLength
				+ ", mWriteLogAsyncMode= " + mWriteLogAsyncMode
				+ ", mLogConsumer= " + mLogConsumer
				+ ", mLogWarehouse= " + mLogWarehouse
				+ ", mWarehouseCapacity= " + mWarehouseCapacity
				+ ", mInited= " + mInited
				+ "]";
    }
}
