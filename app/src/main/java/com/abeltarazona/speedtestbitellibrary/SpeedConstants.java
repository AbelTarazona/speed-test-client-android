package com.abeltarazona.speedtestbitellibrary;

/**
 * Created by AbelTarazona on 20/04/2021
 */
public class SpeedConstants {
    public static final int TIME_OUT = 3000;
    public static final int PACKET_COUNT = 50;
    public static final long TIME_TO_SEND = 500;
    public static final int SERVER_PORT = 5060;
    public static final int PACKET_SIZE = 200;
    public static final int RECEIVE_BUFFER = 300;
    public static final int UPDATE_THRESHOLD = 170;
    public static final int FRAME_UPDATE_TIME = 25;
    public static final long EXPECT_TIME = 21000;
    public static final double BYTE_TO_KILOBIT = 0.008;
    public static final double KILOBIT_TO_MEGABIT = 0.001;
    public static final double KILOBYTE_TO_MEGABIT = 0.008;
    public static final int KILOBYTE_TO_BYTE = 1024;
    public static final long EXPECT_TIME_PERCENT = 20000;
    public static final int SPEED_KB = 1;
    public static final int SPEED_MB = 0;

    /**
     * URL
     */
    public static final String PING = "181.176.254.106";
    public static final String URL = "http://181.176.254.106:8165/qos-service/api/file/";
    public static final String URL_DOWNLOAD = "http://181.176.254.106:8165/qos-service/api/file/download";
    public static final String URL_UPLOAD = "http://181.176.254.106:8165/qos-service/api/file/upload";


    /**
     * States
     */
    public static final int MSG_START_PROCESS = 998;
    public static final int MSG_UPDATE_STATUS = 0;
    public static final int MSG_COMPLETE_STATUS = 2;
    public static final int MSG_CONNECTING = 997;
    public static final int MSG_SERVICE_COMMUNICATION_ERROR = 999;

}
