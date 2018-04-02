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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gszzz.attendclass.server_interaction.BackgroundTaskRetrieveInfo;

import java.util.BitSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AttendanceTaking extends AppCompatActivity{

    private static final String TAG = "AttendanceTaking";
    private BroadcastReceiver advertisingFailureReceiver;
    private BroadcastReceiver scanningFailureReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView defaultTextView;
    private TextView labelTextView;
    private TextView moduleLocationTextView;
    private TextView moduleInfoTextView;
    private TextView dataTextView;
    private ArrayList<ScanResult> scanResults;
    private boolean restIndicator = false;
    private long restStartTime = 0;
    private static BitSet bitmap00 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private static BitSet bitmap01 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private static BitSet bitmap10 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private static BitSet bitmap11 = new BitSet(Constants.MAX_NUMBER_OF_BITS); // at most 20 bytes
    private BitSet relayedBitmap = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    private BitSet temp = new BitSet(Constants.MAX_NUMBER_OF_BITS);
    private static int advertisedTimes = 0;
    private String matricNum;
    private String myName;
    private static int powerLevel = 0;
    private int currentIndex = 0;
    private long startTime = 0;

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
            super((Constants.PERIOD*1000 + Constants.BIAS), 1000);
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

            labelTextView.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
//            Log.i(TAG, Integer.toString(mBluetoothAdapter.getLeMaximumAdvertisingDataLength()));

            // no difference in bitmap; can rest
            if (restIndicator){
                Log.i(TAG,"onTick scan");
                if (AdvertiserService.running){
                    stopAdvertising();
                }
                // if the scanner service is not running yet
                if (!ScannerService.running){
                    taskScan();
                } else {
                // if it is currently scanning & it has rest for more than 5 min
                    if ( (millis-restStartTime)/1000 > 5 * 60 ){
                        restIndicator = false;
                        stopScanning();
                    }
                }
            }
            // there is difference in bitmap; need to advertise
            else{
                if (( secondsUntilFinish < Constants.PERIOD + range) & (secondsUntilFinish > Constants.PERIOD - range)) {
                    advertisePeriodEnd = false;
                    taskAdvertise(0);
                } else if (( secondsUntilFinish < Constants.PERIOD - Constants.ADVERTISING_INTERVAL + range) & (secondsUntilFinish > Constants.PERIOD - Constants.ADVERTISING_INTERVAL - range)) {
                    advertisePeriodEnd = false;
                    taskAdvertise(1);
                } else if (( secondsUntilFinish < Constants.PERIOD - Constants.ADVERTISING_INTERVAL*2 + range) & (secondsUntilFinish > Constants.PERIOD - Constants.ADVERTISING_INTERVAL*2 - range)) {
                    advertisePeriodEnd = false;
                    taskAdvertise(2);
                } else if (( secondsUntilFinish < Constants.PERIOD - Constants.ADVERTISING_INTERVAL*3 + range) & (secondsUntilFinish > Constants.PERIOD - Constants.ADVERTISING_INTERVAL*3 - range)) {
                    Log.i(TAG,"here");
                    taskAdvertise(3);
                    advertisePeriodEnd = true;
                } else if (advertisePeriodEnd  && ( secondsUntilFinish < Constants.PERIOD - Constants.ADVERTISING_INTERVAL*4 + range) & (secondsUntilFinish > Constants.PERIOD - Constants.ADVERTISING_INTERVAL*4 - range)){
                    if (!ScannerService.running){
                        taskScan();
//                        randomizeBitmaps();
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
        defaultTextView = findViewById(R.id.defaultText);
        moduleLocationTextView = findViewById(R.id.moduleLocation);
        moduleInfoTextView = findViewById(R.id.moduleInfo);

        //set page number
        bitmap01.set(1);
        bitmap10.set(0);
        bitmap11.set(0);
        bitmap11.set(1);
//        randomizeBitmaps();

        scanResults = new ArrayList<>();
        IntentFilter filter = new IntentFilter(ScannerService.NEW_DEVICE_FOUND);
        registerReceiver(scanResultsReceiver, filter);
        filter = new IntentFilter("classDataReceived");
        registerReceiver(classDataReceiver, filter);

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

                        BackgroundTaskRetrieveInfo backgroundTaskRetrieveInfo = new BackgroundTaskRetrieveInfo(getApplicationContext());
                        backgroundTaskRetrieveInfo.execute("query_class_list");
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
                        BackgroundTaskRetrieveInfo backgroundTaskRetrieveInfo = new BackgroundTaskRetrieveInfo(getApplicationContext());
                        backgroundTaskRetrieveInfo.execute("query_class_list");
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

    private void taskAdvertise(int pageNum){
        if(ScannerService.running) {
            stopScanning();
        }
        if(AdvertiserService.running){
            stopAdvertising();
        }
        StringBuilder s = new StringBuilder();
        switch (pageNum) {
            case 0:
                for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
                    s.append(bitmap00.get(i) ? "1" : "0");
                }
                break;
            case 1:
                for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
                    s.append(bitmap01.get(i) ? "1" : "0");
                }
                break;
            case 2:
                for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
                    s.append(bitmap10.get(i) ? "1" : "0");
                }
                break;
            case 3:
                for (int i = 0; i < Constants.MAX_NUMBER_OF_BITS; i++) {
                    s.append(bitmap11.get(i) ? "1" : "0");
                }
                break;
            default:
                Toast.makeText(getApplicationContext(),"error! no page number!", Toast.LENGTH_SHORT).show();
        }
        startAdvertising(s.substring(0,2));
        moduleLocationTextView.setText(Integer.toString(advertisedTimes));
        dataTextView.setText(s);
    }

    private void taskScan(){
        if(AdvertiserService.running){
            stopAdvertising();
        }
        if(!ScannerService.running) {
            startScanning();
        }
//        moduleLocationTextView.setText("Now is scanning");
        dataTextView.setText("Not advertising...");
        Log.i(TAG,"scan");
    }

    private void startAdvertising(String pageNumber) {
        Context c = getApplicationContext();
        c.startService(getAdvertiseServiceIntent(c, pageNumber));
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
        switch (pageNumber) {
            case "00":
                intent.putExtra("bitmap", bitmap00.toByteArray());
                break;
            case "01":
                intent.putExtra("bitmap", bitmap01.toByteArray());
                break;
            case "10":
                intent.putExtra("bitmap", bitmap10.toByteArray());
                break;
            case "11":
                intent.putExtra("bitmap", bitmap11.toByteArray());
                break;
        }
        advertisedTimes += 1;
        intent.putExtra("power", powerLevel);
        return intent;
    }

    private static Intent getAdvertiseServiceIntent(Context c) {
        Log.i(TAG, "getAdvertiseServiceIntent(Context c) ");
        return new Intent(c, AdvertiserService.class);
    }

    private static Intent getScannerServiceIntent(Context c) {
//        intent.putExtra("message", "Put message here!!");
        return new Intent(c, ScannerService.class);
    }

    public static void randomizeBitmaps(){
        bitmap00.clear(2,Constants.MAX_NUMBER_OF_BITS);
        bitmap01.clear(2,Constants.MAX_NUMBER_OF_BITS);
        bitmap10.clear(2,Constants.MAX_NUMBER_OF_BITS);
        bitmap11.clear(2,Constants.MAX_NUMBER_OF_BITS);
        int randomPage = ThreadLocalRandom.current().nextInt(0, 4);
        int randomNum = ThreadLocalRandom.current().nextInt(2, Constants.MAX_NUMBER_OF_BITS);
        switch (randomPage) {
            case (0):
                bitmap00.set(randomNum);
                break;
            case (1):
                bitmap01.set(randomNum);
                break;
            case (2):
                bitmap10.set(randomNum);
                break;
            case (3):
                bitmap11.set(randomNum);
                break;
            default:
        }
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
        IntentFilter intentFilterAd = new IntentFilter(AdvertiserService.ADVERTISING_FAILED);
        IntentFilter intentFilterSc = new IntentFilter(ScannerService.SCANNING_FAILED);
        registerReceiver(advertisingFailureReceiver, intentFilterAd);
        registerReceiver(scanningFailureReceiver, intentFilterSc);
        Log.i(TAG, "activity resumed");
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
        Log.i(TAG, "activity paused");
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
                            if (!restIndicator) {
                                restStartTime = System.currentTimeMillis();
                            }
                            restIndicator = true;
                        } else {
                            restIndicator = false;
                            bitmap00.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 0");
                        }
                    } else if (!relayedBitmap.get(0) && relayedBitmap.get(1)) {
                        // 01 = false true
                        temp.xor(bitmap01.get(2, Constants.MAX_NUMBER_OF_BITS));
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                            if (!restIndicator) {
                                restStartTime = System.currentTimeMillis();
                            }
                            restIndicator = true;
                        } else {
                            restIndicator = false;
                            bitmap01.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 1");
                        }
                    } else if (relayedBitmap.get(0) && !relayedBitmap.get(1)) {
                        // 10 = true false
                        temp.xor(bitmap10.get(2, Constants.MAX_NUMBER_OF_BITS));
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                            if (!restIndicator) {
                                restStartTime = System.currentTimeMillis();
                            }
                            restIndicator = true;
                        } else {
                            restIndicator = false;
                            bitmap10.or(relayedBitmap);
                            Log.i(TAG, "OR-ing bitmap 2");
                        }
                    } else if (relayedBitmap.get(0) && relayedBitmap.get(1)) {
                        // 11 = true true
                        temp.xor(bitmap11.get(2, Constants.MAX_NUMBER_OF_BITS));
                        // the two bitmaps are same
                        if (temp.isEmpty()) {
                            if (!restIndicator) {
                                restStartTime = System.currentTimeMillis();
                            }
                            restIndicator = true;
                        } else {
                            restIndicator = false;
                            bitmap11.or(relayedBitmap);
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

    private final BroadcastReceiver classDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("classDataReceived")){
                String className = intent.getStringExtra("className");
                String[] nameList = intent.getStringArrayExtra("nameList");
                moduleInfoTextView.setText(String.format("Welcome to %s", className));
                Log.i("length of nameList: ", Integer.toString(nameList.length));
                for (int i=1;i<nameList.length;i++){
                    // temp is the matric no being looped thru
                    matricNum = (nameList[i].split(" "))[1];
                    if (StudentTab.globalUsername.equals(matricNum)){
                        myName = (nameList[i].split(" "))[0];
                        defaultTextView.setText(String.format("Hi %s (%s)", myName, matricNum));
                        // set the specific bit to 1
                        if(i<=Constants.MAX_NUMBER_OF_BITS-2)
                            bitmap00.set(i+1);
                        else if(i<=2*(Constants.MAX_NUMBER_OF_BITS-2))
                            bitmap01.set(i+1-(Constants.MAX_NUMBER_OF_BITS-2));
                        else if(i<=3*(Constants.MAX_NUMBER_OF_BITS-2))
                            bitmap10.set(i+1-2*(Constants.MAX_NUMBER_OF_BITS-2));
                        else
                            bitmap11.set(i+1-3*(Constants.MAX_NUMBER_OF_BITS-2));
                        break;
                    }
                }
                Log.i("00: ", bitmap00.toString());
                Log.i("01: ", bitmap01.toString());
                Log.i("10: ", bitmap10.toString());
                Log.i("11: ", bitmap11.toString());
                startTime = System.currentTimeMillis();
                handler.post(runnableCode);
            }
        }
    };
}