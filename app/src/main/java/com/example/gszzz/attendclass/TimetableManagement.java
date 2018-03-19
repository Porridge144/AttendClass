package com.example.gszzz.attendclass;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.gszzz.attendclass.server_interaction.BackgroundTaskRetrieveInfo;

public class TimetableManagement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_management);
    }

    public void retrieveButtonOnclick(View view) {
        BackgroundTaskRetrieveInfo backgroundTaskRetrieveInfo = new BackgroundTaskRetrieveInfo(this);
        backgroundTaskRetrieveInfo.execute("query_class_list");
    }
}
