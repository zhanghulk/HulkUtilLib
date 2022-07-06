package hulk.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程线程池套件
 * @author: zhanghao
 * @Time: 2021-05-29 15:52
 */
public class ThreadPoolUtils {
    /**
     *  通过线程池进行需要在线程中进行的操作；此线程池类似于newCachedThreadPool创建的线程池，
     *  只是调整了corePoolSize数量，降低了最大线程数量的限制，
     *  同时任务被拒绝时，舍弃任务（默认会抛RejectedExecutionException异常）；
     */
    private static final ExecutorService THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(0, 128,
            60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.DiscardPolicy());

    /**
     * 在线程池中执行异步任务
     * @param command object of Runnable.
     */
    public static void execute(Runnable command) {
        THREAD_POOL_EXECUTOR.execute(command);
    }
}
