package com.example.gszzz.attendclass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.lang.Math.abs;

public class AttendanceChecking extends AppCompatActivity {

    private BroadcastReceiver scanningFailureReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView totalNumTextView;
    private TextView bitmapTextView;
    private TextView timerTextView;
    private ArrayList<ScanResult> scanResults;
    public static BitSet bitmap0 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // 20 bytes
    public static BitSet bitmap1 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // 20 bytes
    public static BitSet bitmap2 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // 20 bytes
    public static BitSet bitmap3 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // 20 bytes
    public static BitSet relayedBitmap = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    public static BitSet temp = new BitSet();
    public static int currentIndex = 0;
    long startTime = 0;
    private static final String TAG = "AttendanceChecking";

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            timerTextView.setText(String.format("%d:%02d", minutes, seconds));

            timerHandler.postDelayed(this, 500);
        }
    };

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
            super(45*60*1000, 1000);
            this.e = ext;
            this.h = han;
        }

        @Override
        public void onTick(long millisUntilFinish) {
            double range = 0.2;
            double secondsUntilFinish = (millisUntilFinish / 1000.0);
            StringBuilder s = new StringBuilder();

            if (abs(secondsUntilFinish % 1) < range || abs(secondsUntilFinish % 1 - 1) < range) {
                for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
                    s.append(bitmap0.get(i) ? "1" : "0");
                }
                s.append("\n");
                for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
                    s.append(bitmap1.get(i) ? "1" : "0");
                }
                s.append("\n");
                for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
                    s.append(bitmap2.get(i) ? "1" : "0");
                }
                s.append("\n");
                for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
                    s.append(bitmap3.get(i) ? "1" : "0");
                }
                s.append("\n");
                bitmapTextView.setText(s.toString());
            }
//            Toast.makeText(getApplicationContext(), "Bitmap updated", Toast.LENGTH_SHORT).show();
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
        setContentView(R.layout.activity_attendance_checking);

        scanResults = new ArrayList<>();
        totalNumTextView = findViewById(R.id.totalNumTextView);
        timerTextView = findViewById(R.id.textView6);
        bitmapTextView = findViewById(R.id.textView5);

        IntentFilter filter = new IntentFilter(ScannerService.NEW_DEVICE_FOUND);
        registerReceiver(scanResultsReceiver, filter);

        // set page number
        bitmap1.set(0);
        bitmap2.set(1);
        bitmap3.set(0);
        bitmap3.set(1);

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

                        startTime = System.currentTimeMillis();
                        timerHandler.post(timerRunnable);
                        handler.post(runnableCode);
                        startScanning();
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

                        startTime = System.currentTimeMillis();
                        timerHandler.post(timerRunnable);
                        handler.post(runnableCode);
                        //Start service
                        startScanning();
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

    private void startAdvertising(String lecturerLabel) {
        Context c = getApplicationContext();
        c.startService(getAdvertiseServiceIntent(c, lecturerLabel));
    }

    private void stopAdvertising() {
        Context c = getApplicationContext();
        c.stopService(getAdvertiseServiceIntent(c));
    }

    /**
     * Returns Intent addressed to the {@code AdvertiserService} class.
     */

    private static Intent getAdvertiseServiceIntent(Context c) {
        Intent intent = new Intent(c, AdvertiserService.class);
        //TODO: Use this to send info to Advertise Service
        //intent.putExtra("message", "Put message here!!");
        return intent;
    }

    private static Intent getAdvertiseServiceIntent(Context c, String lecturerLabel) {
        Intent intent = new Intent(c, AdvertiserService.class);
        //TODO: Use this to send info to Advertise Service
        //intent.setAction("ClassBegins");
        return intent;
    }

    private static Intent getScannerServiceIntent(Context c) {
        Intent intent = new Intent(c, ScannerService.class);
        //TODO: Use this to send info to Scanner Service
//        intent.putExtra("message", "Put message here!!");
        return intent;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scanResultsReceiver);
        timerHandler.removeCallbacks(timerRunnable);
        Toast.makeText(getApplicationContext(), "Scanning activity get destroyed....", Toast.LENGTH_SHORT).show();
        if (ScannerService.running) {
            stopScanning();
        }
        if (AdvertiserService.running){
            stopAdvertising();
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
    }

    /**
     * When app goes off screen, unregister the Advertising failure Receiver to stop memory leaks.
     * (and because the app doesn't care if Advertising fails while the UI isn't active)
     */
    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        unregisterReceiver(scanningFailureReceiver);
    }

    public void enterNameList(View view) {
        Intent intent = new Intent(this, NameList.class);
        startActivity(intent);
    }

    public void stopScanningOnClicked(View view) {
        if (ScannerService.running) {
            Toast.makeText(getApplicationContext(), "Is running... Stopping the service...", Toast.LENGTH_SHORT).show();
            stopScanning();
        }
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
                            // TODO: record the time when it rests
                        } else {
                            // the two bitmaps are different, xor the parts other than page number
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
                            // TODO: record the time when it rests
                        } else {
                            // the two bitmaps are different, xor the parts other than page number
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
                            // TODO: record the time when it rests
                        } else {
                            // the two bitmaps are different, xor the parts other than page number
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
                            // TODO: record the time when it rests
                        } else {
                            // the two bitmaps are different, xor the parts other than page number
                            bitmap3.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 3");
                        }
                    }

                    String totalNumber = " " + scanResults.size() + " ";
                    totalNumTextView.setText(totalNumber);
                } catch (Resources.NotFoundException e) {
                    Toast.makeText(getApplicationContext(), "AttendanceChecking: NotFoundException...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "AttendanceChecking: " + e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
}
