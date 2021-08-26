package com.jimiyoupin.jimicheckpro;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Bundle;

import java.util.HashSet;

public class VsitooS6UpdateActivity extends CommonUpdateActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.isDebug = true;
        super.debugAddresses = new HashSet<>();
        super.debugAddresses.add("A4:C1:38:BE:EF:23");
        super.debugLatestVersion = "1.0.94";

        super.modelName = "S6";
        super.modelNameReg = "^Hi-\\w{0,14}-.2ANU.{2}(\\w{4})";

        super.serviceUuid = "0000a300-0000-1000-8000-00805f9b34fb";
        super.characteristicNotifyUuid = "0000a302-0000-1000-8000-00805f9b34fb";
        super.characteristicWriteUuid = "0000a301-0000-1000-8000-00805f9b34fb";
        super.characteristicUpdateUuid = "0000a303-0000-1000-8000-00805f9b34fb";
        super.cmdGetVersion = new byte[]{0x02};
        super.cmdReset = new byte[]{0x01,0x01};

        super.show_lv_foundDevice = true;
        super.show_lv_log = false;

        super.onCreate(savedInstanceState);
    }

    @Override
    public String getFirmwareVersionFromCharacteristValue(byte[] value) {
        return value[2] +"."+value[3]+"."+value[4];
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public String getFirmwareVersionFromScanResult(ScanResult scanResult) {
        byte[] scanRecordData = scanResult.getScanRecord().getBytes();
        return scanRecordData[24]+"."+scanRecordData[25]+"."+scanRecordData[26];
    }
}