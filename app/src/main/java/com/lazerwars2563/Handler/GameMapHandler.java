package com.lazerwars2563.Handler;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.lazerwars2563.Activitys.GameActivity;
import com.lazerwars2563.Class.GeoSpot;
import com.lazerwars2563.Class.PlayerData;
import com.lazerwars2563.R;
import com.lazerwars2563.services.DataService;
import com.lazerwars2563.util.UserClient;
import com.lazerwars2563.util.ViewWeightAnimationWrapper;

import static com.lazerwars2563.util.Constants.MAPVIEW_BUNDLE_KEY;


public class GameMapHandler implements View.OnTouchListener{
    private static String TAG = "GameMapHandler";


    private FusedLocationProviderClient fusedLocationProviderClient;
    private Intent dataServiceIntent;
    private LatLngBounds mapBoundary;
    float window_height;



    //over all map view window [MapZoom X MapZoom]
    private static final double MapZoom = 0.005;//smaller the value bigger the map zoom

    //from activity
    private MapView mapView;
    private Context context;
    private OnMapReadyCallback onMapReadyCallback;

    private RelativeLayout mMapContainer;
    private RelativeLayout mMainContainer;
    private ImageView dragView;

    //after Cunstractor
    private GameDatabaseHandler gameDatabaseHandler;

    public GameMapHandler(MapView mapView, Context context, OnMapReadyCallback onMapReadyCallback, RelativeLayout mMapContainer, RelativeLayout mMainContainer, ImageView dragView, Bundle savedInstanceState) {
        this.mapView = mapView;
        this.context = context;
        this.onMapReadyCallback = onMapReadyCallback;
        this.mMapContainer = mMapContainer;
        this.mMainContainer = mMainContainer;
        this.dragView = dragView;

        initGoogleMap(savedInstanceState);
    }

    //starts the map view and prossess
    private void initGoogleMap(Bundle savedInstanceState) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        // Gets the MapView from the XML layout and creates it
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(onMapReadyCallback);

        changeSize();
    }

    //change map size
    private void changeSize() {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        window_height = displayMetrics.heightPixels;

        dragView.setOnTouchListener(this);
    }

    //updates in app and calls SaveUserDataInDb to save in db
    //to update Score just change score and call updateUserData
    public void UpdateUserData(final String id, final int score, final GoogleMap mGoogleMap) {
        Log.d(TAG, "UpdateUserData Called");
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    GeoSpot geoPoint = new GeoSpot(location.getLatitude(), location.getLongitude());

                    PlayerData newData = new PlayerData(geoPoint,id,score,null);

                    UserClient.getInstance().setCurrentScore(score);
                    SetCameraView(newData,mGoogleMap);

                    gameDatabaseHandler.SaveUserDataInDb(newData);
                }
            }
        });
    }

    //creates the map bounds
    public void SetCameraView(final PlayerData myData, GoogleMap mGoogleMap)
    {
        double bottomBoundary = myData.getGeoSpot().getLatitude() - MapZoom;
        double leftBoundary = myData.getGeoSpot().getLongitude() - MapZoom;
        double topBoundary = myData.getGeoSpot().getLatitude() + MapZoom;
        double rightBoundary = myData.getGeoSpot().getLongitude() + MapZoom;

        mapBoundary = new LatLngBounds(
                new LatLng(bottomBoundary,leftBoundary),
                new LatLng(topBoundary,rightBoundary)
        );

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mapBoundary,0));
    }


    int map_current_weight = 50;
    private void moveMapAnimation(float drag, float height){
        int map_new_weight = (int) drag;
        Log.d(TAG, "window_height - map_new_weight: " + (window_height - map_new_weight));

        if(window_height - map_new_weight >= 50)//dont let the map dissapear
        {
            ViewWeightAnimationWrapper mapAnimationWrapper = new ViewWeightAnimationWrapper(mMapContainer);
            ObjectAnimator mapAnimation = ObjectAnimator.ofFloat(mapAnimationWrapper,
                    "weight",
                    map_current_weight,
                    map_new_weight);
            mapAnimation.setDuration(0);

            ViewWeightAnimationWrapper recyclerAnimationWrapper = new ViewWeightAnimationWrapper(mMainContainer);
            ObjectAnimator recyclerAnimation = ObjectAnimator.ofFloat(recyclerAnimationWrapper,
                    "weight",
                    window_height - height - map_new_weight,
                    window_height - height - map_current_weight);
            recyclerAnimation.setDuration(0);
            map_current_weight = map_new_weight;
            recyclerAnimation.start();
            mapAnimation.start();
        }
    }

    private float dY;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.dragView) {
            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    dY = v.getY() - event.getRawY();

                case MotionEvent.ACTION_MOVE:
                    moveMapAnimation(event.getRawY() + dY, v.getHeight());
            }

            return true;
        }
        return false;
    }

    public void setGameDatabaseHandler(GameDatabaseHandler gameDatabaseHandler) {
        this.gameDatabaseHandler = gameDatabaseHandler;
    }
}
