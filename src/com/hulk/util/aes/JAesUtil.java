package com.hulk.util.aes;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES通用加密解密工具类(Android和Java均可使用)
 * <p>注意
 * <p>1. 密码必须是16个字母和符号组成，不能多不能少(目前测试这样)
 * <p>2. android中不能使用key不能使用随机种子生成，不然会出现问题：
 * 同样的data和key, 每次加密的结果可能不同,赞成android与Java中解密结果不一致，无法解密
 * <p>以上问题只是目前出现的不知是否存在局限，后续再研究
 * Created by zhanghao on 18-3-19.
 */

public class JAesUtil {
    public final static String TAG = "AndroidAesUtil";

	public static final String CHARSET = "UTF-8";

	private static final String AES = "AES";//AES 加密
    //AES是加密方式/CBC是工作模式/PKCS5Padding是填充模式
	private static final String CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";

    //加密aes密钥,必须时16个byte,否则无效,因为美国对软件出口的控制,需要重新下载其jar替换
	// https://blog.csdn.net/wangjunjun2008/article/details/50847426
	private static final int KEY_SIZE = 128;//加密位数: 256 bits or 128 bits,192bits
    private static final String SHA1PRNG="SHA1PRNG";// SHA1PRNG 强随机种子算法, 要区别4.2以上版本的调用方法
	
    /**
     * 后面blockSize个字节为iv值
     * @param blockSize
     * @param data
     * @return
     */
    private static byte[] getIvData(int blockSize, byte[] data) {
    	int count = data.length;
    	byte[] iv = new byte[blockSize];
    	int j = 0;
    	for(int i = count - blockSize -1; i < count; i++) {
    		iv[j] = data[i];
        }
    	return iv;
    }
    
    /**
     * AES 加密
     *
     * @param content 明文
     * @param key　生成秘钥的关键字
     * @return
     */
    public static byte[] encrypt(String content, String key, byte[] ivData) {
        try {
            byte[] data = content.getBytes(CHARSET);
            byte[] keyData = getRawKey(key);
            if (data == null) {
                log("encrypt failed for clear data is null ");
                return null;
            }
            if (keyData == null) {
                log("encrypt failed for key data is null ");
                return null;
            }
            return encrypt(data, keyData, ivData);
        } catch (Exception e) {
            log("encrypt Exception: " + e, e);
        }
        return null;
    }
    
    /**
     * AES解密函数
     * @param data 明文数据
     * @param keyData 生成秘钥的关键字
     * @return
     */
    public static byte[] encrypt(byte[] data, byte[] keyData, byte[] ivData) {
        try {
        	SecretKeySpec keySpec = new SecretKeySpec(keyData, "AES");
			Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
			int blockSize = cipher.getBlockSize();
			IvParameterSpec iv = new IvParameterSpec(ivData);
	        log("Cipher blockSize= " + blockSize);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            byte[] encryptedData = cipher.doFinal(data);
            log("encrypt success encrypted Data: " + byte2HexStr(encryptedData));
            return encryptedData;
        } catch (Exception e) {
            log("encrypt failed fordata: " + byte2HexStr(data), e);
        }
        return null;
    }

    /**
     * AES 解密
     * 加密数据byte数组先进行解密，再转化为字符串
     * @param encryptedData　密文
     * @param key 生成秘钥的关键字
     * @return
     */
    public static String decrypt(byte[] encryptedData, String key, byte[] ivData) {
        try {
        	byte[] keyData = getRawKey(key);
            byte[] decryptedData = decrypt(encryptedData, keyData, ivData);
            if (decryptedData != null) {
                String clearText = new String(decryptedData, CHARSET);
                log("decryptData clear test: " + clearText);
                return clearText;
            } else {
                log("decryptedData failed for data is null ");
            }
        } catch (Exception e) {
            log("decryptData failed " + e, e);
            //System.out.println("decryptData failed: " + e);
            //e.printStackTrace();
        }
        return null;
    }

    /**
     * AES 解密
     *
     * @param data
     *            密文
     * @param keyData
     *            生成秘钥的关键字
     * @return
     */
    public static byte[] decrypt(byte[] data, byte[] keyData, byte[] ivData) {
        try {
        	SecretKeySpec keySpec = new SecretKeySpec(keyData, "AES");
			Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
			int blockSize = cipher.getBlockSize();
			IvParameterSpec iv = new IvParameterSpec(ivData);
	        log("Cipher blockSize= " + blockSize);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            byte[] decryptedData = cipher.doFinal(data);
            return decryptedData;
        }  catch (Exception e) {
            log("decrypt failed to decrypt data HexStr: " + byte2HexStr(data), e);
            //System.out.println("decrypt failed Exception : " + e);
            //e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 字节数组转化为大写16进制字符串
     *
     * @param b
     * @return
     */
    public static String byte2HexStr(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String s = Integer.toHexString(b[i] & 0xFF);
            if (s.length() == 1) {
                sb.append("0");
            }
            sb.append(s.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 16进制字符串转字节数组
     *
     * @param s
     * @return
     */
    public static byte[] str2ByteArray(String s) {
        int byteArrayLength = s.length() / 2;
        byte[] b = new byte[byteArrayLength];
        for (int i = 0; i < byteArrayLength; i++) {
            byte b0 = (byte) Integer.valueOf(s.substring(i * 2, i * 2 + 2), 16)
                    .intValue();
            b[i] = b0;
        }

        return b;
    }
    
    private static void log(String s) {
    	//System.out.println(TAG + ":" + s);
    }
    
    private static void log(String s, Throwable e) {
    	System.out.println(TAG + ":" + s);
    	if(e != null) {
    		System.out.println(e.getCause() + "  " + e.getMessage());
    		e.printStackTrace();
    	}
    }
    
    public static byte[] getRawKey(String key) {
        try {
            return getRawKey(key.getBytes());
        } catch (Exception e) {
            log("getRawKey: " + e, e);
        }
        return null;
    }

    /**
     * 对密钥进行处理
     * @param seed
     * @return
     * @throws Exception
     */
    private static byte[] getRawKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance(AES);
        SecureRandom sr = null;
        // for Java
        sr = SecureRandom.getInstance(SHA1PRNG);
        
        // for Android
        // 在4.2以上版本中，SecureRandom获取方式发生了改变
        /*if (android.os.Build.VERSION.SDK_INT >= 17) {
            sr = SecureRandom.getInstance(SHA1PRNG, "Crypto");
        } else {
            sr = SecureRandom.getInstance(SHA1PRNG);
        }*/
        sr.setSeed(seed);
        kgen.init(KEY_SIZE, sr); //128 bits,192bits or 256 bits (192 and 256 need another java security jar)
        //AES中128位密钥版本有10个加密循环，192比特密钥版本有12个加密循环，256比特密钥版本则有14个加密循环。
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        return raw;
    }
    
    /*
     * 生成随机数，可以当做动态的密钥 加密和解密的密钥必须一致，不然将不能解密
     */
    public static String generateKey(int size) {
        try {
            SecureRandom sr = SecureRandom.getInstance(SHA1PRNG);
            byte[] bytes_key = new byte[size];
            sr.nextBytes(bytes_key);
            String str_key = byte2HexStr(bytes_key);
            return str_key;
        } catch (Exception e) {
            log("generateKey: " + e, e);
        }
        return null;
    }
    
    public static byte[] generateData(int size) {
        try {
            SecureRandom sr = SecureRandom.getInstance(SHA1PRNG);
            byte[] bytes_key = new byte[size];
            sr.nextBytes(bytes_key);
            return bytes_key;
        } catch (Exception e) {
            log("generateData: " + e, e);
        }
        return null;
    }
}
