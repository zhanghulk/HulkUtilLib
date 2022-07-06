package com.hulk.util.aes;

import hulk.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES通用加密解密工具类(Android和Java均可使用)
 * <p>注意
 * <p>1. 密码必须是16个字母和符号组成，不能多不能少(目前测试这样)
 * <p>2. android中不能使用key不能使用随机种子生成，不然会出现问题：
 * <p>同样的data和key, 每次加密的结果可能不同,造成android与Java中解密结果不一致，无法解密
 * <p>以上问题只是目前出现的不知是否存在局限，后续再研究
 * Created by zhanghao on 18-3-19.
 */

public class AndrAesUtil {
    public final static String TAG = "AndroidAesUtil";

    //public static final String VIPARA = "1269571569321021";
	public static final String CHARSET = "UTF-8";

    //AES是加密方式/CBC是工作模式/PKCS5Padding是填充模式
	private static final String CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";

    //加密aes密钥,必须时16个byte,否则无效
	public final static String KEY = "1234567890ABCDEF";

    public static void main(String[] args) {
        test();
    }

    public static void test() {
        String text = "123456789";
        byte[] encryptedData = AndrAesUtil.encrypt(text, KEY);
        String cliperText = byte2HexStr(encryptedData);
        System.out.println("Oraginal  text: " + text);
        System.out.println("encrypted HexStr >>> " + cliperText);

        String decryptedText = decryptData(encryptedData, KEY);
        System.out.println("decrypted text: " + decryptedText);
    }

    /**
     * AES 加密
     *
     * @param content 明文
     * @param key　生成秘钥的关键字
     * @return
     */
    public static byte[] encrypt(String content, String key) {
        try {
            byte[] data = content.getBytes(CHARSET);
            byte[] keyData = key.getBytes();
            if (data == null) {
                Log.w(TAG, "encrypt failed for clear data is null ");
                return null;
            }
            if (keyData == null) {
                Log.w(TAG, "encrypt failed for key data is null ");
                return null;
            }
            return encrypt(data, keyData);
        } catch (Exception e) {
            Log.e(TAG, "encrypt Exception: " + e, e);
            //System.out.println("encrypt failed Exception : " + e);
            //e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param data 明文数据
     * @param keyData 生成秘钥的关键字
     * @return
     */
    public static byte[] encrypt(byte[] data, byte[] keyData) {
        try {
            SecretKeySpec key = new SecretKeySpec(keyData, "AES");
            Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
            //IvParameterSpec zeroIv = new IvParameterSpec(VIPARA.getBytes());
            int blockSize = cipher.getBlockSize();
            IvParameterSpec zeroIv = new IvParameterSpec(new byte[blockSize]);
            cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
            byte[] encryptedData = cipher.doFinal(data);
            Log.i(TAG, "encrypt success encrypted Data: " + byte2HexStr(encryptedData));
            return encryptedData;
        } catch (Exception e) {
            Log.e(TAG, "encrypt failed fordata: " + byte2HexStr(data), e);
            //System.out.println("encrypt failed Exception : " + e);
            //e.printStackTrace();
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
    public static String decryptData(byte[] encryptedData, String key) {
        try {
            byte[] decryptedData = decrypt(encryptedData, key.getBytes());
            if (decryptedData != null) {
                String clearText = new String(decryptedData, CHARSET);
                Log.i(TAG, "decryptData clear test: " + clearText);
                return clearText;
            } else {
                Log.w(TAG, "decryptedData failed for data is null ");
            }
        } catch (Exception e) {
            Log.e(TAG, "decryptData failed " + e, e);
            //System.out.println("decryptData failed: " + e);
            //e.printStackTrace();
        }
        return null;
    }

    /**
     * AES 解密
     *
     * @param content
     *            密文
     * @param key
     *            生成秘钥的关键字
     * @return
     */
    public static byte[] decryptText(String content, String key) {
        byte[] byteMi=  str2ByteArray(content);
        return decrypt(byteMi, key.getBytes());
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
    public static byte[] decrypt(byte[] data, byte[] keyData) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(keyData, "AES");
            Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
            //IvParameterSpec zeroIv = new IvParameterSpec(VIPARA.getBytes());
            int blockSize = cipher.getBlockSize();
            IvParameterSpec zeroIv = new IvParameterSpec(new byte[blockSize]);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, zeroIv);
            byte[] decryptedData = cipher.doFinal(data);
            return decryptedData;
        }  catch (Exception e) {
            Log.e(TAG, "decrypt failed to decrypt data HexStr: " + byte2HexStr(data), e);
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
    private static byte[] str2ByteArray(String s) {
        int byteArrayLength = s.length() / 2;
        byte[] b = new byte[byteArrayLength];
        for (int i = 0; i < byteArrayLength; i++) {
            byte b0 = (byte) Integer.valueOf(s.substring(i * 2, i * 2 + 2), 16)
                    .intValue();
            b[i] = b0;
        }

        return b;
    }
}
