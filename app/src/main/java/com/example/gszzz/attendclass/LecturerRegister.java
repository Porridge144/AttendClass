package com.example.gszzz.attendclass;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LecturerRegister extends AppCompatActivity {

    EditText nameText, usernameText, passwordText1;
    String name, username, password1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_register);
        nameText = findViewById(R.id.passwordText);
        usernameText =  findViewById(R.id.usernameText);
        passwordText1 =  findViewById(R.id.passwordText);
    }

    public void registerOnClicked(View view) {
        name = nameText.getText().toString();
        username = usernameText.getText().toString();
        password1 = passwordText1.getText().toString();
        String method = "register";
        BackgroundTask1 backgroundTask1 = new BackgroundTask1(this);
        backgroundTask1.execute(method, name, username, password1);
    }
}
