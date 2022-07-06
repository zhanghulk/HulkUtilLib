package com.hulk.util.rsa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import hulk.util.Log;
import hulk.util.base64.Base64;

/**
 * Android版本的RSA加密算法实现 Java也适用
 * 1. 可生成密钥对
 * 2. 解析X509格式的公钥私钥
 * @author hulk
 *
 */
public class RSAUtils {

	private static final String TAG = "RSAUtils";

	// 构建Cipher实例时所传入的的字符串，默认为"RSA/NONE/PKCS1Padding"
	private static String RSA_TRANSFORM = "RSA/NONE/PKCS1Padding";


	/**
	 * 产生密钥对
	 * 
	 * @param keyLength
	 *            密钥长度，小于1024长度的密钥已经被证实是不安全的，通常设置为1024或者2048，建议2048
	 */
	public static KeyPair generateRSAKeyPair(int keyLength) {
		KeyPair keyPair = null;
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(keyLength); // 设置密钥长度
			keyPair = keyPairGenerator.generateKeyPair(); // 产生密钥对
		} catch (NoSuchAlgorithmException e) {
			// e.printStackTrace();
			Log.e(TAG, "generateRSAKeyPair: " + e, e);
		}
		return keyPair;
	}
	
	/**
	 * 将字符串形式的公钥转换为公钥对象
	 * 
	 * @param publicKeyStr
	 * @param isBase64
	 * @return
	 */
	public static PublicKey keyStrToPublicKey(String publicKeyStr, boolean isBase64) {
		PublicKey publicKey = null;
		byte[] keyBytes = null;
		String fixedKeyStr = fixKey(publicKeyStr);
		if(isBase64) {
			keyBytes = Base64.decodeBase64(fixedKeyStr.getBytes());
		} else {
			keyBytes = fixedKeyStr.getBytes();
		}
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			publicKey = keyFactory.generatePublic(keySpec);
		} catch (NoSuchAlgorithmException e) {
			//e.printStackTrace();
			Log.e(TAG,  "keyStrToPublicKey: " + e, e);
		} catch (InvalidKeySpecException e) {
			//e.printStackTrace();
			Log.e(TAG,  "keyStrToPublicKey: " + e, e);
		}

		return publicKey;
	}

	/**
	 * 将字符串形式的私钥，转换为私钥对象
	 * 
	 * @param privateKeyStr
	 * @param isBase64
	 * @return
	 */
	public static PrivateKey keyStrToPrivate(String privateKeyStr, boolean isBase64) {
		PrivateKey privateKey = null;
		byte[] keyBytes = null;
		String fixedKeyStr = fixKey(privateKeyStr);
		if(isBase64) {
			keyBytes = Base64.decodeBase64(fixedKeyStr.getBytes());
		} else {
			keyBytes = fixedKeyStr.getBytes();
		}
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			privateKey = keyFactory.generatePrivate(keySpec);
		} catch (NoSuchAlgorithmException e) {
			//e.printStackTrace();
			Log.e(TAG,  "keyStrToPrivate: " + e, e);
		} catch (InvalidKeySpecException e) {
			//e.printStackTrace();
			Log.e(TAG,  "keyStrToPrivate: " + e, e);
		}

		return privateKey;
	}
	
	/**
     * 去除字符串中的空格、回车、换行符、制表符
     * @param str
     * @return
     */
    public static String fixKey(String srcKeyStr) {
        String dest = "";
        Log.i(TAG, "fixKey src: " + srcKeyStr);
        if (srcKeyStr != null) {
        	//去掉key string中的非法字符和首尾标记
        	srcKeyStr = srcKeyStr.replaceAll("-----BEGIN PUBLIC KEY-----|-----END PUBLIC KEY-----", "");
        	//去掉字符串中的"\n"
        	srcKeyStr = removeSlashN(srcKeyStr);
        	//去除字符串中的空格、回车、换行符、制表符
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(srcKeyStr);
            dest = m.replaceAll("");
        }
        Log.i(TAG, "fixKey dest: " + dest);
        return dest;
    }
    
    /**
     * 去掉字符串中的"\n"
     * @param key
     * @return
     */
    private static String removeSlashN(String key) {
    	String[] arr = key.split("\\\\n");
    	Log.i(TAG, "removeSlashN arr: " + Arrays.toString(arr));
    	StringBuffer buff = new StringBuffer();
    	if(arr != null) {
    		for (String string : arr) {
    			buff.append(string);
    		}
    	}
    	return buff.toString();
    }

	/**
	 * 加密或解密数据的通用方法
	 * 
	 * @param srcData
	 *            待处理的数据 明文 or 密文原始数据
	 * @param key
	 *            公钥或者私钥
	 * @param mode
	 *            指定是加密还是解密，值为Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
	 * @return
	 */
	public static byte[] processData(byte[] srcData, Key key, int mode) {
		byte[] destData = null;
		try {
			// 获取Cipher实例
			Cipher cipher = Cipher.getInstance(RSA_TRANSFORM);
			// 初始化Cipher，mode指定是加密还是解密，key为公钥或私钥
			cipher.init(mode, key);
			destData = cipher.doFinal(srcData); // 处理数据
		} catch (NoSuchAlgorithmException e) {
			// e.printStackTrace();
			Log.e(TAG, "processData: " + e, e);
		} catch (NoSuchPaddingException e) {
			// e.printStackTrace();
			Log.e(TAG, "processData: " + e, e);
		} catch (InvalidKeyException e) {
			// e.printStackTrace();
			Log.e(TAG, "processData: " + e, e);
		} catch (BadPaddingException e) {
			// e.printStackTrace();
			Log.e(TAG, "processData: " + e, e);
		} catch (IllegalBlockSizeException e) {
			// e.printStackTrace();
			Log.e(TAG, "processData: " + e, e);
		}

		return destData;
	}

	/**
	 * 使用公钥加密数据，结果用Base64转码
	 * 
	 * @param srcData
	 * @param publicKey
	 * @return
	 * @throws IOException 
	 */
	public static String encryptDataByPublicKey(byte[] srcData, PublicKey publicKey) throws UnsupportedEncodingException {
		byte[] resultBytes = processData(srcData, publicKey, Cipher.ENCRYPT_MODE);
		return new String(Base64.encodeBase64(resultBytes));
	}
	
	/**
	 * 使用公钥加密数据，结果用Base64转码
	 * 
	 * @param srcData
	 * @param publicKey
	 * @return
	 * @throws IOException 
	 */
	public static String encryptDataByPublicKey(byte[] srcData, PublicKey publicKey, String charsetName) throws UnsupportedEncodingException {
		byte[] resultBytes = processData(srcData, publicKey, Cipher.ENCRYPT_MODE);
		return new String(Base64.encodeBase64(resultBytes), charsetName);
	}

	/**
	 * 使用私钥解密，返回解码数据
	 * 
	 * @param encryptedData
	 * @param privateKey
	 * @return
	 */
	public static byte[] decryptDataByPrivate(String encryptedData, PrivateKey privateKey) {
		byte[] bytes = Base64.decodeBase64(encryptedData.getBytes());
		return processData(bytes, privateKey, Cipher.DECRYPT_MODE);
	}

	/**
	 * 使用私钥进行解密，解密数据转换为字符串，使用utf-8编码格式
	 * 
	 * @param encryptedData
	 * @param privateKey
	 * @return
	 */
	public static String decryptedToStrByPrivate(String encryptedData, PrivateKey privateKey) {
		return new String(decryptDataByPrivate(encryptedData, privateKey));
	}

	/**
	 * 使用私钥解密，解密数据转换为字符串，并指定字符集
	 * 
	 * @param encryptedData
	 * @param privateKey
	 * @param charset
	 * @return
	 */
	public static String decryptedToStrByPrivate(String encryptedData, PrivateKey privateKey, String charset) {
		try {
			return new String(decryptDataByPrivate(encryptedData, privateKey), charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 使用私钥加密，结果用Base64转码
	 * 
	 * @param srcData
	 * @param privateKey
	 * @return
	 */
	public static String encryptDataByPrivateKey(byte[] srcData, PrivateKey privateKey) {
		byte[] resultBytes = processData(srcData, privateKey, Cipher.ENCRYPT_MODE);
		return new String(Base64.encodeBase64(resultBytes));
	}
	
	/**
	 * 使用私钥加密，结果用Base64转码
	 * 
	 * @param srcData
	 * @param privateKey
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public static String encryptDataByPrivateKey(byte[] srcData, PrivateKey privateKey, String charsetName) throws UnsupportedEncodingException {
		byte[] resultBytes = processData(srcData, privateKey, Cipher.ENCRYPT_MODE);
		return new String(Base64.encodeBase64(resultBytes), charsetName);
	}

	/**
	 * 使用公钥解密，返回解密数据
	 * 
	 * @param encryptedData
	 * @param publicKey
	 * @return
	 */
	public static byte[] decryptDataByPublicKey(String encryptedData, PublicKey publicKey) {
		byte[] bytes = Base64.decodeBase64(encryptedData.getBytes());
		return processData(bytes, publicKey, Cipher.DECRYPT_MODE);
	}

	/**
	 * 使用公钥解密，结果转换为字符串，使用默认字符集utf-8
	 * 
	 * @param encryptedData
	 * @param publicKey
	 * @return
	 */
	public static String decryptedToStrByPublicKey(String encryptedData, PublicKey publicKey) {
		return new String(decryptDataByPublicKey(encryptedData, publicKey));
	}

	/**
	 * 使用公钥解密，结果转换为字符串，使用指定字符集
	 * 
	 * @param encryptedData
	 * @param publicKey
	 * @param charset
	 * @return
	 */
	public static String decryptedToStrByPublicKey(String encryptedData, PublicKey publicKey, String charset) {
		try {
			return new String(decryptDataByPublicKey(encryptedData, publicKey), charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
