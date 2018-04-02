package com.example.gszzz.attendclass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.gszzz.attendclass.server_interaction.BackgroundTaskRetrieveInfo;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SectionsPagerAdapter mAdapter;
    private ViewPager mViewPager;
    protected static String[] nameList;
    protected static String[] matricNum;
    protected static String className;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BackgroundTaskRetrieveInfo backgroundTaskRetrieveInfo = new BackgroundTaskRetrieveInfo(getApplicationContext());
        backgroundTaskRetrieveInfo.execute("query_class_list");

        IntentFilter filter = new IntentFilter("classDataReceived");
        registerReceiver(classDataReceiver, filter);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        setupViewPager(mViewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("classDataReceived");
        registerReceiver(classDataReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(classDataReceiver);
    }

    private void setupViewPager(ViewPager viewPager) {
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new StudentTab(), "Student");
        adapter.addFragment(new LecturerTab(), "Lecturer");
        viewPager.setAdapter(adapter);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    private final BroadcastReceiver classDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("classDataReceived")) {
                className = intent.getStringExtra("className");
                nameList = intent.getStringArrayExtra("nameList");
                if(className.equals("No Class For Now")){
                    Toast.makeText(getApplicationContext(),"No Class For Now", Toast.LENGTH_LONG).show();
                } else {
                    for (int i = 1; i < nameList.length; i++) {
                        // temp is the matric no being looped thru
                        matricNum[i] = (nameList[i].split(" "))[1];
                    }
                    Toast.makeText(getApplicationContext(),"Loading...", Toast.LENGTH_LONG).show();
                }
            }
        }
    };
}
