package com.hulk.util.file;

import com.hulk.model.pc.core.*;

/**
 * 日志打印消费者实现类
 * <p>消费者实际消费： 把日志信息写到文件里面
 * @author zhanghao
 *
 */
public class LogConsumer extends ConsumerBase<String> {

	private static final String TAG = "LogConsumer";
	
	TxtFile mTxtFile = null;
	boolean mAppend = true;

	public LogConsumer(IWarehouse<String> warehouse) {
		super(warehouse);
	}
	
	public LogConsumer(IWarehouse<String> warehouse, TxtFile txtFile, boolean append) {
		super(warehouse);
		this.mTxtFile = txtFile;
		this.mAppend = append;
	}

	public void setAppend(boolean append) {
		this.mAppend = append;
	}
	
	public boolean isAppendMode() {
		return this.mAppend;
	}
	
	public void setTxtFile(TxtFile txtFile) {
		this.mTxtFile = txtFile;
	}
	
	public boolean hasTxtFile() {
		return mTxtFile != null;
	}
	
	/**
	 * 手动触发停止. 添加一个对象,使其自动跳出循环
	 * if(mWarehouse != null) {
			mWarehouse.put("Stopped");
		}
	 */
	@Override
	protected void triggerStoppedManually() {
		if(mWarehouse != null) {
			mWarehouse.put("triggerStoppedManually");
		}
	}
	
	/**
	 * 消费者实际消费： 把日志信息写到文件里面
	 */
	@Override
	protected boolean doConsume(String data) {
		//SysLog.i(TAG, "doConsume: ### Consumed data=" + data + "\n\n");
		boolean written = writeToFile(data);
        return written;
	}
	
	protected boolean writeToFile(String text) {
		if(mTxtFile == null) {
    		//为空说明之前初始化时已经失败，此处没不要重复创建，有些设备会耗时过长，出现ANR
			if(isDebugMode()) {
				SysLog.e(TAG, "writeToFile: mTxtFile is null");
			}
    		return false;
    	}
		boolean written = false;
		try {
			written = mTxtFile.write(text, mAppend);
		} catch (Throwable e) {
			if(isDebugMode()) {
				SysLog.e(TAG, "writeToFile Failed:" + e);
			}
		}
        return written;
	}
}
