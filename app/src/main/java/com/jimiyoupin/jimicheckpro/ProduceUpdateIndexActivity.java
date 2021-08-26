package com.jimiyoupin.jimicheckpro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ProduceUpdateIndexActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_produce_update_index);

        // 设置标题
        setTitle("生产升级");

        // 显示返回按钮
        final ActionBar supportActionBar = getSupportActionBar();
        if(supportActionBar != null){
            supportActionBar.setHomeButtonEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);


        }

    }

    public void gotoActivity(View v){
        Intent intent;
        switch (v.getId()){
            case R.id.btn_updateVsitooR1:
                intent = new Intent(this, VsitooR1UpdateActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_updateVsitooR2:
                intent = new Intent(this, VsitooR2UpdateActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_updateVsitooS2:
                intent = new Intent(this, VsitooS2UpdateActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_updateVsitooS3Pro:
                intent = new Intent(this, VsitooS3ProActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_updateVsitooS6:
                intent = new Intent(this, VsitooS6UpdateActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_updateVsitooT1Pro:
                intent = new Intent(this, VsitooT1ProActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_updateVsitooV1:
                intent = new Intent(this, VsitooV1UpdateActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_updateVanowV1:
                intent = new Intent(this, VanowV1UpdateActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_updateVanowV2:
                intent = new Intent(this, VanowV2UpdateActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_updateIPorketM1:
                intent = new Intent(this, IPorketM1Activity.class);
                startActivity(intent);
                break;
        }
    }

}