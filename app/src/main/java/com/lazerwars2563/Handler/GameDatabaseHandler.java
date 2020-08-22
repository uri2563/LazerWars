package com.lazerwars2563.Handler;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lazerwars2563.Class.PlayerLocationData;
import com.lazerwars2563.Class.PlayerViewer;
import com.lazerwars2563.services.DataService;

import java.util.HashMap;
import java.util.Map;

public class GameDatabaseHandler {
    private static String TAG = "GameDatabaseHandler";

    private Intent dataServiceIntent;

    //from activity
    private Map<String, PlayerLocationData> players;
    private GameMakerHandler gameMakerHandler = null;
    private FirebaseDatabase database;
    private DatabaseReference roomRef;
    private String roomName;
    private PlayerViewer userData;
    private Context context;


    public GameDatabaseHandler(GameMakerHandler gameMakerHandler,FirebaseDatabase database, DatabaseReference roomRef, String roomName, PlayerViewer userData, Context context ) {
        this.gameMakerHandler = gameMakerHandler;
        this.database = database;
        this.roomRef = roomRef;
        this.roomName = roomName;
        this.userData = userData;
        this.context = context;

    }

    //saves manually (only once) first data of the current user
    //and calls the CreateUserListener func
    //will start the DataService (calls startLocationService func)
    boolean firstInit = true;
    public void SaveUserDataInDb(PlayerLocationData myData)
        {
        roomRef.child(roomName).child("UsersLocation").child(userData.getId()).setValue(myData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG,"SaveUserDataInDb: saving user Data in DB");
                if(firstInit)
                {
                    firstInit = false;
                    startDataService();
                }
            }
        });
    }

  /*  private boolean isDataServiceRunning() {
       ActivityManager manager = (ActivityManager)context.getSystemService(context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("com.codingwithmitch.googledirectionstest.services.LocationService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.");
        return false;
    }*/

    boolean isDataServiceRunning = false;
    private void startDataService(){
        if(!isDataServiceRunning){
            isDataServiceRunning = true;
            dataServiceIntent = new Intent(context, DataService.class);
            Log.d(TAG,"startLocationService: StartLocationService");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
               context.startForegroundService(dataServiceIntent);
            }else{
                context.startService(dataServiceIntent);
            }
        }
    }


    public void DestroyHendler()
    {
        //stop service
        if(isDataServiceRunning)
        {
            Log.d(TAG,"RemoveListenersAndServices: Stop services");
            isDataServiceRunning = false;
            context.stopService(dataServiceIntent);
        }
    }


}

