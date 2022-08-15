package hulk.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import hulk.text.TextUtils;

/**
 * 格式化日志格式，调用System.out.println输出日志.
 * @author zhanghao
 *
 */
public class PrintUtil {
	
	private static final String TAG = "PrintUtil";
	public static final byte[] NEW_LINE_CHAR_DATA = "\n".getBytes();
	public static final String ANDROID_LOG_FORMAT = "%s %s %s/%s: %s";
	
	public static void v(String tag, String text) {
		v(tag, text, null);
	}
	
	public static void v(String tag, String text, String threadInfo) {
		System.out.println(formatLogStr("V", tag, text, threadInfo));
	}
	
	public static void d(String tag, String text) {
		v(tag, text, null);
	}
	
	public static void d(String tag, String text, String threadInfo) {
		System.out.println(formatLogStr("D", tag, text, threadInfo));
	}
	
	public static void i(String tag, String text) {
		i(tag, text, null);
	}
	
	public static void i(String tag, String text, String threadInfo) {
		System.out.println(formatLogStr("I", tag, text, threadInfo));
	}
	
	public static void w(String tag, String text) {
		w(tag, text, null);
	}
	
	public static void w(String tag, String text, String threadInfo) {
		w(tag, text, threadInfo, null);
	}
	
	public static void w(String tag, String text, String threadInfo, Throwable e) {
		String str = formatLogStr("W", tag, text, threadInfo, e);
		System.err.println(str);
	}
	
	public static void e(String tag, String text) {
		e(tag, text, null);
	}
	
	public static void e(String tag, String text, Throwable e) {
		e(tag, text, null, e);
	}
	
	public static void e(String tag, String text, String threadInfo, Throwable e) {
		String str = formatLogStr("E", tag, text, threadInfo, e);
		System.err.println(str);
	}
	
	/**
	 * 格式化日志信息：自动加上当前时间和线程ID
	 * <p>eg: 12-03 14:40:28.806 10675 W SslClient: createSocketConnect: Finished to connect !
	 * @param level
	 * @param tag
	 * @param text
	 * @return
	 */
	public static String formatLogStr(String level, String tag, String text) {
        return formatLogStr(level, tag, text, null);
    }
	
	public static String formatLogStr(String level, String tag, String text, String threadInfo) {
    	String timeStr = getLogCurentTime();
    	String tStr = fixThreadInfo(threadInfo);
    	//"%s %s %s/%s: %s"
    	String msg = String.format(ANDROID_LOG_FORMAT, timeStr, tStr, level, tag, text);
        return msg;
    }
	
	public static String formatLogStr2(String level, String tag, String text, String threadInfo) {
    	StringBuffer buff = new StringBuffer();
    	String timeStr = getLogCurentTime();
    	buff.append(timeStr);
    	String tStr = fixThreadInfo(threadInfo);
    	buff.append("  ").append(tStr);
    	buff.append("  ").append(level);
    	buff.append("  ").append(tag).append(": ").append(text);
        return buff.toString();
    }
	
	/**
     * 格式化日志信息
     * @param level 日志级别
     * @param tag  日志TAG
     * @param text 日志信息
     * @param threadInfo  线程信息，可包含进程号等等,建议格式pid/tid 如果为空默认:tName-tid(java不方便获取进程号)
     * @param e 异常堆栈
     * @return
     */
	public static String formatLogStr(String level, String tag, String text, String threadInfo, Throwable e) {
    	String logStr = formatLogStr(level, tag, text, threadInfo);
    	if(e == null) {
    		return logStr;
    	}
    	return formatStackTrace(logStr, e);
    }
	
	/**
     * 获取异常堆栈信息
     * @param e
     * @return
     */
    public static String getStackTrace(Throwable e) {
        if (e == null) {
            return "";
        }
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            String str = baos.toString();
            return str;
        } catch (Exception ex) {
            PrintUtil.e(TAG, "getStackTrace failed: " + ex, ex);
        } finally {
        	if(baos != null) {
        		try {
        			baos.close();
        		} catch (Exception ex) {
        			//ignored
        		}
        	}
        }
        return "";
    }
    
    /**
     * 获取异常和cause等关键信息
     * 如果cause为空则打印所有
     * @param e
     * @return
     */
    public static String getDetailCause(Throwable e) {
        if (e == null) {
            return "";
        }
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            StringBuffer buff = new StringBuffer();
            Throwable cause = e.getCause();
            if(cause != null) {
            	//简化的detail和cause
            	buff.append(e.toString());
            	cause.printStackTrace(ps);
            	String str = baos.toString();
                buff.append("\nCaused by: ").append(str);
            } else {
            	//打印所有trace
            	e.printStackTrace(ps);
            	String str = baos.toString();
                buff.append(str);
            }
            return buff.toString();
        } catch (Throwable ex) {
            PrintUtil.e(TAG, "getDetailCause failed: " + ex, ex);
        } finally {
        	if(baos != null) {
        		try {
        			baos.close();
        		} catch (Exception ex) {
        			//ignored
        		}
        	}
        }
        return "";
    }
	
	/**
     * 合并异常堆栈信息
     * <p>text后面追加异常堆栈信息
     * @param text
     * @param e
     * @return
     */
    public static String formatStackTrace(String text, Throwable e) {
        if (text == null) {
            return text;
        }
        if (e == null) {
            return text;
        }
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            //text放在e的trace上面
            baos.write(text.getBytes());
            baos.write(NEW_LINE_CHAR_DATA);
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            String str = baos.toString();
            return str;
        } catch (Exception ex) {
            PrintUtil.e(TAG, "format Stack Trace failed: " + ex, ex);
        } finally {
        	if(baos != null) {
        		try {
        			baos.close();
        		} catch (Exception ex) {
        			//ignored
        		}
        	}
        }
        return text;
    }
    
    /**
     * 合并异常堆栈信息
     * <p>追加异常堆栈信息
     * @param text
     * @param e
     * @return
     */
    public static String mergeStackTrace(String text, Throwable e) {
        return formatStackTrace(text, e);
    }
    
	/**
     * 当前时间 yyyy-MM-dd HH:mm:ss.SSS
     * @return
     */
	public static String getLogCurentTime() {
    	return DateTimeUtil.formatTimeMillisecond(System.currentTimeMillis());
    }
	
	/**
     * 日志打印
     * @param func 函数名
     * @param text 信息体
     * @param args 参数值
     */
    public static String buildFuncLogMsg(String func, String text, Object... args) {
        StringBuilder builder = new StringBuilder();
        boolean hasFunc = !TextUtils.isEmpty(func);
        if (hasFunc) {
            builder.append(func).append(": ");
        }
        boolean hasMsg = !TextUtils.isEmpty(text);
        if (hasMsg) {
            builder.append(text);
        }
        if (args != null) {
            if (hasMsg && !text.endsWith("=")) {
                builder.append(", args=");
            }
            builder.append(Arrays.toString(args));
        }
        String msg = builder.toString();
        return msg;
    }
    
    /**
     * 修复线程信息：如果为null取当前线程信息
     * @param threadInfo
     * @return
     */
    public static String fixThreadInfo(String threadInfo) {
		if(threadInfo != null && !threadInfo.equals("")) {
			return threadInfo;
		}
		Thread t = Thread.currentThread();
		String tStr = t.getName() + "-" + t.getId();
		return tStr;
	}
}
