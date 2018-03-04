package com.example.gszzz.attendclass;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AdvertiserService extends Service {

    //Constants
    private static final int FOREGROUND_NOTIFICATION_ID = 1332;

    /**
     * A global variable to let AdvertiserFragment check if the Service is running without needing
     * to start or bind to it.
     * This is the best practice method as defined here:
     * https://groups.google.com/forum/#!topic/android-developers/jEvXMWgbgzE
     */
    public static boolean running = false;

    public static final String ADVERTISING_FAILED =
            "com.example.gszzz.attendclass.advertising_failed";
    public static final String ADVERTISING_FAILED_EXTRA_CODE = "failureCode";
    public static final int ADVERTISING_TIMED_OUT = 6;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdvertiseCallback;
    private Handler mHandler;
    private Runnable timeoutRunnable;
    private byte[] advertisingData;
    private String instruction;

    private static final String TAG = "AdvertiserService";

    /**
     * Length of time to allow advertising before automatically shutting off. (5 minutes)
     */
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    @Override
    public void onCreate() {
        running = true;
        initialize();
        setTimeout();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instruction = intent.getStringExtra("Instruction");
        if(instruction.equals("1000")){
            advertisingData = AttendanceTaking.bitmap0.toByteArray();
        }
        if(instruction.equals("0100")){
            advertisingData = AttendanceTaking.bitmap1.toByteArray();
        }
        if(instruction.equals("0010")){
            advertisingData = AttendanceTaking.bitmap2.toByteArray();
        }
        if(instruction.equals("0001")){
            advertisingData = AttendanceTaking.bitmap3.toByteArray();
        }
        Toast.makeText(this, advertisingData.toString(), Toast.LENGTH_LONG).show();
        startAdvertising();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        running = false;
        stopAdvertising();
//        unregisterReceiver(receiver);
        mHandler.removeCallbacks(timeoutRunnable);
        stopForeground(true);
        super.onDestroy();
    }

    private void startAdvertising() {
        goForeground();

        //Toast.makeText(getApplicationContext(), "Starting Advertising ", Toast.LENGTH_LONG).show();

        if (mAdvertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
            mAdvertiseCallback = new SampleAdvertiseCallback();

            if (mBluetoothLeAdvertiser != null) {
                mBluetoothLeAdvertiser.startAdvertising(settings, data,
                        mAdvertiseCallback);
            }
        }
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private AdvertiseData buildAdvertiseData() {

        /*
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(Constants.Service_UUID);
        dataBuilder.setIncludeDeviceName(false);
        dataBuilder.addServiceData(Constants.Service_UUID, advertisingData);

        /* For example - this will cause advertising to fail (exceeds size limit) */
        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());

        return dataBuilder.build();
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    /*
     * Move service to the foreground, to avoid execution limits on background processes.
     * <p>
     * Callers should call stopForeground(true) when background work is complete.
     */
    private void goForeground() {
        Intent notificationIntent = new Intent(this, AttendanceTaking.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle("Advertising device via Bluetooth")
                .setContentText("This device is discoverable to others nearby.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);
        startForeground(FOREGROUND_NOTIFICATION_ID, builder.build());
    }

    private void stopAdvertising() {
        //Toast.makeText(getApplicationContext(), "Stopping Advertising ", Toast.LENGTH_LONG).show();
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertiseCallback = null;
            //Toast.makeText(getApplicationContext(), "Advertisement stopped successfully... ", Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Get references to system Bluetooth objects if we don't have them already.
     */
    private void initialize() {
        if (mBluetoothLeAdvertiser == null) {
            BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
                if (mBluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                } else {
                    Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show();
            }
        }

    }

    /**
     * Starts a delayed Runnable that will cause the BLE Advertising to timeout and stop after a
     * set amount of time.
     */
    private void setTimeout() {
        mHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                sendFailureIntent(ADVERTISING_TIMED_OUT);
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
        failureIntent.setAction(ADVERTISING_FAILED);
        failureIntent.putExtra(ADVERTISING_FAILED_EXTRA_CODE, errorCode);
        sendBroadcast(failureIntent);
    }

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class SampleAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            sendFailureIntent(errorCode);
            stopSelf();
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            //Toast.makeText(getApplicationContext(), "Advertisement started successfully...", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

//    /*
//     * When app goes off screen, unregister the Advertising failure Receiver to stop memory leaks.
//     * (and because the app doesn't care if Advertising fails while the UI isn't active)
//     */
//    @Override
//    public void onPause() {
//        super.onPause();
//        getActivity().unregisterReceiver(advertisingFailureReceiver);
//    }
}