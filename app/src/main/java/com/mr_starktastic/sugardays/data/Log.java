package com.mr_starktastic.sugardays.data;

import android.text.TextUtils;

import com.google.android.gms.location.places.Place;

import java.util.ArrayList;

public class Log {
    private int type;
    private long time;
    private Place place;
    private String location;
    private String photoPath;
    private BloodSugar bg;
    private ArrayList<Food> foods;
    private Float carbSum, corrBolus, mealBolus, basal;
    private TempBasal tempBasal;
    private ArrayList<Pill> pills;
    private String notes;

    public Log() {
        // Empty default constructor
    }

    public static String getLocationText(Place place) {
        final String address = place.getAddress().toString(), name = place.getName().toString();
        return !address.isEmpty() ?
                !address.contains(name) ? name + ", " + address : address : name;
    }

    public int getType() {
        return type;
    }

    public Log setType(int type) {
        this.type = type;
        return this;
    }

    public long getTime() {
        return time;
    }

    public Log setTime(long time) {
        this.time = time;
        return this;
    }

    public String getLocation() {
        return place != null ? getLocationText(place) : location;
    }

    public Log setLocation(String location) {
        this.location = !TextUtils.isEmpty(location) ? location.trim() : null;
        return this;
    }

    public Log setPlace(Place place) {
        this.place = place;
        return this;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public Log setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
        return this;
    }

    public BloodSugar getBloodSugar() {
        return bg;
    }

    public Log setBloodSugar(BloodSugar bg) {
        this.bg = bg;
        return this;
    }

    public ArrayList<Food> getFoods() {
        return foods;
    }

    public Log setFoods(ArrayList<Food> foods) {
        this.foods = foods;
        return this;
    }

    public Float getCarbSum() {
        return carbSum;
    }

    public Log setCarbSum(Float carbSum) {
        this.carbSum = carbSum;
        return this;
    }

    public Float getCorrBolus() {
        return corrBolus;
    }

    public Log setCorrBolus(Float corrBolus) {
        this.corrBolus = corrBolus;
        return this;
    }

    public Float getMealBolus() {
        return mealBolus;
    }

    public Log setMealBolus(Float mealBolus) {
        this.mealBolus = mealBolus;
        return this;
    }

    public Float getBasal() {
        return basal;
    }

    public Log setBasal(Float basal) {
        this.basal = basal;
        return this;
    }

    public TempBasal getTempBasal() {
        return tempBasal;
    }

    public Log setTempBasal(TempBasal tempBasal) {
        this.tempBasal = tempBasal;
        return this;
    }

    public ArrayList<Pill> getPills() {
        return pills;
    }

    public Log setPills(ArrayList<Pill> pills) {
        this.pills = pills;
        return this;
    }

    public String getNotes() {
        return notes;
    }

    public Log setNotes(String notes) {
        this.notes = notes;
        return this;
    }
}
