package com.abeltarazona.speedtestbitellibrary;

public class SpeedInfo {
    public enum FunctionType {PING_TYPE, DOWNLOAD_TYPE, UPLOAD_TYPE}

    public int      type;
    public long     ping;
    public double   kilobit;
    public double   kilobytes   = 0;
    public double   megabit     = 0;
    public double   downspeed   = 0;
    public double   upspeed     = 0;

    /**
     * PING
     */
    public double   latency;
    public double   minLatency;
    public double   variationLatency;
    public double   lossPackages;

    /**
     * DOWNLOAD
     */
    public double   downloadSpeed;
    public double   maxDownloadSpeed;

    /**
     * UPLOAD
     */
    public double   uploadSpeed;
    public double   maxUploadSpeed;
}


