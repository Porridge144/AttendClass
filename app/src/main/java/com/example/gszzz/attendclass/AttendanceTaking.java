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
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
//import android.widget.Switch;
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

    public static final String PARCELABLE_SCANRESULTS = "ParcelScanResults";
    long startTime = 0;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            labelTextView.setText(String.format("%d:%02d", minutes, seconds));

            timerHandler.postDelayed(this, 500);

            if (seconds == 1 && !restIndicator) {
                // 1~3s, page 00
                stopScanning();
                startAdvertising(bitmap0.get(0, 2).toString());
                moduleLocationTextView.setText("Now is advertising page " + Arrays.toString(bitmap0.get(0, 2).toByteArray()));
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < bitmap0.size(); i++) {
                    s.append(bitmap0.get(i) ? "1" : "0");
                }
                dataTextView.setText(s);
            } else if (seconds == 3 && !restIndicator) {
                // 3~5s, page 10
                stopAdvertising();
                startAdvertising(bitmap1.get(0, 2).toString());
                moduleLocationTextView.setText("Now is advertising page " + Arrays.toString(bitmap1.get(0, 2).toByteArray()));
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < bitmap1.size(); i++) {
                    s.append(bitmap1.get(i) ? "1" : "0");
                }
                dataTextView.setText(s.toString());
//                dataTextView.setText(Arrays.toString(bitmap1.get(4,Constants.MAX_NUMBER_OF_BITS-1).toByteArray()));
            } else if (seconds == 5 && !restIndicator) {
                // 5~7s, page 01
                stopAdvertising();
                startAdvertising(bitmap2.get(0, 2).toString());
                moduleLocationTextView.setText("Now is advertising page " + Arrays.toString(bitmap2.get(0, 2).toByteArray()));
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < bitmap2.size(); i++) {
                    s.append(bitmap2.get(i) ? "1" : "0");
                }
                dataTextView.setText(s.toString());
//                dataTextView.setText(Arrays.toString(bitmap2.get(4,Constants.MAX_NUMBER_OF_BITS-1).toByteArray()));
            } else if (seconds == 7 && !restIndicator) {
                // 7~9s, page 11
                stopAdvertising();
                startAdvertising(bitmap3.get(0, 2).toString());
                moduleLocationTextView.setText("Now is advertising page " + Arrays.toString(bitmap3.get(0, 2).toByteArray()));
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < bitmap3.size(); i++) {
                    s.append(bitmap3.get(i) ? "1" : "0");
                }
                dataTextView.setText(s.toString());
//                dataTextView.setText(Arrays.toString(bitmap3.get(4,Constants.MAX_NUMBER_OF_BITS-1).toByteArray()));
            } else if (seconds == 9 || restIndicator) {
                // from 9s onwards
                stopAdvertising();
                startScanning();
                moduleLocationTextView.setText("Now is scanning");
                dataTextView.setText("Not advertising...");
            }

            //TODO: implement the timeout function according to resting time

        }
    };

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_taking);

        labelTextView = findViewById(R.id.textView4);
        dataTextView = findViewById(R.id.textView3);
        moduleLocationTextView = findViewById(R.id.moduleLocation);

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);

        //set page number
        bitmap1.set(0);
        bitmap2.set(1);
        bitmap3.set(0);
        bitmap3.set(1);

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
        if (ScannerService.running) {
            stopScanning();
        }
        if (AdvertiserService.running){
            stopAdvertising();
        }
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
        //TODO: Use this to send info to Advertise Service
        //intent.setAction("SelfData");
//        intent.putExtra("Instruction", "" );
//        intent.putExtra("Indicator", "stu");
        return new Intent(c, AdvertiserService.class);
    }

    private static Intent getScannerServiceIntent(Context c) {
        //TODO: Use this to send info to Scanner Service
//        intent.putExtra("message", "Put message here!!");
        return new Intent(c, ScannerService.class);
    }

    private final BroadcastReceiver scanResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ScannerService.NEW_DEVICE_FOUND)) {
                try {
                    scanResults = intent.getParcelableArrayListExtra(ScannerService.PARCELABLE_SCANRESULTS);

                    List<ParcelUuid> uuidData = scanResults.get(0).getScanRecord().getServiceUuids();
                    byte[] receivedData = scanResults.get(0).getScanRecord().getServiceData().get(uuidData.get(0));

                    // set the relayed bitmap
                    BitSet relayedBitmap = new BitSet(Constants.MAX_NUMBER_OF_BITS);
                    for (int i = 0; i < receivedData.length * 8; i++) {
                        if ((receivedData[receivedData.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
                            relayedBitmap.set(i);
                        }
                    }
                    String relayedPageNumber = relayedBitmap.get(0, 2).toString();

                    // check the page number
                    if (bitmap0.get(0, 2) == relayedBitmap.get(0, 2)) {
                        BitSet temp = bitmap0;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap0)) {
                            restIndicator = true;
                            // TODO: record the time when it rests
                            startScanning();
                        } else {
                            // the two bitmaps are different, xor the parts other than page number
                            restIndicator = false;
                            bitmap0.get(2, Constants.MAX_NUMBER_OF_BITS).xor(relayedBitmap.get(2, Constants.MAX_NUMBER_OF_BITS));
                            startAdvertising(relayedPageNumber);
                        }
                    } else if (bitmap1.get(0, 2) == relayedBitmap.get(0, 2)) {
                        BitSet temp = bitmap1;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap1)) {
                            restIndicator = true;
                            startScanning();
                        } else {
                            restIndicator = false;
                            bitmap1.get(2, Constants.MAX_NUMBER_OF_BITS).xor(relayedBitmap.get(2, Constants.MAX_NUMBER_OF_BITS));
                            startAdvertising(relayedPageNumber);
                        }
                    } else if (bitmap2.get(0, 2) == relayedBitmap.get(0, 2)) {
                        BitSet temp = bitmap2;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap2)) {
                            restIndicator = true;
                            startScanning();
                        } else {
                            restIndicator = false;
                            bitmap2.get(2, Constants.MAX_NUMBER_OF_BITS).xor(relayedBitmap.get(2, Constants.MAX_NUMBER_OF_BITS));
                            startAdvertising(relayedPageNumber);
                        }
                    } else if (bitmap3.get(0, 2) == relayedBitmap.get(0, 2)) {
                        BitSet temp = bitmap3;
                        temp.or(relayedBitmap);
                        // the two bitmaps are same
                        if (temp.equals(bitmap3)) {
                            restIndicator = true;
                            startScanning();
                        } else {
                            restIndicator = false;
                            bitmap3.get(2, Constants.MAX_NUMBER_OF_BITS).xor(relayedBitmap.get(2, Constants.MAX_NUMBER_OF_BITS));
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
}