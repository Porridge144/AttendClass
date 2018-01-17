package com.example.gszzz.attendclass;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LecturerRegister extends AppCompatActivity {

    EditText nameText, usernameText, passwordText1, passwordText2;
    String name, username, password1, password2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_register);
        nameText = (EditText) findViewById(R.id.passwordText);
        usernameText = (EditText) findViewById(R.id.usernameText);
        passwordText1 = (EditText) findViewById(R.id.passwordText);
        passwordText2 = (EditText) findViewById(R.id.passwordText2);
    }

    public void registerOnClicked(View view) {
        name = nameText.getText().toString();
        username = usernameText.getText().toString();
        password1 = passwordText1.getText().toString();
        password2 = passwordText2.getText().toString();
        if (!password1.equals(password2)) {
            Toast.makeText(this, "Password and Password(confirm) are different! Please re-enter.", Toast.LENGTH_LONG).show();
            return;
        }
        String method = "register";
        BackgroundTask1 backgroundTask1 = new BackgroundTask1(this);
        backgroundTask1.execute(method, name, username, password1);
    }
}
