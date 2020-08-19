package com.lazerwars2563.Class;

import java.util.ArrayList;

public class Room {
    private String name;
    private String game;
    private boolean recruiting;

    public  Room()
    {
    }

    public Room(String name, String game, boolean recruiting) {
        this.name = name;
        this.game = game;
        this.recruiting = recruiting;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public void setRecruiting(boolean recruiting) {
        this.recruiting = recruiting;
    }

    public String getName() {
        return name;
    }

    public String getGame() {
        return game;
    }

    public boolean isRecruiting() {
        return recruiting;
    }
}
