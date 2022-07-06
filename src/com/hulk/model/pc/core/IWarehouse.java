package com.hulk.model.pc.core;

/**
 * 仓库接口
 * <p>生产者消费者模式
 * @author zhanghao
 *
 * @param <T>
 */
public interface IWarehouse<T> {
	/**
	 * 存放数据
	 * @param data
	 */
	void put(T data);
	
	/**
	 * 获取数据
	 * @return
	 */
	T get();
}
