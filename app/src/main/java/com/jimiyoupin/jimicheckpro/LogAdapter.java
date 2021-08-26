package com.jimiyoupin.jimicheckpro;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class LogAdapter extends BaseAdapter {
    Context context;
    private ArrayList<String> logList;
    public LogAdapter(Context context, ArrayList<String> logList){
        this.context = context;
        this.logList = logList;
    }

    @Override
    public int getCount() {
        return logList.size();
    }

    @Override
    public Object getItem(int position) {
        return logList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if(convertView==null){
            textView = new TextView(context);
        }else{
            textView = (TextView) convertView;
        }
        textView.setText(logList.get(position));
        return textView;
    }
}
