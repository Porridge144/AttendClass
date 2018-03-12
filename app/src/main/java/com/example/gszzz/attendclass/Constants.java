package com.example.gszzz.attendclass;

import android.os.ParcelUuid;

public class Constants {
    public static final ParcelUuid Service_UUID = ParcelUuid.fromString("0000a90e-0000-1000-8000-00805f9b34fb");

    public static final int REQUEST_ENABLE_BT = 1;

    public static final int MAX_NUMBER_OF_BITS = 6;  // 10 bytes

    public static final int ADVERTISING_INTERVAL = 2; // 2 seconds

    public static final int DURATION = 12; // 10 seconds

    public static final int BIAS = 2; // 2 seconds

    public static final int CLASS_DURATION = 30 * 60; // 30 minutes
}
