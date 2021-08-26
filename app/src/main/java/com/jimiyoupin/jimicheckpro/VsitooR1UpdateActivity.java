package com.jimiyoupin.jimicheckpro;

import android.bluetooth.le.ScanResult;
import android.os.Bundle;

import java.util.HashSet;

public final class VsitooR1UpdateActivity extends CommonUpdateActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.isDebug = true;
        super.debugAddresses = new HashSet<>();
//        super.debugAddresses.add("A4:C1:38:0B:04:DB");
        super.debugAddresses.add("A4:C1:38:A2:5A:90");
        super.debugLatestVersion = "1.1.75";

        super.modelName = "VSITOO R1";
        super.modelNameReg = "^VSITOO R1";

        super.serviceUuid = "0000a300-0000-1000-8000-00805f9b34fb";
        super.characteristicNotifyUuid = "0000a303-0000-1000-8000-00805f9b34fb";
        super.characteristicWriteUuid = "0000a301-0000-1000-8000-00805f9b34fb";
        super.characteristicUpdateUuid = "0000a302-0000-1000-8000-00805f9b34fb";
        super.cmdGetVersion = new byte[]{0x0B};
        super.cmdReset = new byte[]{0x08,0x01};

        super.onCreate(savedInstanceState);
    }

    @Override
    public String getFirmwareVersionFromCharacteristValue(byte[] value) {
        return value[2] +"."+value[3]+"."+value[4];
    }

}