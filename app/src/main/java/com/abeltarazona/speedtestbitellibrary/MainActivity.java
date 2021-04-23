package com.abeltarazona.speedtestbitellibrary;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.MemoryFile;
import android.os.Message;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.abeltarazona.speedtestbitellibrary.databinding.ActivityMainBinding;
import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import static com.abeltarazona.speedtestbitellibrary.SpeedConstants.MSG_COMPLETE_STATUS;
import static com.abeltarazona.speedtestbitellibrary.SpeedConstants.MSG_CONNECTING;
import static com.abeltarazona.speedtestbitellibrary.SpeedConstants.MSG_SERVICE_COMMUNICATION_ERROR;
import static com.abeltarazona.speedtestbitellibrary.SpeedConstants.MSG_START_PROCESS;
import static com.abeltarazona.speedtestbitellibrary.SpeedConstants.MSG_UPDATE_STATUS;
import static com.abeltarazona.speedtestbitellibrary.SpeedConstants.SPEED_MB;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Thread mainThread = null;

    private ActivityMainBinding binding;

    private History history = new History();


    /**
     * PING
     */
    private int timeOutCount = 0;
    private int packetsFinished = 0;
    private long totalLatencyTime = 0;
    private long mAvgLatency = 0;
    private long maxLatencyTime = 0;
    private long minLatencyTime = SpeedConstants.TIME_OUT;
    private long[] packetsTime = new long[SpeedConstants.PACKET_COUNT];
    private ArrayList<Integer> mCountPing = new ArrayList<>();
    private ArrayList<Double> mPingSpeeds = new ArrayList<>();

    /**
     * DOWNLOAD
     */
    private long maxSizeToDownload = 0;
    private long totalDownload = 0;
    public double maxDownSpeed = 0;
    private long mTotalDownloadTime = 0;
    private long mTotalDownloadData = 0;
    public double maxUploadSpeed = 0;
    private ArrayList<Integer> mCountDownload = new ArrayList<>();
    private ArrayList<Double> mDownloadSpeeds = new ArrayList<>();
    public double avgDownloadSpeed = 0;

    /**
     * UPLOAD
     */
    private long maxSizeToUpload = 0;
    private long totalUpload = 0;
    private long mTotalUploadTime = 0;
    private long mTotalUploadData = 0;
    private ArrayList<Integer> mCountUpload = new ArrayList<>();
    private ArrayList<Double> mUploadSpeeds = new ArrayList<>();
    public double avgUploadSpeed = 0;

    /**
     * GENERAL
     */
    private String startTime = "";
    private String endTime = "";
    private int mSpeedTestUnit;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.button.setOnClickListener(view -> {
            clearData();
            mSpeedTestUnit = SpeedConstants.SPEED_MB;
            runProcess();
        });
        binding.button2.setOnClickListener(view -> {
            clearData();
            mSpeedTestUnit = SpeedConstants.SPEED_KB;
            runProcess();
        });
    }

    private void clearData() {
        timeOutCount = 0;
        packetsFinished = 0;
        totalLatencyTime = 0;
        mAvgLatency = 0;
        maxLatencyTime = 0;
        minLatencyTime = SpeedConstants.TIME_OUT;
        packetsTime = new long[SpeedConstants.PACKET_COUNT];
        mCountPing.clear();
        mPingSpeeds.clear();

        maxSizeToDownload = 0;
        totalDownload = 0;
        maxDownSpeed = 0;
        mTotalDownloadTime = 0;
        mTotalDownloadData = 0;
        maxUploadSpeed = 0;
        mCountDownload.clear();
        mDownloadSpeeds.clear();
        avgDownloadSpeed = 0;

        maxSizeToUpload = 0;
        totalUpload = 0;
        mTotalUploadTime = 0;
        mTotalUploadData = 0;
        mCountUpload.clear();
        mUploadSpeeds.clear();
        avgUploadSpeed = 0;

        startTime = "";
        endTime = "";
    }

    private void runProcess() {
        mainThread = new Thread(UdpLatencenyTester);
        mainThread.start();
    }

    private Runnable UdpLatencenyTester = () -> {

        // reset the timeout counter
        timeOutCount = 0;

        // reset number of finished packets
        packetsFinished = 0;

        totalLatencyTime = 0;

        mAvgLatency = 0;

        // reset timers
        maxLatencyTime = 0;
        minLatencyTime = SpeedConstants.TIME_OUT;

        // reset packet data
        packetsTime = new long[SpeedConstants.PACKET_COUNT];

        // rest latency results
        mCountPing.clear();

        mPingSpeeds.clear();

        for (int i = 0; i < SpeedConstants.PACKET_COUNT; i++) {

            long t1 = System.currentTimeMillis();
            UDPThread thread = new UDPThread(i);
            thread.start();
            long t2 = System.currentTimeMillis();

            long timeToSleep = (SpeedConstants.TIME_TO_SEND - (t2 - t1));

            try {
                Thread.sleep(timeToSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    };

    private class UDPThread extends Thread {

        int packetNumber;

        UDPThread(int packetNumber) {
            this.packetNumber = packetNumber;
        }

        @Override
        public void run() {
            DatagramSocket socket = null;
            long RTT = 0; // round trip time
            boolean timeOut = false;

            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(SpeedConstants.TIME_OUT);
                InetAddress address = InetAddress.getByName(SpeedConstants.PING);

                if (address == null) {
                    throw new NullPointerException();
                }

                byte[] sendBuf = new byte[SpeedConstants.PACKET_SIZE];
                byte[] buf = new byte[SpeedConstants.RECEIVE_BUFFER];

                DatagramPacket sendPacket =
                        new DatagramPacket(sendBuf, sendBuf.length, address, SpeedConstants.SERVER_PORT);

                DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

                // send request and count time
                long t1 = System.currentTimeMillis();
                socket.send(sendPacket);

                // get response
                try {
                    socket.receive(receivePacket);
                } catch (Exception e) {
                    // time out happened
                    e.printStackTrace();
                    timeOutCount++;
                    timeOut = true;
                }

                // get time after sending
                long t2 = System.currentTimeMillis();

                // RTT = time to send and time to receive
                RTT = t2 - t1;

                packetsTime[packetNumber] = RTT;

                // calculateSpeed max and min time
                if (RTT > maxLatencyTime && !timeOut) {
                    maxLatencyTime = RTT;
                } else if (RTT < minLatencyTime && !timeOut) {
                    minLatencyTime = RTT;
                }

                // only count time of successful packets
                if (!timeOut) {
                    totalLatencyTime += RTT;
                }

            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }

            // finish this packet
            finishPacket();
            mCountPing.add(packetsFinished);
            mPingSpeeds.add((double) RTT);

            SpeedInfo speedInfo1 = new SpeedInfo();
            speedInfo1.type = SpeedInfo.FunctionType.PING_TYPE.ordinal();
            Message msg = Message.obtain(mHandler, MSG_UPDATE_STATUS, speedInfo1);
            msg.arg1 = packetsFinished;
            msg.obj = speedInfo1;
            mHandler.sendMessage(msg);

            // check to see if this is the last packet
            if (packetsFinished > SpeedConstants.PACKET_COUNT - 1) {

                int successPackets = (packetsFinished - timeOutCount);

                try {
                    mAvgLatency = totalLatencyTime / successPackets;
                } catch (Exception e) {
                    mAvgLatency = 0;
                }


                SpeedInfo speedInfo = new SpeedInfo();
                speedInfo.type = SpeedInfo.FunctionType.PING_TYPE.ordinal();
                speedInfo.minLatency = minLatencyTime;
                speedInfo.latency = mAvgLatency;
                speedInfo.lossPackages = ((double) timeOutCount / SpeedConstants.PACKET_COUNT) * 100;
                speedInfo.variationLatency = SpeedUtils.calculateJitter(mPingSpeeds);

                final Message message = Message.obtain(mHandler, MSG_COMPLETE_STATUS, speedInfo);
                mHandler.sendMessage(message);
            }
        }
    }

    public synchronized void finishPacket() {
        packetsFinished++;
    }

    // DOWNLOAD

    private Runnable mDownloadWorker = () -> {
        // Begin Download
        maxSizeToDownload = 0;
        processDownload();
    };

    private void processDownload() {

        InputStream stream = null;
        try {
            // Start download
            totalDownload = 0;
            maxDownSpeed = 0;
            mTotalDownloadTime = 0;
            mTotalDownloadData = 0;

            maxSizeToDownload = 50 * 1024;

            byte[] buffer = new byte[50 * 1024];
            Message msgStart = Message.obtain(mHandler, MSG_START_PROCESS, null);
            msgStart.arg1 = SpeedInfo.FunctionType.DOWNLOAD_TYPE.ordinal();
            mHandler.sendMessage(msgStart);
            int bytesIn = 0;

            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(SpeedConstants.URL_DOWNLOAD + "?n=" + maxSizeToDownload);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                stream = entity.getContent();


                new Thread(mCalculateDownloadWorker).start();

                int currentByte;
                long start = System.currentTimeMillis();

                while ((currentByte = stream.read(buffer)) != -1 && (System.currentTimeMillis() - start < SpeedConstants.EXPECT_TIME)) {
                    bytesIn += currentByte;
                    totalDownload += currentByte;
                }

                long downloadTime = (System.currentTimeMillis() - start);

                Log.d(TAG, "Total download time:" + downloadTime);
                Log.d(TAG, "Total download data:" + totalDownload);


                if (downloadTime == 0) {
                    downloadTime = 1;
                }

                mTotalDownloadTime = downloadTime;
                mTotalDownloadData = totalDownload;
                totalDownload = 0;

                avgDownloadSpeed = mTotalDownloadData / (double) mTotalDownloadTime;

                SpeedInfo speedInfo = calculateSpeed(downloadTime, totalDownload, SpeedInfo.FunctionType.DOWNLOAD_TYPE.ordinal());
                speedInfo.downspeed = mSpeedTestUnit == SPEED_MB ? avgDownloadSpeed * SpeedConstants.KILOBYTE_TO_MEGABIT : avgDownloadSpeed;
                speedInfo.downloadSpeed = SpeedUtils.convertKBToBit(avgDownloadSpeed);
                speedInfo.maxDownloadSpeed = SpeedUtils.convertKBToBit(maxDownSpeed);

                Message msg = Message.obtain(mHandler, MSG_COMPLETE_STATUS, speedInfo);

                msg.arg1 = bytesIn;
                mHandler.sendMessage(msg);
                Log.d(TAG, "Download Complete");

            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            processServiceCommunicationError();
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Runnable mCalculateDownloadWorker = this::processCalculateSpeedDownload;

    private void processCalculateSpeedDownload() {
        long start = System.currentTimeMillis();
        long updateStart = System.currentTimeMillis();
        long startDownload = System.currentTimeMillis();
        long updateDelta = 0;
        int count = 0;

        while (totalDownload > 0) {
            if ((System.currentTimeMillis() - startDownload >= SpeedConstants.FRAME_UPDATE_TIME)) {

                if (updateDelta >= SpeedConstants.UPDATE_THRESHOLD) {
                    SpeedInfo speedInfo = calculateSpeed(System.currentTimeMillis() - start, totalDownload, SpeedInfo.FunctionType.DOWNLOAD_TYPE.ordinal());

                    if (speedInfo.kilobytes > 0) {
                        long timeToDownloading = System.currentTimeMillis() - start;
                        double percent = (double) timeToDownloading / SpeedConstants.EXPECT_TIME_PERCENT;
                        int progress = (int) (percent * 100);

                        mCountDownload.add(++count);
                        mDownloadSpeeds.add(speedInfo.kilobytes);
                        Message msg = Message.obtain(mHandler, MSG_UPDATE_STATUS, speedInfo);
                        msg.arg1 = progress;
                        msg.arg2 = (int) totalDownload;
                        mHandler.sendMessage(msg);
                        //Reset
                        updateStart = System.currentTimeMillis();
                    }
                }
            } else {
                start = System.currentTimeMillis();
                updateStart = System.currentTimeMillis();
                totalDownload = 1;
            }
            updateDelta = System.currentTimeMillis() - updateStart;
        }
    }


    private SpeedInfo calculateSpeed(final long downloadTime, final long bytesIn, int type) {
        SpeedInfo info = new SpeedInfo();
        info.type = type;

        double bytesPerSecond = (bytesIn / downloadTime) * 1000;

        double kiloBit = bytesPerSecond * SpeedConstants.BYTE_TO_KILOBIT;
        double megaBit = kiloBit * SpeedConstants.KILOBIT_TO_MEGABIT;
        double kiloBytes = bytesPerSecond / SpeedConstants.KILOBYTE_TO_BYTE;

        if (type == SpeedInfo.FunctionType.DOWNLOAD_TYPE.ordinal()) {
            info.downspeed = bytesPerSecond;

            if (kiloBytes > maxDownSpeed) {
                maxDownSpeed = kiloBytes;
            }
        } else if (type == SpeedInfo.FunctionType.UPLOAD_TYPE.ordinal()) {
            info.upspeed = bytesPerSecond;

            if (kiloBytes > maxUploadSpeed) {
                maxUploadSpeed = kiloBytes;
            }
        }

        info.kilobit = kiloBit;
        info.megabit = megaBit;
        info.kilobytes = kiloBytes;

        return info;
    }

    // UPLOAD
    private Runnable mUploadWorker = () -> {
        // Begin Upload
        maxSizeToUpload = 0;
        processUpload();
    };

    private void processUpload() {

        mTotalUploadData = 0;
        mTotalUploadTime = 0;
        totalUpload = 0;
        maxUploadSpeed = 0;
        maxSizeToUpload = 50 * 1024;
        long EXPECTED_UPLOAD_SIZE_IN_BYTES = maxSizeToUpload * 1024;
        Message msgStart = Message.obtain(mHandler, MSG_START_PROCESS, null);
        msgStart.arg1 = SpeedInfo.FunctionType.UPLOAD_TYPE.ordinal();
        mHandler.sendMessage(msgStart);
        String fileName = "file_";

        HttpURLConnection conn;
        OutputStream outputStream = null;
        MemoryFile memoryFile = null;
        InputStream fileInputStream = null;
        URL url;
        final String LINE_FEED = "\r\n";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024;

        try {

            Random mRandom = new Random();
            byte[] bytesToUpload = new byte[(int) EXPECTED_UPLOAD_SIZE_IN_BYTES];
            mRandom.nextBytes(bytesToUpload);
            String fileNameUpload = "upload" + System.currentTimeMillis() + ".bin";
            memoryFile = new MemoryFile(fileNameUpload, (int) EXPECTED_UPLOAD_SIZE_IN_BYTES);
            memoryFile.writeBytes(bytesToUpload, 0, 0, (int) EXPECTED_UPLOAD_SIZE_IN_BYTES);
            url = new URL(SpeedConstants.URL_UPLOAD);
            fileInputStream = memoryFile.getInputStream();

            // Open a HTTP  connection to  the URL
            String boundary = "" + System.currentTimeMillis() + "";
            conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setDoOutput(true); // indicates POST method
            conn.setDoInput(true);
            conn.setChunkedStreamingMode(1024);

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-length", String.valueOf(fileInputStream.available()));
            Log.d("Bitelito fileNameUpload", fileInputStream.available() + "");

            outputStream = conn.getOutputStream();

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "utf-8"),
                    true);
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + "uploadForm" + "\"")
                    .append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=" + "utf-8").append(
                    LINE_FEED);
            writer.append(LINE_FEED);
            writer.append("1").append(LINE_FEED);
            writer.flush();
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append(
                    "Content-Disposition: form-data; name=\"" + "uploadForm"
                            + "\"; filename=\"" + fileName + "\"")
                    .append(LINE_FEED);
            writer.append(
                    "Content-Type: "
                            + URLConnection.guessContentTypeFromName(fileName))
                    .append(LINE_FEED);
            writer.append("charset=" + "utf-8").append(
                    LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();
            outputStream.flush();

            // create a buffer of  maximum size
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            int bytesOut = 0;
            // read file and write it into form...
            long start = System.currentTimeMillis();
            int bytesInThreshold = 0;

            new Thread(mCalculateUploadWorker).start();
            // Add file Part
            while ((bytesRead = fileInputStream.read(buffer, 0, bufferSize)) != -1 && (System.currentTimeMillis() - start < (SpeedConstants.EXPECT_TIME))) {
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                outputStream.write(buffer, 0, bytesRead);
                bytesOut += bytesRead;
                bytesInThreshold += bytesRead;

                totalUpload += bytesRead;
            }

            outputStream.flush();

            writer.append(LINE_FEED);
            // finish
            writer.append(LINE_FEED).append("--").append(boundary).append("--")
                    .append(LINE_FEED);
            writer.close();

            Log.d(TAG, "End upload");

            long uploadTime = System.currentTimeMillis() - start;

            Log.d(TAG, "Total upload Time: " + uploadTime);
            Log.d(TAG, "Total upload Data: " + totalUpload);
            mTotalUploadData = totalUpload;
            mTotalUploadTime = uploadTime;
            totalUpload = 0;


            long downloadTime = (System.currentTimeMillis() - start);

            if (downloadTime == 0) {
                downloadTime = 1;
            }

            avgUploadSpeed = mTotalUploadData / (double) mTotalUploadTime;

            SpeedInfo speedInfo = calculateSpeed(downloadTime, bytesOut, SpeedInfo.FunctionType.UPLOAD_TYPE.ordinal());
            speedInfo.upspeed = mSpeedTestUnit == SPEED_MB ? avgUploadSpeed * SpeedConstants.KILOBYTE_TO_MEGABIT : avgUploadSpeed;
            speedInfo.uploadSpeed = SpeedUtils.convertKBToBit(avgUploadSpeed);
            speedInfo.maxUploadSpeed = SpeedUtils.convertKBToBit(maxUploadSpeed);

            Message msg = Message.obtain(mHandler, MSG_COMPLETE_STATUS, speedInfo);
            msg.arg1 = bytesOut;
            mHandler.sendMessage(msg);
            Log.d(TAG, "Upload Complete");

        } catch (Exception e) {
            processServiceCommunicationError();
            Log.e("Upload file to server", "error: " + e.getMessage(), e);
        } finally {
            try {
                outputStream.close();
                fileInputStream.close();
                memoryFile.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private Runnable mCalculateUploadWorker = this::processCalculateSpeedUpload;

    private void processCalculateSpeedUpload() {
        long start = System.currentTimeMillis();
        long updateStart = System.currentTimeMillis();
        long startUpload = System.currentTimeMillis();
        long updateDelta = 0;
        int count = 0;

        while (totalUpload > 0) {

            if (updateDelta >= SpeedConstants.UPDATE_THRESHOLD) {

                if ((System.currentTimeMillis() - startUpload >= SpeedConstants.FRAME_UPDATE_TIME)) {
                    SpeedInfo speedInfo = calculateSpeed(System.currentTimeMillis() - start, totalUpload, SpeedInfo.FunctionType.UPLOAD_TYPE.ordinal());

                    if (speedInfo.kilobytes > 0) {

                        long timeToDownloading = System.currentTimeMillis() - start;
                        double percent = (double) timeToDownloading / SpeedConstants.EXPECT_TIME_PERCENT;
                        int progress = (int) (percent * 100);
                        mCountUpload.add(++count);
                        mUploadSpeeds.add(speedInfo.kilobytes);
                        Message msg = Message.obtain(mHandler, MSG_UPDATE_STATUS, speedInfo);
                        msg.arg1 = progress;
                        msg.arg2 = (int) totalUpload;
                        mHandler.sendMessage(msg);
                        //Reset
                        updateStart = System.currentTimeMillis();
                    }
                } else {
                    start = System.currentTimeMillis();
                    updateStart = System.currentTimeMillis();
                    totalUpload = 1;
                }
            }
            updateDelta = System.currentTimeMillis() - updateStart;
        }
    }


    // GENERAL

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(final Message msg) {

            switch (msg.what) {
                case MSG_START_PROCESS:
                    int functionType = msg.arg1;
                    // Get Start Time for Test
                    if (functionType == SpeedInfo.FunctionType.PING_TYPE.ordinal()) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ");
                        startTime = simpleDateFormat.format(Calendar.getInstance().getTime());
                    }

                    if (functionType == SpeedInfo.FunctionType.UPLOAD_TYPE.ordinal()) {
                    }

                    if (functionType == SpeedInfo.FunctionType.DOWNLOAD_TYPE.ordinal()) {
                    }
                    break;
                case MSG_UPDATE_STATUS:

                    try {
                        final SpeedInfo info = (SpeedInfo) msg.obj;

                        if (info.type == SpeedInfo.FunctionType.PING_TYPE.ordinal()) {

                            int pg = (msg.arg1 + 1) * 100 / SpeedConstants.PACKET_COUNT;
                            binding.textView5.setText(pg + " %");

                        } else if (info.type == SpeedInfo.FunctionType.DOWNLOAD_TYPE.ordinal()) {

                            binding.textView10.setText(msg.arg1 + " %");

                            Double mbps = mSpeedTestUnit == SPEED_MB ? SpeedUtils.convertKibyteToMbps(info.kilobytes) : info.kilobytes;
                            String dataMbps = mSpeedTestUnit == SPEED_MB ? SpeedUtils.formatSpeed(mbps, SpeedConstants.SPEED_MB) + " Mbps" : SpeedUtils.formatSpeed(mbps, SpeedConstants.SPEED_KB) + " Kb/s";
                            binding.textView11.setText(dataMbps);

                        } else if (info.type == SpeedInfo.FunctionType.UPLOAD_TYPE.ordinal()) {
                            binding.textView13.setText(msg.arg1 + " %");
                            Double mbps = mSpeedTestUnit == SPEED_MB ? SpeedUtils.convertKibyteToMbps(info.kilobytes) : info.kilobytes;
                            String dataMbps = mSpeedTestUnit == SPEED_MB ? SpeedUtils.formatSpeed(mbps, SpeedConstants.SPEED_MB) + " Mbps" : SpeedUtils.formatSpeed(mbps, SpeedConstants.SPEED_KB) + " Kb/s";
                            binding.textView14.setText(dataMbps);

                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case MSG_COMPLETE_STATUS:

                    try {
                        final SpeedInfo info = (SpeedInfo) msg.obj;

                        if (info.type == SpeedInfo.FunctionType.PING_TYPE.ordinal()) {
                            binding.textView5.setText("100 %");
                            binding.textView6.setText(info.latency + " ms");
                            binding.textView7.setText(info.variationLatency + " ms");
                            binding.textView8.setText(info.lossPackages + " %");

                            history.setLatency(info.latency);
                            history.setVariationLatency(info.variationLatency);
                            history.setLossPackages(info.lossPackages);
                            history.setMinLatency(info.minLatency);

                            new Handler(Looper.getMainLooper()).postDelayed(() -> new Thread(mDownloadWorker).start(), 1200);
                        } else if (info.type == SpeedInfo.FunctionType.DOWNLOAD_TYPE.ordinal()) {
                            binding.textView10.setText("100 %");
                            String dataMbps = mSpeedTestUnit == SPEED_MB ? SpeedUtils.formatSpeed(info.downspeed, SpeedConstants.SPEED_MB) + " Mbps" : SpeedUtils.formatSpeed(info.downspeed, SpeedConstants.SPEED_KB) + " Kb/s";
                            binding.textView11.setText(dataMbps);

                            history.setDownloadSpeed(info.downloadSpeed);
                            history.setMaxDownloadSpeed(info.maxDownloadSpeed);

                            new Handler(Looper.getMainLooper()).postDelayed(() -> new Thread(mUploadWorker).start(), 1200);

                        } else if (info.type == SpeedInfo.FunctionType.UPLOAD_TYPE.ordinal()) {
                            binding.textView13.setText("100 %");

                            String dataMbps = mSpeedTestUnit == SPEED_MB ? SpeedUtils.formatSpeed(info.upspeed, SpeedConstants.SPEED_MB) + " Mbps" : SpeedUtils.formatSpeed(info.upspeed, SpeedConstants.SPEED_KB) + " Kb/s";
                            binding.textView14.setText(dataMbps);

                            history.setUploadSpeed(info.uploadSpeed);
                            history.setMaxUploadSpeed(info.maxUploadSpeed);

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ");
                            endTime = simpleDateFormat.format(Calendar.getInstance().getTime());

                            Log.d("Abel", new Gson().toJson(history));
                        }

/*                            if (evaluateNoErrors(info2)) {
                                completeAnProcess(info2);

                                if (info2.type == SpeedInfo.FunctionType.DOWNLOAD_TYPE.ordinal()) {
                                    mSpeedometerView.calculateAngleOfDeviation(-100, mSpeedTestUnit);
                                    int progress = 100;
                                    mProgressBar.setProgress(progress);

                                } else if (info2.type == SpeedInfo.FunctionType.UPLOAD_TYPE.ordinal()) {
                                    mSpeedometerView.calculateAngleOfDeviation(-100, mSpeedTestUnit);
                                    mStartButton.setVisibility(View.VISIBLE);
                                    int progress = 100;
                                    mProgressBar.setProgress(progress);

                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ");
                                    endTime = simpleDateFormat.format(Calendar.getInstance().getTime());
                                    mProgressBar.setProgress(0);
                                    // Store History
                                    createHistory();
                                }
                            }*/

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case MSG_CONNECTING:
                    //mSpeedometerView.calculateAngleOfDeviation(-100, mSpeedTestUnit);
                    break;
                case MSG_SERVICE_COMMUNICATION_ERROR:
/*                        beginProcess();
                        showAlertBox(getActivity().getResources().getString(R.string.service_connection_issue));
                        mSpeedometerView.calculateAngleOfDeviation(-100, mSpeedTestUnit);*/
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private void processServiceCommunicationError() {
        Message msgStart = Message.obtain(mHandler, MSG_SERVICE_COMMUNICATION_ERROR, null);
        mHandler.sendMessage(msgStart);
    }


}