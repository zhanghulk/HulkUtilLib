package com.hulk.model.pc.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

/**
 * 日志仓库基类
 * 使用synchronized关键字实现同步锁.
 * @author zhanghao
 *
 * @param <T>
 */
public abstract class WarehouseBase<T> implements IWarehouse<T> {

	private static final String TAG = "WarehouseBase";
	
	/**
	 * 默认一次获取或者存放睡眠时间，避免一直占用CPN导致程序占用套多资源
	 * <p> 如果确实需要调整策略，可以自行在seepTime()函数中返回具体数值，0表示不睡眠。
	 */
	public static final long DEFAULT_ONCE_SLEEP_TIME = 50;
	
	/**
	 * 默认缓冲区大小.
	 */
	public static final int DEFAULT_PRODUST_CAPACITY = 100;
	
	/**
	 * 缓冲区变化梯度.
	 */
	public static final int CAPACITY_GRADIENT_UNIT = DEFAULT_PRODUST_CAPACITY;

	/**
	 * 缓冲区最大数目
	 */
	public static final int MAX_PRODUST_CAPACITY = DEFAULT_PRODUST_CAPACITY + CAPACITY_GRADIENT_UNIT * 10;
	
	/**
	 * 产品列表缓冲区
	 */
	protected LinkedList<T> mProductBuffer;
	/**
	 * 缓冲区容量
	 * <p>小于等于0时不进行等待
	 */
	protected int capacity = DEFAULT_PRODUST_CAPACITY;
	
	/**
	 * 缓冲区容量最大值
	 */
	protected int mMaxCapacity = MAX_PRODUST_CAPACITY;
	
	/**
	 * 存放缓存等待是否禁用: 在缓冲区满,如果线程不能等待,就把缓存轻质清除掉,避免等待.
	 */
	protected boolean mPutFullWaitingDisabled = false;
	
	/**
	 * 容量是否自动增长
	 */
	protected volatile boolean mCapacityAuto = false;
	
	protected boolean mDebugMode = false;
	
	protected boolean mShowInfoMode = false;
	
	protected boolean mOnceSleepEnabled = false;
	
	protected volatile boolean mProductBufferEmpty = false;
	protected volatile boolean mProductBufferFull = false;
	
	protected OnWarehouseListener mListener;
	
	public WarehouseBase() {
		this.mProductBuffer = new LinkedList<T>();
	}
	
	public WarehouseBase(int capacity, LinkedList<T> products) {
		this.capacity = capacity;
		this.mProductBuffer = products;
	}
	
	/**
	 * 存放产品
	 * <p>生产者调用，如果缓存列表的产品数量超了最大值，该生产者线程就处于等待状态.
	 */
	@Override
	public void put(T product) {
		synchronized (mProductBuffer) {
			try {
				boolean prepared = preparePut(product);
				if(!prepared) {
					//如果缓冲区已经满了,且不能等待,此时表示未准备好,
					//此时,这个产品不能放进去,系统日志打印出来,避免Android场景出现ANR
					SysLog.i(TAG, "put: Not prepared, Can not put product=" + product);
					return;
				} 
				//放入数据
				doPut(product);
				
				//建议每次睡眠间隔一定时间，避免出现该线程一直占用cup情况
				doThreadSleep();
			} catch (InterruptedException e) {
				SysLog.e(TAG, "put Interrupted: " + e, e);
			} finally {
				//产品放完后，通知其他线程释放锁
				mProductBuffer.notify();
			}
		}
	}
	
	/**
	 * 准备放入数据缓冲区
	 * <p>如果缓冲区已经满了,且不能等待,此时表示未准备好,这个产品不能放进去;
	 * <p>注: wait(排队等待)也是准摆好的一种情况.
	 * @param product
	 * @return 如果缓冲区已经满了,且不能等待,此时返回false,默认返回true
	 * @throws InterruptedException
	 */
	protected boolean preparePut(T product) throws InterruptedException {
		boolean isFull = checkProductBufferFull();
		if(!isFull) {
			//不满就直接通过
			return true;
		}
		doPutFullCallback(product);
		boolean waitingEnabled = checkPutFullWaitingEnabled();
		String thread = getCurrentThreadInfo();
		SysLog.w(TAG, "preparePut: ## products buffer is full for thread=" + thread);
		if(waitingEnabled) {		
			SysLog.w(TAG, "preparePut: waiting is enabled, please waiting a moment for consumed buffer.");
			mProductBuffer.wait();
		} else {
			//存放缓存等待是否禁用: 在缓冲区满,如果线程不能等待,直接略过,避免内存溢出.
			SysLog.w(TAG, "preparePut: waiting is not enabled");
			return false;
		}
		//默认准比好了
		return true;
	}
	
	/**
	 * 执行具体的放入数据
	 * @param product
	 */
	protected void doPut(T product) {
		boolean put = onPut(product);
		if(!put) {
			//子类没有执行加入，自定加入队列
			addProduct(product);
			if(isShowInfoMode()) {
				SysLog.i(TAG, "doPut: add product=" + product);
			}
		}
	}
	
	/**
	 * 放入数据
	 * 子类实现具体的放入数据
	 * @param product
	 * @return 如果完全接管放入函数，则返回true，不执行默认添加功能.
	 */
	protected abstract boolean onPut(T product);
	
	/**
	 * 检查缓冲区满时,是否启用等待
	 * <p>主线程默认不能等待,避免android出现ANE
	 * @return
	 */
	protected boolean checkPutFullWaitingEnabled() {
		if(mPutFullWaitingDisabled) {
			return false;
		}
		String threadName = Thread.currentThread().getName();
		//默认可等待
		boolean waitingEnabled = true;
		if("main".equals(threadName)) {
			//主线程默认不能等待,在android中,等待会出现ANR
			waitingEnabled = false;
		}
		if(mListener != null) {
			waitingEnabled = mListener.onPutWaitingEnabled(this);
		}
		return waitingEnabled;
	}
	
	/**
	 * Put满时回调
	 * @param product
	 */
	protected void doPutFullCallback(T product) {
		if(mListener != null) {
			mListener.onPutFull(this, product);
		}
	}
	
	/**
	 * 获取产品
	 * <p>消费者调用，如果仓库缓存列表的产品数量为空，该消费者线程就处于等待状态.
	 */
	@Override
	public T get() {
		synchronized (mProductBuffer) {
			try {
				//准备获取
				prepareGet();
				
				//获取数据
				T product = doGet();
				
				//建议每次睡眠间隔一定时间，避免出现该线程一直占用cup情况
				doThreadSleep();
				return product;
			} catch (InterruptedException e) {
				String thread = getCurrentThreadInfo();
				SysLog.e(TAG, "put Interrupted: " + e + ", thread=" + thread, e);
			} finally {
				//产品放完后，通知其他线程释放锁
				mProductBuffer.notify();
			}
		}
		return null;
	}
	
	protected void prepareGet() throws InterruptedException {
		if(isShowInfoMode()) {
			int size = this.mProductBuffer.size();
			SysLog.i(TAG, "prepareGet: Current mProductBuffer size=" + size);
		}
		mProductBufferEmpty = this.isProductBufferEmpty();
		if(mProductBufferEmpty) {
			doGetEmptyCallback();
			if(isDebugMode()) {
				String thread = getCurrentThreadInfo();
				SysLog.w(TAG, "prepareGet: ## products is empty, please wait...thread=" + thread);
			}
			mProductBuffer.wait();
		}
	}
	
	/**
	 * Get为空时回调
	 * @param product
	 */
	protected void doGetEmptyCallback() {
		if(mListener != null) {
			mListener.onGetEmpty(this);
		}
	}
	
	/**
	 * 执行具体的获取数据
	 * @return
	 */
	protected T doGet() {
		T product = onGet();
		if(product == null) {
			//子类没有执行具体获取产品，自动获取第一个
			product = removeFistProduct();
			if(isShowInfoMode()) {
				SysLog.i(TAG, "doGet: onGet result is null, remove fist product " + product);
			}
		}
		return product;
	}
	
	/**
	 * 子类实现具体的获取数据
	 * @return
	 */
	protected abstract T onGet();
	
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	
	public int getCapacity() {
		return this.capacity;
	}
	
	/**
	 * 获取修正后最大缓冲区数量
	 * @return
	 */
	public int fixMaxCapacity() {
		if(mCapacityAuto) {
			//每次增加一个变化梯度
			int fixCapacity = capacity + CAPACITY_GRADIENT_UNIT;
			if(fixCapacity < mMaxCapacity) {	
				capacity = fixCapacity;
				mProductBufferFull = isProductBufferFull();
				SysLog.w(TAG, "fixMaxCapacity: Fixed capacity=" + capacity + ",  Full=" + mProductBufferFull);
			}
		}
		return this.capacity;
	}
	
	public void setCapacityAuto(boolean capacityAuto) {
		this.mCapacityAuto = capacityAuto;
	}
	
	public void setMaxCapacity(int maxCapacity) {
		this.mMaxCapacity = maxCapacity;
	}
	
	public int getMaxCapacity() {
		return this.mMaxCapacity;
	}
	
	private void ensureProductsAvailable() {
		if(mProductBuffer == null) {
			throw new IllegalStateException("mProductBuffer is null, please create it in constructors");
		}
	}
	
	public boolean isProductBufferEmpty() {
		return mProductBuffer == null || mProductBuffer.isEmpty();
	}
	
	public boolean checkProductBufferEmpty() {
		mProductBufferEmpty = isProductBufferEmpty();
		return mProductBufferEmpty;
	}
	
	/**
	 * 检查缓冲区是否满了
	 * @return
	 */
	public boolean checkProductBufferFull() {
		if(capacity <= 0) {
			return false;
		}
		mProductBufferFull = isProductBufferFull();
		if(mProductBufferFull) {			
			//如果已经满了，获取修正后的容量,进行二次检查
			fixMaxCapacity();
		}
		return mProductBufferFull;
	}
	
	public boolean isProductBufferFull() {
		return mProductBuffer != null && mProductBuffer.size() >= capacity;
	}
	
	/**
	 * 添加产品
	 * @param product
	 */
	protected void addProduct(T product) {
		ensureProductsAvailable();
		mProductBuffer.add(product);
	}
	
	/**
	 * 添加产品
	 * @param index
	 * @param product
	 */
	protected void addProduct(int index, T product) {
		ensureProductsAvailable();
		mProductBuffer.add(index, product);
	}
	
	/**
	 * 添加产品列表
	 * @param products
	 */
	protected void addProducts(T... products) {
		if(products == null) {
			return;
		}
		ensureProductsAvailable();
		mProductBuffer.addAll(Arrays.asList(products));
	}
	
	/**
	 * 添加产品集合
	 * @param c
	 */
	protected void addProducts(Collection<? extends T> c) {
		if(c == null || c.isEmpty()) {
			return;
		}
		ensureProductsAvailable();
		mProductBuffer.addAll(c);
	}
	
	/**
	 * 移除并返回最前面一个
	 */
	protected T removeFistProduct() {
		if(isProductBufferEmpty()) {
			return null;
		}
		return mProductBuffer.removeFirst();
	}
	
	/**
	 * 移除并返回最后一个
	 */
	protected T removeLastProduct() {
		if(isProductBufferEmpty()) {
			return null;
		}
		return mProductBuffer.removeLast();
	}
	
	protected T getFirstProduct() {
		if(isProductBufferEmpty()) {
			return null;
		}
		return mProductBuffer.getFirst();
	}
	
	protected T getLastProduct() {
		if(isProductBufferEmpty()) {
			return null;
		}
		return mProductBuffer.getLast();
	}
	
	protected T getProduct(int index) {
		if(isProductBufferEmpty()) {
			return null;
		}
		if(index >= mProductBuffer.size()) {
			return null;
		}
		return mProductBuffer.get(index);
	}
	
	protected int getProductSize() {
		if(mProductBuffer == null) {
			return 0;
		}
		int size = mProductBuffer.size();
		return size;
	}
	
	protected int size() {
		return getProductSize();
	}
	
	/**
	 * 建议每次睡眠间隔一定时间，避免出现该线程一直占用cup情况
	 * @throws InterruptedException
	 */
	private void doThreadSleep() throws InterruptedException {
		if(!isOnceSleepEnabled()) {
			return;
		}
		long sleepTime = onSleepTime();
		if (sleepTime > 0) {
			Thread.sleep(sleepTime);
		}
	}
	
	/**
	 * 每次循环睡眠时间， 0表示不睡眠
	 * <p> 如果确实需要调整策略，可以自行在子类onSleepTime()函数中返回具体数值，0表示不睡眠。
	 * @return
	 */
	protected long onSleepTime() {
		return DEFAULT_ONCE_SLEEP_TIME;
	}
	
	public void setOnceSleepEnabled(boolean onceSleepEnabled) {
		mOnceSleepEnabled = onceSleepEnabled;
	}
	
	public boolean isOnceSleepEnabled() {
		return mOnceSleepEnabled;
	}
	
	/**
	 * 获取当前线程信息
	 * @return
	 */
	protected String getCurrentThreadInfo() {
		return SysLog.getCurrentThreadInfo();
	}
	
	public void setDebugMode(boolean debugMode) {
    	this.mDebugMode = debugMode;
    }
    
    public boolean isDebugMode() {
    	return this.mDebugMode;
    }
    
    public void setShowInfoMode(boolean showInfoMode) {
    	this.mShowInfoMode = showInfoMode;
    }
    
    public boolean isShowInfoMode() {
    	return this.mShowInfoMode;
    }
    
    public void setListener(OnWarehouseListener<?> listener) {
    	mListener = listener;
	}
    
    /**
     * 设置存放缓存等待是否禁用: 在缓冲区满,如果线程不能等待,就把缓存轻质清除掉,避免等待.
     * @param putFullWaitingDisabled
     */
    public void setPutFullWaitingDisabled(boolean putFullWaitingDisabled) {
    	this.mPutFullWaitingDisabled = putFullWaitingDisabled;
    }
    
    /**
     * 禁用存放缓存等待: 在缓冲区满,如果线程不能等待,就把缓存轻质清除掉,避免等待.
     */
    public void disablePutFullWaiting() {
    	this.mPutFullWaitingDisabled = true;
    }
    
    protected void logProductInfo(String tag, String func, String product) {
		String thread = getCurrentThreadInfo();
		int size = getProductSize();
		SysLog.i(tag, func + ": product=" + product + ", size=" + size + ", thread=" + thread);
	}
}
