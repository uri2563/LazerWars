package com.lazerwars2563.Class;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class ClusterMarker implements ClusterItem {
    private  int team;
    private LatLng position;
    private String title;
    protected String snippet;
    private String iconPicture;
    private String userId;

    public ClusterMarker(int team,LatLng position, String title, String snippet, String iconPicture, String userId) {
        this.team = team;
        this.position = position;
        this.title = title;
        this.snippet = snippet;
        this.iconPicture = iconPicture;
        this.userId = userId;
    }

    public ClusterMarker(ClusterMarker marker)
    {
        this.team = marker.team;
        this.position = marker.position;
        this.title = marker.title;
        this.snippet = marker.snippet;
        this.iconPicture = marker.iconPicture;
        this.userId = marker.userId;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    @Nullable
    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return snippet;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getIconPicture() {
        return iconPicture;
    }

    public void setIconPicture(String iconPicture) {
        this.iconPicture = iconPicture;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "ClusterMarker{" +
                "team=" + team +
                ", position=" + position +
                ", title='" + title + '\'' +
                ", snippet='" + snippet + '\'' +
                ", iconPicture='" + iconPicture + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
