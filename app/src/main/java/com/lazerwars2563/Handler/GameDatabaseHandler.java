package com.lazerwars2563.Handler;

import android.app.ActivityManager;
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
import com.lazerwars2563.Activitys.GameActivity;
import com.lazerwars2563.Class.PlayerData;
import com.lazerwars2563.Class.PlayerViewer;
import com.lazerwars2563.services.CustomTimer;
import com.lazerwars2563.services.DataService;

import java.util.HashMap;
import java.util.Map;

public class GameDatabaseHandler {
    private static String TAG = "GameDatabaseHandler";

    private CustomTimer.ChangeListener listener;
    private Intent dataServiceIntent;

    //from activity
    private Map<String, PlayerData> players;
    private GameMakerHandler gameMakerHandler = null;
    private FirebaseDatabase database;
    private DatabaseReference roomRef;
    private String roomName;
    private PlayerViewer userData;
    private Context context;


    public GameDatabaseHandler(GameMakerHandler gameMakerHandler,FirebaseDatabase database, DatabaseReference roomRef, String roomName, PlayerViewer userData, Context context ) {
        setPlayers();
        this.gameMakerHandler = gameMakerHandler;
        this.database = database;
        this.roomRef = roomRef;
        this.roomName = roomName;
        this.userData = userData;
        this.context = context;

    }

    //create the listener for the users data
    private void CreateUserListener() {
        Log.d(TAG, "CreateUserListener: UserListener initiated");
        DatabaseReference usersRef = database.getReference("Rooms/" + roomName + "/users");
        ValueEventListener userListener = usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "CreateUserListener: updating The Players Data from database");
                for (DataSnapshot data : snapshot.getChildren()) {
                    PlayerData player = data.getValue(PlayerData.class);
                    players.put(player.getUserId(), player);
                }
                if(gameMakerHandler != null) {
                    gameMakerHandler.MoveMapMarkers(players);
                }
                //Show UserData in view
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    //if players change!
    public Map<String, PlayerData> getPlayers() {
        return players;
    }

    //saves manually (only once) first data of the current user
    //and calls the CreateUserListener func
    //will start the DataService (calls startLocationService func)
    boolean firstInit = true;
    public void SaveUserDataInDb(PlayerData myData)
        {
        roomRef.child(roomName).child("users").child(userData.getId()).setValue(myData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if(firstInit)
                {
                    firstInit = false;
                    CreateUserListener();
                    startDataService();
                }
            }
        });
    }
    private boolean isDataServiceRunning() {
        ActivityManager manager = (ActivityManager)context.getSystemService(context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("com.codingwithmitch.googledirectionstest.services.LocationService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.");
        return false;
    }

    private void startDataService(){
        if(!isDataServiceRunning()){
            dataServiceIntent = new Intent(context, DataService.class);
//        this.startService(serviceIntent);
            Log.d(TAG,"startLocationService: StartLocationService");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){

                context.startForegroundService(dataServiceIntent);
            }else{
                context.startService(dataServiceIntent);
            }
        }
    }
    public void stopDataService()
    {
        if(isDataServiceRunning())
        {
            Log.d(TAG,"RemoveListenersAndServices: Stop services");
            context.stopService(dataServiceIntent);
        }
    }

    public void setPlayers() {
        this.players  = new HashMap<>();
        if (listener != null) listener.onChange();
    }

    public CustomTimer.ChangeListener getListener() {
        return listener;
    }

    public void setListener(CustomTimer.ChangeListener listener) {
        this.listener = listener;
    }

    public interface ChangeListener {
        void onChange();
    }


}
