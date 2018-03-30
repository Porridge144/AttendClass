package com.example.gszzz.attendclass;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.gszzz.attendclass.server_interaction.BackgroundTaskRetrieveInfo;
import com.example.gszzz.attendclass.service_notification.AutoCheckService;

public class TimetableManagement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_management);
    }

    public void retrieveButtonOnclick(View view) {
        BackgroundTaskRetrieveInfo backgroundTaskRetrieveInfo = new BackgroundTaskRetrieveInfo(this);
        backgroundTaskRetrieveInfo.execute("query_class_list");

//        Intent pushIntent = new Intent(this, AutoCheckService.class);
//        startService(pushIntent);
    }
}
