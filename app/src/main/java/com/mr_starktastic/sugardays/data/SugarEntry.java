package com.mr_starktastic.sugardays.data;

import android.text.TextUtils;

import org.apache.commons.lang3.time.FastDateFormat;

import java.util.ArrayList;
import java.util.Comparator;

public class SugarEntry {
    public static FastDateFormat TIME_FORMAT =
            FastDateFormat.getTimeInstance(FastDateFormat.SHORT);
    public static FastDateFormat DATE_FORMAT =
            FastDateFormat.getInstance(FastDateFormat.getInstance("EEE, ").getPattern() +
                    FastDateFormat.getDateInstance(FastDateFormat.MEDIUM).getPattern());

    private int type;
    private long time;
    private String location;
    private String photoPath;
    private int primaryColor;
    private BloodSugar bg;
    private ArrayList<Food> foods;
    private Float carbSum, corrBolus, mealBolus, basal;
    private TempBasal tempBasal;
    private ArrayList<Pill> pills;
    private String notes;

    public SugarEntry() {
        // Empty default constructor
    }

    public static Comparator<SugarEntry> getCompByTime() {
        return new Comparator<SugarEntry>() {
            @Override
            public int compare(SugarEntry l1, SugarEntry l2) {
                final long diff = l1.time - l2.time;
                return diff < 0 ? -1 : diff > 0 ? 1 : 0;
            }
        };
    }

    public int getType() {
        return type;
    }

    public SugarEntry setType(int type) {
        this.type = type;
        return this;
    }

    public long getTime() {
        return time;
    }

    public SugarEntry setTime(long time) {
        this.time = time;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public SugarEntry setLocation(String location) {
        this.location = !TextUtils.isEmpty(location) ? location : null;
        return this;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public SugarEntry setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
        return this;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public SugarEntry setPrimaryColor(int primaryColor) {
        this.primaryColor = primaryColor;
        return this;
    }

    public BloodSugar getBloodSugar() {
        return bg;
    }

    public SugarEntry setBloodSugar(BloodSugar bg) {
        this.bg = bg;
        return this;
    }

    public ArrayList<Food> getFoods() {
        return foods;
    }

    public SugarEntry setFoods(ArrayList<Food> foods) {
        this.foods = foods;
        return this;
    }

    public Float getCarbSum() {
        return carbSum;
    }

    public SugarEntry setCarbSum(Float carbSum) {
        this.carbSum = carbSum;
        return this;
    }

    public Float getCorrBolus() {
        return corrBolus;
    }

    public SugarEntry setCorrBolus(Float corrBolus) {
        this.corrBolus = corrBolus;
        return this;
    }

    public Float getMealBolus() {
        return mealBolus;
    }

    public SugarEntry setMealBolus(Float mealBolus) {
        this.mealBolus = mealBolus;
        return this;
    }

    public Float getBasal() {
        return basal;
    }

    public SugarEntry setBasal(Float basal) {
        this.basal = basal;
        return this;
    }

    public TempBasal getTempBasal() {
        return tempBasal;
    }

    public SugarEntry setTempBasal(TempBasal tempBasal) {
        this.tempBasal = tempBasal;
        return this;
    }

    public ArrayList<Pill> getPills() {
        return pills;
    }

    public SugarEntry setPills(ArrayList<Pill> pills) {
        this.pills = pills;
        return this;
    }

    public String getNotes() {
        return notes;
    }

    public SugarEntry setNotes(String notes) {
        this.notes = !TextUtils.isEmpty(notes) ? notes : null;
        return this;
    }
}
