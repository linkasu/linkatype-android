package ru.ibakaidov.distypepro.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.HashMap;

public class HashMapAdapter extends BaseAdapter {
    private Context context;
    private HashMap<String, String> mData;
    private String[] mKeys;
    public HashMapAdapter(Context context, HashMap<String, String> data){
        this.context = context;
        mData  = data;
        mKeys = mData.keySet().toArray(new String[data.size()]);
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    public String[] getEntry(int position){
        return new String[] {mKeys[position], getItem(position)};
    }

    @Override
    public String getItem(int position) {
        return mData.get(mKeys[position]);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, parent, false);
        }
        String value = getItem(pos);
        ((TextView) convertView).setText(value);
        return convertView;
    }

    public String getKey(int position) {
        return mKeys[position];
    }
}