package com.lazerwars2563.util;

import com.lazerwars2563.Class.UserDetails;

import java.util.Map;

public class UserClient{

    private static UserClient userInstance;
    private UserDetails user;
    private int currentScore;
    private String currentRoom;
    private String gameId;

    public void setUser(UserDetails user)
    {
        userInstance.user = user;
        currentScore = 0;
        currentRoom ="";
    }

    public UserDetails getUser()
    {
        return userInstance.user;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public void setCurrentScore(int currentScore) {
        this.currentScore = currentScore;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    public static UserClient getInstance()
    {
        if(userInstance == null) {
            userInstance = new UserClient();
        }

        return userInstance;
    }
}
