package com.mr_starktastic.sugardays.data;

import android.support.v7.graphics.Palette;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Comparator;

public class SugarLog {
    private int type;
    private long time;
    private String location;
    private String photoPath;
    private Palette.Swatch swatch;
    private BloodSugar bg;
    private ArrayList<Food> foods;
    private Float carbSum, corrBolus, mealBolus, basal;
    private TempBasal tempBasal;
    private ArrayList<Pill> pills;
    private String notes;

    public SugarLog() {
        // Empty default constructor
    }

    public static Comparator<SugarLog> getCompByTime() {
        return new Comparator<SugarLog>() {
            @Override
            public int compare(SugarLog l1, SugarLog l2) {
                final long diff = l1.time - l2.time;
                return diff < 0 ? -1 : diff > 0 ? 1 : 0;
            }
        };
    }

    public int getType() {
        return type;
    }

    public SugarLog setType(int type) {
        this.type = type;
        return this;
    }

    public long getTime() {
        return time;
    }

    public SugarLog setTime(long time) {
        this.time = time;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public SugarLog setLocation(String location) {
        this.location = !TextUtils.isEmpty(location) ? location : null;
        return this;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public SugarLog setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
        return this;
    }

    public Palette.Swatch getSwatch() {
        return swatch;
    }

    public SugarLog setSwatch(Palette.Swatch swatch) {
        this.swatch = swatch;
        return this;
    }

    public BloodSugar getBloodSugar() {
        return bg;
    }

    public SugarLog setBloodSugar(BloodSugar bg) {
        this.bg = bg;
        return this;
    }

    public ArrayList<Food> getFoods() {
        return foods;
    }

    public SugarLog setFoods(ArrayList<Food> foods) {
        this.foods = foods;
        return this;
    }

    public Float getCarbSum() {
        return carbSum;
    }

    public SugarLog setCarbSum(Float carbSum) {
        this.carbSum = carbSum;
        return this;
    }

    public Float getCorrBolus() {
        return corrBolus;
    }

    public SugarLog setCorrBolus(Float corrBolus) {
        this.corrBolus = corrBolus;
        return this;
    }

    public Float getMealBolus() {
        return mealBolus;
    }

    public SugarLog setMealBolus(Float mealBolus) {
        this.mealBolus = mealBolus;
        return this;
    }

    public Float getBasal() {
        return basal;
    }

    public SugarLog setBasal(Float basal) {
        this.basal = basal;
        return this;
    }

    public TempBasal getTempBasal() {
        return tempBasal;
    }

    public SugarLog setTempBasal(TempBasal tempBasal) {
        this.tempBasal = tempBasal;
        return this;
    }

    public ArrayList<Pill> getPills() {
        return pills;
    }

    public SugarLog setPills(ArrayList<Pill> pills) {
        this.pills = pills;
        return this;
    }

    public String getNotes() {
        return notes;
    }

    public SugarLog setNotes(String notes) {
        this.notes = !TextUtils.isEmpty(notes) ? notes : null;
        return this;
    }
}
