package com.example.gszzz.attendclass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
    protected static String[] nameList;
    protected static String className = "Null";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.stu_tab_main, container, false);
        usernameText = v.findViewById(R.id.matricNumInput);
        stuLogin = v.findViewById(R.id.loginButton);
        stuLogin.setOnClickListener(this);
        return v;
    }

    private void storeUsername(String username) {
        SharedPreferences mspref = this.getActivity().getSharedPreferences("stuUsername", Context.MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mspref.edit();
        mEditor.putString("username", username);
        mEditor.apply();
    }

    private void storeNamelist(String[] namelist) {
        SharedPreferences mspref = this.getActivity().getSharedPreferences("stuNamelist", Context.MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mspref.edit();
        StringBuilder s = new StringBuilder();
        for(int i=0;i< namelist.length;i++){
            s.append(namelist[i]).append(",");
        }
        mEditor.putString("namelist", s.toString());
        mEditor.apply();
    }

    private String getUsername() {
        SharedPreferences mspref = this.getActivity().getSharedPreferences("stuUsername", Context.MODE_PRIVATE);
        return mspref.getString("username", "");
    }

    private String getNamelist() {
        SharedPreferences mspref = this.getActivity().getSharedPreferences("stuNamelist", Context.MODE_PRIVATE);
        return mspref.getString("namelist", "none,");
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("classDataReceived");
        getActivity().registerReceiver(classDataReceiver, filter);
        // check the username stored in share preferences
        if(!getUsername().equals("")) {
            username = getUsername();
            String[] s = getNamelist().split(",");
            if(!s[0].equals("none")) {
                for (int i = 1; i < s.length; i++) {
                    if ((s[i].split(" ")[1]).equals(username)) {
                        Intent intent = new Intent(getActivity(), AttendanceTaking.class);
                        intent.putExtra("username", username);
                        intent.putExtra("nameList", s);
                        startActivity(intent);
                        break;
                    }
                }
            }
        }
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
                // store the username into shared preferences
                storeUsername(username);
                Log.i("class name: ", className);
                if(className.equals("No Class For Now")) {
                    Toast.makeText(getActivity(), "No Class For Now", Toast.LENGTH_LONG).show();
                }
                else {
                    for (int i = 1; !(nameList == null) && i < nameList.length; i++) {
                        if ((nameList[i].split(" ")[1]).equals(username)) {
                            Intent intent = new Intent(getActivity(), AttendanceTaking.class);
                            intent.putExtra("username", username);
                            intent.putExtra("nameList", nameList);
                            startActivity(intent);
                            break;
                        }
                    }
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
                storeNamelist(nameList);
            }
        }
    };
}

