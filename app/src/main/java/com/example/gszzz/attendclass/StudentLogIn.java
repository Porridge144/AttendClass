package com.example.gszzz.attendclass;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class StudentLogIn extends AppCompatActivity {

    EditText usernameText, passwordText;
    String username, password;

    public static String globalUsername = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_log_in);
        usernameText = (EditText) findViewById(R.id.usernameText);
        passwordText = (EditText) findViewById(R.id.passwordText);
    }

    public void enterStudentRegister(View view){
        Intent i = new Intent(this, StudentRegister.class);
        startActivity(i);
    }

    public void enterAttendanceTaking(View view){
        username = usernameText.getText().toString();
        globalUsername = username;
        password = passwordText.getText().toString();
        String method = "login";
        Intent i = new Intent(this, AttendanceTaking.class);
        startActivity(i);
    }
}
