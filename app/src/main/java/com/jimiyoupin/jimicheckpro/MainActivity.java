package com.jimiyoupin.jimicheckpro;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public final static String APP_NAME = "Jimi Check Pro";
    Button bleTestButton,
            btnProduceUpdate,
            btnProduceCrossUpdate;
    TextView tv_packageVersion;
    private String latestVersion = "0.0.0",latestVersionDescription="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 加载布局
        setContentView(R.layout.activity_main);



        bleTestButton = findViewById(R.id.button3);
        btnProduceUpdate = findViewById(R.id.btn_produceUpdate);
        btnProduceCrossUpdate = findViewById(R.id.btn_produceCrossUpdate);

        bleTestButton.setOnClickListener(this);
        btnProduceUpdate.setOnClickListener(this);
        btnProduceCrossUpdate.setOnClickListener(this);

        tv_packageVersion = findViewById(R.id.tv_packageVersion);

//        getPackageVersion();
    }

    private void getPackageVersion(){
        String version = MyTools.getPackageVersion(this);
        if(null!=version){
            tv_packageVersion.setText("版本:"+version);

            //@todo 检测是否需要升级
            checkAppLatestVersion("1.0.1");
        }else{
            tv_packageVersion.setText("不能获取当前版本");
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){
            case R.id.button3:
                intent = new Intent(MainActivity.this, BleTestActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_produceUpdate:
                intent = new Intent(MainActivity.this, ProduceUpdateIndexActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_produceCrossUpdate:
                intent = new Intent(MainActivity.this, ProductAcrossActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void checkAppLatestVersion(String currentVersion){
        new Thread(){
            @Override
            public void run() {
                String checkAppVerResponseStr =new String( MyTools.doPost("https://update.jimiyoupin.com/api/app/check_version", "app_name="+APP_NAME+"&platform=Android&version_num=" + currentVersion + "&language=zh-cn"));
                System.out.println(checkAppVerResponseStr);
                try {
                    JSONObject response = new JSONObject(checkAppVerResponseStr);
                    int responseCode = response.getInt("code");
                    if(0==responseCode){
                        JSONObject responseData = response.getJSONObject("data");
                        if(responseData.getBoolean("need_update")){
                            latestVersion = responseData.getString("version_num");
                            latestVersionDescription = responseData.getString("description");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setTitle("发现版本").setMessage("版本号："+latestVersion+"\r\n"+latestVersionDescription)
                                    .setPositiveButton("升级", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            downloadApp(latestVersion);
                                        }
                                    }).setNegativeButton("以后再说", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    });
                                    AlertDialog alertDialog = builder.create();
                                    alertDialog.show();
                                }
                            });
                        }
                    }else{
                        Toast.makeText(MainActivity.this,"检测APP版本错误", Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void downloadApp(String latestVersion){
        System.out.println("下载app:"+latestVersion);
        new Thread(){
            @Override
            public void run() {
                String updateFileSourcePath = "https://update.jimiyoupin.com/api/app/download_update_file/app_name/"+APP_NAME+"/platform/Android/version_num/"+latestVersion;
                String fileName = MyTools.downloadFile(updateFileSourcePath,getFilesDir().getPath());
                System.out.println("下载完毕>>> "+fileName);
                installApk(fileName);

//                MultiDownload multiDownload = new MultiDownload(updateFileSourcePath,getFilesDir().getPath(),3);
//                multiDownload.start(new MultiDownload.OnDownloadComplete() {
//                    @Override
//                    public void downloadComplete(String filePath) {
//                        System.out.println("处理："+filePath);
//                        installApk(filePath);
//                    }
//                });

            }
        }.start();
    }

    private void installApk(String fileName){
        File apk = new File(fileName);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(this, getPackageName()+".fileprovider", apk);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        }else{
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://"+fileName), "application/vnd.android.package-archive");
        }
        startActivity(intent);
    }



}