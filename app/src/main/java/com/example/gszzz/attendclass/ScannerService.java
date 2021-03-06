package com.example.gszzz.attendclass;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ScannerService extends Service {

    //Constants
    private static final int FOREGROUND_NOTIFICATION_ID = 1333;

    /**
     * A global variable to let AdvertiserFragment check if the Service is running without needing
     * to start or bind to it.
     * This is the best practice method as defined here:
     * https://groups.google.com/forum/#!topic/android-developers/jEvXMWgbgzE
     */
    protected static boolean running = false;
    private ArrayList<ScanResult> scanResults;

    protected static final String NEW_DEVICE_FOUND = "ble_new_device_found";
    protected static final String SCANNING_FAILED =
            "com.example.gszzz.attendclass.scanning_failed";
    protected static final String SCANNING_FAILED_EXTRA_CODE = "failureCode";
    protected static final int SCANNING_TIMED_OUT = 6;
    protected static final String PARCELABLE_SCANRESULTS = "ParcelScanResults";
    protected static final String TAG = "ScannerService";
    private static int powerLevel;

    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private Handler mHandler;

    private Runnable timeoutRunnable;

    /**
     * Length of time to allow advertising before automatically shutting off.
     */
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(Constants.CLASS_DURATION, TimeUnit.MINUTES);

    @Override
    public void onCreate() {
        running = true;
//        scanResults = new ArrayList<>();
        initialize();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scanResults = new ArrayList<>();
        powerLevel = intent.getIntExtra("power", 2);
        startScanning();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startScanning() {
        goForeground();
        if (mScanCallback == null) {
            //set timeout for scanning
            setTimeout();
            //start a new scan
            mScanCallback = new SampleScanCallback();
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
            Toast.makeText(getApplicationContext(), "Scan frequency: " + buildScanSettings().getScanMode(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), R.string.scanning_already_started, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        stopScanning();
        mHandler.removeCallbacks(timeoutRunnable);
        stopForeground(true);
        //Toast.makeText(getApplicationContext(), "Scanning stopped...", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }


    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class SampleScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results) {
                addScanResult(result);
            }
//            Toast.makeText(getApplicationContext(), "Multiple new devices detected... (" + scanResults.size() + ")", Toast.LENGTH_SHORT).show();

            Intent newDeviceFoundIntent = new Intent();
            newDeviceFoundIntent.setAction(NEW_DEVICE_FOUND);
            newDeviceFoundIntent.putParcelableArrayListExtra(PARCELABLE_SCANRESULTS, scanResults);
            sendBroadcast(newDeviceFoundIntent);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //add to the scanResults list
            addScanResult(result);
            Intent newDeviceFoundIntent = new Intent();
            newDeviceFoundIntent.setAction(NEW_DEVICE_FOUND);
            newDeviceFoundIntent.putParcelableArrayListExtra(PARCELABLE_SCANRESULTS, scanResults);
            sendBroadcast(newDeviceFoundIntent);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getApplicationContext(), "Scan failed with error: " + errorCode, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void addScanResult(ScanResult result) {

        int existingPosition = getPosition(result.getDevice().getAddress());

        if (existingPosition >= 0) {
            // Device is already in list, update its record.
            scanResults.set(existingPosition, result);
        } else {
            // Add new Device's ScanResult to list.
            scanResults.add(result);
        }
    }

    /**
     * Search the adapter for an existing device address and return it, otherwise return -1.
     */
    private int getPosition(String address) {
        int position = -1;
        for (int i = 0; i < scanResults.size(); i++) {
            if (scanResults.get(i).getDevice().getAddress().equals(address)) {
                position = i;
                break;
            }
        }
        return position;
    }


    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(Constants.Service_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        switch (powerLevel) {
            case 0:
                builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
                Log.i(TAG, "mode is 0, low power");
                break;
            case 1:
                builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
                Log.i(TAG, "mode is 1, balanced");
                break;
            case 2:
                builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                Log.i(TAG, "mode is 2, high power");
                break;
        }
        return builder.build();
    }

    /**
     * Move service to the foreground, to avoid execution limits on background processes.
     *
     * Callers should call stopForeground(true) when background work is complete.
     */
    private void goForeground() {
        Intent notificationIntent = new Intent(this, AttendanceTaking.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle("Scanning students' devices")
                .setContentText("Student name list will be updated once found...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);
        startForeground(FOREGROUND_NOTIFICATION_ID, builder.build());
    }

    private void stopScanning() {
        //Toast.makeText(getApplicationContext(), "Stopping scanning...", Toast.LENGTH_SHORT).show();
        //Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;

        //TODO: Update name list??
    }

    private void setTimeout() {
        mHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                sendFailureIntent(SCANNING_TIMED_OUT);
                stopSelf();
            }
        };
        mHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    /**
     * Builds and sends a broadcast intent indicating Advertising has failed. Includes the error
     * code as an extra. This is intended to be picked up by the {@code AdvertiserFragment}.
     */
    private void sendFailureIntent(int errorCode) {
        Intent failureIntent = new Intent();
        failureIntent.setAction(SCANNING_FAILED);
        failureIntent.putExtra(SCANNING_FAILED_EXTRA_CODE, errorCode);
        sendBroadcast(failureIntent);
    }

    /**
     * Get references to system Bluetooth objects if we don't have them already.
     */
    private void initialize() {
        if (mBluetoothLeScanner == null) {
            BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager !=null) {
                BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
                if (mBluetoothAdapter != null) {
//                    Log.i(TAG, "before got scanner");
                    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
//                    Log.i(TAG, "got scanner");
                } else {
                    Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
