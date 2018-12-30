package com.spartronics4915.frc2019.vision;

/**
 * A container class for Targets detected by the vision system, containing the
 * location in three-dimensional space, as well as their timestamps
 */
public class TargetInfo {
    protected double x = 1.0;
    protected double y;
    protected double z;

    double timestamp;

    public TargetInfo(double y, double z, double timestamp) {
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getTimestamp() {
        return timestamp;
    }
}