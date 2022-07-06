package com.hulk.model.pc.test;

import java.util.LinkedList;

import com.hulk.model.pc.core.WarehouseBase;

/**
 * 日志仓库
 * 使用synchronized关键字实现同步锁.
 * @author zhanghao
 *
 * @param <T>
 */
public class HulkTestWarehouse extends WarehouseBase<String> {

	private static final String TAG = "HulkTestWarehouse";
	
	public HulkTestWarehouse() {
		super();
	}
	
	public HulkTestWarehouse(int max, LinkedList<String> products) {
		super(max, products);
	}
	
	/**
	 * 具体放入数据
	 */
	protected boolean onPut(String product) {
		if(isDebugMode()) {
			logProductInfo(TAG, "onPut", product);
		}
		return false;
	}
	
	/**
	 * 具体获取数据
	 */
	protected String onGet() {
		String product = removeFistProduct();
		if(isDebugMode()) {			
			logProductInfo(TAG, "onGet", product);
		}
		return product;
	}
}
