package com.hulk.model.pc.core;

/**
 * 生产者基类
 * @author zhanghao
 *
 * @param <T>
 */
public abstract class ProducerBase<T> implements Runnable, IPCModel<T> {

	private static final String TAG = "ProducerBase";
	/**
	 * 产品仓库
	 */
	protected IWarehouse<T> mWarehouse;
	
	/**
	 * 线程名称
	 */
	protected String mName;
	
	/**
	 * 是否正在运行
	 */
	protected volatile boolean running = false;
	
	/**
	 * 是否被外界设置为停止的
	 */
	protected volatile boolean stopped = false;
	
	/**
	 * 循环工作次数
	 */
	protected int loopCount = 0;
	
	private boolean mDebugMode = false;
	
	/**
	 * 默认一次获取或者存放睡眠时间，避免一直占用CPN导致程序占用套多资源
	 * <p> 如果确实需要调整策略，可以自行在seepTime()函数中返回具体数值，0表示不睡眠。
	 */
	protected long mSleepTime = DEFAULT_ONCE_SLEEP_TIME;
	
	protected OnPCListener mPCListener;
	
	public ProducerBase(IWarehouse<T> warehouse) {
		this.mWarehouse = warehouse;
		initName();
	}
	
	@Override
	public void run() {
		SysLog.i(TAG, "run: Stasrting...");
		onPCStarting();
		running = true;
		stopped = false;
		doRun();
		running = false;
		SysLog.w(TAG, "run: Finished");
		onPCFinised();
	}

	/**
	 * 执行run函数
	 * 死循环服务生产产品
	 */
	private void doRun() {
		while(true) {
			try {
				if(isStopped()) {
					String thread = getCurrentThreadInfo();
					SysLog.w(TAG,  "doRun: Stopped, loopCount= " + loopCount + ", thread=" + thread);
					return;
				}
				T product = doProduce();
				if(product != null) {
					//mWarehouse.get()为阻塞仓库，没有货物是会等待
					mWarehouse.put(product);
					loopCount++;
				} else {
					SysLog.e(TAG,  "doRun: Failed to produce for product is null.");
				}
				//建议每次睡眠间隔一定时间，避免出现该线程一直占用cup情况
				doSleep();
			} catch (Exception e) {
				SysLog.e(TAG, "doRun Exception: " + e, e);
			} finally {
				//do something
				doOnceFinal();
			}
		}
	}
	
	/**
	 * 线程睡眠
	 * 建议每次睡眠间隔一定时间，避免出现该线程一直占用cup情况
	 */
	protected void doSleep() {
		long sleepTime = sleepTime();
		if (sleepTime <= 0) {
			return;
		}
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			SysLog.e(TAG, "doSleep Interrupted: " + e);
		}
	}
	
	/**
	 * 实际完成生产的函数
	 * @return
	 */
	protected abstract T doProduce();
	
	/**
	 * 每次循环睡眠时间， 0表示不睡眠
	 * 默认一次获取或者存放睡眠时间，避免一直占用CPN导致程序占用套多资源
	 * <p> 如果确实需要调整策略，可以自行在seepTime()函数中返回具体数值，0表示不睡眠。
	 * @return
	 */
	protected long sleepTime() {
		return mSleepTime;
	}
	
	/**
	 * 获取当前线程信息
	 * @return
	 */
	protected String getCurrentThreadInfo() {
		return SysLog.getCurrentThreadInfo();
	}
	
	/**
	 * do something in finally
	 */
	protected void doOnceFinal() {
		//do something in finally
	}
	
	public boolean isProductCacheFull() {
		if(mWarehouse instanceof WarehouseBase) {
			return ((WarehouseBase)mWarehouse).isProductCacheFull();
		}
		return false;
	}
	
	public T removeFistProduct() {
		if(mWarehouse instanceof WarehouseBase) {
			return (T) ((WarehouseBase)mWarehouse).removeFistProduct();
		}
		return null;
	}
	
	public T removeLastProduct() {
		if(mWarehouse instanceof WarehouseBase) {
			return (T) ((WarehouseBase)mWarehouse).removeLastProduct();
		}
		return null;
	}
	
	public void setStopped(boolean stopped) {
		this.stopped = stopped;
	}
	
	public boolean isStopped() {
		if(onStopped()) {
			return true;
		}
		return this.stopped;
	}
	
	/**
	 * 子类可以动态实现是否需要停止
	 * @return
	 */
	public boolean onStopped() {
		return false;
	}
	
	/**
	 * 线程开始
	 */
	protected void onPCStarting() {
		if(mPCListener != null) {
			mPCListener.onPCStarting(this, TAG);
		}
	}
	
	/**
	 * 线程结束
	 */
	protected void onPCFinised() {
		if(mPCListener != null) {
			mPCListener.onPCFinised(this, TAG);
		}
	}
	
    @Override
	public IWarehouse<T> getWarehouse() {
		return mWarehouse;
	}

	@Override
	public void setWarehouse(IWarehouse<T> warehouse) {
		mWarehouse = warehouse;
	}

	protected void initName() {
		if(mName == null || mName.equals("")) {
			mName = getClass().getName();
		}
	}
	
	@Override
	public void start() {
		initName();
		SysLog.i(TAG, "start: " + mName);
		Thread t = new Thread(this, mName);
		t.start();
	}
	
	public void stop() {
		SysLog.w(TAG, "stop: " + mName);
		this.stopped = true;
		if(isProductCacheFull()) {
			//数据为满员,需要手动移除一个元素,触发下一次循环
			triggerStoppedManually();
		}
	}
	
	/**
	 * 手动触发停止. 添加一个对象,跳出循环 eg:
	 * if(mWarehouse != null) {
			mWarehouse.put("Stopped");
		}
	 */
	protected void triggerStoppedManually() {
		//子函数实现:
		removeLastProduct();
	}
	
	@Override
	public boolean isRunning() {
		return this.running;
	}
	
	@Override
	public void setDebugMode(boolean debugMode) {
    	this.mDebugMode = debugMode;
    }
    
    public boolean isDebugMode() {
    	return this.mDebugMode;
    }
    
    @Override
    public int getLoopCount() {
    	return this.loopCount;
    }
    
    @Override
	public void setSleepTime(long sleepTime) {
    	mSleepTime = sleepTime;
	}
    
    @Override
	public void setOnPCListener(OnPCListener listener) {
		mPCListener = listener;
	}
}
