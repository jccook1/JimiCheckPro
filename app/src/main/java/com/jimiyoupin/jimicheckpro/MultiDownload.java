package com.jimiyoupin.jimicheckpro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class MultiDownload {
    static String sourcePath;
    static String destinationPath;
    static String fileShortName;
    static int threadCount;

    static OnDownloadComplete onDownloadComplete;

    public MultiDownload(String sourcePath, String destinationPath, int threadCount){
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
        this.threadCount = threadCount;
    }

    public void start(OnDownloadComplete onDownloadComplete){
        this.onDownloadComplete = onDownloadComplete;
        try {
            System.out.println(sourcePath);
            URL url = new URL(sourcePath);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setConnectTimeout(5000);
            int code = httpURLConnection.getResponseCode();
            if(200==code){
                // [1]获取服务器文件大小
                int fileLength = httpURLConnection.getContentLength();
                System.out.println("fileLength:"+fileLength);
                if(-1==fileLength){
                    System.out.println("不支持多线程下载");
                }else{
                    fileShortName = sourcePath.substring(sourcePath.lastIndexOf('/')+1);
                    System.out.println("fileShortName:"+fileShortName);

                    // [2]在客户端创建一个大小和服务器文件一模一样的文件，提前申请好空间
                    RandomAccessFile randomAccessFile = new RandomAccessFile(destinationPath+"/"+fileShortName, "rw");
                    randomAccessFile.setLength(fileLength);

                    // 每个线程下载的大小
                    int blockSize = fileLength/threadCount;

                    // 计算出每个线程的开始和结束位置
                    for (int i = 0; i < threadCount; i++) {
                        int startIndex = i*blockSize;
                        int endIndex = (i+1)*blockSize-1;
                        //最后一个线程 的结束位置
                        if(i==threadCount-1){
                            endIndex = fileLength-1;
                        }
                        // 开始下载文件
                        new DownloadThread(startIndex,endIndex, i).start();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class DownloadThread extends Thread{
        private int startIndex;
        private int endIndex;
        private int threadId;
        private static int runningThreadCount=0;

        DownloadThread(int startIndex,int endIndex, int threadId){
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            runningThreadCount++;
            try {
//                System.out.println("线程开始:"+threadId+"  "+startIndex+"-"+endIndex);
                // 如果中间断开过，继续上次的位置下载
                File file = new File(destinationPath+"/thread_position_"+threadId);
                if(file.exists()&& file.length()>0){
                    FileInputStream fis = new FileInputStream(file);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                    int lastPosition = Integer.parseInt(br.readLine()) ;
                    System.out.println("线程id:"+threadId+",下载位置:"+lastPosition);
                    startIndex  = lastPosition;
                    fis.close();
                }

                URL url = new URL(sourcePath);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setConnectTimeout(5000);
                // 设置一个请求头Range（作用是通知服务器该线程下载的开始和结束位置）
                String range = "bytes="+startIndex+"-"+endIndex;
                httpURLConnection.setRequestProperty("Range", range);

                int code = httpURLConnection.getResponseCode();
                switch (code){
                    // 206表示请求部分资源成功， 200表示获取服务器全部数据成功
                    case HttpURLConnection.HTTP_PARTIAL:
                        // 创建随即读写文件对象
                        RandomAccessFile randomAccessFile = new RandomAccessFile(destinationPath+"/"+fileShortName, "rw");
                        // 每个线程从自己的开始位置写
                        randomAccessFile.seek(startIndex);
                        InputStream inputStream = httpURLConnection.getInputStream();
                        // 写入文件
                        int len = -1;
                        byte[] buffer = new byte[1024*1024];
                        int total = 0;
                        while (-1!=(len=inputStream.read(buffer))){
                            randomAccessFile.write(buffer,0,len);

                            total+=len;
                            int currentThreadPositon = startIndex + total;

                            // 存储当前线程下载的位置
                            RandomAccessFile raf = new RandomAccessFile(destinationPath+"/thread_position_"+threadId, "rwd");
                            raf.write(String.valueOf(currentThreadPositon).getBytes());
                            raf.close();
                        }
                        randomAccessFile.close();
                        System.out.println("线程id:"+threadId+"---下载完毕");

                        synchronized (DownloadThread.class){
                            runningThreadCount--;
                            if(runningThreadCount==0){
                                for (int i = 0; i < threadCount; i++) {
                                    File delFile = new File(destinationPath+"/thread_position_"+i);
                                    delFile.delete();
                                    System.out.println("删除断点记录文件 "+destinationPath+"/thread_position_"+i);
                                }
                                onDownloadComplete.downloadComplete(destinationPath+"/"+fileShortName);
                            }
                        }
                        break;

                    case HttpURLConnection.HTTP_OK:
                        System.out.println("线程id:"+threadId+"---不支持多线程");
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnDownloadComplete{
        void downloadComplete(String filePath);
    }
}
