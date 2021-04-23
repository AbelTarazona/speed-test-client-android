package com.abeltarazona.speedtestbitellibrary;

/**
 * Created by AbelTarazona on 20/04/2021
 */
public class History {
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

    public double getLatency() {
        return latency;
    }

    public void setLatency(double latency) {
        this.latency = latency;
    }

    public double getMinLatency() {
        return minLatency;
    }

    public void setMinLatency(double minLatency) {
        this.minLatency = minLatency;
    }

    public double getVariationLatency() {
        return variationLatency;
    }

    public void setVariationLatency(double variationLatency) {
        this.variationLatency = variationLatency;
    }

    public double getLossPackages() {
        return lossPackages;
    }

    public void setLossPackages(double lossPackages) {
        this.lossPackages = lossPackages;
    }

    public double getDownloadSpeed() {
        return downloadSpeed;
    }

    public void setDownloadSpeed(double downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public double getMaxDownloadSpeed() {
        return maxDownloadSpeed;
    }

    public void setMaxDownloadSpeed(double maxDownloadSpeed) {
        this.maxDownloadSpeed = maxDownloadSpeed;
    }

    public double getUploadSpeed() {
        return uploadSpeed;
    }

    public void setUploadSpeed(double uploadSpeed) {
        this.uploadSpeed = uploadSpeed;
    }

    public double getMaxUploadSpeed() {
        return maxUploadSpeed;
    }

    public void setMaxUploadSpeed(double maxUploadSpeed) {
        this.maxUploadSpeed = maxUploadSpeed;
    }
}
