package com.example.gszzz.attendclass;

import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class NameList extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private ListView listView;
    private DeviceListAdapter deviceListAdapter;
    private ArrayList<ScanResult> scanResults;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name_list);
        listView = (ListView) findViewById(R.id.devicesListView);
        scanResults = new ArrayList<>();

        //Register the broadcast receiver
        IntentFilter filter = new IntentFilter(ScannerService.NEW_DEVICE_FOUND);
        registerReceiver(scanResultsReceiver, filter);

    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(scanResultsReceiver);
        super.onDestroy();
    }

    private final BroadcastReceiver scanResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ScannerService.NEW_DEVICE_FOUND)) {
                scanResults = intent.getParcelableArrayListExtra("ParcelScanResults");
                deviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, scanResults);
                listView.setAdapter(deviceListAdapter);
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String deviceName = scanResults.get(i).getDevice().getName();
        List deviceUuid = scanResults.get(i).getScanRecord().getServiceUuids();
        String deviceMessage = new String(scanResults.get(i).getScanRecord().getServiceData().get(deviceUuid.get(0)));
        Toast.makeText(getApplicationContext(), "You have clicked device: " + deviceMessage, Toast.LENGTH_SHORT).show();
    }
}
