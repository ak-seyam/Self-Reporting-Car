package com.example.src.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ObstaclePojo {
    @SerializedName("longitude")
    @Expose
    private double currentLong;
    @SerializedName("latitude")
    @Expose
    private double currentLat;

    public double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(double currentLat) {
        this.currentLat = currentLat;
    }

    public double getCurrentLong() {
        return currentLong;
    }

    public void setCurrentLong(double currentLong) {
        this.currentLong = currentLong;
    }
}
