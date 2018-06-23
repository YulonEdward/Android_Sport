package com.example.yulon.newblecommunicate.adapter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.yulon.newblecommunicate.R;

import java.util.ArrayList;
import java.util.List;

public class ListViewAdspter extends BaseAdapter {

    private Activity mContext;
    public List<BluetoothDevice> mBleDevices;
    public List<Double> mRssi;

    public ListViewAdspter(Activity mContext){
        this.mContext = mContext;
        mBleDevices = new ArrayList<BluetoothDevice>();
        mRssi = new ArrayList<Double>();
    }

    public void addDevice(BluetoothDevice device, Double rssi){
        if(!mBleDevices.contains(device)){
            mBleDevices.add(device);
            mRssi.add(rssi);
        }
        notifyDataSetChanged();
    }

    public BluetoothDevice getDevice(int position){
        if(mBleDevices != null){
            return mBleDevices.get(position);
        }
        return null;
    }

    public void clear(){
        mBleDevices.clear();
    }

    @Override
    public int getCount() {
        if(mBleDevices != null){
            return mBleDevices.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return mBleDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.txt_name = (TextView)convertView.findViewById(R.id.txt_name);
            viewHolder.txt_mac = (TextView)convertView.findViewById(R.id.txt_mac);
            viewHolder.txt_rssi = (TextView)convertView.findViewById(R.id.txt_rssi);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }
        BluetoothDevice mdevice = mBleDevices.get(position);
        Double mrssi = mRssi.get(position);

        String name = mdevice.getName();
        String mac = mdevice.getAddress();
        viewHolder.txt_name.setText(name);
        viewHolder.txt_mac.setText(mac);
        viewHolder.txt_rssi.setText(String.format("%.2f", mrssi) + "m");
        return convertView;
    }

}

class ViewHolder{
    TextView txt_name;
    TextView txt_mac;
    TextView txt_rssi;
}
