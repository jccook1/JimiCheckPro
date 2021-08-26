package com.jimiyoupin.jimicheckpro;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceListAdapter extends BaseAdapter {
    Context context;
    ArrayList<BluetoothDevice> list;
    DeviceListAdapter(Context context, ArrayList<BluetoothDevice> list){
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        /*
        TextView tv;
        if(convertView == null){
            tv = new TextView(context);
        }else{
            // 复用历史缓存对象
            tv = (TextView) convertView;
        }
        tv.setText("hello "+position);
        return tv;
         */

        View view;
        if(convertView == null){
            // 第一种方法 （推荐）
            view = View.inflate(context, R.layout.device_list_item, null);

            // 第二种方法
//            view = LayoutInflater.from(context).inflate(R.layout.device_list_item, null);

            // 第三种方法
//            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            view = inflater.inflate(R.layout.device_list_item,null);
        }else{
            view = convertView;
        }
        TextView tv = view.findViewById(R.id.tv_deviceName);
        tv.setText(list.get(position).getName());

         tv = view.findViewById(R.id.device_mac);
        tv.setText(list.get(position).getAddress());

        return view;
    }
}
