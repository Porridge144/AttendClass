package com.example.gszzz.attendclass;

import android.os.ParcelUuid;

public class Constants {
    public static final ParcelUuid Service_UUID = ParcelUuid.fromString("0000a90e-0000-1000-8000-00805f9b34fb");

    public static final int REQUEST_ENABLE_BT = 1;

    public static final int MAX_NUMBER_OF_BITS = 10;  // number of bits of one bitmap

    public static final int BIAS = 4; // bias time added before starting to advertise

    public static final int ADVERTISING_INTERVAL = 2; // time for advertising one bitmap

    public static final int SCANNING_INTERVAL = 4; // time for scanning

    public static final int DURATION = BIAS + ADVERTISING_INTERVAL*4 + SCANNING_INTERVAL; // total time for one period of advertise and scan

    public static final int CLASS_DURATION = 30; // in minutes, total duration for taking attendance
}
