package hulk.util;

/**
 * 日志工具类
 * @Author: zhanghao
 * @Time: 2022-07-07 19:01:33
 * @Since: 1.0.0
 */

import java.util.Arrays;

import hulk.text.TextUtils;

/**
 * UES的日志打印器
 * <p>格式化打印日直性子,变量模板类型如下:
 * <p>String msgFormat = "My name=%s"
 * <p> UesLogger.log()
 * <p>格式化字符串	对象参数/值
 * b 或 B	它表示一个布尔值。
 * h 或 H	它代表一个十六进制值。
 * s 或 S	它表示一个字符串值。
 * c 或 C	它代表一个字符值。
 * d	    它表示一个整数值。
 * f	    它代表一个浮点值。
 * o	    它表示一个八进制整数值。
 * x 或 X	它表示一个十六进制整数。
 * e 或 E	它表示计算机科学计数法中的十进制数。
 * t 或 T	它表示日期和时间转换字符。
 * @author: zhanghao
 * @Time: 2022-05-25 17:51
 */
public class HulkLogger {

    private static final String TAG = "HulkLogger";
    private static boolean sDebugMode = true;
    /**
     * log级别,预留字段,HulkLogger目前没有分级,后期有必要再实现分级
     */
    private static int sLogLevel = Log.INFO;

    /**
     * 日志打印, DEBUG模式打印
     *
     * @param msgFormat 信息体模板
     * @param args      参数值
     */
    public static void log(String msgFormat, Object... args) {
        log(TAG, "", msgFormat, args);
    }

    /**
     * 日志打印, DEBUG模式打印
     *
     * @param tag       TAG
     * @param msgFormat 信息体模板
     * @param args      参数值
     */
    public static void log(String tag, String msgFormat, Object... args) {
        log(tag, "", msgFormat, args);
    }

    /**
     * 日志打印, DEBUG模式打印
     *
     * @param tag       TAG
     * @param func      函数名
     * @param msgFormat 信息体模板
     * @param args      参数值
     */
    public static void log(String tag, String func, String msgFormat, Object... args) {
        if (sDebugMode) {
            printLog(tag, func, msgFormat, args);
        }
    }

    /**
     * 日志打印
     *
     * @param tag       TAG
     * @param func      函数名
     * @param msgFormat 信息体模板
     * @param args      参数值
     */
    public static void printLog(String tag, String func, String msgFormat, Object... args) {
        //格式化
        String msg = buildLogMag(func, msgFormat, args);
        Log.i(tag, msg);
    }

    public static void w(String tag, String msgFormat, Object... args) {
        w(tag, "", msgFormat, args);
    }

    /**
     * 日志打印, DEBUG模式打印
     *
     * @param tag       TAG
     * @param func      函数名
     * @param msgFormat 信息体模板
     * @param args      参数值
     */
    public static void w(String tag, String func, String msgFormat, Object... args) {
        if (sDebugMode) {
            printLogW(tag, func, msgFormat, args);
        }
    }

    /**
     * 日志打印
     *
     * @param tag       TAG
     * @param func      函数名
     * @param msgFormat 信息体模板
     * @param args      参数值
     */
    public static void printLogW(String tag, String func, String msgFormat, Object... args) {
        //格式化
        String msg = buildLogMag(func, msgFormat, args);
        Log.w(tag, msg);
    }

    public static void e(String tag, String msgFormat, Object... args) {
        e(tag, "", msgFormat, null, args);
    }

    public static void e(String tag, String msgFormat, Throwable e, Object... args) {
        e(tag, "", msgFormat, e, args);
    }

    /**
     * 日志打印, DEBUG模式打印
     *
     * @param tag       TAG
     * @param func      函数名
     * @param msgFormat 信息体模板
     * @param e
     * @param args      参数值
     */
    public static void e(String tag, String func, String msgFormat, Throwable e, Object... args) {
        if (sDebugMode) {
            printLogE(tag, func, msgFormat, e, args);
        }
    }

    /**
     * 日志打印
     *
     * @param tag       TAG
     * @param func      函数名
     * @param msgFormat 信息体模板
     * @param e
     * @param args      参数值
     */
    public static void printLogE(String tag, String func, String msgFormat, Throwable e, Object... args) {
        //格式化
        String msg = buildLogMag(func, msgFormat, e, args);
        Log.e(tag, msg);
    }

    /**
     * 构建log信息
     *
     * @param func
     * @param msgFormat
     * @param args
     * @return
     */
    public static String buildLogMag(String func, String msgFormat, Object... args) {
        return buildLogMag(func, msgFormat, null, args);
    }

    /**
     * 构建log信息
     *
     * @param func
     * @param msgFormat
     * @param th
     * @param args
     * @return
     */
    public static String buildLogMag(String func, String msgFormat, Throwable th, Object... args) {
        StringBuilder builder = new StringBuilder();
        boolean hasFunc = !TextUtils.isEmpty(func);
        if (hasFunc) {
            builder.append(func);
            if (!func.endsWith(":") && !func.endsWith(": ")) {
                builder.append(": ");
            }
        }
        //格式化
        String msg;
        if (args != null && args.length > 0) {
            try {
                msg = String.format(msgFormat, args);
                builder.append(msg);
            } catch (Throwable throwable) {
                //失败后直接拼字符串,避免出现日志打印崩溃
                builder.append(msgFormat).append("=").append(Arrays.toString(args));
            }
        } else {
            msg = msgFormat;
            builder.append(msg);
        }
        if (th != null) {
            //加上栈信息
            builder.append("\n").append(Log.getStackTraceString(th));
        }
        return builder.toString();
    }

    public static void setDebugMode(boolean debugMode) {
        sDebugMode = debugMode;
    }

    public static boolean isDebugMode() {
        return sDebugMode;
    }

    public static void setLogLevel(int logLevel) {
        sLogLevel = logLevel;
    }
}
