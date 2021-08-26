package com.jimiyoupin.jimicheckpro;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class IPorketM1Activity extends CommonUpdateActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.modelName = "iPocket M1";
        super.modelNameReg = "^midea-39-";

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