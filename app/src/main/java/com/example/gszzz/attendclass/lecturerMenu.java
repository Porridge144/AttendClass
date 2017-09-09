package com.example.gszzz.attendclass;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class lecturerMenu extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    TextView timerText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Put here tentatively
        unregisterReceiver(broadcastReceiver2);
    }

    public void enterAttendanceChecking(View view){
        Intent i = new Intent(this, attendanceChecking.class);
        startActivity(i);
    }

    public void enterTimetableManagement(View view){
        Intent i =  new Intent(this, timetableManagement.class);
        startActivity(i);
    }

    public void bluetoothDiscoverableOnClicked(View view) {
        timerText = (TextView) findViewById(R.id.timerText);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device does not have BT capabilities!", Toast.LENGTH_SHORT).show();
        } else if(!bluetoothAdapter.isEnabled()){
            Toast.makeText(getApplicationContext(), "Enabling Bluetooth....", Toast.LENGTH_SHORT).show();
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(broadcastReceiver1, BTIntent);
        } else {
            onDiscoverability();
        }
    }

    public void onDiscoverability(){
        //Off Broadcast Receiver if it is on
        try {
            unregisterReceiver(broadcastReceiver1);
        } catch (Exception ignored){
        }
        Toast.makeText(getApplicationContext(), "Making device discoverable for 300 seconds...", Toast.LENGTH_SHORT).show();

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(broadcastReceiver2,intentFilter);
    }

    private final BroadcastReceiver broadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        Toast.makeText(getApplicationContext(), "Bluetooth has been Turned On.", Toast.LENGTH_LONG).show();
                        onDiscoverability();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Toast.makeText(getApplicationContext(), "Turning on bluetooth...", Toast.LENGTH_LONG).show();
                        break;
                }
                //What if failed to turn on??
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)){
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                switch (mode) {
                    //In Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Toast.makeText(getApplicationContext(), "Discoverable Enabled.", Toast.LENGTH_LONG).show();
                        new CountDownTimer(300000, 1000) {
                            public void onTick(long millisUntilFinished) {
                                String tmp = "" + millisUntilFinished / 1000;
                                timerText.setText(tmp);
                            }
                            public void onFinish() {
                                timerText.setText("");
                            }
                        }.start();
                        break;
                    //Device not in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Toast.makeText(getApplicationContext(), "Discoverable Disabled. Able to receive connections.", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Toast.makeText(getApplicationContext(), "Not able to receive connections", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Toast.makeText(getApplicationContext(), "Connecting.", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Toast.makeText(getApplicationContext(), "Connected.", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    };
}
