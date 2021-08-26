package com.jimiyoupin.jimicheckpro;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class ScanResultAdapter extends BaseAdapter {
    public Context context;
    public ArrayList<ScanResult> scanResults;
    public String latestVersion = "0.0.0";
    public ScanResultAdapter(Context context, ArrayList<ScanResult> scanResults){
        this.context = context;
        this.scanResults = scanResults;
    }
    public ScanResultAdapter(Context context, ArrayList<ScanResult> scanResults, String latestVersion){
        this.context = context;
        this.scanResults = scanResults;
        this.latestVersion = latestVersion;
    }
    @Override
    public int getCount() {
        return scanResults.size();
    }

    @Override
    public Object getItem(int position) {
        return scanResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if(null==convertView){
            view  = View.inflate(context, R.layout.list_item_scan_result, null);
        }else{
            view = convertView;
        }

        BluetoothDevice bluetoothDevice = scanResults.get(position).getDevice();
        byte[] scanRecordData = scanResults.get(position).getScanRecord().getBytes();
        String currentVersion = scanRecordData[24]+"."+scanRecordData[25]+"."+scanRecordData[26];
        boolean needUpdate = MyTools.compareVersion(latestVersion, currentVersion)>0;

        TextView tv_deviceName = view.findViewById(R.id.tv_deviceName);
        tv_deviceName.setText(bluetoothDevice.getName());


        TextView tv_deviceInfo =  view.findViewById(R.id.tv_deviceInfo);
        tv_deviceInfo.setText(bluetoothDevice.getAddress()+"["+currentVersion+"]");

        TextView tv_rssi =  view.findViewById(R.id.tv_rssi);
        tv_rssi.setText(scanResults.get(position).getRssi()+"");

        if(needUpdate){
            tv_deviceName.setTextColor(Color.BLACK);
            tv_deviceInfo.setTextColor(Color.BLACK);
            tv_rssi.setTextColor(Color.BLACK);
        }else{
            tv_deviceName.setTextColor(Color.LTGRAY);
            tv_deviceInfo.setTextColor(Color.LTGRAY);
            tv_rssi.setTextColor(Color.LTGRAY);
        }

        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean add(ScanResult scanResult){
        for(int i=0, len = scanResults.size(); i<len; i++){
            if(scanResult.getDevice().getAddress().equals(scanResults.get(i).getDevice().getAddress()) ){
                scanResults.remove(i);
                scanResults.add(i, scanResult);
                return false;
            }
        }
        scanResults.add(scanResult);
        return true;
    }
}
