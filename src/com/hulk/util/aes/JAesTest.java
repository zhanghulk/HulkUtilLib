package com.hulk.util.aes;

public class JAesTest {

	public final static String KEY = "1234567890ABCDEF";
	//IV参数长度和密钥长度一致
	public static final String VIPARA = "abcdefghijklmnop";
	
	public static void main(String[] args) {
		//testConstantIV();//IV为常量
		test();//随机IV
	}

	/**
	 * 使用随机数作为iv值，相同的数据，每次生成的加密数据都不同
	 * 发送方：随机生成16个字节作为iv值，原字符串加密后的数据后面加上这15个字节的随机数；
	 * 接收方：取后面16个字节为iv值，对前面的数据进行解密，
	 */
	public static void test() {
		//发送端
        String text = "zhanghao";
        log("Oraginal plain text: " + text);
        byte[] encIvData = JAesUtil.generateData(16);//随机IV为固定的16个字符
        log("encIvData: " + JAesUtil.byte2HexStr(encIvData) + ", length= " + encIvData.length);
        byte[] encryptedData = JAesUtil.encrypt(text, KEY, encIvData);
        log("EncryptedData: " + JAesUtil.byte2HexStr(encryptedData) + ", length= " + encryptedData.length);
        int length = encryptedData.length + encIvData.length;
        //实际传输的数据为encryptedData+encIvData
        byte[] postData = new byte[length];
        System.arraycopy(encryptedData, 0, postData, 0, encryptedData.length);
        System.arraycopy(encIvData, 0, postData, encryptedData.length, encIvData.length);
        log("PostData: " + JAesUtil.byte2HexStr(postData) + ", length= " + postData.length);
        
        //接收端
        byte[] receivedData = postData;
        byte[] originData = new byte[receivedData.length - 16];
        System.arraycopy(receivedData, 0, originData, 0, receivedData.length - 16);//后16位为iv值
        log("Received originData: " + JAesUtil.byte2HexStr(originData) + ", length= " + originData.length);
        byte[] decIvData = new byte[16];
        System.arraycopy(receivedData, receivedData.length - 16, decIvData, 0, 16);//前面的为源数据
        log("decIvData: " + JAesUtil.byte2HexStr(decIvData) + ", length= " + decIvData.length);
        String decryptedText = JAesUtil.decrypt(originData, KEY, decIvData);
        log("decrypted text: " + decryptedText);
    }
	
	public static void testConstantIV() {
        String text = "zhanghao";
        byte[] encIvData = VIPARA.getBytes();
        log("Oraginal  text: " + text);
        byte[] encryptedData = JAesUtil.encrypt(text, KEY, encIvData);
        String cliperText = JAesUtil.byte2HexStr(encryptedData);
        log("encrypted HexStr >>> " + cliperText);
        
        byte[] decIvData = VIPARA.getBytes();
        String decryptedText = JAesUtil.decrypt(encryptedData, KEY, decIvData);
        log("decrypted text: " + decryptedText);
    }
	
	private static void log(String s) {
    	System.out.println(s);
    }
}
