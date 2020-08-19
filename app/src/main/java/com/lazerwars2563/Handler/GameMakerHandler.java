package com.lazerwars2563.Handler;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;
import com.lazerwars2563.Class.ClusterMarker;
import com.lazerwars2563.Class.PlayerData;
import com.lazerwars2563.Class.PlayerViewer;
import com.lazerwars2563.util.MyClusterManagerRenderer;

import java.util.HashMap;
import java.util.Map;

public class GameMakerHandler {
    private static String TAG = "addMapMarkers";

    //cluster marker
    private ClusterManager clusterManager;
    private MyClusterManagerRenderer clusterManagerRenderer;
    private Map<String, ClusterMarker> clusterMarkers = new HashMap<>();//list of marker currently on the map

    //getting from activity
    private Map<String, PlayerData> players;
    private PlayerViewer userData;
    private GoogleMap mGoogleMap;
    private Context context;

    private Map<String, Integer> teamsMap;
    private Map<String, String> usersNameMap;
    private Map<String, String> imageMap;

    private boolean showAll;

    public GameMakerHandler(Map<String, PlayerData> players, PlayerViewer userData, GoogleMap mGoogleMap,
                            Context context, Map<String, Integer> teamsMap, Map<String, String> usersNameMap, Map<String, String> imageMap, boolean showAll) {
        this.players = players;
        this.userData = userData;
        this.mGoogleMap = mGoogleMap;
        this.context = context;
        this.teamsMap = teamsMap;
        this.usersNameMap = usersNameMap;
        this.imageMap = imageMap;
        this.showAll = showAll;

        addMapMarkers();
    }

    //set players makers
    //Debug - set Image and team Color
    public void addMapMarkers()
    {
        if(mGoogleMap != null) {
            if (clusterManager == null) {
                clusterManager = new ClusterManager<ClusterMarker>(context, mGoogleMap);
            }
            if(clusterManagerRenderer == null)
            {
                clusterManagerRenderer = new MyClusterManagerRenderer(
                        context,
                        mGoogleMap,
                        clusterManager
                );
                clusterManager.setRenderer(clusterManagerRenderer);
            }
            for (Map.Entry<String, String> entry1 : usersNameMap.entrySet()) {
                String id = entry1.getKey();
                String name = entry1.getValue();
                int team = teamsMap.get(id);//can set color of team in the background
                Log.d(TAG, "addMapMarkers: add marker: "+ name);
                Log.d(TAG, "addMapMarkers: my team: "+ userData.getTeam());
                //if not current user
                if (!id.equals(userData.getId()) && (showAll || (team == userData.getTeam())))
                {//show only members of the same team
                    try {
                        String snippet = "Score: 0";//: " + player.getScore();
                        String avatar = imageMap.get(id) ;
                        ClusterMarker newClusterMarker = new ClusterMarker(
                                team,
                                new LatLng(0,0),
                                name,
                                snippet,
                                avatar,
                                id
                        );
                        clusterManager.addItem(newClusterMarker);
                        clusterMarkers.put(id,newClusterMarker);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage());
                    }
                }
            }
            clusterManager.cluster();
        }
    }

    public void MoveMapMarkers() {
        Log.d(TAG, "MoveMapMarkers: moving Markers");
        clusterManager.clearItems();
        mGoogleMap.clear();
        for (Map.Entry<String, ClusterMarker> marker : clusterMarkers.entrySet()) {
            if (players.containsKey(marker.getKey())) {
                Log.d(TAG, "MoveMapMarkers: marker: " + marker.getValue().toString());
                PlayerData player = players.get(marker.getKey());
                marker.getValue().setPosition(new LatLng(player.getGeoSpot().getLatitude(), player.getGeoSpot().getLongitude()));
                marker.getValue().setSnippet("Score: " + marker.getValue());
                Log.d(TAG, "MoveMapMarkers: after: " + marker.getValue().toString());
                ClusterMarker newMarker = new ClusterMarker(marker.getValue());

                clusterManager.addItem(newMarker);//
            }
        }
        clusterManager.cluster();
    }
}
