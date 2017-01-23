package com.simoncherry.averageface;

/**
 * Created by Simon on 2017/1/23.
 */

public class Landmark {

    private int x;
    private int y;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Landmark{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
