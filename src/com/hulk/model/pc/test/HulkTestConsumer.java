package com.hulk.model.pc.test;

import com.hulk.model.pc.core.ConsumerBase;
import com.hulk.model.pc.core.IWarehouse;
import com.hulk.model.pc.core.SysLog;

public class HulkTestConsumer extends ConsumerBase<String> {
	private static final String TAG = "HulkTestConsumer";
	
	private static final int TEST_MAX = 0;

	public HulkTestConsumer(IWarehouse<String> warehouse) {
		super(warehouse);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean doConsume(String product) {
		SysLog.i(TAG, "doConsume: ### Consumed product=" + product + "\n\n");
		return true;
	}

	public boolean onStopped() {
		if(TEST_MAX > 0 && loopCount > TEST_MAX) {
			//SysLog.e(TAG,  "onStopped: loopCount > TEST_MAX， loopCount=" + loopCount);
			return true;
		}
		return false;
	}
}
