package com.jimiyoupin.jimicheckpro;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.le.ScanResult;
import android.os.Bundle;

import java.util.HashSet;

public class VsitooS2UpdateActivity extends CommonUpdateActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        super.isDebug = true;
//        super.debugAddresses = new HashSet<>();
//        super.debugAddresses.add("A4:C1:86:6B:71:EB");
//        super.debugLatestVersion = "0.2.9";

        super.modelName = "VSITOO S2";
        super.modelNameReg = "^Hi-\\w{0,14}-.2B32.{2}(\\w{4})";

        super.serviceUuid = "0000a300-0000-1000-8000-00805f9b34fb";
        super.characteristicNotifyUuid = "0000a302-0000-1000-8000-00805f9b34fb";
        super.characteristicWriteUuid = "0000a301-0000-1000-8000-00805f9b34fb";
        super.characteristicUpdateUuid = "0000a303-0000-1000-8000-00805f9b34fb";
        super.cmdGetVersion = new byte[]{0x04};
        super.cmdReset = new byte[]{0x01,0x01};

        super.onCreate(savedInstanceState);
    }

    @Override
    public String getFirmwareVersionFromCharacteristValue(byte[] value) {
        return value[2] +"."+value[3]+"."+value[4];
    }

}