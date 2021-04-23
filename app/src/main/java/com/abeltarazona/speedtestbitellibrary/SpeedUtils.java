package com.abeltarazona.speedtestbitellibrary;

import java.util.ArrayList;

/**
 * Created by AbelTarazona on 20/04/2021
 */
public class SpeedUtils {
    public static double calculateJitter(ArrayList<Double> latencys) {
        double totalDifference = 0;
        int count = 0;

        for (int i = 0; i < latencys.size() - 1; i++) {
            Double first = latencys.get(i);
            Double second = latencys.get(i + 1);
            totalDifference += Math.abs(first - second);
            count += 1;
        }

        return Math.round(totalDifference / count);
    }

    public static String formatSpeed(double speed, int speedUnit) {

        if (speedUnit == SpeedConstants.SPEED_MB) {
            return String.format("%.2f", speed);
        } else {
            return String.format("%.1f", speed);
        }
    }

    public static double convertKBToBit(Double kB) {
        if (kB != null) {
            return kB * 8000;
        }
        return 0.0;
    }

    public static double convertBitToMegabit(Double bit) {
        if (bit != null) {
            return (bit / 1e+6);
        }
        return 0.0;
    }

    public static double convertBitToKb(Double bit) {
        if (bit != null) {
            return (bit / 8000);
        }
        return 0.0;
    }

    public static double convertKibyteToMbps(Double bit) {
        if (bit != null) {
            return (bit * SpeedConstants.KILOBYTE_TO_MEGABIT);
        }
        return 0.0;
    }
}
