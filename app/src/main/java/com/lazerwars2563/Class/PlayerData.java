package com.lazerwars2563.Class;

import android.util.Log;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

public class PlayerData {
    GeoSpot geoSpot;
    private String userId = "";
    private int score = 0;
    private @ServerTimestamp
    String timestamp;

    public PlayerData(GeoSpot geoPoint, String userId, int score, String timestamp) {
        this.geoSpot = geoPoint;
        this.userId = userId;
        this.score = score;
        this.timestamp = timestamp;
    }

    public PlayerData(String userId) {
        this.userId = userId;
    }

    public PlayerData() {
    }

    public void setGeoSpot(GeoSpot geoPoint) {
        this.geoSpot = geoPoint;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public GeoSpot getGeoSpot() {
        return geoSpot;
    }

    public String getUserId() {
        return userId;
    }

    public int getScore() {
        return score;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "geoSpot=" + geoSpot +
                ", userId='" + userId + '\'' +
                ", score=" + score +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }

    public boolean equals(PlayerData data)
    {
        if(data.score == score && data.geoSpot.equals(geoSpot) && compareIdAndTime(data)) {
            return true;
        }
        return false;
    }

    private boolean compareIdAndTime(PlayerData data)
    {
        if (((data.timestamp == null && timestamp == null) || data.timestamp.equals(timestamp))
        &&(data.userId == null && userId == null) || data.userId.equals(userId)
        )
        {
            return true;
        }
        return false;
    }
}
