package com.jimiyoupin.jimicheckpro;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class CommonUpdateActivity extends AppCompatActivity {
    protected boolean isDebug = false;
    protected HashSet<String> debugAddresses = new HashSet<>();
    protected String debugLatestVersion;

    protected String modelName;
    protected String modelNameReg;

    private String _latestVersion = "0.0.0", _currentVersion="0.0.0";
    private TextView _tv_tips1, _tv_tips2, _tv_tips3,_tv_progress,_tv_statusBar;
    private CheckBox _cb_update,_cb_reset;
    private ProgressBar _pb_progress;
    private Button _btn_toggleStartStop;
    private ListView _lv_log,lv_foundDevice;
    public boolean show_lv_foundDevice = false,
            show_lv_log = true;
    private Handler uiHandler, _checkNotifyHandler = new Handler(), _scanHandler = new Handler();
    private Runnable _checkNotifyRunnable = new Runnable() {
        @Override
        public void run() {
            addLog("[×]"+MyTools.getDateTime("HH:mm:ss")+"设备无响应:"+ currentBluetoothDevice.getAddress());
            _isUpdating = false;
            sendStopUpdateCmd();
        }
    };

    private ArrayList<byte[]> _updateData = new ArrayList<>();
    private int _updateDataSize = 0;
    private int _updateCursor = 0;
    private int _updateProgress = 0;

    private boolean _isProceeding = false, _isUpdating = false;

    private Ble ble;
    private ScanCallback scanCallback;
    private BluetoothDevice currentBluetoothDevice;
    private BluetoothGattCallback bluetoothGattCallback;

    protected String serviceUuid;
    protected String characteristicNotifyUuid;
    protected String characteristicWriteUuid;
    protected String characteristicUpdateUuid;
    protected byte[] cmdGetVersion;
    protected byte[] cmdReset;

    private long _updateStartTime = 0;
    private long _updateEndTime = 0;

    private ArrayList<String> logList = new ArrayList<>();
    private LogAdapter logAdapter = new LogAdapter(this, logList);
    private ArrayList<ScanResult> scanResultArrayList = new ArrayList<>();
    private ScanResultAdapter scanResultAdapter;

    private int _numUpdateSuccess = 0, _numResetSuccess=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_update);

        // 打开屏幕常量
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 设置标题
        setTitle(modelName);

        // 显示返回按钮
        final ActionBar supportActionBar = getSupportActionBar();
        if(supportActionBar != null){
            supportActionBar.setHomeButtonEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }

        bindViews();
        bindHandlers();

        checkLatestVersion();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            // 触发返回按钮激发事件
            case android.R.id.home:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 关闭屏幕常量
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        System.out.println(modelName +" UpdateAtivity Destory");
        stopProcess();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 关闭屏幕常量
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 打开屏幕常量
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * 绑定UI元素
     */
    private void bindViews(){
        _tv_tips1 = findViewById(R.id.tv_tips1);
        _tv_tips2 = findViewById(R.id.tv_tips2);
        _tv_tips3 = findViewById(R.id.tv_tips3);
        _tv_progress = findViewById(R.id.tv_progress);
        _tv_statusBar = findViewById(R.id.tv_statusBar);

        _cb_update = findViewById(R.id.cb_update);
        _cb_reset = findViewById(R.id.cb_reset);

        _btn_toggleStartStop = findViewById(R.id.btn_toggleStartStop);

        _pb_progress = findViewById(R.id.pb_progress);

        _lv_log = findViewById(R.id.lv_log);
        _lv_log.setVisibility(show_lv_log?View.VISIBLE:View.GONE);
        _lv_log.setAdapter(logAdapter);


        lv_foundDevice = findViewById(R.id.lv_foundDevice);
        lv_foundDevice.setVisibility(show_lv_foundDevice?View.VISIBLE:View.GONE);
    }

    /**
     * 绑定handlers
     */
    private void bindHandlers(){
        uiHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what){
                    case R.id.tv_tips1:
                        _tv_tips1.setText((String) msg.obj);
                        break;

                    case R.id.tv_tips2:
                        _tv_tips2.setText((String) msg.obj);
                        break;

                    case R.id.tv_tips3:
                        _tv_tips3.setText((String) msg.obj);
                        break;

                    case R.id.tv_progress:
                        int progress = (int) msg.obj;
                        _tv_progress.setText(progress+"%");
                        _pb_progress.setProgress(progress);
                        break;
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setScanCallback();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setBluetoothGattCallback();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setScanCallback(){
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {

                    BluetoothDevice bluetoothDevice = result.getDevice();
                    String address = bluetoothDevice.getAddress();
                    String name = bluetoothDevice.getName();
                    int rssi = result.getRssi();
                    System.out.println("Address:"+address+"  ModelName:"+name);
                    if(MyTools.match(modelNameReg, name)&&rssi>=-80){
                        System.out.println("rssi["+rssi+"]");

                        String currentFirmwareVersion = getFirmwareVersionFromScanResult(result);
                        if(null!=currentFirmwareVersion){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    scanResultAdapter.add(result);
                                }
                            });
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Message msg = Message.obtain();
                                msg.what = R.id.tv_tips2;
                                msg.obj = "搜到"+ scanResultAdapter.getCount()+"设备";
                                uiHandler.sendMessage(msg);
                            }
                        });
                        if(isDebug){
                            //@todo 调试
                            if(debugAddresses.contains(address)){
                                connect(bluetoothDevice);
                            }
                        }else {
                            if (null==currentFirmwareVersion) {
                                connect(bluetoothDevice);
                            }else{
                                if(MyTools.compareVersion(_latestVersion, currentFirmwareVersion) > 0){
                                    connect(bluetoothDevice);
                                }
                            }
                        }
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    System.out.println("发现设备 err:"+errorCode);
                }
            };

    }

    private void connect(BluetoothDevice bluetoothDevice){
        System.out.println("找到 "+ modelName +"["+bluetoothDevice.getAddress()+"]");
        ble.stopScan();
        Message msg = Message.obtain();
        msg.what = R.id.tv_tips2;
        msg.obj = "获取设备:"+bluetoothDevice.getAddress();
        uiHandler.sendMessage(msg);
        currentBluetoothDevice = bluetoothDevice;

        if(_isProceeding){
            ble.connect(currentBluetoothDevice, bluetoothGattCallback);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setBluetoothGattCallback(){
            bluetoothGattCallback = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    String text = "连接状态：";
                    switch (status){
                        case BluetoothGatt.GATT_SUCCESS:
                            text += gatt.getDevice().getAddress()+"连接状态改变，值:"+newState+"，";
                            switch (newState){
                                case BluetoothGatt.STATE_DISCONNECTED:
                                    text +="水杯断开连接，";
                                    break;

                                case BluetoothGatt.STATE_CONNECTING:
                                    text +="水杯连接中...，";
                                    break;

                                case BluetoothGatt.STATE_CONNECTED:
                                    text +="水杯已连接，";
                                    if(_isProceeding){
                                        ble.getServices();
                                    }
                                    break;

                                case BluetoothGatt.STATE_DISCONNECTING:
                                    text +="水杯断开连接中...，";
                                    break;
                            }
                            break;

                        default:
                            text += "未知错误，";

                            ble.disconnect();
                            _isUpdating = false;
                            if(_isProceeding){
                                startProcess();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        _btn_toggleStartStop.setText("停止");
                                    }
                                });
                            }else{
                                stopProcess();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        _btn_toggleStartStop.setText("开始");
                                    }
                                });
                            }
                            break;
                    }

                    text+="状态码:"+status;
                    System.out.println(text);

                    String finalText = text;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            _tv_tips3.setText(finalText);
                        }
                    });
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    System.out.println("发现服务，状态码:"+status);

                    List<BluetoothGattService> bluetoothGattServices = gatt.getServices();
                    for(BluetoothGattService service: bluetoothGattServices){
                        System.out.println("服务:"+service.getUuid().toString());
                        ble.addService(service);
                    }
                    if(_isProceeding){
                        ble.setNotification(serviceUuid, characteristicNotifyUuid, true);

                        uiHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(_cb_update.isChecked()){
                                    // 获取固件版本
                                    ble.writeValue(serviceUuid, characteristicWriteUuid, cmdGetVersion);
                                    _checkNotifyHandler.postDelayed(_checkNotifyRunnable,3000);
                                }else{
                                    // 恢复出厂
                                    ble.writeValue(serviceUuid,characteristicWriteUuid,cmdReset);
                                    setSuccessNumber(_numUpdateSuccess, ++_numResetSuccess);
                                    addLog("[✓]"+MyTools.getDateTime("HH:mm:ss")+" 恢复出厂成功:"+ currentBluetoothDevice.getAddress());
                                }
                            }
                        },2000);
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    String characteristicUuid = characteristic.getUuid().toString();
                    if(characteristicUuid.equals(characteristicUpdateUuid)){
                        if(_isUpdating){
                            if(_updateCursor<_updateDataSize){
                                ble.writeValue(serviceUuid, characteristicUpdateUuid, _updateData.get(_updateCursor++));
                                int progress = _updateCursor*100/_updateDataSize;
                                if(progress!=_updateProgress){
                                    _updateProgress = progress;
                                    Message msg = Message.obtain();
                                    msg.what = R.id.tv_progress;
                                    msg.obj = progress;
                                    uiHandler.sendMessage(msg);
                                }
                            }else{
                                _updateEndTime = System.currentTimeMillis();
                                Message msg = Message.obtain();
                                msg.what = R.id.tv_progress;
                                msg.obj = 100;
                                uiHandler.sendMessage(msg);
                                System.out.println("update ok，用时:"+ (_updateEndTime-_updateStartTime));
                                addLog("[✓]"+MyTools.getDateTime("HH:mm:ss")+" 升级成功:"+ currentBluetoothDevice.getAddress());
                                setSuccessNumber(++_numUpdateSuccess, _numResetSuccess);
                                setUpdating(false);
                            }
                        }
                    }else{
                        System.out.println("写入特征"+characteristicUuid+"，状态码:"+status+"，值："+ MyTools.byteArrayToHex(characteristic.getValue()));
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    if(_isUpdating){
                        System.out.println("正在升级，不能写入其他数据");
                        return;
                    }
                    byte[] value = characteristic.getValue();
                    System.out.println("特征改变"+characteristic.getUuid().toString()+"，值："+ MyTools.byteArrayToHex(value));
                    if(value.length>0){
                        if(value[0] == cmdGetVersion[0]){
//                            _currentVersion = value[2] +"."+value[3]+"."+value[4];
                            _currentVersion = getFirmwareVersionFromCharacteristValue(value);
                            _checkNotifyHandler.removeCallbacks(_checkNotifyRunnable);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    _tv_tips3.setText("当前版本:"+_currentVersion);

                                    if(isDebug){
                                        // @todo 测试
                                        System.out.println("开始升级 debug");
                                        sendUpdateData(0);
                                    }else{
                                        if(MyTools.compareVersion(_latestVersion, _currentVersion) > 0){
                                            System.out.println("需要升级,开始升级...");
                                            sendUpdateData(0);
                                        }else{
                                            // 恢复出厂
                                            System.out.println("需要恢复出厂");
                                            ble.writeValue(serviceUuid,characteristicWriteUuid,cmdReset);
                                            setSuccessNumber(_numUpdateSuccess, ++_numResetSuccess);
                                            addLog("[✓]"+MyTools.getDateTime("HH:mm:ss")+" 恢复出厂成功:"+ currentBluetoothDevice.getAddress());
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            };

    }

    /**
     * 从特征值中获取版本号
     * @param value
     * @return
     */
    abstract public String getFirmwareVersionFromCharacteristValue(byte[] value);

    public String getFirmwareVersionFromScanResult(ScanResult scanResult){
        return null;
    };

    /**
     * 检测并下载最新版本
     */
    private void checkLatestVersion(){
        new Thread(){
            @Override
            public void run() {
                try {
                    Message msg= Message.obtain();
                    byte[] checkVersionResponse = MyTools.doPost("http://update.jimiyoupin.com/api/firmware/check_version", "model_name="+ modelName +"&version_num=0.0.0&language=zh-cn");
                    if(null == checkVersionResponse){
                        msg.what = R.id.tv_tips1;
                        msg.obj = "不能获取最新固件版本信息";
                        uiHandler.sendMessage(msg);
                    }else{
                        String checkVersionResponseStr = new String(checkVersionResponse);
                        System.out.println(checkVersionResponseStr);

                        JSONObject jsonResponse = new JSONObject(checkVersionResponseStr);
                        int jsonResponseCode = jsonResponse.getInt("code");
                        if(0!=jsonResponseCode){
                            msg = Message.obtain();
                            msg.what = R.id.tv_tips1;
                            msg.obj = "不能获取最新固件版本信息："+jsonResponseCode;
                            uiHandler.sendMessage(msg);
                        }else{
                            JSONObject jsonResponseData = jsonResponse.getJSONObject("data");
                            if(jsonResponseData.getBoolean("need_update")){
                                _latestVersion = jsonResponseData.getString("version_num");

                                msg = Message.obtain();
                                msg.what = R.id.tv_tips1;
                                msg.obj = "最新版本:"+ _latestVersion +"\r\n版本描述:"+jsonResponseData.getString("description");
                                uiHandler.sendMessage(msg);
                                // @todo 测试
                                if(isDebug&&null!=debugLatestVersion){
                                    _latestVersion = debugLatestVersion;
                                }
                                byte[] getUpdateDataResponse = MyTools.doGet("http://update.jimiyoupin.com/api/firmware/get_update_file_content/model_name/"+ modelName  +"/version_num/"+ _latestVersion +"/format/hex");
                                if(null==getUpdateDataResponse){
                                    msg = Message.obtain();
                                    msg.what = R.id.tv_tips2;
                                    msg.obj = "不能获取升级数据";
                                    uiHandler.sendMessage(msg);
                                }else{
                                    String getUpdateDataResponseStr = new String(getUpdateDataResponse);
                                    System.out.println(getUpdateDataResponseStr);
                                    JSONObject jsonGetUpdateDataResponse = new JSONObject(getUpdateDataResponseStr);
                                    int jsonGetUpdateDataResponseCode = jsonGetUpdateDataResponse.getInt("code");
                                    if(0!=jsonGetUpdateDataResponseCode){
                                        msg = Message.obtain();
                                        msg.what = R.id.tv_tips1;
                                        msg.obj = "不能获取升级数据："+jsonGetUpdateDataResponseCode;
                                        uiHandler.sendMessage(msg);
                                    }else{
                                        JSONObject jsonGetUpdateDataResponseData = jsonGetUpdateDataResponse.getJSONObject("data");
                                        JSONArray jsonGetUpdateDataResponseDataContent = jsonGetUpdateDataResponseData.getJSONArray("content");
                                        if(jsonGetUpdateDataResponseDataContent.length()>0){
                                            _updateData.clear();
                                            _updateData.add(MyTools.hexToByteArray("01FF"));
                                            for(int i=0,len = jsonGetUpdateDataResponseDataContent.length(); i<len; i++){
                                                _updateData.add(MyTools.hexToByteArray(jsonGetUpdateDataResponseDataContent.getString(i)));
                                            }
                                            _updateData.add(MyTools.hexToByteArray("02FF"));
                                            _updateDataSize = _updateData.size();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    _tv_tips2.setText("数据下载成功["+ _updateData.size()+"]");
                                                    _btn_toggleStartStop.setEnabled(true);
                                                }
                                            });
                                        }else{
                                            msg = Message.obtain();
                                            msg.what = R.id.tv_tips1;
                                            msg.obj = "没有升级数据";
                                            uiHandler.sendMessage(msg);
                                        }
                                    }
                                }
                            }else{
                                msg = Message.obtain();
                                msg.what = R.id.tv_tips1;
                                msg.obj = "没有更新的固件版本";
                                uiHandler.sendMessage(msg);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void toggleStartStop(View v){
        if(_isProceeding){
            _isProceeding = false;
            stopProcess();
            _btn_toggleStartStop.setText("开始");
        }else{
            if(_cb_update.isChecked()||_cb_reset.isChecked()){
                if(startProcess()){
                    _isProceeding = true;
                    _btn_toggleStartStop.setText("停止");
                    scanResultAdapter = new ScanResultAdapter(this, scanResultArrayList, _latestVersion);
                    lv_foundDevice.setAdapter(scanResultAdapter);
                }else{
                    _btn_toggleStartStop.setText("开始");
                }
            }else{
                Toast.makeText(this, "升级和恢复出厂至少选择一项", Toast.LENGTH_LONG).show();
            }
        }
    }

    public boolean startProcess(){
        if(null == ble){
            ble = new Ble(this);
        }

        if(!ble.checkBluetoothLEAccess()){
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_SHORT).show();
            return false;
        }

        if(!ble.getPermissions()){
            return false;
        }

        if(!(ble.isEnabled() || ble.enableBluetoothAdapter())){
            Toast.makeText(this, "BLE未开启", Toast.LENGTH_SHORT).show();
            return false;
        }

        if(null== scanCallback) {
            Toast.makeText(this, "Android版本要在"+Build.VERSION_CODES.LOLLIPOP+"以上", Toast.LENGTH_SHORT).show();
            return false;
        }

        scanResultArrayList.clear();
        new Thread(){
            @Override
            public void run() {
                if(_isProceeding) {
                    ble.startScan(scanCallback, false);
                }
            }
        }.start();
        return true;
    }

    public void stopProcess(){
        setUpdating(false);
        new Thread(){
            @Override
            public void run() {
                if(null!= ble){
                    ble.stopScan();
                    ble.disconnect();
                }
            }
        }.start();
    }

    private void sendUpdateData(int cursor){
        setUpdating(true);
        _updateStartTime = System.currentTimeMillis();
        _updateCursor = cursor;
        if(_updateCursor<_updateDataSize){
            ble.writeValue(serviceUuid, characteristicUpdateUuid, _updateData.get(_updateCursor++));
        }
    }

    /**
     * 设置是否正在升级中
     * @param enabled
     */
    public void setUpdating(boolean enabled){
        _isUpdating = enabled;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _btn_toggleStartStop.setEnabled(!_isUpdating);
            }
        });
    }

    private void setSuccessNumber(int numUpdate, int numReset){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _tv_statusBar.setText("已升级设备"+numUpdate+"，已恢复出厂"+numReset);
            }
        });
    }

    /**
     * 添加日志
     * @param log
     */
    private void addLog(String log){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 需要在主线程中处理 BaseAdapter
                logList.add(log);
            }
        });
    }

    private void sendStopUpdateCmd(){
        ble.writeValue(serviceUuid, characteristicUpdateUuid,MyTools.hexToByteArray("01FF"));
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ble.writeValue(serviceUuid, characteristicUpdateUuid,MyTools.hexToByteArray("00002680000000005d024b4e4c5471038800b911"));
                System.out.println("设备无响应，停止升级");
            }
        },5);
    }


}