package com.hulk.util.aes;

import hulk.text.TextUtils;
import hulk.util.Log;

import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.hulk.util.common.Base64Decoder;
import com.hulk.util.common.Base64Encoder;

/**
 * 高级加密标准（英语：Advanced Encryption Standard，缩写：AES），
 * 在密码学中又称Rijndael加密法，是美国联邦政府采用的一种区块加密标准。
 * 这个标准用来替代原先的DES，已经被多方分析且广为全世界所使用
 * http://www.cnblogs.com/whoislcj/p/5473030.html
 * @author zhanghao
 *
 */
public class AesUtils {
    private static final  String TAG = "AesUtils";

	private static final  String HEX = "0123456789ABCDEF";

    private static final String AES = "AES";//AES 加密
    private static final int KEY_SIZE = 256;//加密位数: 256 bits or 128 bits,192bits
    private static final String CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";//AES是加密方式 / CBC是工作模式 / PKCS5Padding是填充模式
    private static final String SHA1PRNG="SHA1PRNG";// SHA1PRNG 强随机种子算法, 要区别4.2以上版本的调用方法
    
    private static boolean sSecureRandomCryptoMode = true;
    
    public static void setSecureRandomCryptoMode(boolean cryptoMode) {
    	sSecureRandomCryptoMode = cryptoMode;
    }
    
    public static void disableSecureRandomCryptoMode() {
    	sSecureRandomCryptoMode = false;
    }

    /*
     * 生成随机数，可以当做动态的密钥 加密和解密的密钥必须一致，不然将不能解密
     */
    public static String generateKey() {
        try {
            SecureRandom localSecureRandom = SecureRandom.getInstance(SHA1PRNG);
            byte[] bytes_key = new byte[20];
            localSecureRandom.nextBytes(bytes_key);
            String str_key = toHex(bytes_key);
            return str_key;
        } catch (Exception e) {
            Log.e(TAG, "generateKey: " + e, e);
        }
        return null;
    }

    public static byte[] getRawKey(String key) {
        try {
            return getRawKey(key.getBytes());
        } catch (Exception e) {
            Log.e(TAG, "getRawKey: " + e, e);
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
        //for android
        SecureRandom sr = null;
        // 在4.2以上版本中，SecureRandom获取方式发生了改变
        if (sSecureRandomCryptoMode) {
            sr = SecureRandom.getInstance(SHA1PRNG, "Crypto");
        } else {
            sr = SecureRandom.getInstance(SHA1PRNG);
        }
        // for Java
        // secureRandom = SecureRandom.getInstance(SHA1PRNG);
        sr.setSeed(seed);
        kgen.init(KEY_SIZE, sr); //256 bits or 128 bits,192bits
        //AES中128位密钥版本有10个加密循环，192比特密钥版本有12个加密循环，256比特密钥版本则有14个加密循环。
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        Log.v(TAG, "Create RawKey data: " + Arrays.toString(seed) + " >>> " + Arrays.toString(raw));
        return raw;
    }

    public static String encrypt(String key, String clearText) {
        byte[] rawKey = getRawKey(key);
        if (rawKey == null) {
            Log.e(TAG, "encrypt text failed for process raw key failed !! ");
            return "";
        }
        return encrypt(rawKey, clearText);
    }

    /**
     * 加密
     * @param rawKey
     * @param clearText
     * @return  被加密后，经过Base64的字符串
     */
    public static String encrypt(byte[] rawKey, String clearText) {
        try {
            byte[] data = encryptData(rawKey, clearText);
            if (data == null) {
                Log.w(TAG, "encrypt data is null !! ");
                return "";
            }
            return Base64Encoder.encode(data);
        } catch (Exception e) {
            Log.e(TAG, "Failed to Base64 Exception: " + e, e);
        }
        return null;
    }

    /**
     * 加密
     * @param rawKey
     * @param clearText
     * @return  被加密后的byte数组
     */
    public static byte[] encryptData(byte[] rawKey, String clearText) {
        if (TextUtils.isEmpty(clearText)) {
            return null;
        }
        try {
            byte[] result = encrypt(rawKey, clearText.getBytes());
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Failed to encrypt: " + clearText, e);
        }
        return null;
    }

    /**
     * 加密
     * @param rawKey
     * @param clearData
     * @return
     * @throws Exception
     */
    public static byte[] encrypt(byte[] rawKey, byte[] clearData) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(rawKey, AES);
        Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
        byte[] encrypted = cipher.doFinal(clearData);
        return encrypted;
    }

    public static String decrypt(String key, String encryptedText) {
        byte[] rawKey = getRawKey(key);
        if (rawKey == null) {
            Log.e(TAG, "decrypt text failed for process raw key failed !! ");
            return "";
        }
        return decrypt(rawKey, encryptedText);
    }

    /**
     * 解密
     * @param rawKey
     * @param encryptedText
     * @return
     */
    public static String decrypt(byte[] rawKey, String encryptedText) {
        if (TextUtils.isEmpty(encryptedText)) {
            return encryptedText;
        }
        try {
            byte[] enc = Base64Decoder.decodeToBytes(encryptedText);
            byte[] result = decrypt(rawKey, enc);
            return new String(result);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt: " + encryptedText, e);
        }
        return null;
    }

    /**
     * 解密
     * @param rawKey
     * @param encrypted
     * @return
     * @throws Exception
     */
    public static byte[] decrypt(byte[] rawKey, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(rawKey, AES);
        Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }
    
  //二进制转字符
    public static String toHex(byte[] buf) {
        if (buf == null)
            return "";
        StringBuffer result = new StringBuffer(2 * buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
    }
}
