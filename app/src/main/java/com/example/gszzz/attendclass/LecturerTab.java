package com.example.gszzz.attendclass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.view.LayoutInflater;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LecturerTab extends Fragment implements OnClickListener {

    private EditText usernameText;
    private EditText passwordText;
    private Button lecLogin;
    private String username;
    private String password;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.lec_tab_main, container, false);
        usernameText = v.findViewById(R.id.lecIDInput);
        passwordText = v.findViewById(R.id.passwordInput);
        lecLogin = v.findViewById(R.id.loginButton);
        lecLogin.setOnClickListener(this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.loginButton:
                username = usernameText.getText().toString();
                password = passwordText.getText().toString();
                // Add below to enable the login function
//                String method = "login";
//                BackgroundTask1 backgroundTask1 = new BackgroundTask1(this);
//                backgroundTask1.execute(method, username, password);
                Intent i1 = new Intent(getActivity(), AttendanceChecking.class);
                startActivity(i1);
                break;
            default:
                break;
        }
    }
}