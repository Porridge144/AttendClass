package com.example.gszzz.attendclass;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class StudentLogIn extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_log_in);
    }

    public void enterStudentRegister(View view){
        Intent i = new Intent(this, StudentRegister.class);
        startActivity(i);
    }

    public void enterAttendanceTaking(View view){
        Intent i = new Intent(this, AttendanceTaking.class);
        startActivity(i);
    }
}
