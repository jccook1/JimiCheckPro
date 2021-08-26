package com.jimiyoupin.jimicheckpro;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Ble {
    private Context _context;
    private BluetoothAdapter _bluetoothAdapter;
    private BluetoothLeScanner _bluetoothLeScanner;

    private boolean _isScanning = false;

    private ScanCallback _scanCallback;

    public BluetoothGatt bluetoothGatt;

    public HashMap<String, BleService> bluetoothGattServiceHashMap = new HashMap<>();

    public Ble(Context context){
        _context = context;
    }

    /**
     * 检测是否支持 BLE
     */
    public boolean checkBluetoothLEAccess(){
        if(_context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
//            Toast.makeText(context, "支持BLE", Toast.LENGTH_SHORT).show();
            return true;
        }else{
            Toast.makeText(_context, "不支持BLE", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * 手动获取权限
     */
    public boolean getPermissions(){
        if(ActivityCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions((Activity) _context, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return false;
        }
        if(ActivityCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions((Activity) _context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return false;
        }
        return true;
    }

    public boolean isEnabled(){
        // 获取蓝牙适配器
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            final BluetoothManager bluetoothManager = (BluetoothManager) _context.getSystemService(Context.BLUETOOTH_SERVICE);
            if(_bluetoothAdapter ==null){
                _bluetoothAdapter = bluetoothManager.getAdapter();
            }
            return _bluetoothAdapter.isEnabled();
        }
        return false;
    }

    /**
     * 开启蓝牙适配器
     */
    public boolean enableBluetoothAdapter(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // 获取蓝牙适配器
            final BluetoothManager bluetoothManager = (BluetoothManager) _context.getSystemService(Context.BLUETOOTH_SERVICE);
            if(_bluetoothAdapter ==null){
                _bluetoothAdapter = bluetoothManager.getAdapter();
            }

            if(!_bluetoothAdapter.isEnabled()){
                Intent enabledBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                ((AppCompatActivity) _context).startActivityForResult(enabledBtIntent, 1);
                return false;
            }
        }
        return true;
    }

    /**
     * 关闭蓝牙适配器
     */
    public void disableBluetoothAdapter(){
        if(null!= _bluetoothAdapter) _bluetoothAdapter.disable();
    }


    /**
     * 开始扫描
     * 扫描占用大量资源，需要及时关闭
     * 需要手动关闭
     */
    public boolean startScan(ScanCallback scanCallback, boolean firstMath){
        this._scanCallback = scanCallback;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(_bluetoothLeScanner == null){
                _bluetoothLeScanner = _bluetoothAdapter.getBluetoothLeScanner();
            }

            ArrayList<ScanFilter> scanFilters = new ArrayList<>();
//            ScanFilter scanFilter = new ScanFilter.Builder().setDeviceName("VSITOO R1").build();
//            scanFilters.add(scanFilter);

            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(firstMath){
                    scanSettingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
                }else{
                    scanSettingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
                }
            }
            ScanSettings scanSettings =    scanSettingsBuilder.build();
            _bluetoothLeScanner.startScan(scanFilters,scanSettings,scanCallback);
//            _bluetoothLeScanner.startScan(scanCallback);
            _isScanning = true;
            return true;
        }else return false;
    }

    /**
     * 关闭扫描
     */
    public void stopScan(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(_bluetoothLeScanner !=null){
                _bluetoothLeScanner.stopScan(_scanCallback);
                System.out.println("停止扫描");
                _isScanning = false;
            }
        }
    }

    /**
     * 获取扫描状态
     * @return
     */
    public boolean isScanning(){
        return _isScanning;
    }

    /**
     * 连接
     * @param bluetoothDevice
     * @param bluetoothGattCallback
     */
    public void connect(BluetoothDevice bluetoothDevice, BluetoothGattCallback bluetoothGattCallback){
        bluetoothGatt = bluetoothDevice.connectGatt(_context,false,bluetoothGattCallback);
    }

    /**
     * 连接
     * @param address
     * @param bluetoothGattCallback
     */
    public void connect(String address, BluetoothGattCallback bluetoothGattCallback){
        bluetoothGatt = _bluetoothAdapter.getRemoteDevice(address).connectGatt(_context,false,bluetoothGattCallback);
        System.out.println("广播名："+ bluetoothGatt.getDevice().getName());
    }

    /**
     * 断开连接
     */
    public void disconnect(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(bluetoothGatt !=null){
                System.out.println("正在断开连接...");
                bluetoothGatt.disconnect();
                // 关闭GATT客户端， 否则会出现重复连接
                bluetoothGatt.close();
                bluetoothGatt = null;
            }else{
                System.out.println("设备没有被连接");
            }
        }
    }

    /**
     * 查找服务
     */
    public void getServices(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(bluetoothGatt !=null){
                bluetoothGattServiceHashMap.clear();
                bluetoothGatt.discoverServices();
            }else{
                System.out.println("设备没有被连接");
            }
        }
    }

    /**
     * 做一个类存放特征数据
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static class BleService{
        public BluetoothGattService bluetoothGattService;
        public HashMap<String, BluetoothGattCharacteristic> bluetoothGattCharacteristicHashMap = new HashMap<>();

        BleService(BluetoothGattService service){
            bluetoothGattService = service;
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for(BluetoothGattCharacteristic characteristic:characteristics){
                System.out.println(characteristic.getUuid().toString());
                bluetoothGattCharacteristicHashMap.put(characteristic.getUuid().toString(), characteristic);
            }
        }
    }

    public void addService(BluetoothGattService service){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bluetoothGattServiceHashMap.put(service.getUuid().toString(), new BleService(service));
        }
    }

    /**
     * 根据服务和特征字符串获取特征
     * @param serviceUuid
     * @param characteristicUuid
     * @return
     */
    public BluetoothGattCharacteristic  getBluetoothGattCharacteristic(String serviceUuid, String characteristicUuid){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return bluetoothGattServiceHashMap.get(serviceUuid).bluetoothGattCharacteristicHashMap.get(characteristicUuid);
        }else{
            return null;
        }
    }

    /**
     * 设置监听
     * @param serviceUuid
     * @param characteristicUuid
     * @param enable
     * @return
     */
    public boolean setNotification(String serviceUuid, String characteristicUuid, boolean enable){
        BluetoothGattCharacteristic characteristic = getBluetoothGattCharacteristic(serviceUuid, characteristicUuid);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(bluetoothGatt !=null){
                if(bluetoothGatt.setCharacteristicNotification(characteristic, enable)){
                    System.out.println("设置监听 ok");
                    return true;
                }else{
                    System.out.println("设置监听 err");
                }
            }else{
                System.out.println("设备没有被连接");
            }
        }
        return false;
    }

    /**
     * 写入数据
     */
    public boolean writeValue(String serviceUuid, String characteristicUuid, byte[] value){
        BluetoothGattCharacteristic characteristic = getBluetoothGattCharacteristic(serviceUuid, characteristicUuid);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(bluetoothGatt !=null){
                characteristic.setValue(value);
                return bluetoothGatt.writeCharacteristic(characteristic);
            }else{
                System.out.println("设备没有被连接");
            }
        }
        return false;
    }

}
