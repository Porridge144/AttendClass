package com.example.gszzz.attendclass;

import android.os.ParcelUuid;
import java.util.Random;

public class Constants {
    private static Random r = new Random();

    public static final ParcelUuid Service_UUID = ParcelUuid.fromString("0000a90e-0000-1000-8000-00805f9b34fb");

    public static final int REQUEST_ENABLE_BT = 1;

    public static final int MAX_NUMBER_OF_BITS = 6;  // number of bits of one bitmap

    public static final int ADVERTISING_INTERVAL = 4; // time for advertising one bitmap

    public static final int BIAS = r.nextInt(ADVERTISING_INTERVAL*1000); // bias time (ms) added before starting to advertise

    public static final int SCANNING_INTERVAL = 4; // time for scanning

    public static final int PERIOD = ADVERTISING_INTERVAL*4 + SCANNING_INTERVAL; // total time for one period of advertise and scan

    public static final int CLASS_DURATION = 30; // in minutes, total duration for taking attendance

    public static final int STUDENTS = (MAX_NUMBER_OF_BITS-2)*4;  // number of students
}
