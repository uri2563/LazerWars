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

    private GameDatabaseHandler.ChangeListener listener;
    private Intent dataServiceIntent;

    private ValueEventListener userListener;
    private DatabaseReference usersRef;

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

    //create the listener for the users data
    private void CreateUserListener() {
        Log.d(TAG, "CreateUserListener: UserListener initiated");
        usersRef = database.getReference("Rooms/" + roomName + "/UsersLocation");
        userListener = usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "CreateUserListener: updating The Players Data from database");
                setPlayers();
                for (DataSnapshot data : snapshot.getChildren()) {
                    PlayerLocationData player = data.getValue(PlayerLocationData.class);
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
    public Map<String, PlayerLocationData> getPlayers() {
        return players;
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
                    CreateUserListener();
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


    public void setPlayers() {
        this.players  = new HashMap<>();
        if (listener != null) listener.onChange();
    }

    public GameDatabaseHandler.ChangeListener getListener() {
        return listener;
    }

    public void setListener(GameDatabaseHandler.ChangeListener listener) {
        this.listener = listener;
    }

    public interface ChangeListener {
        void onChange();
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

        if(usersRef != null && userListener != null) {//Debug - needed?
            Log.d(TAG,"RemoveListenersAndServices: removing listeners");
            usersRef.removeEventListener(userListener);
        }
    }


}

