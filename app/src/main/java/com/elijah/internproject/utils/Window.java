package com.elijah.internproject.utils;

public class Window {
    private static final double Q = 0.5;

    public double gausseFrame(double n, double frameSize) {
        double a = (frameSize - 1.0) / 2.0;
        double t = (n - a) / (Q * a);
        t = t * t;
        return Math.exp(-t / 2.0);
    }
}
