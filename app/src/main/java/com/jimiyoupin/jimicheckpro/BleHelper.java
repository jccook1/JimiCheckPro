package com.jimiyoupin.jimicheckpro;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class BleHelper {
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothAdapter.LeScanCallback scanCallback;
    private BluetoothGattCallback bluetoothGattCallback;
    private Handler scanHandler,connectHandler,getServicesHandler,notificationHandler;
    private Pattern namePattern;
    private HashMap<String, BluetoothDevice> scanedDevices = new HashMap<String,BluetoothDevice>();
    private BluetoothGatt bluetoothGatt;

    public class MsgType{
        public static final int SCAN_STATE = 1;
        public static final int SCAN_RESULT = 2;
    }

    public static final int STATE_SCAN_STOP = 0;
    public static final int STATE_SCANNING = 1;



    public BleHelper(Context context){
        this.context = context;
    }
    /**
     * 初始化
     * @return
     */
    public BluetoothAdapter initBluetoothAdapter(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // 获取蓝牙适配器
            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if(bluetoothAdapter == null){
                bluetoothAdapter = bluetoothManager.getAdapter();
            }

            if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions((AppCompatActivity)context, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }

            // 启动蓝牙
            if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                ((AppCompatActivity)context).startActivityForResult(enableBtIntent,1);
                System.out.println("启动蓝牙适配器");
            }else{
                System.out.println("蓝牙适配器已启动");
            }

            // 设置扫描回调函数
                scanCallback = new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        if(device.getName()!=null && namePattern.matcher(device.getName()).matches()&&rssi>=-80&&"A4:C1:38:0B:04:DB".equals(device.getAddress())){
                            String address = device.getAddress();
                            System.out.println("$$"+device.getName()+" "+address+ " "+ rssi);
                            scanedDevices.put(address, device);

                            Message msg = Message.obtain();
                            msg.what = MsgType.SCAN_RESULT;

                            HashMap<String,Object> scanedData = new HashMap<>();
                            scanedData.put("device", device);
                            scanedData.put("rssi", rssi);
                            scanedData.put("scanRecord", scanRecord);
                            msg.obj = scanedData;

                            scanHandler.sendMessage(msg);
                        }
                    }
                };

            bluetoothGattCallback = new BluetoothGattCallback() {
                @Override
                public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                    super.onPhyUpdate(gatt, txPhy, rxPhy, status);
                }

                @Override
                public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                    super.onPhyRead(gatt, txPhy, rxPhy, status);
                }

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    Message msg = Message.obtain();
                    HashMap<String,Integer> hashMap = new HashMap<>();
                    hashMap.put("status", status);
                    hashMap.put("connectionState", newState);
                    msg.obj = hashMap;

                    connectHandler.sendMessage(msg);
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if(status == BluetoothGatt.GATT_SUCCESS){
//                        System.out.println("获取服务成功");
                        Message msg = Message.obtain();
                        HashMap<String,Object> hashMap = new HashMap<>();
                        ArrayList<BluetoothGattService> bluetoothGattServices = (ArrayList<BluetoothGattService>) gatt.getServices();
                        hashMap.put("status", status);
                        hashMap.put("services", bluetoothGattServices);
                        msg.obj = hashMap;

                        getServicesHandler.sendMessage(msg);

                    }else{
                        System.out.println("获取服务成功");
                    }

                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    System.out.println("notify ");
                    Message msg = Message.obtain();
                    msg.obj = characteristic;
                    notificationHandler.sendMessage(msg);
                }

                @Override
                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorRead(gatt, descriptor, status);
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorWrite(gatt, descriptor, status);
                }

                @Override
                public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                    super.onReliableWriteCompleted(gatt, status);
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    super.onReadRemoteRssi(gatt, rssi, status);
                }

                @Override
                public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                    super.onMtuChanged(gatt, mtu, status);
                }
            };

        }
        return bluetoothAdapter;
    }

    public void startScan(Handler scanHandler){
        startScan(15000,scanHandler);
    }

    public void startScan(long delayMillis, Handler scanHandler){
        this.scanHandler = scanHandler;
        scanedDevices.clear();
        new Thread(){
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Message msg = Message.obtain();
                    msg.what = MsgType.SCAN_STATE;
                    msg.obj = STATE_SCANNING;
                    BleHelper.this.scanHandler.sendMessage(msg);

                    bluetoothAdapter.startLeScan(scanCallback);
                    BleHelper.this.scanHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = Message.obtain();
                            msg.what = MsgType.SCAN_STATE;
                            msg.obj = STATE_SCAN_STOP;
                            BleHelper.this.scanHandler.sendMessage(msg);
                            bluetoothAdapter.stopLeScan(scanCallback);
                        }
                    }, delayMillis);
                }
            }
        }.start();
    }

    public void startScan(Pattern nameFilter, long delayMillis,Handler scanHandler){
        this.namePattern = nameFilter;
        startScan(delayMillis,scanHandler);
    }

    public void stopScan(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bluetoothAdapter.stopLeScan(scanCallback);
        }
    }

    public HashMap<String,BluetoothDevice> getScanedDevices() {
        return scanedDevices;
    }

    public void connect(BluetoothDevice bluetoothDevice, Handler connectHandler){
        this.connectHandler = connectHandler;
        new Thread(){
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    bluetoothGatt = bluetoothDevice.connectGatt(context, false, bluetoothGattCallback);
                }
            }
        }.start();
    }

    public void disconnect(){
        new Thread(){
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    bluetoothGatt.disconnect();
                }
            }
        }.start();

    }

    public void getServices(Handler getServicesHandler){
        this.getServicesHandler = getServicesHandler;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bluetoothGatt.discoverServices();
        }
    }

    public void setNotification(BluetoothGattCharacteristic characteristic,Boolean enable, Handler notificationHandler){
        this.notificationHandler = notificationHandler;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bluetoothGatt.setCharacteristicNotification(characteristic, enable);
        }
    }

    public void write(BluetoothGattCharacteristic characteristic, byte[] value){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            System.out.println("写入~~~~~~~~~~");
            characteristic.setValue(value);
            bluetoothGatt.writeCharacteristic(characteristic);
        }

    }

}
