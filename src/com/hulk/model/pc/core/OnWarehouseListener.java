package com.hulk.model.pc.core;

/**
 * 仓库监听器
 * @author hulk
 *
 */
public interface OnWarehouseListener {
	/**
	 * 仓库已满
	 * @param warehouse
	 */
	void onWarehouseFull(IWarehouse warehouse);
	
	/**
	 * 仓库为空
	 * @param warehouse
	 */
	void onWarehouseEmpty(IWarehouse warehouse);
}
