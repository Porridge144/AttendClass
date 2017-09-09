package com.example.gszzz.attendclass;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class AttendanceChecking extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_checking);
    }

    public void enterNameList(View view){
        Intent i = new Intent(this, NameList.class);
        startActivity(i);
    }

}
