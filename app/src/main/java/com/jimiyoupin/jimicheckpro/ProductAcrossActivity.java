package com.jimiyoupin.jimicheckpro;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductAcrossActivity extends AppCompatActivity {
    private String targetModel;
    private Handler uiHandler;

    private EditText etMac;
    private Spinner spinnerSelectService,
            spinnerSelectCharacteristic,
            spinnerSelectTargetModel;
    private ListView lv_log;

    Ble ble;
    private BluetoothGattCallback bluetoothGattCallback;

    protected String serviceUuid="0000a300-0000-1000-8000-00805f9b34fb";
    protected String characteristicUpdateUuid="0000a302-0000-1000-8000-00805f9b34fb";

    private ArrayList<byte[]> _updateData = new ArrayList<>();
    private int _updateDataSize = 0;
    private int _updateCursor = 0;
    private int _updateProgress = 0;
    private boolean _isUpdating = false;

    private String _latestVersion = "0.0.0";

    private ArrayList<String> services = new ArrayList<>(),
            characteristics = new ArrayList(),
            models = new ArrayList<>();
    private ArrayAdapter<String> serviceAdapter, characteristicAdapter, modelAdapter;

    private String selectedService,selectedCharacteristic;

    private ArrayList<String> logList = new ArrayList<>();
    private LogAdapter logAdapter = new LogAdapter(this, logList);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_across);

        bindViews();
        bindHandlers();

        getModelList();
    }

    /**
     * 绑定UI元素
     */
    private void bindViews(){
        etMac = findViewById(R.id.et_mac);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        etMac.setText(sharedPref.getString("connectedAddress",""));
        // 监听输入
        etMac.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String parseStr = s.toString().toUpperCase().replaceAll("\\:|\\s", "");
                String mac = "";
                for (int i = 0,len = parseStr.length(); i < len&&i<12; i+=2) {
                    if(i>0){
                        mac += ":";
                    }
                    mac +=  parseStr.substring(i,i+2>parseStr.length()?parseStr.length():i+2);
                }
//                System.out.println("parseStr:"+parseStr);
//                System.out.println("mac:"+mac);
                if (!mac.equals(etMac.getText().toString())){
                    etMac.setText(mac);
                    etMac.setSelection(mac.length());
                }
            }
        });

        spinnerSelectService = findViewById(R.id.spinner_selectService);
        services.add("");
        selectedService = "";
        serviceAdapter = new ArrayAdapter<>(ProductAcrossActivity.this, android.R.layout.simple_spinner_item, services);
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelectService.setAdapter(serviceAdapter);
        spinnerSelectService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedService = services.get(position);
                System.out.println("选择服务"+position+":"+selectedService);

                characteristics.clear();
                characteristics.add("");
                if(null!=ble){
                    List<BluetoothGattCharacteristic> characteristics =  ble.bluetoothGattServiceHashMap.get(services.get(position)).bluetoothGattService.getCharacteristics();
                    for(BluetoothGattCharacteristic characteristic:characteristics){
                        ProductAcrossActivity.this.characteristics.add(characteristic.getUuid().toString());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                System.out.println("没有选择："+parent);
            }
        });

        spinnerSelectCharacteristic = findViewById(R.id.spinner_selectCharacteristic);
        characteristics.add("");
        characteristicAdapter = new ArrayAdapter<>(ProductAcrossActivity.this, android.R.layout.simple_spinner_item, characteristics);
        characteristicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelectCharacteristic.setAdapter(characteristicAdapter);
        spinnerSelectCharacteristic.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCharacteristic = characteristics.get(position);
                System.out.println("已选特征"+position+"："+selectedCharacteristic);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerSelectTargetModel = findViewById(R.id.spinner_selectTargetModel);
        models.add("");
        selectedCharacteristic = "";
        modelAdapter = new ArrayAdapter<>(ProductAcrossActivity.this, android.R.layout.simple_spinner_item, models);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelectTargetModel.setAdapter(modelAdapter);
        spinnerSelectTargetModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                targetModel = models.get(position);
                System.out.println("已选目标型号："+targetModel);
                checkLatestVersion(targetModel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        lv_log = findViewById(R.id.lv_log);
        lv_log.setAdapter(logAdapter);
    }

    /**
     * 绑定handlers
     */
    private void bindHandlers(){
        uiHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what){
                    case 1:
                        logList.add((String) msg.obj);
                        logAdapter.notifyDataSetChanged();
                        break;
                }
            }
        };


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setBluetoothGattCallback();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setBluetoothGattCallback(){
        bluetoothGattCallback = new BluetoothGattCallback() {
            @RequiresApi(api = Build.VERSION_CODES.R)
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
                                addLog("设备名称："+gatt.getDevice().getName());
                                    ble.getServices();
                                break;

                            case BluetoothGatt.STATE_DISCONNECTING:
                                text +="水杯断开连接中...，";
                                break;
                        }
                        break;

                    default:
                        text += "未知错误，";

                        ble.disconnect();
                        break;
                }

                text+="状态码:"+status;
                System.out.println(text);
                addLog(text);
            }
//
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                System.out.println("发现服务，状态码:"+status);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        services.clear();
                        services.add("");
                    }
                });
                List<BluetoothGattService> bluetoothGattServices = gatt.getServices();
                for(BluetoothGattService service: bluetoothGattServices){
                    System.out.println("服务:"+service.getUuid().toString());
                    ble.addService(service);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            services.add(service.getUuid().toString());
                        }
                    });
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
                            }
                        }else{
                            System.out.println("升级完成");
                            addLog("升级完成");
                        }
                    }
                }else{
                    System.out.println("写入特征"+characteristicUuid+"，状态码:"+status+"，值："+ MyTools.byteArrayToHex(characteristic.getValue()));
                }
            }

        };

    }

    public void connect(View v){
        if(null == ble){
            ble = new Ble(this);
        }

        if(!ble.checkBluetoothLEAccess()){
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!ble.getPermissions()){
            return;
        }

        if(!(ble.isEnabled() || ble.enableBluetoothAdapter())){
            Toast.makeText(this, "BLE未开启", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(){
            @Override
            public void run() {
                String address = etMac.getText().toString();
                ble.connect(address,bluetoothGattCallback);
                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor  editor = sharedPref.edit();
                editor.putString("connectedAddress", address);
                editor.commit();
            }
        }.start();

    }

    public void disconnect(View v){
        if(null!=ble){
            ble.disconnect();
        }
    }

    /**
     * 检测并下载最新版本
     */
    private void checkLatestVersion(String modelName){
        if(modelName.equals("")) return;

        new Thread(){
            @Override
            public void run() {
                try {
                    byte[] checkVersionResponse = MyTools.doPost("http://update.jimiyoupin.com/api/firmware_debug/check_version", "model_name="+ modelName +"&version_num=0.0.0&language=zh-cn");
                    if(null == checkVersionResponse){
                        addLog("不能获取最新固件版本信息");
                    }else{
                        String checkVersionResponseStr = new String(checkVersionResponse);
                        System.out.println(checkVersionResponseStr);

                        JSONObject jsonResponse = new JSONObject(checkVersionResponseStr);
                        int jsonResponseCode = jsonResponse.getInt("code");
                        if(0!=jsonResponseCode){
                            addLog("不能获取最新固件版本信息："+jsonResponseCode);
                        }else{
                            JSONObject jsonResponseData = jsonResponse.getJSONObject("data");
                            if(jsonResponseData.getBoolean("need_update")){
                                _latestVersion = jsonResponseData.getString("version_num");
                                addLog("最新版本:"+ _latestVersion +"\r\n版本描述:"+jsonResponseData.getString("description"));

                                byte[] getUpdateDataResponse = MyTools.doGet("http://update.jimiyoupin.com/api/firmware_debug/get_update_file_content/model_name/"+ modelName  +"/version_num/"+ _latestVersion +"/format/hex");
                                if(null==getUpdateDataResponse){
                                    addLog("不能获取升级数据");
                                }else{
                                    String getUpdateDataResponseStr = new String(getUpdateDataResponse);
                                    System.out.println(getUpdateDataResponseStr);
                                    JSONObject jsonGetUpdateDataResponse = new JSONObject(getUpdateDataResponseStr);
                                    int jsonGetUpdateDataResponseCode = jsonGetUpdateDataResponse.getInt("code");
                                    if(0!=jsonGetUpdateDataResponseCode){
                                        addLog("不能获取升级数据："+jsonGetUpdateDataResponseCode);
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
                                            addLog("数据下载成功["+ _updateData.size()+"]");
                                        }else{
                                            addLog("没有升级数据");
                                        }
                                    }
                                }
                            }else{
                                addLog("没有更新的固件版本");
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    public void startUpdate(View v){
        if(ble==null ||ble.bluetoothGatt.getDevice()==null){
            Toast.makeText(this, "请连接蓝牙", Toast.LENGTH_LONG).show();
            return;
        }

        if(selectedService.equals("")|| selectedCharacteristic.equals("")){
            Toast.makeText(this,"请选择服务和升级特征",Toast.LENGTH_LONG).show();
            return;
        }

        if(targetModel.equals("")||_updateData.size()==0){
            Toast.makeText(this,"请选择型号",Toast.LENGTH_LONG).show();
            return;
        }

        sendUpdateData(0);
    }

    private void sendUpdateData(int cursor){
        _isUpdating = true;
//        _updateStartTime = System.currentTimeMillis();
        _updateCursor = cursor;
        if(_updateCursor<_updateDataSize){
            ble.writeValue(serviceUuid, characteristicUpdateUuid, _updateData.get(_updateCursor++));
        }
    }

    private void getModelList(){
        new Thread(){
            @Override
            public void run() {
                byte[] getModelsResponse = MyTools.doGet("http://update.jimiyoupin.com/api/model/get_all_models");
                if(null==getModelsResponse){
                    addLog("不能获取型号数据");
                }else{
                    String getModelsResponseStr = new String(getModelsResponse);
                    System.out.println(getModelsResponseStr);
                    JSONObject jsonGetModelsResponse = null;
                    try {
                        jsonGetModelsResponse = new JSONObject(getModelsResponseStr);
                        int jsonGetModelsResponseCode = jsonGetModelsResponse.getInt("code");
                        if(0!= jsonGetModelsResponseCode){
                            addLog("不能获取数据："+ jsonGetModelsResponseCode);
                        }else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    models.clear();
                                    models.add("");
                                }
                            });
                            JSONArray jsonGetModelsResponseData = jsonGetModelsResponse.getJSONArray("data");
                            for (int i = 0,len=jsonGetModelsResponseData.length(); i < len; i++) {
                                String model = (String) jsonGetModelsResponseData.get(i);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        models.add(model);
                                    }
                                });
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        }.start();
    }

    /**
     * 添加日志
     * @param log
     */
    private void addLog(String log){
        Message msg = Message.obtain();
        msg.what = 1;
        msg.obj = log;
        uiHandler.sendMessage(msg);
        System.out.println(log);
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                // 需要在主线程中处理 BaseAdapter
//                System.out.println(log);
//                logList.add(log);
//            }
//        });
    }
}