package com.example.gszzz.attendclass;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

//1
public class studentRegisterForClass extends AppCompatActivity implements AdapterView.OnItemClickListener{

    BluetoothAdapter bluetoothAdapter;
    ListView listView;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter deviceListAdapter;
    AlertDialog.Builder dlgAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register_for_class);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(studentRegisterForClass.this);
        //Register broadcast listener 4 to received successfully paired broadcast
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver4, filter);
        //Set up alert dialog
        dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setTitle("Done");
        dlgAlert.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        dlgAlert.setCancelable(true);

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device does not have BT capabilities!", Toast.LENGTH_SHORT).show();
        } else if(!bluetoothAdapter.isEnabled()){
            Toast.makeText(getApplicationContext(), "Enabling Bluetooth....", Toast.LENGTH_SHORT).show();
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(broadcastReceiver1, BTIntent);
        } else {
            startDiscovering();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver3);
        unregisterReceiver(broadcastReceiver4);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        bluetoothAdapter.cancelDiscovery();

        String deviceName = mBTDevices.get(position).getName();
        String deviceAddress = mBTDevices.get(position).getAddress();
        Toast.makeText(getApplicationContext(), "Trying to pair with " + deviceName + "@" + deviceAddress, Toast.LENGTH_LONG).show();
        mBTDevices.get(position).createBond();

    }


    //Scan for unpaired discoverable devices (ie. Lecturer's device)
    private void startDiscovering() {
        //Off broadcast receiver 1 if it is on.
        try {
            unregisterReceiver(broadcastReceiver1);
        } catch (Exception ignored){
        }

        if (bluetoothAdapter.isDiscovering()) {
            Toast.makeText(getApplicationContext(), "Canceling Discovery and Restarting...", Toast.LENGTH_LONG).show();
            bluetoothAdapter.cancelDiscovery();
            checkBTPermissions();
            Toast.makeText(getApplicationContext(), "Looking for unpaired devices...", Toast.LENGTH_LONG).show();
            bluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(broadcastReceiver3, discoverDevicesIntent);
        } else {
            Toast.makeText(getApplicationContext(), "Looking for unpaired devices...", Toast.LENGTH_LONG).show();
            checkBTPermissions();
            bluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(broadcastReceiver3, discoverDevicesIntent);
        }
    }
    //Check for permission to discover other devices
    //Test
    private void checkBTPermissions() {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            Toast.makeText(getApplicationContext(), "Requesting for permission...", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "SDK version < LOLIPOP. No need permission check.", Toast.LENGTH_LONG).show();
        }
    }

    private final BroadcastReceiver broadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Toast.makeText(getApplicationContext(), "Device Found: " + device.getName() + "@" + device.getAddress(), Toast.LENGTH_SHORT).show();
                deviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                listView.setAdapter(deviceListAdapter);

            }
        }
    };

    private final BroadcastReceiver broadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases
                //case 1 : already bonded
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    dlgAlert.setMessage("Successfully Paired with " + mDevice.getName());
                    dlgAlert.create().show();
                }
                //case 2 : creating a bonding
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Toast.makeText(getApplicationContext(), "Paring...", Toast.LENGTH_SHORT).show();
                }
                //case 3 : breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Toast.makeText(getApplicationContext(), "Unpaired.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        Toast.makeText(getApplicationContext(), "Bluetooth has been Turned On.", Toast.LENGTH_LONG).show();
                        startDiscovering();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Toast.makeText(getApplicationContext(), "Turning on bluetooth...", Toast.LENGTH_LONG).show();
                        break;
                }
                //What if failed to turn on??
            }
        }
    };
}
