package com.jimiyoupin.jimicheckpro;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class BluetoothDeviceAdapter extends BaseAdapter {
    Context context;
    ArrayList<BluetoothDevice> bluetoothDevices;

    public BluetoothDeviceAdapter(Context context, ArrayList<BluetoothDevice> bluetoothDevices){
        this.context = context;
        this.bluetoothDevices = bluetoothDevices;
    }

    @Override
    public int getCount() {
        return bluetoothDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return bluetoothDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if(convertView==null){
            view = View.inflate(context,R.layout.device_list_item, null);
        }else{
            view = convertView;
        }

        TextView device_name  = view.findViewById(R.id.tv_deviceName);
        device_name.setText("["+bluetoothDevices.get(position).getName()+"]");
        TextView device_mac = view.findViewById(R.id.device_mac);
        BluetoothDevice bluetoothDevice = bluetoothDevices.get(position);
        device_mac.setText(bluetoothDevice.getAddress());

        return view;
    }

    public boolean add(BluetoothDevice bluetoothDevice){
        int index = bluetoothDevices.indexOf(bluetoothDevice);
        if(index>=0) {
            bluetoothDevices.add(bluetoothDevice);
            return false;
        }
        bluetoothDevices.add(bluetoothDevice);
        return true;
    }

}
