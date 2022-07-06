package com.hulk.util.common;

import hulk.util.Log;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 检查是否连接互联网:
 * 警务通: 目前采用的方法是 ping www.baidu.com www.hulk.cn www.qq.com , 通了说明处于互联网
 * Created by zhanghao on 18-3-24.
 */

public class NetPingUtil {

    public interface PingCallback {
        void onPing(boolean isConnected, int status, String pingMsg);
    }

    static final String TAG = "NetCheckUtil";
    
    public static boolean sInternetConnected = false;

    //互联网链接状态缓存字段KEY
    static final String INTERNET_CONNECTED_STATE = "internet_connected_state";

    static final String[] TEST_ADDRESSES = {"www.baidu.com", "www.hulk.cn"/*, "www.qq.com"*/};
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    public static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

    /**
     * 目前采用的方法是 ping www.baidu.com www.hulk.cn www.qq.com , 通了说明处于互联网
     * @return
     */
    public static boolean pingAddress() {
        for (String address: TEST_ADDRESSES) {
            boolean connected = pingAddress(address);
            if (connected) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ping一次, 超时设置为3秒
     * @param address
     * @return
     */
    public static boolean pingAddress(String address) {
        return pingAddress(address, 1, 3);
    }

    /**
     * 需要在Android中使用LInux底层的命令：如执行Ping命令 格式为 ping -c 1 -w 5
     * <p>
     * 其中参数-c 1是指ping的次数为1次，-w是指执行的最后期限,单位为秒，也就是执行的时间为5秒，超过5秒则失败.
     * <p>
     * Ping命令代码:Process p = Runtime.getRuntime().exec("ping -c 1 -w 5 " + ip);
     *
     * @param address
     * @param pingCount
     * @param timeout
     * @return
     */
    public static boolean pingAddress(String address, int pingCount, int timeout) {
        return pingAddress(address, pingCount, timeout, null);
    }

    public static boolean pingAddress(String address, int pingCount, int timeout, PingCallback callback) {
        Process process = null;
        try {
            // process = Runtime.getRuntime().exec("ping -c 2 -w 5 " + address);
            //System.out.println("Connecting to " + address + " ...... ");
            String pingStr = "ping -c " + pingCount + " -w " + timeout + " " + address;
            Log.i(TAG, "pingAddress pingStr: " + pingStr);
            long startTime = System.currentTimeMillis();
            process = Runtime.getRuntime().exec(pingStr);
            //直接通过process.waitFor返回状态判断网络状态：0表示正常，其他表示未连接
            int status = process.waitFor();
            InputStreamReader r = new InputStreamReader(process.getInputStream());
            LineNumberReader returnData = new LineNumberReader(r);
            String returnMsg = "";
            String line = "";
            while ((line = returnData.readLine()) != null) {
                //System.out.println("pingAddress received line: " + line);
                //Log.i(TAG, "received line: " + line);
                returnMsg += line + "\n";
            }
            //直接通过process.waitFor返回状态判断网络状态：0表示正常，其他表示未连接
            boolean connected = status == 0;
            Log.i(TAG, "pingAddress status= " + status + ", returnMsg ===================:\n"
                    + returnMsg + " ## Ping spent time: " + (System.currentTimeMillis() - startTime));
            if (callback != null) {
                callback.onPing(connected, status, returnMsg);
            }
            return connected;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                System.out.println("process.destroy() ");
                process.destroy();
            }
        }
        return false;
    }

    /**
     * 解析返回字符串,判定链接是否成功:
     * 22 packets transmitted, 22 received, 0% packet loss, time 21031ms
     * 不丢包算作成功
     * 完整:
     PING www.a.shifen.com (220.181.111.188) 56(84) bytes of data.
     64 bytes from 220.181.111.188: icmp_seq=1 ttl=49 time=2.81 ms
     64 bytes from 220.181.111.188: icmp_seq=21 ttl=49 time=2.69 ms
     64 bytes from 220.181.111.188: icmp_seq=22 ttl=49 time=2.70 ms
     ......
     --- www.a.shifen.com ping statistics ---
     22 packets transmitted, 22 received, 0% packet loss, time 21031ms
     rtt min/avg/max/mdev = 2.637/2.718/2.817/0.063 ms
     * @return 不丢包算作连接成功
     * @param msg  PING 返回的结果信息
     * @return
     */
    public static boolean checkConnected(String msg) {
        if (msg != null) {
        	List<String> lines = ConvertUtil.listLines(msg, "packets transmitted");
        	if (lines != null && !lines.isEmpty()) {
        		try {
        			String line = lines.get(0);
            		String[] arr = line.split(",");
            		String transmitted = arr[0];
            		String received = arr[1];
            		String loss = arr[2];
            		if("0%".equals(loss.trim()) && transmitted.trim().equals(received.trim())) {
            			return true;
            		}
        		} catch (Exception e) {
        			Log.e(TAG, "checkConnected: " + e + ", msg: " + msg, e);
        		}
        	}
        }
        Log.w(TAG, "check Connected false msg: " + msg);
        return false;
    }

    /**
     * 异步检查互联网链接状态, 网络变化时检查当前网络类型，并缓存起来，避免UI出现ANR
     * @param callback
     * @param remark
     */
    public static void checkInternetConnectedAsync(final PingCallback callback, final String remark) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String address: TEST_ADDRESSES) {
                    Log.w(TAG, remark+ " >> Start ping address: " + address);
                    boolean connected = pingAddress(address, 1, 3, callback);
                    Log.w(TAG, remark + " << ping result connected= " + connected);
                    if (connected) {
                        if(callback != null) {
                        	callback.onPing(true, 0, "Connected");
                        }
                        return;
                    }
                }
                if(callback != null) {
                	callback.onPing(false, 1, "Not Connected");
                }
            }
        }, "checkInternetConnectedAsync-Thread").start();
    }

    public static void setInternetConnectedState(boolean internetConnected) {
    	sInternetConnected = internetConnected;
    }

    public static boolean getInternetConnectedState() {
        return sInternetConnected;
    }
}
