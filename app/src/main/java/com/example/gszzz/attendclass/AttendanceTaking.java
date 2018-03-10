package com.example.gszzz.attendclass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
//import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.AdvertiseCallback;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.BitSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
//import java.util.concurrent.ExecutionException;

public class AttendanceTaking extends AppCompatActivity {

    EditText setLabel;
    private static final String TAG = "AttendanceTaking";
    private BroadcastReceiver advertisingFailureReceiver;
    private BroadcastReceiver scanningFailureReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView labelTextView;
    private TextView moduleLocationTextView;
    private TextView dataTextView;
    private ArrayList<ScanResult> scanResults;
    public boolean restIndicator = false;
    public static BitSet bitmap0 = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    public static BitSet bitmap1 = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    public static BitSet bitmap2 = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    public static BitSet bitmap3 = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    public static BitSet relayedBitmap = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    public static BitSet temp = new BitSet();
    public static int currentIndex = 0;
    long startTime = 0;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            labelTextView.setText(String.format("%d:%02d", minutes, seconds));

            timerHandler.postDelayed(this, 500);
        }
    };

    // Create the Handler object (on the main thread by default)
    final Handler handler = new Handler();
    // Define the code block to be executed
    final Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            Log.i("Handlers", "Called on main thread");
            CountDownTimer countDownTimer = new AttendanceTaking.CDT(this, handler);
            Log.i("Handlers", "CDT Start");
            countDownTimer.start();
        }
    };

    private class CDT extends CountDownTimer {
        private Runnable e;
        private Handler h;
        boolean advertisePeriodEnd = false;
        CDT(Runnable ext, Handler han){
            super(30*1000, 3000);
            this.e = ext;
            this.h = han;
        }

        @Override
        public void onTick(long millisUntilFinish) {
            double range = 0.2;
            double secondsUntilFinish = (millisUntilFinish / 1000.0);

            // no difference in bitmap; can rest
            if (restIndicator){
                Log.i(TAG,"onTick scan");
                if (AdvertiserService.running){
                    stopAdvertising();
                }
                // if the scanner service is not running yet
                if (!ScannerService.running){
                    taskScan();
                }
            }
            // there is difference in bitmap; need to advertise
            else{
                if (( secondsUntilFinish < 27.0 + range) & (secondsUntilFinish > 27.0 - range)) {
                    advertisePeriodEnd = false;
                    taskPage00();
                } else if (( secondsUntilFinish < 24.0 + range) & (secondsUntilFinish > 24.0 - range)) {
                    advertisePeriodEnd = false;
                    taskPage10();
                } else if (( secondsUntilFinish < 21.0 + range) & (secondsUntilFinish > 21.0 - range)) {
                    advertisePeriodEnd = false;
                    taskPage01();
                } else if (( secondsUntilFinish < 18.0 + range) & (secondsUntilFinish > 18.0 - range)) {
                    Log.i(TAG,"here");
                    taskPage11();
                    advertisePeriodEnd = true;
                } else if (advertisePeriodEnd){
                    if (!ScannerService.running){
                        taskScan();
                        randomizeBitmaps();
                    }
                }
            }
        }
        @Override
        public void onFinish() {
            h.post(e);
//            Log.i("Handlers", "Finish");
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_taking);

        labelTextView = findViewById(R.id.textView4);
        dataTextView = findViewById(R.id.textView3);
        moduleLocationTextView = findViewById(R.id.moduleLocation);

        //set page number
        bitmap1.set(0);
        bitmap2.set(1);
        bitmap3.set(0);
        bitmap3.set(1);

        randomizeBitmaps();

        scanResults = new ArrayList<>();
        IntentFilter filter = new IntentFilter(ScannerService.NEW_DEVICE_FOUND);
        registerReceiver(scanResultsReceiver, filter);

        advertisingFailureReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int errorCode = intent.getIntExtra(AdvertiserService.ADVERTISING_FAILED_EXTRA_CODE, -1);

                String errorMessage = getString(R.string.start_error_prefix);
                switch (errorCode) {
                    case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                        errorMessage += " " + getString(R.string.start_error_already_started);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                        errorMessage += " " + getString(R.string.start_error_too_large);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage += " " + getString(R.string.start_error_unsupported);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                        errorMessage += " " + getString(R.string.start_error_internal);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                        errorMessage += " " + getString(R.string.start_error_too_many);
                        break;
                    case AdvertiserService.ADVERTISING_TIMED_OUT:
                        errorMessage = " " + getString(R.string.advertising_timedout);
                        break;
                    default:
                        errorMessage += " " + getString(R.string.start_error_unknown);
                }

                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        };

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

            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {

                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {

                    // Are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        // Everything is supported and enabled...
                        checkBTPermissions();

                        startTime = System.currentTimeMillis();
                        timerHandler.post(timerRunnable);
                        handler.post(runnableCode);

                    } else {

                        // Bluetooth Advertisements are not supported.
                        Toast.makeText(getApplicationContext(), R.string.bluetooth_advertisment_not_supported, Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            } else {

                // Bluetooth is not supported.
                Toast.makeText(getApplicationContext(), R.string.bluetooth_not_supported, Toast.LENGTH_LONG).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:

                if (resultCode == RESULT_OK) {

                    // Bluetooth is now Enabled, are Bluetooth Advertisements supported on
                    // this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        // Everything is supported and enabled...
                        checkBTPermissions();

                        startTime = System.currentTimeMillis();
                        timerHandler.post(timerRunnable);
                        handler.post(runnableCode);
                    } else {

                        // Bluetooth Advertisements are not supported.
                        Toast.makeText(getApplicationContext(), R.string.bluetooth_not_supported, Toast.LENGTH_LONG).show();
                    }
                } else {

                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(getApplicationContext(), R.string.user_declined_on_bluetooth, Toast.LENGTH_LONG).show();
                    finish();
                }

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

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

    /**
     * When app comes on screen, check if BLE Advertisements are running, set switch accordingly,
     * and register the Receiver to be notified if Advertising fails.
     */
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(AdvertiserService.ADVERTISING_FAILED);
        registerReceiver(advertisingFailureReceiver, intentFilter);
        registerReceiver(scanningFailureReceiver, intentFilter);
    }

    /**
     * When app goes off screen, unregister the Advertising failure Receiver to stop memory leaks.
     * (and because the app doesn't care if Advertising fails while the UI isn't active)
     */
    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        unregisterReceiver(advertisingFailureReceiver);
        unregisterReceiver(scanningFailureReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scanResultsReceiver);
        unregisterReceiver(advertisingFailureReceiver);
        unregisterReceiver(scanningFailureReceiver);
        timerHandler.removeCallbacks(timerRunnable);
        if (ScannerService.running) {
            stopScanning();
        }
        if (AdvertiserService.running){
            stopAdvertising();
        }
    }

    private void taskPage00(){
        Log.i("Handlers", "taskPage00");
        if(ScannerService.running) {
            stopScanning();
        }
        if(AdvertiserService.running){
            stopAdvertising();
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
            s.append(bitmap0.get(i) ? "1" : "0");
        }
        startAdvertising(s.substring(0,2));
        moduleLocationTextView.setText(String.format("Now is advertising page %s", s.substring(0, 2)));
        dataTextView.setText(s.substring(2, Constants.MAX_NUMBER_OF_BITS));

        Log.i(TAG,"page 00");
    }

    private void taskPage10(){
//        Log.i("Handlers", "taskPage10");
        if(ScannerService.running) {
            stopScanning();
        }
        if(AdvertiserService.running){
            stopAdvertising();
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
            s.append(bitmap1.get(i) ? "1" : "0");
        }
        startAdvertising(s.substring(0,2));
        moduleLocationTextView.setText(String.format("Now is advertising page %s", s.substring(0, 2)));
        dataTextView.setText(s.substring(2, Constants.MAX_NUMBER_OF_BITS));
        Log.i(TAG,"page 10");
    }

    private void taskPage01(){
//        Log.i("Handlers", "taskPage01");
        if(ScannerService.running) {
            stopScanning();
        }
        if(AdvertiserService.running){
            stopAdvertising();
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
            s.append(bitmap2.get(i) ? "1" : "0");
        }
        startAdvertising(s.substring(0,2));
        moduleLocationTextView.setText(String.format("Now is advertising page %s", s.substring(0, 2)));
        dataTextView.setText(s.substring(2, Constants.MAX_NUMBER_OF_BITS));
        Log.i(TAG,"page 01");
    }

    private void taskPage11(){
//        Log.i("Handlers", "taskPage11");
        if(ScannerService.running) {
            stopScanning();
        }
        if(AdvertiserService.running){
            stopAdvertising();
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
            s.append(bitmap3.get(i) ? "1" : "0");
        }
        startAdvertising(s.substring(0,2));
        moduleLocationTextView.setText(String.format("Now is advertising page %s", s.substring(0, 2)));
        dataTextView.setText(s.substring(2, Constants.MAX_NUMBER_OF_BITS));
        Log.i(TAG,"page 11");
    }

    private void taskScan(){
//        Log.i("Handlers", "taskScan");
        if(AdvertiserService.running){
            stopAdvertising();
        }
        if(!ScannerService.running) {
            startScanning();
        }
        moduleLocationTextView.setText("Now is scanning");
        dataTextView.setText("Not advertising...");
        Log.i(TAG,"scan");
    }


    private void startAdvertising(String pageNumber) {
        Context c = getApplicationContext();
        c.startService(getAdvertiseServiceIntent(c, pageNumber));
    }

    private void startAdvertising() {
        Context c = getApplicationContext();
        c.startService(getAdvertiseServiceIntent(c));
    }

    private void stopAdvertising() {
        Context c = getApplicationContext();
        c.stopService(getAdvertiseServiceIntent(c));
    }

    private void startScanning() {
        Context c = getApplicationContext();
        c.startService(getScannerServiceIntent(c));
    }

    private void stopScanning() {
        Context c = getApplicationContext();
        c.stopService(getScannerServiceIntent(c));
    }

    /**
     * Returns Intent addressed to the {@code AdvertiserService} class.
     */
    private static Intent getAdvertiseServiceIntent(Context c, String pageNumber) {
        Intent intent = new Intent(c, AdvertiserService.class);
        //TODO: Use this to send info to Advertise Service
        intent.putExtra("Instruction", pageNumber);
        intent.putExtra("bitmap0", bitmap0.toByteArray());
        intent.putExtra("bitmap1", bitmap1.toByteArray());
        intent.putExtra("bitmap2", bitmap2.toByteArray());
        intent.putExtra("bitmap3", bitmap3.toByteArray());
        return intent;
    }

    private static Intent getAdvertiseServiceIntent(Context c) {
        Log.i(TAG, "getAdvertiseServiceIntent(Context c) ");
        //TODO: Use this to send info to Advertise Service
        return new Intent(c, AdvertiserService.class);
    }

    private static Intent getScannerServiceIntent(Context c) {
        //TODO: Use this to send info to Scanner Service
//        intent.putExtra("message", "Put message here!!");
        return new Intent(c, ScannerService.class);
    }

    public static void randomizeBitmaps(){
//        bitmap0.clear(2,Constants.MAX_NUMBER_OF_BITS);
//        bitmap1.clear(2,Constants.MAX_NUMBER_OF_BITS);
//        bitmap2.clear(2,Constants.MAX_NUMBER_OF_BITS);
//        bitmap3.clear(2,Constants.MAX_NUMBER_OF_BITS);
        int randomPage = ThreadLocalRandom.current().nextInt(0, 4);
        int randomNum = ThreadLocalRandom.current().nextInt(2, Constants.MAX_NUMBER_OF_BITS);
        switch (randomPage) {
            case (0):
                bitmap0.set(randomNum);
                break;
            case (1):
                bitmap1.set(randomNum);
                break;
            case (2):
                bitmap2.set(randomNum);
                break;
            case (3):
                bitmap3.set(randomNum);
                break;
            default:
        }
    }

    private final BroadcastReceiver scanResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ScannerService.NEW_DEVICE_FOUND)) {
                try {
                    scanResults = intent.getParcelableArrayListExtra(ScannerService.PARCELABLE_SCANRESULTS);

                    List<ParcelUuid> uuidData = scanResults.get(currentIndex).getScanRecord().getServiceUuids();
                    byte[] receivedData = scanResults.get(currentIndex).getScanRecord().getServiceData().get(uuidData.get(0));
                    currentIndex += 1;

                    // set the relayed bitmap as the value of received data
                    relayedBitmap.clear();
                    temp.clear();
                    relayedBitmap.or(BitSet.valueOf(receivedData));

                    StringBuilder s = new StringBuilder();
                    s.setLength(0);
                    for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
                        s.append(relayedBitmap.get(i) ? "1" : "0");
                    }
                    Toast.makeText(getApplicationContext(), "bitmap: " + s.toString(), Toast.LENGTH_LONG).show();

                    // check the page number
                    if (s.substring(0,2).equals("00")) {
                        temp.or(bitmap0.get(2, Constants.MAX_NUMBER_OF_BITS));
                        temp.xor(relayedBitmap.get(2, Constants.MAX_NUMBER_OF_BITS));
                        Log.i(TAG, "XOR-ing bitmap 0");
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                            Log.i(TAG, "relayed bitmap empty");
                            restIndicator = true;
                            // TODO: record the time when it rests
                        } else {
                            // the two bitmaps are different, xor the parts other than page number
                            restIndicator = false;
                            bitmap0.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 0");
                        }
                    } else if (s.substring(0,2).equals("10")) {
                        temp.or(bitmap1.get(2, Constants.MAX_NUMBER_OF_BITS));
                        temp.xor(relayedBitmap.get(2, Constants.MAX_NUMBER_OF_BITS));
                        Log.i(TAG, "XOR-ing bitmap 1");
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                            Log.i(TAG, "relayed bitmap empty");
                            restIndicator = true;
                            // TODO: record the time when it rests
                        } else {
                            // the two bitmaps are different, xor the parts other than page number
                            restIndicator = false;
                            bitmap1.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 1");
                        }
                    } else if (s.substring(0,2).equals("01")) {
                        temp.or(bitmap2.get(2, Constants.MAX_NUMBER_OF_BITS));
                        temp.xor(relayedBitmap.get(2, Constants.MAX_NUMBER_OF_BITS));
                        Log.i(TAG, "XOR-ing bitmap 2");
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                            Log.i(TAG, "relayed bitmap empty");
                            restIndicator = true;
                            // TODO: record the time when it rests
                        } else {
                            // the two bitmaps are different, xor the parts other than page number
                            restIndicator = false;
                            bitmap2.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 2");
                        }
                    } else if (s.substring(0,2).equals("11")) {
                        temp.or(bitmap3.get(2, Constants.MAX_NUMBER_OF_BITS));
                        temp.xor(relayedBitmap.get(2, Constants.MAX_NUMBER_OF_BITS));
                        Log.i(TAG, "XOR-ing bitmap 3");
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                            Log.i(TAG, "relayed bitmap empty");
                            restIndicator = true;
                            // TODO: record the time when it rests
                        } else {
                            // the two bitmaps are different, xor the parts other than page number
                            restIndicator = false;
                            bitmap3.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 3");
                        }
                    }

                } catch (Resources.NotFoundException e) {
                    Toast.makeText(getApplicationContext(), "AttendanceTaking: NotFoundException...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "AttendanceTaking: " + e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
}