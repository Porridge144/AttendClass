package com.example.gszzz.attendclass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gszzz.attendclass.server_interaction.BackgroundTaskRetrieveInfo;

public class StudentTab extends Fragment implements OnClickListener {

    private EditText usernameText;
    private Button stuLogin;
    private String username;
    public static String globalUsername = "";
    protected static String[] nameList;
    protected static String className;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.stu_tab_main, container, false);
        usernameText = v.findViewById(R.id.matricNumInput);
        stuLogin = v.findViewById(R.id.loginButton);
        stuLogin.setOnClickListener(this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("classDataReceived");
        getActivity().registerReceiver(classDataReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(classDataReceiver);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.loginButton:
                username = usernameText.getText().toString();
                Log.i("class name: ", className);
                if(className.equals("No Class For Now")) {
                    Toast.makeText(getActivity(), "No Class For Now", Toast.LENGTH_LONG).show();
                }
                else {
                    for (int i = 1; !(nameList == null) && i < nameList.length; i++) {
                        if ((nameList[i].split(" ")[1]).equals(username)) {
                            globalUsername = username;
                            Intent intent = new Intent(getActivity(), AttendanceTaking.class);
                            startActivity(intent);
                            break;
                        }
                    }
                    Toast.makeText(getActivity(), "You're in wrong class!", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private final BroadcastReceiver classDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("classDataReceived")) {
                className = intent.getStringExtra("className");
                nameList = intent.getStringArrayExtra("nameList");
                if(className.equals("No Class For Now")){
                    Toast.makeText(getActivity(),"No Class For Now", Toast.LENGTH_LONG).show();
                } else {
                    Log.i("class name: ", className);
                    Toast.makeText(getActivity(),"Loading...", Toast.LENGTH_LONG).show();
                }
            }
        }
    };
}

