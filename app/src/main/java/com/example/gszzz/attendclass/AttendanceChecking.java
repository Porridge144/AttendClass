package com.example.gszzz.attendclass;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gszzz.attendclass.server_interaction.BackgroundTaskRetrieveInfo;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AttendanceChecking extends AppCompatActivity{

    private BroadcastReceiver scanningFailureReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView totalNumTextView;
    private TextView timerTextView;
    private TextView moduleInfoTextView;
    private ListView listView;
    private ArrayAdapter listAdapter;
    private ArrayList<ScanResult> scanResults;
    private static ArrayList<String> names;
    private static ArrayList<String> absentNames;
    private BitSet bitmap00 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private BitSet bitmap01 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private BitSet bitmap10 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private BitSet bitmap11 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private BitSet relayedBitmap = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    private BitSet temp = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    private int currentIndex;
    private int presentStuNumber = 0;
    private long startTime = 0;
    private static final String TAG = "AttendanceChecking";

    // Create the Handler object (on the main thread by default)
    final Handler handler = new Handler();
    // Define the code block to be executed
    final Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
//            Log.i("Handlers", "Called on main thread");
            CountDownTimer countDownTimer = new AttendanceChecking.CDT(this, handler);
//            Log.i("Handlers", "CDT Start");
            countDownTimer.start();
        }
    };

    private class CDT extends CountDownTimer {
        private Runnable e;
        private Handler h;
        CDT(Runnable ext, Handler han){
            super(Constants.CLASS_DURATION*60*1000, 1000);
            this.e = ext;
            this.h = han;
        }

        @Override
        public void onTick(long millisUntilFinish) {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            minutes = minutes % 60;
            seconds = seconds % 60;
            double range = 0.5;
            double secondsUntilFinish = (millisUntilFinish / 1000.0);

            timerTextView.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

            if (secondsUntilFinish%2 < 1 + range && secondsUntilFinish%2 > 1 - range) {
                if(names.size()==0) {
                    Toast.makeText(getApplicationContext(), "No students enrolled", Toast.LENGTH_LONG).show();
                    stopScanning();
                }
                else{
                    presentStuNumber = bitmap00.get(2,Constants.MAX_NUMBER_OF_BITS).cardinality() + bitmap01.get(2,Constants.MAX_NUMBER_OF_BITS).cardinality() + bitmap10.get(2,Constants.MAX_NUMBER_OF_BITS).cardinality() + bitmap11.get(2,Constants.MAX_NUMBER_OF_BITS).cardinality();
                    // save index and top position
                    int index = listView.getFirstVisiblePosition(); //This changed
                    View v = listView.getChildAt(0);
                    int top = (v == null) ? 0 : v.getTop(); //this changed
                    // notify dataset changed or re-assign adapter here
//                    listAdapter.notifyDataSetChanged();
                    listView.setAdapter(listAdapter);
                    // restore the position of listview
                    listAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, absentNames);
                    listView.setSelectionFromTop(index, top);
                    totalNumTextView.setText(String.format("%d/%d", presentStuNumber, names.size()));
                    if (presentStuNumber == names.size()){
                        Toast.makeText(getApplicationContext(), "All students here!", Toast.LENGTH_LONG).show();
                        onFinish();
                    }
                }
            }
        }
        @Override
        public void onFinish() {
            stopScanning();
            h.post(e);
            Log.i("Handlers", "Finish");
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_checking);

        scanResults = new ArrayList<>();
        names = new ArrayList<>();
        absentNames = new ArrayList<>();
        totalNumTextView = findViewById(R.id.totalNumTextView);
        timerTextView = findViewById(R.id.textView6);
        listView = findViewById(R.id.devicesListView);
        moduleInfoTextView = findViewById(R.id.moduleInfo);
//        listAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, absentNames);
//        listView.setAdapter(listAdapter);

        IntentFilter filter = new IntentFilter(ScannerService.NEW_DEVICE_FOUND);
        registerReceiver(scanResultsReceiver, filter);
        IntentFilter filter1 = new IntentFilter("classDataReceived");
        registerReceiver(classDataReceiver, filter1);

        // set page number
        bitmap01.set(1);
        bitmap10.set(0);
        bitmap11.set(0);
        bitmap11.set(1);

        scanningFailureReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int errorCode = intent.getIntExtra(ScannerService.SCANNING_FAILED_EXTRA_CODE, -1);

                String errorMessage = getString(R.string.start_error_prefix);
                switch (errorCode) {
                    case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                        errorMessage += " " + getString(R.string.start_error_already_started);
                        break;
                    case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage += " " + getString(R.string.start_error_unsupported);
                        break;
                    case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                        errorMessage += " " + getString(R.string.start_error_internal);
                        break;
                    case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                        errorMessage += " " + getString(R.string.start_error_registration_failed);
                        break;
                    case ScannerService.SCANNING_TIMED_OUT:
                        errorMessage = " " + getString(R.string.scanning_time_out);
                        break;
                    default:
                        errorMessage += " " + getString(R.string.start_error_unknown);
                }
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        };

        if (savedInstanceState == null) {

            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter();

            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {

                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {

                    // Are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        // Everything is supported and enabled
                        checkBTPermissions();
//                        names.add("Arnold");
//                        names.add("Bernard");
//                        names.add("Ceisei");
//                        names.add("Dolores");
//                        names.add("Edward");
//                        names.add("Frank");
//                        names.add("Goth");
//                        names.add("Hepper");
//                        names.add("Inn");
                        BackgroundTaskRetrieveInfo backgroundTaskRetrieveInfo = new BackgroundTaskRetrieveInfo(getApplicationContext());
                        backgroundTaskRetrieveInfo.execute("query_class_list");
                    } else {

                        // Bluetooth Advertisements are not supported.
                        Toast.makeText(getApplicationContext(), R.string.bluetooth_advertisment_not_supported, Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {

                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            } else {

                // Bluetooth is not supported.
                Toast.makeText(getApplicationContext(), R.string.bluetooth_not_supported, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:

                if (resultCode == RESULT_OK) {

                    // Bluetooth is now Enabled, are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        // Everything is supported and enabled
                        checkBTPermissions();
                        BackgroundTaskRetrieveInfo backgroundTaskRetrieveInfo = new BackgroundTaskRetrieveInfo(getApplicationContext());
                        backgroundTaskRetrieveInfo.execute("query_class_list");
                    } else {

                        // Bluetooth Advertisements are not supported.
                        Toast.makeText(getApplicationContext(), R.string.bluetooth_advertisment_not_supported, Toast.LENGTH_LONG).show();
                    }
                } else {

                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, R.string.user_declined_on_bluetooth,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startScanning() {
        Context c = getApplicationContext();
        c.startService(getScannerServiceIntent(c));
    }

    private void stopScanning() {
        Context c = getApplicationContext();
        c.stopService(getScannerServiceIntent(c));
    }

    private static Intent getScannerServiceIntent(Context c) {
        Intent intent = new Intent(c, ScannerService.class);
        return intent;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scanResultsReceiver);
        handler.removeCallbacks(runnableCode);
        Log.i(TAG, "activity destroyed");
        if (ScannerService.running) {
            stopScanning();
        }
    }

    /**
     * When app comes on screen, check if BLE Advertisements are running, set switch accordingly,
     * and register the Receiver to be notified if Advertising fails.
     */
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ScannerService.SCANNING_FAILED);
        registerReceiver(scanningFailureReceiver, intentFilter);
        Log.i(TAG, "activity resumed");
    }

    /**
     * When app goes off screen, unregister the Advertising failure Receiver to stop memory leaks.
     * (and because the app doesn't care if Advertising fails while the UI isn't active)
     */
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(scanningFailureReceiver);
        Log.i(TAG, "activity paused");
    }

    public void enterNameList(View view) {
        Intent intent = new Intent(this, NameList.class);
        startActivity(intent);
    }

    //Check for permission to discover other devices
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        } else {
            Toast.makeText(getApplicationContext(), "SDK version < LOLIPOP. No need permission check.", Toast.LENGTH_LONG).show();
        }
    }

    private void countNames(){
        absentNames.clear();
        Log.i("names in method: ", names.toString());
        // make sure "0"s beyond namelist size are not counted as absent
        for(int i=0;names.size()>0 && i<Math.min(names.size(),Constants.MAX_NUMBER_OF_BITS-2);i++){
            if(!bitmap00.get(i+2)){
                absentNames.add(names.get(i));
            }
        }
        for(int i=0;names.size()>(Constants.MAX_NUMBER_OF_BITS-2) && i<Math.min(names.size()-(Constants.MAX_NUMBER_OF_BITS-2),Constants.MAX_NUMBER_OF_BITS-2);i++){
            if(!bitmap01.get(i+2)){
                absentNames.add(names.get(i+(Constants.MAX_NUMBER_OF_BITS-2)));
            }
        }
        for(int i=0;names.size()>2*(Constants.MAX_NUMBER_OF_BITS-2) && i<Math.min(names.size()-2*(Constants.MAX_NUMBER_OF_BITS-2),Constants.MAX_NUMBER_OF_BITS-2);i++){
            if(!bitmap10.get(i+2)){
                absentNames.add(names.get(i+(Constants.MAX_NUMBER_OF_BITS-2)*2));
            }
        }
        for(int i=0;names.size()>3*(Constants.MAX_NUMBER_OF_BITS-2) && i<Math.min(names.size()-3*(Constants.MAX_NUMBER_OF_BITS-2),Constants.MAX_NUMBER_OF_BITS-2);i++){
            if(!bitmap11.get(i+2)){
                absentNames.add(names.get(i+(Constants.MAX_NUMBER_OF_BITS-2)*3));
            }
        }
        Log.i("absent names: ", absentNames.toString());
    }

    private final BroadcastReceiver scanResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ScannerService.NEW_DEVICE_FOUND)) {
                try {
                    scanResults = intent.getParcelableArrayListExtra(ScannerService.PARCELABLE_SCANRESULTS);
                    currentIndex = scanResults.size() - 1 ;
                    List<ParcelUuid> uuidData = scanResults.get(currentIndex).getScanRecord().getServiceUuids();
                    byte[] receivedData = scanResults.get(currentIndex).getScanRecord().getServiceData().get(uuidData.get(0));

//                    Log.i(TAG, Integer.toString(scanResults.size()));
                    // set the relayed bitmap as the value of received data
                    relayedBitmap.clear();
                    temp.clear();
                    relayedBitmap.or(BitSet.valueOf(receivedData));
                    temp.or(relayedBitmap.get(2, Constants.MAX_NUMBER_OF_BITS));

                    // check the page number
                    if (!relayedBitmap.get(0) && !relayedBitmap.get(1)) {
                        // 00 = false false
                        temp.xor(bitmap00.get(2, Constants.MAX_NUMBER_OF_BITS));
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                        } else {
                            bitmap00.or(relayedBitmap);
                        }
                    } else if (!relayedBitmap.get(0) && relayedBitmap.get(1)) {
                        // 01 = false true
                        temp.xor(bitmap01.get(2, Constants.MAX_NUMBER_OF_BITS));
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                        } else {
                            bitmap01.or(relayedBitmap);
                        }
                    } else if (relayedBitmap.get(0) && !relayedBitmap.get(1)) {
                        // 10 = true false
                        temp.xor(bitmap10.get(2, Constants.MAX_NUMBER_OF_BITS));
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                        } else {
                            bitmap10.or(relayedBitmap);
                        }
                    } else if (relayedBitmap.get(0) && relayedBitmap.get(1)) {
                        // 11 = true true
                        temp.xor(bitmap11.get(2, Constants.MAX_NUMBER_OF_BITS));
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                        } else {
                            bitmap11.or(relayedBitmap);
                        }
                    }
                    countNames();
                    String totalNumber = " " + scanResults.size() + " ";
                } catch (Resources.NotFoundException e) {
                    Toast.makeText(getApplicationContext(), "AttendanceChecking: NotFoundException...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "AttendanceChecking: " + e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private final BroadcastReceiver classDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("classDataReceived")){
                String className = intent.getStringExtra("className");
                String[] nameList = intent.getStringArrayExtra("nameList");
                moduleInfoTextView.setText(String.format("Welcome to %s", className));
                for(int i=1;i<nameList.length;i++){
                    names.add((nameList[i].split(" "))[0]);
                }
                Log.i("names", names.toString());
                absentNames = (ArrayList<String>)names.clone();
                Log.i("00: ", bitmap00.toString());
                Log.i("01: ", bitmap01.toString());
                Log.i("10: ", bitmap10.toString());
                Log.i("11: ", bitmap11.toString());
                countNames();

                Log.i("absent names: ", absentNames.toString());

                startTime = System.currentTimeMillis();
                handler.post(runnableCode);
                if(nameList.length>1)
                    startScanning();
                else
                    totalNumTextView.setText("No students retrieved in this class");
            }
        }
    };
}
