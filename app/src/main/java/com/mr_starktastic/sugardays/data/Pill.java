package com.mr_starktastic.sugardays.data;

import com.mr_starktastic.sugardays.util.NumericTextUtil;

public class Pill {
    private float quantity;
    private String name;

    public Pill(float quantity, String name) {
        this.quantity = quantity;
        this.name = name;
    }

    public float getQuantity() {
        return quantity;
    }

    public void setQuantity(float quantity) {
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return NumericTextUtil.trim(quantity) + " \u00D7 " + name;
    }
}
