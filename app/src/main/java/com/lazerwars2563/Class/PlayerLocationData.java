package com.lazerwars2563.Class;

import com.google.firebase.firestore.ServerTimestamp;

public class PlayerLocationData {
    GeoSpot geoSpot;
    private String userId = "";
    private @ServerTimestamp
    String timestamp;

    public PlayerLocationData(GeoSpot geoPoint, String userId, String timestamp) {
        this.geoSpot = geoPoint;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public PlayerLocationData(String userId) {
        this.userId = userId;
    }

    public PlayerLocationData() {
    }

    public void setGeoSpot(GeoSpot geoPoint) {
        this.geoSpot = geoPoint;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PlayerLocationData{" +
                "geoSpot=" + geoSpot +
                ", userId='" + userId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }

    public boolean equals(PlayerLocationData data)
    {
        if(data.geoSpot.equals(geoSpot) && compareIdAndTime(data)) {
            return true;
        }
        return false;
    }

    private boolean compareIdAndTime(PlayerLocationData data)
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
