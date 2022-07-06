package com.hulk.model.pc.core;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * 系统日志打印工具类
 * @author zhanghao
 *
 */
public class SysLog {
	
	private static String TAG = "SysLog";
	/**
	 * "yyyy-MM-dd HH:mm:ss.SSS"
	 */
	private static String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

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
    	StringBuffer buff = new StringBuffer();
    	buff.append(getLogCurentTime());
    	String tStr = "";
    	if(threadInfo == null || threadInfo.equals("")) {
    		Thread t = Thread.currentThread();
    		tStr = t.getName() + "-" + t.getId();
    	} else {
    		tStr = threadInfo;
    	}
    	if(tStr != null && !tStr.equals("")) {
    		buff.append("  ").append(tStr);
    	}
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
            System.err.println(ex);
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
            System.err.println(ex);
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
        if (e == null) {
            return text;
        }
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            if (text != null) {
            	text = text + "\n";
                //text放在e的trace上面
                baos.write(text.getBytes());
            }
            PrintStream ps = new PrintStream(baos);
            if (e != null) {
                e.printStackTrace(ps);
            }
            String str = baos.toString();
            return str;
        } catch (Exception ex) {
            System.err.println(ex);
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
    	return formatTimeMillisecond(System.currentTimeMillis());
    }
	
	/**
     * format time as "yyyy-MM-dd HH:mm:ss.SSS"
     * @param timeMillis
     * @return
     */
	public static String formatTimeMillisecond(long timeMillis) {
        return formatTimeMillis(timeMillis, DATE_TIME_FORMAT);
    }
	
	/**
     * format time
     * @param timeMillis
     * @param pattern 时间格式  "yyyy-MM-dd HH:mm:ss.SSS"
     * @return
     */
	public static String formatTimeMillis(long timeMillis, String pattern) {
        DateFormat df = new SimpleDateFormat(pattern);
        return df.format(timeMillis);
    }

	
	/**
	 * 获取当前线程信息
	 * @return
	 */
	public static String getCurrentThreadInfo() {
		Thread t = Thread.currentThread();
		return t.getName() + "-" + t.getId();
	}
}
