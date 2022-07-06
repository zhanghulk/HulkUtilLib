package com.hulk.util.common;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import hulk.text.TextUtils;
import hulk.util.PrintUtil;

public class ZipUtil {
    private static final String TAG = "ZipUtil";
    /**
     * 缓冲大小
     */
    private static int BUFFERSIZE = 2 << 10;

    /**
     * 压缩
     * <p> eg:
     * <p>  String[] files = new String[] {"D:/A/feng/feng/src/com/feng/util/ZipUtil.java", "D:/A/feng/feng/src/com/feng/test"};
     * <p>  zip(files,"E:/test/test.zip");
     * <p>android是SD卡路径为SD路径
     * @param srcPaths 源文件路径数组，数组里面可以包含文件夹和文件.
     * @param destZipPath 压缩后的ip文件路径
     *
     */
    public static boolean zip(String[] srcPaths, String destZipPath) throws IOException, RuntimeException {
        if (srcPaths == null || srcPaths.length == 0 || TextUtils.isEmpty(destZipPath)) {
            PrintUtil.w(TAG, "zip: Invalid srcPaths " + srcPaths + " or destZipPath: " + destZipPath);
            throw new IllegalArgumentException("zip: Invalid srcPaths: " + srcPaths + ", destZipPath: " + destZipPath);
        }
        List<File> files = new ArrayList<File>();
        for(String filePath : srcPaths) {
            File file = new File(filePath);
            files.add(file);
        }
        return zip(files.toArray(new File[files.size()]), new File(destZipPath));
    }
    
    /**
     * 压缩文件数组(可以文件件和文件夹混搭)
     * <p> eg:
     * <p>  String[] files = new String[] {"D:/A/feng/feng/src/com/feng/util/ZipUtil.java", "D:/A/feng/feng/src/com/feng/test"};
     * <p>  zip(files,"E:/test/test.zip");
     * <p>android是SD卡路径为SD路径
     * @param srcFiles 源文件数组，数组里面可以包含文件夹和文件.
     * @param destZipFilePath 压缩后的ip文件路径
     *
     */
    public static boolean zip(File[] srcFiles, File destZipFile) throws IOException, RuntimeException {
        if (srcFiles == null || destZipFile == null) {
            PrintUtil.w(TAG, "zip: Invalid srcFiles: " + srcFiles + ", destZipFile: " + destZipFile);
            throw new IllegalArgumentException("zip: Invalid srcFiles: " + srcFiles + ", destZipFile: " + destZipFile);
        }
        ZipOutputStream zos = null;
        try {
        	//确保父文件夹存在
        	File destDir = destZipFile.getParentFile();
        	if(!destDir.exists()) {
        		if(!destDir.mkdirs()) {
        			PrintUtil.w(TAG, "zip: Failed to mkdirs destDir: " + destDir);
        			throw new RuntimeException("zip: Failed to mkdirs destDir: " + destDir);
        		}
        	}
            zos = new ZipOutputStream(new FileOutputStream(destZipFile));
            for(File file : srcFiles) {
            	if(!file.exists()) {
            		PrintUtil.w(TAG, "zip: Not existed failed: " + file);
            		continue;
            	}
                //递归压缩文件
                String relativePath = file.getName();
                if(file.isDirectory()) {
                    //文件夹的话要加上路径斜线
                    relativePath += File.separator;
                }
                zipFile(file, relativePath, zos);
            }
            return true;
        } catch (IOException e) {
            PrintUtil.e(TAG, "zip error: " + e, e);
            throw e;
        } finally {
            try {
                if(zos != null) {
                    zos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 压缩文件/文件夹
     * @param file  源文件:文件或者文件夹
     * @param relativePath  文件的相对路径
     * @param zos 要入zip文件输出流
     * @throws IOException
     */
    public static boolean zipFile(File file, String relativePath, ZipOutputStream zos) throws IOException, RuntimeException {
        InputStream is = null;
        if(file == null) {
        	PrintUtil.w(TAG, "zipFile: Invalid file: " + file + ", relativePath: " + relativePath + ", zos: " + zos);
            //throw new NullPointerException("zipFile: Invalid file: " + file + ", relativePath: " + relativePath + ", zos: " + zos);
        	return false;
        }
        if(!file.exists()) {
        	PrintUtil.w(TAG, "zipFile: Not existed file: " + file);
            //throw new RuntimeException("zipFile: Not existed file: " + file);
        	return false;
        }
        try {
            if(!file.isDirectory()) {
                ZipEntry zp = new ZipEntry(relativePath);
                zos.putNextEntry(zp);
                is = new FileInputStream(file);
                byte[] buffer = new byte[BUFFERSIZE];
                int length = 0;
                while ((length = is.read(buffer)) >= 0) {
                    zos.write(buffer, 0, length);
                }
                zos.flush();
                zos.closeEntry();
            } else {
                String tempPath = null;
                File[] files = file.listFiles();
                if (files != null) {
                    for(File f: files) {
                        //地柜压缩子文件夹
                        tempPath = relativePath + f.getName();
                        if(f.isDirectory()) {
                            //文件夹的话要加上路径斜线
                            tempPath += File.separator;
                        }
                        zipFile(f, tempPath, zos);
                    }
                }
            }
            return true;
        } catch (IOException e) {
            PrintUtil.e(TAG, "zipFile error: " + e + ", file= " + file, e);
            throw e;
        } finally {
            try {
                if(is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 解压缩
     *<p>eg: unzip("E:/test/test.zip", "E:/test/");
     * @param zipFilePath 压缩文件路径
     * @param destDir 解压后的文件目录
     */
    public static boolean unzip(String zipFilePath, String destDir) throws IOException, RuntimeException {
        if (TextUtils.isEmpty(zipFilePath) || TextUtils.isEmpty(destDir)) {
            PrintUtil.w(TAG, "unzip: invalid zipFilePath: " + zipFilePath + " or destDir:" + destDir);
            throw new IllegalArgumentException("unzip: invalid zipFilePath: " + zipFilePath + " or destDir:" + destDir); 
        }
        if(!destDir.endsWith("/")) {
        	destDir = destDir + "/";
        }
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            ZipFile zf = new ZipFile(new File(zipFilePath));
            Enumeration en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry zn = (ZipEntry) en.nextElement();
                if (!zn.isDirectory()) {
                    is = zf.getInputStream(zn);
                    File f = new File(destDir + zn.getName());
                    File p = f.getParentFile();
                    if(!p.exists()) {
                    	if(!p.mkdirs()) {
                    		PrintUtil.e(TAG, "unzip failed to mk parent:" + p);
                    	}
                    }
                    fos = new FileOutputStream(f);
                    int len = 0;
                    byte bufer[] = new byte[BUFFERSIZE];
                    while (-1 != (len = is.read(bufer))) {
                        fos.write(bufer, 0, len);
                    }
                    fos.close();
                }
            }
            return true;
        } catch (ZipException e) {
            PrintUtil.e(TAG, "unzip ZipException: " + e, e);
            throw e;
        } catch (IOException e) {
            PrintUtil.e(TAG, "unzip IOException: " + e, e);
            throw e;
        } finally {
            try {
                if(null != is) {
                    is.close();
                }if(null != fos) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 直接读取已存储在硬盘上的zip文件内容(文办文件)，这种方式可以直接读取zip文件，当然也可以先解压再读取解压后的文件。
     * @param filePath
     * @return
     * @throws Exception
     */
    public static String readZipFileText(String filePath) throws Exception {
    	StringBuffer buff = new StringBuffer();
        ZipFile zf = null;
        InputStream in = null;
        ZipInputStream zin = null;
        ZipEntry ze;
        try {
        	zf = new ZipFile(filePath);
            in = new BufferedInputStream(new FileInputStream(filePath));
            zin = new ZipInputStream(in);
            
            while ((ze = zin.getNextEntry()) != null) {
                long size = ze.getSize();
                if (size > 0) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(zf.getInputStream(ze)));
                    String line;
                    while ((line = br.readLine()) != null) {
                        //System.out.println(line);
                    	buff.append(line).append('\n');
                    }
                    br.close();
                }
                //System.out.println();
            }
            buff.deleteCharAt(buff.length() - 1);//去掉最后一个换行符
            return buff.toString();
        } catch (Exception e) {
        	PrintUtil.e(TAG, "unzip Exception: " + e, e);
        	throw e;
        } finally {
        	if (zin != null) {
        		try {
        			zin.closeEntry();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
        	}
        	if (in != null) {
        		try {
        			in.close();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
        	}
        }
    }
    
    /**
     * 利用ZipInputStream流对象直接读取zip文件的各个文件内容，
     * 通过getNextEntry用来设置文件的读取位置，第一次get让文件操作指针跳转到第一个文件的读取入口，
     * 第二次get跳转到第二个文件的读取入口，以此类推。在实际项目进行zip读取操作时，
     * 如果只能获得压缩文件的流，可以通过这种方式读取压缩文件，否则就要绕远路了。
     * 这里要注意一下，文件名如果包含中文会抛异常
     * @param zipInputStream  zip文件输入流
     * @return
     * @throws IOException
     */
    public static String readZipFileText(ZipInputStream zipInputStream) throws IOException {
        StringBuffer buff = new StringBuffer();
        //不能捕获，如果异常直接抛出去，便于查错误
        while((zipInputStream.getNextEntry()) != null){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(zipInputStream));
            String line;
            while((line = bufferedReader.readLine())!= null){
                //System.out.println(line);
                buff.append(line).append("\n");
            }
        }
    	buff.deleteCharAt(buff.length() - 1);//去掉最后一个换行符
    	return buff.toString();
    }
    
    /**
     * 利用ZipInputStream流对象直接读取zip文件的各个文件内容数据
     * @param zipInputStream  zip文件输入流
     * @return
     * @throws IOException
     */
    public static byte[] readZipFileData(ZipInputStream zipInputStream) throws IOException {
    	ByteArrayOutputStream bOutput = new ByteArrayOutputStream(zipInputStream.available());
		//不能捕获，如果异常直接抛出去，便于查错误
        while((zipInputStream.getNextEntry()) != null){
            byte[] buff = new byte[BUFFERSIZE];
            int read = 0;
            while((read = zipInputStream.read(buff)) != -1) {
            	bOutput.write(buff, 0, read);
            }
        }
    	return bOutput.toByteArray();
    }
    
    /**
     * 读取文件内容数据
     * @param filePath  文件路径
     * @return  byte[] data of file.
     * @throws IOException
     */
    public static byte[] readFileData(String filePath) throws IOException {
    	FileInputStream fis = null;
    	ByteArrayOutputStream bOutput = null;
    	try {
    		fis = new FileInputStream(filePath);
    		bOutput = new ByteArrayOutputStream(fis.available());
    		int readed = 0;
    		byte[] buff = new byte[BUFFERSIZE];
    		while ((readed = fis.read(buff)) != -1) {
    			bOutput.write(buff, 0, readed);
    		}
    		bOutput.toByteArray();
    	} catch (IOException e) {
    		PrintUtil.e(TAG, "readZipFileData Exception: " + e, e);
    		throw e;
    	} finally {
    		if (fis != null) {
    			try {
    				fis.close();
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    		if (bOutput != null) {
    			try {
    				bOutput.close();
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    	}
    	return null;
    }
    
    /**
     * PC机上Java测试用的main函数
     * @param args
     */
    public static void main(String[] args) {
        try {
            String[] files = new String[] {"D:/A/feng/feng/src/com/feng/util/ZipUtil.java", "D:/A/feng/feng/src/com/feng/test"};
            zip(files,"E:/test/test.zip");
            unzip("E:/test/test.zip", "E:/test/");
        } catch (Exception e) {
            PrintUtil.e(TAG, "main error: " + e, e);
        }
    }
}
