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

public class LecturerMenu extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void enterAttendanceChecking(View view){
        Intent i = new Intent(this, AttendanceChecking.class);
        startActivity(i);
    }

    public void enterTimetableManagement(View view){
        Intent i =  new Intent(this, TimetableManagement.class);
        startActivity(i);
    }


}
