package com.mr_starktastic.sugardays.data;

public class Pill {
    public float quantity;
    public String name;

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
}
