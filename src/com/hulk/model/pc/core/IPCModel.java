package com.hulk.model.pc.core;

/**
 * 生产者-消费者 模型接口
 * @author zhanghao
 *
 * @param <T>
 */
public interface IPCModel<T> {

	/**
	 * 默认一次获取或者存放睡眠时间，避免一直占用CPN导致程序占用套多资源
	 * <p> 如果确实需要调整策略，可以自行在seepTime()函数中返回具体数值，0表示不睡眠。
	 */
	public static final long DEFAULT_ONCE_SLEEP_TIME = 20;
	
	/**
	 * 获取产品仓库
	 * @return
	 */
	IWarehouse<T> getWarehouse();
	
	/**
	 * 设置产品仓库
	 * @param warehouse
	 */
	void setWarehouse(IWarehouse<T> warehouse);
	
	void setOnPCListener(OnPCListener listener);
	
	/**
	 * 启动线程运行
	 */
	void start();
	
	/**
	 * 停止线程运行
	 */
	void stop();
	
	/**
	 * 获取循环次数
	 * @return
	 */
	int getLoopCount();
	
	/**
	 * 线程是否正在运行
	 * @return
	 */
	boolean isRunning();
	
	/**
	 * 设置debug模式
	 * @param debugMode
	 */
	void setDebugMode(boolean debugMode);
	
	/**
	 * 设置每次小配置后睡眠时间
	 * @param sleepTime
	 */
	void setSleepTime(long sleepTime);
}
