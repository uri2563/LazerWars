package com.lazerwars2563;

import java.util.ArrayList;

public class Room {
    private String name;
    private String type;

    public Room(String name, String game) {
        this.name = name;
        this.type = game;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String game) {
        this.type = game;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

}
