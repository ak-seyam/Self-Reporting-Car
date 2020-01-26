package com.example.src.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SpeedPojo {
    @SerializedName("speed")
    @Expose
    private double speed;

    @SerializedName("endingLong")
    @Expose
    private double endingLong;

    @SerializedName("endingLat")
    @Expose
    private double endingLat;

    public double getEndingLong() {
        return endingLong;
    }

    public void setEndingLong(double endingLong) {
        this.endingLong = endingLong;
    }

    public double getEndingLat() {
        return endingLat;
    }

    public void setEndingLat(double endingLat) {
        this.endingLat = endingLat;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }
}
