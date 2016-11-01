package com.mr_starktastic.sugardays.data;

import android.annotation.SuppressLint;

import java.io.Serializable;

public class TempBasal implements Serializable {
    private int hours, minutes, rate;
    private TempBasal oldInstance;

    public TempBasal(TempBasal other) {
        if (other != null) {
            hours = other.hours;
            minutes = other.minutes;
            rate = other.rate;
        }

        oldInstance = other;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public boolean isSameAsOld() {
        return oldInstance != null && hours == oldInstance.hours &&
                minutes == oldInstance.minutes && rate == oldInstance.rate;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("%02d:%02d - %d%%", hours, minutes, rate);
    }
}
