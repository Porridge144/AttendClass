package com.example.gszzz.attendclass;

import android.Manifest;
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
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AttendanceTaking extends AppCompatActivity {

    EditText setLabel;

    private BroadcastReceiver advertisingFailureReceiver;
    private BroadcastReceiver scanningFailureReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    public static Switch startAdvertiseSwitch;
    public static Switch relaySwitch;
    private ArrayList<ScanResult> scanResults;
    public static String mlabelData;

    public static final String PARCELABLE_SCANRESULTS = "ParcelScanResults";
    public static final int ADVERTISE_DURATION = 3*1000; // 3s
    public static final int SCAN_DURATION = 7*1000;      // 7s
    public static final int PERIOD = ADVERTISE_DURATION + SCAN_DURATION;  // period is 10s

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_taking);

        setLabel = (EditText) findViewById(R.id.setLabel);

        if (savedInstanceState == null) {

            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter();

            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {

                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {

                    // Are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        // Everything is supported and enabled...
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

        startAdvertiseSwitch = (Switch)findViewById(R.id.startAdvertiseSwitch);
        relaySwitch = (Switch)findViewById(R.id.relaySwitch);

        advertisingFailureReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int errorCode = intent.getIntExtra(AdvertiserService.ADVERTISING_FAILED_EXTRA_CODE, -1);

                startAdvertiseSwitch.setChecked(false);

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

        //TODO: if it's scanning something, note down the label number it hears
//        if (ScannerService.running){
//
//        }
    }

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

    public void confirmLabelOnClick(View view){
        mlabelData = setLabel.getText().toString();
        Toast.makeText(getApplicationContext(), "My label is " + mlabelData, Toast.LENGTH_SHORT).show();

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void relayOnClicked(View view) {
        boolean on = ((Switch) view).isChecked();
        if (startAdvertiseSwitch.isChecked()){
            // if it is advertising, switch off it to avoid collision
            startAdvertiseSwitch.setChecked(false);
        }
        if (on) {
            startScanning();
        } else {
            stopScanning();
        }

        scanResults = new ArrayList<>();
        IntentFilter filter = new IntentFilter(ScannerService.NEW_DEVICE_FOUND);
        registerReceiver(scanResultsReceiver, filter);

        //startScanning();
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

    private final BroadcastReceiver scanResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ScannerService.NEW_DEVICE_FOUND)) {
                try {
                    scanResults = intent.getParcelableArrayListExtra(ScannerService.PARCELABLE_SCANRESULTS);

                    List<ParcelUuid> uuidData = scanResults.get(0).getScanRecord().getServiceUuids();
                    String receivedData = new String(scanResults.get(0).getScanRecord().getServiceData().get(uuidData.get(0)));
//                    String receivedData = scanResults.get(0).getDevice().getName();

                    String[] temp = receivedData.split("\\s");
                    int relayedLabelData = Integer.parseInt(temp[0]);
                    String relayedUserID = temp[1];
                    StudentLogIn.globalRelayUsername = relayedUserID;

                    if (relayedLabelData > Integer.parseInt(mlabelData)) {
                        // need to relay
                        stopScanning();
                        context.unregisterReceiver(scanResultsReceiver);
                        Toast.makeText(getApplicationContext(), "User ID is " + relayedUserID + " and label is " + relayedLabelData, Toast.LENGTH_LONG).show();
                        // stop scan to avoid collision
                        startAdvertising(relayedUserID);
                    }

                } catch (Resources.NotFoundException e) {
                    Toast.makeText(getApplicationContext(), "AttendanceChecking: NotFoundException...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "AttendanceChecking: " + e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    /**
     * When app comes on screen, check if BLE Advertisements are running, set switch accordingly,
     * and register the Receiver to be notified if Advertising fails.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (AdvertiserService.running) {
            startAdvertiseSwitch.setChecked(true);
        } else {
            startAdvertiseSwitch.setChecked(false);
        }
        if (ScannerService.running) {
            relaySwitch.setChecked(true);
        } else {
            relaySwitch.setChecked(false);
        }

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
        unregisterReceiver(advertisingFailureReceiver);
        unregisterReceiver(scanningFailureReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scanResultsReceiver);
        if (ScannerService.running) {
            stopScanning();
            relaySwitch.setChecked(false);
        }
        if (AdvertiserService.running){
            stopAdvertising();
            startAdvertiseSwitch.setChecked(false);
        }
    }

    public void startAdvertiseOnClicked(View view) {
        //Is the toggle on?
        boolean on = ((Switch) view).isChecked();
        if (relaySwitch.isChecked()){
            // if it is scanning, switch off it to avoid collision
            relaySwitch.setChecked(false);
        }
        if (on) {
            startAdvertising();
        } else {
            stopAdvertising();
        }
    }

    private void startAdvertising(String relayedUserID) {
        Context c = getApplicationContext();
        c.startService(getAdvertiseServiceIntent(c, relayedUserID));
    }

    private void startAdvertising() {
        Context c = getApplicationContext();
        c.startService(getAdvertiseServiceIntent(c));
    }

    private void stopAdvertising() {
        Context c = getApplicationContext();
        c.stopService(getAdvertiseServiceIntent(c));
        startAdvertiseSwitch.setChecked(false);
    }

    private void startScanning() {
        Context c = getApplicationContext();
        c.startService(getScannerServiceIntent(c));
    }

    private void stopScanning() {
        Context c = getApplicationContext();
        c.stopService(getScannerServiceIntent(c));
        //relaySwitch.setChecked(false);
    }

    /**
     * Returns Intent addressed to the {@code AdvertiserService} class.
     */
    private static Intent getAdvertiseServiceIntent(Context c, String relayedUserID) {
        Intent intent = new Intent(c, AdvertiserService.class);
        //TODO: Use this to send info to Advertise Service
        //intent.putExtra("message", "Put message here!!");
        //intent.putExtra("RelayData", relayedUserID);
        return intent;
    }

    private static Intent getAdvertiseServiceIntent(Context c) {
        Intent intent = new Intent(c, AdvertiserService.class);
        //TODO: Use this to send info to Advertise Service
        //intent.putExtra("message", "Put message here!!");
        return intent;
    }

    private static Intent getScannerServiceIntent(Context c) {
        Intent intent = new Intent(c, ScannerService.class);
        //TODO: Use this to send info to Scanner Service
//        intent.putExtra("message", "Put message here!!");
        return intent;
    }
}
