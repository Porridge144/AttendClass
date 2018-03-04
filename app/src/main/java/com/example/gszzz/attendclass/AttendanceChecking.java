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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AttendanceChecking extends AppCompatActivity {

    private BroadcastReceiver scanningFailureReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView totalNumTextView;
    private ArrayList<ScanResult> scanResults;
    public static BitSet bitmap0 = new BitSet(160); // 20 bytes
    public static BitSet bitmap1 = new BitSet(160); // 20 bytes
    public static BitSet bitmap2 = new BitSet(160); // 20 bytes
    public static BitSet bitmap3 = new BitSet(160); // 20 bytes

    public static final String PARCELABLE_SCANRESULTS = "ParcelScanResults";

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_checking);

        scanResults = new ArrayList<>();
        totalNumTextView = (TextView) findViewById(R.id.totalNumTextView);
        IntentFilter filter = new IntentFilter(ScannerService.NEW_DEVICE_FOUND);
        registerReceiver(scanResultsReceiver, filter);

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
                        // set page number
                        bitmap0.set(0);
                        bitmap1.set(1);
                        bitmap2.set(2);
                        bitmap3.set(3);
                        //Start service
                        startScanning();
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
                        //Start service
                        startScanning();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scanResultsReceiver);
        Toast.makeText(getApplicationContext(), "Scanning activity get destroyed....", Toast.LENGTH_SHORT).show();
        if (ScannerService.running) {
            stopScanning();
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
        intent.putExtra("Instruction", lecturerLabel);
        intent.putExtra("Indicator", "lec");
        //intent.setAction("ClassBegins");
        return intent;
    }

    private static Intent getScannerServiceIntent(Context c) {
        Intent intent = new Intent(c, ScannerService.class);
        //TODO: Use this to send info to Scanner Service
//        intent.putExtra("message", "Put message here!!");
        return intent;
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

                    List<ParcelUuid> uuidData = scanResults.get(0).getScanRecord().getServiceUuids();
                    String receivedData = new String(scanResults.get(0).getScanRecord().getServiceData().get(uuidData.get(0)));

                    // set the relayed bitmap
                    BitSet relayedBitmap = new BitSet(160);
                    for (int i = 0; i < 160; i++) {
                        if (receivedData.charAt(i) == '1') {
                            relayedBitmap.set(i);
                        }
                    }

                    // check the page number
                    if(bitmap0.get(0,3) == relayedBitmap.get(0,3)){
                        BitSet temp = bitmap0;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap0)){
                            startScanning();
                        } else {
                            // the two bitmaps are different, xor the parts other than page number
                            bitmap0.get(4,160).xor(relayedBitmap.get(4,160));
                        }
                    } else if (bitmap1.get(0,3) == relayedBitmap.get(0,3)){
                        BitSet temp = bitmap1;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap1)){
                            startScanning();
                        } else {
                            bitmap1.get(4,160).xor(relayedBitmap.get(4,160));
                        }
                    } else if (bitmap2.get(0,3) == relayedBitmap.get(0,3)){
                        BitSet temp = bitmap2;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap2)){
                            startScanning();
                        } else {
                            bitmap2.get(4,160).xor(relayedBitmap.get(4,160));
                        }
                    } else if (bitmap3.get(0,3) == relayedBitmap.get(0,3)){
                        BitSet temp = bitmap3;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap3)){
                            startScanning();
                        } else {
                            bitmap3.get(4,160).xor(relayedBitmap.get(4,160));
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
