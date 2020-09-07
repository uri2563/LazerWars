package com.lazerwars2563.Class;

public class PlayerViewer {
    String name;
    String id;
    String arduinoId;
    int team;

    public  PlayerViewer()
    {}

    public PlayerViewer(String name, String id, int team, String arduinoId) {
        this.name = name;
        this.id = id;
        this.team = team;
        this.arduinoId = arduinoId;
    }
    public PlayerViewer(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public String getArduinoId() {
        return arduinoId;
    }

    public void setArduinoId(String arduinoId) {
        this.arduinoId = arduinoId;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
