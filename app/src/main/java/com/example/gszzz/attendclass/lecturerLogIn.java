package com.example.gszzz.attendclass;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class lecturerLogIn extends AppCompatActivity {

    EditText usernameText, passwordText;
    String username, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_log_in);
        usernameText = (EditText) findViewById(R.id.usernameText);
        passwordText = (EditText) findViewById(R.id.passwordText);
    }

    public void enterLecturerRegister(View view){
        Intent i = new Intent(this, lecturerRegister.class);
        startActivity(i);
    }

    public void enterLecturerMenu(View view){
        //Perform login check here
        username = usernameText.getText().toString();
        password = passwordText.getText().toString();
        String method = "login";
        BackgroundTask1 backgroundTask1 = new BackgroundTask1(this);
        backgroundTask1.execute(method, username, password);

    }
}
