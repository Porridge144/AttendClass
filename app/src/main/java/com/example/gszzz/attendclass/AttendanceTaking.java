package com.example.gszzz.attendclass;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class AttendanceTaking extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_taking);
    }

    public void registerClassOnClicked(View view) {
        Intent i = new Intent(this, StudentRegisterForClass.class);
        startActivity(i);
    }

    public void attendOnClicked(View view) {
    }
}
