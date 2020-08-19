package com.lazerwars2563.Class;

import android.graphics.Color;

public class TeamItem {
    private String name;
    private int color;

    public TeamItem(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }
}
