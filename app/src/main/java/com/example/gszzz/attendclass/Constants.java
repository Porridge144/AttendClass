package com.example.gszzz.attendclass;

import android.os.ParcelUuid;

import java.util.concurrent.ThreadLocalRandom;

public class Constants {

//    public static final String CLASS_INFO_QUERY_URL = "http://121.7.122.74:8081/attendance/classinfo_query.php";

    public static final String CLASS_INFO_QUERY_URL = "http://172.17.172.192:8081/attendance/classinfo_query.php";

//    public static final String CLASS_INFO_QUERY_URL = "http://192.168.0.104:8081/attendance/classinfo_query.php";

    static final ParcelUuid Service_UUID = ParcelUuid.fromString("0000a90e-0000-1000-8000-00805f9b34fb");

    static final int REQUEST_ENABLE_BT = 1;

    static final int MAX_NUMBER_OF_BITS = 6;  // number of bits of one bitmap

    static final int ADVERTISING_INTERVAL = 4; // time for advertising one bitmap

    static final int SCANNING_INTERVAL = 10; // time for scanning

    static final int BIAS = ThreadLocalRandom.current().nextInt(0, ADVERTISING_INTERVAL*1000+1); // bias time added before starting to advertise

    static final int PERIOD = ADVERTISING_INTERVAL*4 + SCANNING_INTERVAL; // total time for one period of advertise and scan

    static final int CLASS_DURATION = 30; // in minutes, total duration for taking attendance

    static final int STUDENTS = (MAX_NUMBER_OF_BITS-2)*4;  // number of students
}
