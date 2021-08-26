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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.SparseArray;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BleTestActivity extends AppCompatActivity {
    private BluetoothDevice testBluetoothDevice;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback bluetoothGattCallback;

    public HashMap<String, BleService> bluetoothGattServiceHashMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_test);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            /**
             * 扫描回调
             */
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    System.out.println("发现设备...");
                    if(callbackType == ScanSettings.CALLBACK_TYPE_FIRST_MATCH){
                        BluetoothDevice bluetoothDevice = result.getDevice();
                        System.out.println("Address:"+bluetoothDevice.getAddress());
                        System.out.println("Name:"+bluetoothDevice.getName());
                        if( "A4:C1:38:0B:04:DB".equals(bluetoothDevice.getAddress())){
                            Toast.makeText(BleTestActivity.this, "获取到测试设备:"+bluetoothDevice.getAddress(), Toast.LENGTH_SHORT).show();
                            testBluetoothDevice = bluetoothDevice;
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            System.out.println("别名:"+bluetoothDevice.getAlias());
                        }
                        System.out.println("绑定状态:"+bluetoothDevice.getBondState());
                        System.out.println("蓝牙类型:"+bluetoothDevice.getType());
//                        ParcelUuid[] uuids = bluetoothDevice.getUuids();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            System.out.println("AdvertisingSid:"+result.getAdvertisingSid());
                            System.out.println("DataStatus:"+result.getDataStatus());
                            System.out.println("PeriodicAdvertisingInterval:"+result.getPeriodicAdvertisingInterval());
                            System.out.println("PrimaryPhy:"+result.getPrimaryPhy());
                            System.out.println("SecondaryPhy:"+result.getSecondaryPhy());
                            System.out.println("TxPower:"+result.getTxPower());
                        }
                        System.out.println("Rssi:"+result.getRssi());
                        System.out.println("TimestampNanos:"+result.getTimestampNanos());

                        ScanRecord scanRecord = result.getScanRecord();
                        System.out.println("DeviceName:"+scanRecord.getDeviceName());
                        System.out.println("AdvertiseFlags:"+scanRecord.getAdvertiseFlags());
                        System.out.print("Bytes:");
                        byte[] bytes = scanRecord.getBytes();
                        System.out.print(MyTools.byteArrayToHex(bytes));

//                        for (int i=0; i<bytes.length; i++){
//                            System.out.print(MyTools.byteToHex(bytes[i]));
//                            System.out.print(" ");
//                        }
                        System.out.println();
//                        System.out.println("ManufacturerSpecificData:"+scanRecord.getManufacturerSpecificData());
                        SparseArray<byte[]> manufacturerSpecificData = scanRecord.getManufacturerSpecificData();
                        int len = manufacturerSpecificData.size();
                        System.out.println(len);
                        for(int i=0; i<len; i++){
                            byte[] bytes1 = manufacturerSpecificData.valueAt(i);
                            if(bytes1!=null){
                                System.out.println(MyTools.byteArrayToHex(bytes1));
                            }
                        }

//                        System.out.println("ServiceData:");
//                        List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();

//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                            System.out.println(":"+scanRecord.getServiceSolicitationUuids());
//                        }
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    System.out.println("扫描失败:"+errorCode);
                    Toast.makeText(BleTestActivity.this, "扫描失败:"+errorCode, Toast.LENGTH_LONG).show();
                }
            };

            bluetoothGattCallback = new BluetoothGattCallback() {
                @Override
                public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                    System.out.println("物理层改变，状态码:"+status+"发射PHY:"+txPhy+"，接受PHY:"+rxPhy);
                }

                @Override
                public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                    System.out.println("读取物理层，状态码:"+status+"发射PHY:"+txPhy+"，接受PHY:"+rxPhy);
                }

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (BluetoothGatt.GATT_SUCCESS == status){
                        System.out.print(gatt.getDevice().getAddress()+"连接状态改变，值:"+newState+"，");
                        switch (newState){
                            case BluetoothGatt.STATE_DISCONNECTED:
                                System.out.print("水杯断开连接，");
                                break;

                            case BluetoothGatt.STATE_CONNECTING:
                                System.out.print("水杯连接中...，");
                                break;

                            case BluetoothGatt.STATE_CONNECTED:
                                System.out.print("水杯已连接，");
                                break;

                            case BluetoothGatt.STATE_DISCONNECTING:
                                System.out.print("水杯断开连接中...，");
                                break;
                        }
                    }
                    System.out.println("状态码:"+status);
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    System.out.println("发现服务，状态码:"+status);

                    List<BluetoothGattService> bluetoothGattServices = bluetoothGatt.getServices();
//                    System.out.println("获取服务..."+bluetoothGattServices.size());
                    for(BluetoothGattService service: bluetoothGattServices){
                        System.out.println("服务:"+service.getUuid().toString());
                        bluetoothGattServiceHashMap.put(service.getUuid().toString(), new BleService(service));
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    System.out.println("读取特征"+characteristic.getUuid().toString()+"，状态码:"+status+"，值："+ MyTools.byteArrayToHex(characteristic.getValue()));
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    System.out.println("写入特征"+characteristic.getUuid().toString()+"，状态码:"+status+"，值："+ MyTools.byteArrayToHex(characteristic.getValue()));
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    System.out.println("特征改变"+characteristic.getUuid().toString()+"，值："+ MyTools.byteArrayToHex(characteristic.getValue()));
                }

                @Override
                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    System.out.println("读取描述符："+descriptor.getUuid()+"，状态码:"+status+"，值："+ MyTools.byteArrayToHex(descriptor.getValue()));
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    System.out.println("写入描述符："+descriptor.getUuid()+"，状态码:"+status+"，值："+ MyTools.byteArrayToHex(descriptor.getValue()));
                }

                @Override
                public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                    System.out.println("可靠性写入完成"+gatt.getDevice().getAddress()+"，状态码:"+status);
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    System.out.println("读取rssi，状态码:"+status+"，值："+rssi);
                }

                @Override
                public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                    System.out.println("MTU改变，状态码:"+status+"，值："+mtu);
                }
            };
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("requestCode:"+requestCode);
    }

    /**
     * 检测是否支持 BLE
     * @param v
     */
    public void checkBluetoothAccess(View v){
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, "支持BLE", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 开启蓝牙适配器
     * @param v
     */
    public void enableBluetoothAdapter(View v){

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // 获取蓝牙适配器
            final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            if(bluetoothAdapter==null){
                bluetoothAdapter = bluetoothManager.getAdapter();
            }

            if(!bluetoothAdapter.isEnabled()){
                Intent enabledBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enabledBtIntent, 1);
            }
        }
    }

    /**
     * 关闭蓝牙适配器
     * @param v
     */
    public void disableBluetoothAdapter(View v){
        bluetoothAdapter.disable();
    }

    /**
     * 开始扫描
     * 扫描占用大量资源，需要及时关闭
     * 需要手动关闭
     * @param v
     */
    public void startScan(View v){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(bluetoothLeScanner == null){
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
            ScanFilter scanFilter = new ScanFilter.Builder().setDeviceName("VSITOO R1").build();
            ArrayList<ScanFilter> scanFilters = new ArrayList<>();
            scanFilters.add(scanFilter);

            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                scanSettingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
            }
            ScanSettings scanSettings =    scanSettingsBuilder.build();
            bluetoothLeScanner.startScan(scanFilters,scanSettings,scanCallback);
        }else{
            System.out.println("~~~~~~~~~~~ 获取 BluetoothLeScanner");
            Toast.makeText(this, "不能获取 BluetoothLeScanner", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 关闭扫描
     * @param v
     */
    public void stopScan(View v){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(bluetoothLeScanner!=null){
                bluetoothLeScanner.stopScan(scanCallback);
            }
        }
    }

    /**
     * 连接
     * @param v
     */
    public void connect(View v){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(bluetoothGatt!=null){
                bluetoothGatt.connect();
            }else{
                bluetoothGatt = testBluetoothDevice.connectGatt(this,true,bluetoothGattCallback);
            }

        }
    }

    /**
     * 断开连接
     * @param v
     */
    public void disconnect(View v){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(bluetoothGatt!=null){
                bluetoothGatt.disconnect();
            }else{
                System.out.println("设备没有被连接");
            }
        }
    }

    public void getServices(View v){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(bluetoothGatt!=null){
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
    private class BleService{
        private BluetoothGattService bluetoothGattService;
        public HashMap<String,BluetoothGattCharacteristic> bluetoothGattCharacteristicHashMap = new HashMap<>();

        BleService(BluetoothGattService service){
            bluetoothGattService = service;
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for(BluetoothGattCharacteristic characteristic:characteristics){
                bluetoothGattCharacteristicHashMap.put(characteristic.getUuid().toString(), characteristic);
            }
        }
    }

    /**
     * 根据服务和特征字符串获取特征
     * @param serviceUuid
     * @param characteristicUuid
     * @return
     */
    private BluetoothGattCharacteristic  getBluetoothGattCharacteristic(String serviceUuid, String characteristicUuid){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return bluetoothGattServiceHashMap.get(serviceUuid).bluetoothGattCharacteristicHashMap.get(characteristicUuid);
        }else{
            return null;
        }
    }

    /**
     * 设置监听
     * @param v
     */
    public void enableNotification(View v){
        BluetoothGattCharacteristic characteristic = getBluetoothGattCharacteristic("0000a300-0000-1000-8000-00805f9b34fb", "0000a303-0000-1000-8000-00805f9b34fb");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(bluetoothGatt!=null){
                if(bluetoothGatt.setCharacteristicNotification(characteristic, true)){
                    System.out.println("设置监听 ok");
                }else{
                    System.out.println("设置监听 err");
                }
            }else{
                System.out.println("设备没有被连接");
            }
        }
    }

    /**
     * 取消监听
     * @param v
     */
    public void disableNotification(View v){
        BluetoothGattCharacteristic characteristic = getBluetoothGattCharacteristic("0000a300-0000-1000-8000-00805f9b34fb", "0000a303-0000-1000-8000-00805f9b34fb");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(bluetoothGatt!=null){
                if(bluetoothGatt.setCharacteristicNotification(characteristic, false)){
                    System.out.println("取消监听 ok");
                }else{
                    System.out.println("取消监听 err");
                }
            }else{
                System.out.println("设备没有被连接");
            }
        }
    }

    /**
     * 写入数据
     * @param v
     */
    public void writeValue(View v){
        BluetoothGattCharacteristic characteristic = getBluetoothGattCharacteristic("0000a300-0000-1000-8000-00805f9b34fb", "0000a301-0000-1000-8000-00805f9b34fb");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(bluetoothGatt!=null){
                characteristic.setValue(new byte[]{0x07});
                bluetoothGatt.writeCharacteristic(characteristic);
            }else{
                System.out.println("设备没有被连接");
            }
        }
    }

    /**
     * 读取数据
     * @param v
     * @return
     */
    public void readValue(View v){
        BluetoothGattCharacteristic characteristic = getBluetoothGattCharacteristic("0000a300-0000-1000-8000-00805f9b34fb", "0000a301-0000-1000-8000-00805f9b34fb");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(bluetoothGatt!=null){
                bluetoothGatt.readCharacteristic(characteristic);
                characteristic.getValue();
            }else{
                System.out.println("设备没有被连接");
            }
        }
    }

    /**
     * 释放资源
     * @param v
     */
    public void closeGatt(View v){
        // @todo 释放资源前需要先断开连接
        
        if(bluetoothGatt!=null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                System.out.println("释放资源");
                bluetoothGatt.close();
                bluetoothGatt = null;
            }
        }
    }



}