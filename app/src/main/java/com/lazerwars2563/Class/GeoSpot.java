package com.lazerwars2563.Class;

import com.google.firebase.firestore.GeoPoint;

public class GeoSpot {
    double latitude;
    double longitude;

    private static final double ACCURACY_RATE = 0.000005;//the Accuracy

    public GeoSpot(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GeoSpot(GeoPoint geoPoint) {
        latitude = geoPoint.getLatitude();
        longitude = geoPoint.getLongitude();
    }

    public GeoSpot() {
    }

    public GeoPoint ConvertToGeoPoint()
    {
        return new GeoPoint(latitude,longitude);
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return "GeoSpot{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    public boolean equals(GeoSpot spot)
    {
        if(spot.longitude - longitude < ACCURACY_RATE && spot.latitude - latitude < ACCURACY_RATE)
        {
            return true;
        }
        return false;
    }
}
