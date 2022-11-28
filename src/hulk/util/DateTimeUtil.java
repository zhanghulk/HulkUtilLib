package hulk.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class DateTimeUtil {

	/**
	 * "yyyy-MM-dd HH:mm:ss.SSS"
	 */
	public static final String MILLI_SECOND_FORMAT_STR = "yyyy-MM-dd_HH-mm-ss.SSS";
	public static final DateFormat TIME_MILLI_SECOND_FORMAT = new SimpleDateFormat(MILLI_SECOND_FORMAT_STR);
	public static final DateFormat TIME_SECOND_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	
	/**
     * format time as "yyyy-MM-dd"
     * @param timeMillis
     * @return
     */
	public static String formatDateStr(long timeMillis) {
        return DATE_FORMAT.format(timeMillis);
    }
	
	/**
     * format time as "yyyy-MM-dd HH:mm:ss"
     * @param timeMillis
     * @return
     */
	public static String formatTimeSecond(long timeMillis) {
        return TIME_SECOND_FORMAT.format(timeMillis);
    }
	
	/**
     * format time as "yyyy-MM-dd HH:mm:ss.SSS"
     * @param timeMillis
     * @return
     */
	public static String formatTimeMillisecond(long timeMillis) {
        return TIME_MILLI_SECOND_FORMAT.format(timeMillis);
    }
	
	/**
     * format time
     * @param format 时间格式， eg: "yyyy-MM-dd HH:mm:ss.SSS"
     * @param timeMillis
     * @return
     */
	public static String formatTimeMillis(String format, long timeMillis) {
        DateFormat df = new SimpleDateFormat(format);
        return df.format(timeMillis);
    }
	
	public static void main(String[] args) {
		long timeMillis = System.currentTimeMillis();
		String dateStr = formatDateStr(timeMillis);
		System.out.println("dateStr: " + dateStr);
		
		String timeSecond = formatTimeSecond(timeMillis);
		System.out.println("timeSecond: " + timeSecond);
		
		String timeMillisecond = formatTimeMillisecond(timeMillis);
		System.out.println("timeMillisecond: " + timeMillisecond);
	}
}
