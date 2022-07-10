package com.hulk.model.pc.core;

/**
 * 仓库监听器
 * @author hulk
 *
 * @param <T>
 */
public interface OnWarehouseListener<T> {
	
	/**
	 * 仓库已满回调函数
	 * @param warehouse
	 * @param product
	 */
	void onPutFull(IWarehouse<T> warehouse, T product);
	
	/**
	 * 仓库为空回调函数
	 * @param warehouse
	 */
	void onGetEmpty(IWarehouse<T> warehouse);
	
	/**
	 * 仓库已满时,是否启用等待, 默认启用.
	 * @param warehouse
	 * @return
	 */
	boolean onPutWaitingEnabled(IWarehouse<T> warehouse);
}
