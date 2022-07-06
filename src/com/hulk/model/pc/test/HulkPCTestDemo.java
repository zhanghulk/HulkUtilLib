package com.hulk.model.pc.test;

import java.util.LinkedList;

import com.hulk.model.pc.core.SysLog;

/**
 * 生产者-消费者 模型 测试demo
 * @author zhanghao
 *
 */
public class HulkPCTestDemo {

	private static final String TAG = "HulkTestDemo";

	public static void main(String[] args) {
		HulkTestWarehouse warehouse = new HulkTestWarehouse(5, new LinkedList<String>());
		warehouse.setDebugMode(true);
		
		//多个线程表示多个生产者
		Thread producer = new Thread(new HulkTestProducer(warehouse), "producer");
		Thread producer2 = new Thread(new HulkTestProducer(warehouse), "producer2");
		Thread producer3 = new Thread(new HulkTestProducer(warehouse), "producer3");
		Thread producer4 = new Thread(new HulkTestProducer(warehouse), "producer4");
		
		//消费者线程
		Thread consumer = new Thread(new HulkTestConsumer(warehouse), "consumer");
		
		startThread(producer, producer2, producer3, producer4, consumer);
		//startThread(producer, producer2, consumer);
		//startThread(producer, consumer);
	}

	/**
	 * 启动线程
	 * @param threads
	 */
	private static void startThread(Thread... threads) {
		if(threads == null) {
			SysLog.e(TAG, "startThread: threads is null");
			return;
		}
		for(Thread t: threads) {
			t.start();
		}
	}
}
