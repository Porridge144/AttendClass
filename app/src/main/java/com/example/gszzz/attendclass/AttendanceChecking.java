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
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AttendanceChecking extends AppCompatActivity{

    private BroadcastReceiver scanningFailureReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView totalNumTextView;
    private TextView bitmapTextView;
    private TextView timerTextView;
    private ArrayList<ScanResult> scanResults;
    private BitSet bitmap00 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private BitSet bitmap01 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private BitSet bitmap10 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private BitSet bitmap11 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private BitSet relayedBitmap = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    private BitSet temp = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    private static int powerLevel = 2;
    private int currentIndex;
    private int presentStuNumber = 0;
    private int[] rssi = new int[10000];
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

            if (secondsUntilFinish%2 < 1 + range && secondsUntilFinish%2 > 1 - range && presentStuNumber!=Constants.STUDENTS) {
                presentStuNumber = bitmap00.get(2,Constants.MAX_NUMBER_OF_BITS).cardinality() + bitmap01.get(2,Constants.MAX_NUMBER_OF_BITS).cardinality() + bitmap10.get(2,Constants.MAX_NUMBER_OF_BITS).cardinality() + bitmap11.get(2,Constants.MAX_NUMBER_OF_BITS).cardinality();
                bitmapTextView.setText(String.format("Absent Student Number: %d", Constants.STUDENTS - presentStuNumber));
            }
            if (presentStuNumber == Constants.STUDENTS){
                Toast.makeText(getApplicationContext(), "All students here!", Toast.LENGTH_LONG).show();
                secondsUntilFinish = 0;
            }
//
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
        totalNumTextView = findViewById(R.id.totalNumTextView);
        timerTextView = findViewById(R.id.textView6);
        bitmapTextView = findViewById(R.id.textView5);

//        IntentFilter filter = new IntentFilter(ScannerService.NEW_DEVICE_FOUND);
//        registerReceiver(scanResultsReceiver, filter);

        // set page number
        bitmap01.set(1);
        bitmap10.set(0);
        bitmap11.set(0);
        bitmap11.set(1);

        // format: Fri Mar 30 14:45:12 GMT+08:00 2018
        Date currentTime = Calendar.getInstance().getTime();
        Log.i("date: ", currentTime.toString());


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
                        IntentFilter filter = new IntentFilter(ScannerService.NEW_DEVICE_FOUND);
                        registerReceiver(scanResultsReceiver, filter);
                        startTime = System.currentTimeMillis();
                        handler.post(runnableCode);
                        startScanning();
//                        Log.i(TAG, "after start scanning");
//                        Toast.makeText(getApplicationContext(), "Start Scanning!!!", Toast.LENGTH_LONG).show();
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
                        IntentFilter filter = new IntentFilter(ScannerService.NEW_DEVICE_FOUND);
                        registerReceiver(scanResultsReceiver, filter);
                        startTime = System.currentTimeMillis();
                        handler.post(runnableCode);
                        //Start service
                        startScanning();
//                        Log.i(TAG, "after start scanning");
//                        Toast.makeText(getApplicationContext(), "Start Scanning!!!", Toast.LENGTH_LONG).show();
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
        intent.putExtra("power", powerLevel);
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

    public void lowFreqOnClicked(View view){
        stopScanning();
        powerLevel = 0;
        startScanning();
    }

    public void balancedOnClicked(View view){
        stopScanning();
        powerLevel = 1;
        startScanning();
    }

    public void highFreqOnClicked(View view){
        stopScanning();
        powerLevel = 2;
        startScanning();
    }

    public void stopScanningOnClicked(View view) {
//        if (ScannerService.running) {
//            Toast.makeText(getApplicationContext(), "Is running... Stopping the service...", Toast.LENGTH_SHORT).show();
//            stopScanning();
//        }
        int i;
        int averageRxPower = 0;
        for(i=0; i<currentIndex; i++){
            averageRxPower += rssi[i];
        }
        averageRxPower = averageRxPower /i;
        Toast.makeText(getApplicationContext(), "Ave Rx power is: " + Integer.toString(averageRxPower), Toast.LENGTH_SHORT).show();
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

    private final BroadcastReceiver scanResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ScannerService.NEW_DEVICE_FOUND)) {
                try {
                    scanResults = intent.getParcelableArrayListExtra(ScannerService.PARCELABLE_SCANRESULTS);
                    currentIndex = scanResults.size() - 1 ;
                    List<ParcelUuid> uuidData = scanResults.get(currentIndex).getScanRecord().getServiceUuids();
                    byte[] receivedData = scanResults.get(currentIndex).getScanRecord().getServiceData().get(uuidData.get(0));
                    rssi[currentIndex] = scanResults.get(currentIndex).getRssi();

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
                            Log.i(TAG, "relayed bitmap empty");
                        } else {
                            bitmap00.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 0");
                        }
                    } else if (!relayedBitmap.get(0) && relayedBitmap.get(1)) {
                        // 01 = false true
                        temp.xor(bitmap01.get(2, Constants.MAX_NUMBER_OF_BITS));
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                            Log.i(TAG, "relayed bitmap empty");
                        } else {
                            bitmap01.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 1");
                        }
                    } else if (relayedBitmap.get(0) && !relayedBitmap.get(1)) {
                        // 10 = true false
                        temp.xor(bitmap10.get(2, Constants.MAX_NUMBER_OF_BITS));
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                            Log.i(TAG, "relayed bitmap empty");
                        } else {
                            bitmap10.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 2");
                        }
                    } else if (relayedBitmap.get(0) && relayedBitmap.get(1)) {
                        // 11 = true true
                        temp.xor(bitmap11.get(2, Constants.MAX_NUMBER_OF_BITS));
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                            Log.i(TAG, "relayed bitmap empty");
                        } else {
                            bitmap11.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 3");
                        }
                    }
                    String totalNumber = " " + scanResults.size() + " ";
                    totalNumTextView.setText(Integer.toString(currentIndex+1));
                } catch (Resources.NotFoundException e) {
                    Toast.makeText(getApplicationContext(), "AttendanceChecking: NotFoundException...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "AttendanceChecking: " + e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
}
