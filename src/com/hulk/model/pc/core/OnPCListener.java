package com.hulk.model.pc.core;

/**
 * 生产者/消费者监听器
 * @author hulk
 *
 */
public interface OnPCListener {

	/**
	 * 生产者/消费者线程运行开始
	 * @param model
	 * @param args
	 */
	void onPCStarting(IPCModel<?> model, Object... args);
	
	/**
	 * 生产者/消费者线程运行结束
	 * @param model
	 * @param args
	 */
	void onPCFinised(IPCModel<?> model, Object... args);
}
