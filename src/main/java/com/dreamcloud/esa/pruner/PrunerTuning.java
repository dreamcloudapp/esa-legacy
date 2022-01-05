package com.dreamcloud.esa.pruner;

public class PrunerTuning {
    protected double tunedScore;
    protected int tunedWindowSize;
    protected double tunedWindowDropOff;

    public PrunerTuning(double tunedScore, int tunedWindowSize, double tunedWindowDropOff) {
        this.tunedScore = tunedScore;
        this.tunedWindowSize = tunedWindowSize;
        this.tunedWindowDropOff = tunedWindowDropOff;
    }

    public double getTunedScore() {
        return tunedScore;
    }

    public double getTunedWindowDropOff() {
        return tunedWindowDropOff;
    }

    public int getTunedWindowSize() {
        return tunedWindowSize;
    }
}
