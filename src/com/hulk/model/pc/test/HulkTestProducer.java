package com.hulk.model.pc.test;

import java.util.Random;

import com.hulk.model.pc.core.IWarehouse;
import com.hulk.model.pc.core.ProducerBase;
import com.hulk.model.pc.core.SysLog;

public class HulkTestProducer extends ProducerBase<String> {
	
	private static final String TAG = "HulkTestProducer";
	private static final int TEST_MAX = 10;
	
	protected Random random;

	public HulkTestProducer(IWarehouse<String> warehouse) {
		super(warehouse);
		random = new Random();
	}
	
	@Override
	protected String doProduce() {
		String thread = getCurrentThreadInfo();
		int randomInt = random.nextInt(10) * 1000;
		String product = "log-" + loopCount + ", randomInt=" + randomInt;
		SysLog.i(TAG, "doProduce: product=" + product + ", thread=" + thread);
		return product;
	}
	
	/**
	 * 每次循环睡眠时间， 0表示不睡眠
	 * @return
	 */
	protected long sleepTime() {
		int randomTime = random.nextInt(10) * 1000;
		return randomTime;
	}
	
	public boolean onStopped() {
		if(TEST_MAX > 0 && loopCount > TEST_MAX) {
			SysLog.e(TAG,  "doRun: loopCount > TEST_MAX， loopCount=" + loopCount);
			return true;
		}
		return false;
	}
}
