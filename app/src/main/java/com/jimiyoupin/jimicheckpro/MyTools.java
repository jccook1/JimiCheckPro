package com.jimiyoupin.jimicheckpro;

import android.content.Context;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具类
 */
public class MyTools {
    /**
     * 获取时间字符串
     * @param format 参考 SimpleDateFormat
     * @param time
     * @return
     */
    public static String getDateTime(String format, long time){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        Date date = new Date(time);
        return simpleDateFormat.format(date);
    }

    /**
     * 获取时间字符串
     * @param format 参考 SimpleDateFormat
     * @return
     */
    public static String getDateTime(String format){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(new Date());
    }

    /**
     * 把一个2位的16进制字符串转为byte类型
     * @param hex 只能是2位的16进制字符串
     * @return
     */
    public static byte hexToByte(String hex){
        return (byte) ((Character.digit(hex.charAt(0), 16) << 4) + Character.digit(hex.charAt(1), 16));
    }

    /**
     * 把 byte 类型转为一个2位的16进制字符串
     * @param b
     * @return
     */
    public static String byteToHex(byte b){
        int x = b&0xFF;
        String hex = Integer.toHexString(x);
        if(x<16) hex = "0"+hex;
        return hex;
    }

    /**
     * 把一个16进制字符串转为byte数组类型
     * @param hex
     * @return
     */
    public static byte[] hexToByteArray(String hex){
        if(hex.length()%2==1) return null;
        else{
            byte[] bytes = new byte[hex.length()/2];
            for(int i=0;i<hex.length()/2;i++){
//                System.out.println(hex.substring(i*2,i*2+2));
                bytes[i] = hexToByte(hex.substring(i*2,i*2+2));
            }
            return bytes;
        }
    }

    /**
     * 把 byte 数组转为一个16进制字符串
     * @param bytes
     * @return
     */
    public static String byteArrayToHex(byte[] bytes){
        String s = "";
        for(int i=0; i<bytes.length; i++){
            s += byteToHex(bytes[i]);
        }
        return s;
    }

    /**
     * 版本比较
     * @param version1
     * @param version2
     * @return 如果 version1大于version2,返回1；如果version1小于version2,返回-1；如果version1等于version2,返回 0。
     */
    public static byte compareVersion(String version1, String version2){
        String[] ver1 = version1.split("\\.");
        String[] ver2 = version2.split("\\.");
        int maxLen = ver1.length>ver2.length?ver1.length:ver2.length;
        for (int i=0;i<maxLen;i++){
            int num1 = 0, num2=0;
            if(ver1.length>i){
                num1 = Integer.valueOf(ver1[i]);
            }
            if(ver2.length>i){
                num2 = Integer.valueOf(ver2[i]);
            }
            if(num1>num2){
                return 1;
            }else if(num1<num2){
                return -1;
            }
        }
        return 0;
    }

    /**
     * 获取APP版本号
     * 注意：Android Studio 在 build.gradle 里修改 版本信息
     * @param context
     * @return
     */
    public static String getPackageVersion(Context context){
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 读取流
     * @param inputStream
     * @return
     */
    public static byte[] readStream(InputStream inputStream){
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[]buffer = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(buffer))!=-1){
                outputStream.write(buffer,0,len);
            }
            inputStream.close();
            outputStream.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * post请求
     * @param urlStr
     * @param dataStr
     * @return
     */
    public static byte[] doPost(String urlStr, String dataStr){
//        System.out.println("发出post请求："+urlStr);
//        System.out.println("post参数："+dataStr);
        byte[] response = null;
        try {
            // 1. 请求体的内容  "model_name="+MODEL_NAME+"&version_num=0.0.0";
            String data = dataStr;
            // 2.1. 创建一个url对象，参数是网址
            URL url = new URL(urlStr);
            // 2.2 获取HttpURLConnection连接对象
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            // 3.1 设置参数
            // POST请求要大写
            httpURLConnection.setRequestMethod("POST");
            // 3.2 设置连接网络的超时时间
            httpURLConnection.setConnectTimeout(5000);
            // 3.3 和 GET 方式提交数据区别， 要多设置2个请求头信息
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpURLConnection.setRequestProperty("Content-Length", data.length()+"");
            // 4 以流的形式提交数据
            // 4.1 设置一个标记，允许输出
            httpURLConnection.setDoOutput(true);
            httpURLConnection.getOutputStream().write(data.getBytes());
            // 5. 获取服务器的状态码
            int code = httpURLConnection.getResponseCode();
            if(code == 200 ){
                // 6.1 获取服务器返回的数据， 以流的形式返回
                InputStream inputStream = httpURLConnection.getInputStream();
                // 6.2 把InputStream 转换成 byte[]
                response = readStream(inputStream);
                inputStream.close();
                httpURLConnection.disconnect();
            }else{
                System.out.println("POST请求异常:"+code);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * get请求
     * @param urlStr
     * @return
     */
    public static byte[] doGet(String urlStr){
        byte[] response = null;
        try {
            // 1. 创建一个url对象，参数是网址
            URL url = new URL(urlStr);
            // 2. 获取HttpURLConnection连接对象
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            // 3. 设置参数
            // 默认是GET请求， 要大写
            httpURLConnection.setRequestMethod("GET");
            // 4. 设置连接网络的超时时间
            httpURLConnection.setConnectTimeout(5000);
            // 5. 获取服务器的状态码
            int code = httpURLConnection.getResponseCode();
            if(code == 200 ){
                // 6. 获取服务器返回的数据， 以流的形式返回
                InputStream inputStream = httpURLConnection.getInputStream();
                // 6.1 把InputStream 转换成 byte[]
                response = readStream(inputStream);
                inputStream.close();
                httpURLConnection.disconnect();
            }else{
                System.out.println("GET请求异常:"+code);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 匹配字符串
     * @param reg 正则表达式
     * @param str 要匹配的字符串
     * @return
     */
    public static boolean match(String reg, String str){
        if(null == str) return false;
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    /**
     * 下载文件
     * @param sourcePath
     * @param destinationPath
     * @return 返回目标文件路径
     */
    public static String downloadFile(String sourcePath, String destinationPath){
        try {
            // 1. 创建一个url对象，参数是网址
            URL url = new URL(sourcePath);
            // 2. 获取HttpURLConnection连接对象
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            // 3. 设置参数
            // 默认是GET请求， 要大写
            httpURLConnection.setRequestMethod("GET");
            // 4. 设置连接网络的超时时间
            httpURLConnection.setConnectTimeout(5000);
            // 5. 获取服务器的状态码
            int code = httpURLConnection.getResponseCode();
            String content = httpURLConnection.getHeaderField("Content-Disposition");
            String fileName = destinationPath + "/" + content.substring(content.lastIndexOf('=')+1);
            System.out.println(fileName);
            if(code == 200 ){
                // 6. 获取服务器返回的数据， 以流的形式返回
                InputStream inputStream = httpURLConnection.getInputStream();
                // 6.1 把InputStream 转换成 byte[]
                byte[] response = readStream(inputStream);
                // 7 写入文件
                FileOutputStream fos = new FileOutputStream(fileName);
                fos.write(response);
                fos.close();
                inputStream.close();
                return fileName;
            }else{
                System.out.println("GET请求异常:"+code);
            }
            httpURLConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
