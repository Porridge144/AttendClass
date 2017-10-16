package com.example.gszzz.attendclass;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DeviceListAdapter extends ArrayAdapter<ScanResult> {

    private LayoutInflater mLayoutInflater;
    private ArrayList<ScanResult> results;
    private int mViewResourceID;

    public DeviceListAdapter(@NonNull Context context, @LayoutRes int tvResourceID, @NonNull ArrayList<ScanResult> results) {
        super(context, tvResourceID, results);
        this.results = results;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceID = tvResourceID;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(mViewResourceID, null);
        }

        ScanResult result = results.get(position);

        if (result != null) {
            TextView deviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);
            TextView deviceAddress = (TextView) convertView.findViewById(R.id.tvDeviceAddress);
            TextView deviceData = (TextView) convertView.findViewById(R.id.tvDeviceData);

            if (deviceName != null) {
                if (result.getDevice().getName().equals("")) {
                    deviceName.setText(R.string.no_device_name);
                } else {
                    deviceName.setText(result.getDevice().getName());
                }
            }
            if (deviceAddress != null) {
                deviceAddress.setText(result.getDevice().getAddress());
            }
            if (deviceData != null) {
                try {
                    String str = "";
                    byte[] bytes = new byte[16];
                    for(Map.Entry m:result.getScanRecord().getServiceData().entrySet()){
                        bytes = (byte[]) m.getValue();
                        String str1 = new String(bytes);
                        str += str1;
                    }
                    deviceData.setText(str);
                } catch (Exception ignored) {
                }
            }

        }


        return convertView;
    }
}
