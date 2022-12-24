package hulk.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * 日期时间工具类
 * @author hulk
 *
 */
public class HulkDateUtil {
	/**
	 * "yyyyMMdd"
	 */
	public static SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("yyyyMMdd");
	/**
	 * "yyyyMMdd_HHmmss"
	 */
    public static SimpleDateFormat sDateTimeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    /**
     * "yyyy-MM-dd HH:mm:ss"
     */
    public static DateFormat sSecondDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * "yyyy-MM-dd HH:mm:ss.SSS"
     */
    public static DateFormat sMillisecondDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
    /**
     * format time as "yyyy-MM-dd HH:mm:ss"
     * @param timeMillis
     * @return
     */
	public static String formatTimeSecond(long timeMillis) {
        return sSecondDateFormat.format(timeMillis);
    }
	
	/**
     * get current time as "yyyy-MM-dd HH:mm:ss.SSS"
     * @return
     */
	public static String getCurrentMillisecond() {
		return formatTimeMillisecond(System.currentTimeMillis());
    }
	
	/**
     * format time as "yyyy-MM-dd HH:mm:ss.SSS"
     * @param timeMillis
     * @return
     */
	public static String formatTimeMillisecond(long timeMillis) {
        return sMillisecondDateFormat.format(timeMillis);
    }
    
    /**
     * format time as long value: yyyyMMdd_HHmmss
     * @return
     */
    public static String formatTimeSecondStr(long timeMillis) {
		return sDateTimeFormat.format(timeMillis);
	}
    
    /**
     * format time as yyyyMMdd
     * @return
     */
    public static String formatDateStr(long timeMillis) {
		return sSimpleDateFormat.format(timeMillis);
	}
}
