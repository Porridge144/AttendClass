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
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.BitSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    public String pageNum;
    public boolean restIndicator = false;
    public static BitSet bitmap0 = new BitSet(160); // 20 bytes
    public static BitSet bitmap1 = new BitSet(160); // 20 bytes
    public static BitSet bitmap2 = new BitSet(160); // 20 bytes
    public static BitSet bitmap3 = new BitSet(160); // 20 bytes

    public static final String PARCELABLE_SCANRESULTS = "ParcelScanResults";
    long startTime = 0;

    //runs without a timer by reposting this handler at the end of the runnable
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

            if (seconds == 1 && !restIndicator){
                // 1~2s
                stopScanning();
                pageNum = "1000";
                startAdvertising(pageNum);
                moduleLocationTextView.setText("Now is advertising page " + pageNum);
                dataTextView.setText("Data is " + bitmap0.get(4,160).toString());
            } else if (seconds == 2 && !restIndicator){
                // 2~3s
                stopAdvertising();
                pageNum = "0100";
                startAdvertising(pageNum);
                moduleLocationTextView.setText("Now is advertising page " + pageNum);
                dataTextView.setText("Data is " + bitmap1.get(4,160).toString());
            } else if (seconds == 3 && !restIndicator){
                // 3~4s
                stopAdvertising();
                pageNum = "0010";
                startAdvertising(pageNum);
                moduleLocationTextView.setText("Now is advertising page " + pageNum);
                dataTextView.setText("Data is " + bitmap2.get(4,160).toString());
            } else if (seconds == 4 && !restIndicator){
                // 4~5s
                stopAdvertising();
                pageNum = "0001";
                startAdvertising(pageNum);
                moduleLocationTextView.setText("Now is advertising page " + pageNum);
                dataTextView.setText("Data is " + bitmap3.get(4,160).toString());
            } else if (seconds == 5) {
                // from 5s onwards
                stopAdvertising();
                startScanning();
                moduleLocationTextView.setText("Now is scanning");
                dataTextView.setText("Not advertising...");
            }

            //TODO: implement the timeout function according to resting time

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_taking);

        labelTextView = (TextView) findViewById(R.id.textView4);
        dataTextView = (TextView) findViewById(R.id.textView3);
        moduleLocationTextView = (TextView) findViewById(R.id.moduleLocation);

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
                        bitmap0.set(0);
                        bitmap1.set(1);
                        bitmap2.set(2);
                        bitmap3.set(3);
                        bitmap0.set(15); // make this student to be No. 12 student

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

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);

        scanResults = new ArrayList<>();
        IntentFilter filter = new IntentFilter(ScannerService.NEW_DEVICE_FOUND);
        registerReceiver(scanResultsReceiver, filter);
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
                    String relayedPageNumber = relayedBitmap.get(0,3).toString();

                    // check the page number
                    if(bitmap0.get(0,3) == relayedBitmap.get(0,3)){
                        BitSet temp = bitmap0;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap0)){
                            restIndicator = true;
                            // TODO: record the time when it rests
                            startScanning();
                        } else {
                        // the two bitmaps are different, xor the parts other than page number
                            bitmap0.get(4,160).xor(relayedBitmap.get(4,160));
                            startAdvertising(relayedPageNumber);
                        }
                    } else if (bitmap1.get(0,3) == relayedBitmap.get(0,3)){
                        BitSet temp = bitmap1;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap1)){
                            restIndicator = true;
                            startScanning();
                        } else {
                            bitmap1.get(4,160).xor(relayedBitmap.get(4,160));
                            startAdvertising(relayedPageNumber);
                        }
                    } else if (bitmap2.get(0,3) == relayedBitmap.get(0,3)){
                        BitSet temp = bitmap2;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap2)){
                            restIndicator = true;
                            startScanning();
                        } else {
                            bitmap2.get(4,160).xor(relayedBitmap.get(4,160));
                            startAdvertising(relayedPageNumber);
                        }
                    } else if (bitmap3.get(0,3) == relayedBitmap.get(0,3)){
                        BitSet temp = bitmap3;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap3)){
                            restIndicator = true;
                            startScanning();
                        } else {
                            bitmap3.get(4,160).xor(relayedBitmap.get(4,160));
                            startAdvertising(relayedPageNumber);
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
        timerHandler.removeCallbacks(timerRunnable);
        stopScanning();
        stopAdvertising();
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
        return intent;
    }

    private static Intent getAdvertiseServiceIntent(Context c) {
        Log.i(TAG, "getAdvertiseServiceIntent(Context c) ");
        Intent intent = new Intent(c, AdvertiserService.class);
        //TODO: Use this to send info to Advertise Service
        //intent.setAction("SelfData");
//        intent.putExtra("Instruction", "" );
//        intent.putExtra("Indicator", "stu");
        return intent;
    }

    private static Intent getScannerServiceIntent(Context c) {
        Intent intent = new Intent(c, ScannerService.class);
        //TODO: Use this to send info to Scanner Service
//        intent.putExtra("message", "Put message here!!");
        return intent;
    }
}
