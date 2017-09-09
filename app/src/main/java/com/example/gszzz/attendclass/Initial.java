package com.example.gszzz.attendclass;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class Initial extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);
    }

    public void enterStudentInterface(View view){
        Intent i = new Intent(this, StudentLogIn.class);
        startActivity(i);
    }

    public void enterLecturerInterface(View view){
        Intent i = new Intent(this, LecturerLogIn.class);
        startActivity(i);
    }
}
