package com.lazerwars2563.Activitys;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.collection.ArraySet;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.lazerwars2563.Handler.AudioHandler;
import com.lazerwars2563.Class.Message;
import com.lazerwars2563.Class.PlayerViewer;
import com.lazerwars2563.Handler.GameDatabaseHandler;
import com.lazerwars2563.Handler.GameMakerHandler;
import com.lazerwars2563.Handler.GameMapHandler;
import com.lazerwars2563.Handler.GamePlayHandler;
import com.lazerwars2563.Handler.MessagesHandler;
import com.lazerwars2563.Handler.CustomTimer;
import com.lazerwars2563.util.UserClient;
import com.lazerwars2563.Class.UserDetails;
import com.lazerwars2563.R;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameActivity extends FragmentActivity implements OnMapReadyCallback {
    private static String TAG = "GameActivity";

    private boolean showAll = false;

    private boolean isAdmin;
    int score = 0;

    //map
    private RelativeLayout mMapContainer;
    private RelativeLayout mMainContainer;
    private ImageView dragView;
    private MapView mapView;
    private TextView timerText;

    //fireStore db
    private FirebaseFirestore db;
    private DocumentReference roomStoreRef;

    //firebase realTime db
    private FirebaseDatabase database;
    private DatabaseReference roomRef;

    //players maps:
    private Map<String, Integer> teamsMap;
    private Map<String, String> usersNameMap;
    private Map<String, String> imageMap;

    private String roomName;
    private String gameType;


    private GoogleMap mGoogleMap;

    //cluster marker
    private GameMakerHandler gameMakerHandler;
    private MessagesHandler messagesHandler;
    private GameDatabaseHandler gameDatabaseHandler;
    private GameMapHandler gameMapHandler;
    private GamePlayHandler gamePlayHandler;

    private UserDetails user;
    private PlayerViewer userData;

    private AudioHandler audioHandler;
    private String audioTo;

    private ImageButton recordButton;

    private int[] teamsColorArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);//dont allow rotation

        //set RealTime db
        database = FirebaseDatabase.getInstance();
        roomRef = database.getReference("Rooms");

        //get extra
        Bundle extras = getIntent().getExtras();
        roomName = extras.getString("name");
        gameType = extras.getString("game");
        isAdmin = extras.getBoolean("admin");

        UserDetails();
        //get all data from firestore
        GetFireStoreData();

        mMapContainer = findViewById(R.id.relative_layout_map);
        mMainContainer = findViewById(R.id.relative_layout_main);
        dragView = findViewById(R.id.dragView);
        mapView = findViewById(R.id.map_view);
        timerText = findViewById(R.id.game_timer_view);

        //initGoogleMap(savedInstanceState);
        gameMapHandler = new GameMapHandler( mapView, this, this,mMapContainer,mMainContainer,dragView,savedInstanceState);

        //set spinner
        Spinner spinnerType = findViewById(R.id.audio_to_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.audio_to, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(spinnerAdapter);
        spinnerType.setOnItemSelectedListener(new AudioSpinnerClass());

        //put all other actions onMapReady
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            Log.e(TAG, "couldnt fined permission ACCESS_FINE_LOCATION");
            return;
        }
        googleMap.setMyLocationEnabled(true);
        mGoogleMap = googleMap;

        SetVoiceMessage();

        //create markers for players
        gameMakerHandler = new GameMakerHandler( userData, mGoogleMap, GameActivity.this,  teamsMap,  usersNameMap,  imageMap, showAll, database,roomName);
        //set listeners to database
        gameDatabaseHandler = new GameDatabaseHandler( gameMakerHandler ,database,  roomRef,  roomName,  userData, GameActivity.this);
        gameMakerHandler.CreateUserListener();

        //finish game map handler
        gameMapHandler.setGameDatabaseHandler(gameDatabaseHandler);
        gameMapHandler.UpdateUserData(userData.getId(),score,mGoogleMap);//for the first time

        SetScoreView();

        //SetChat();
        RecyclerView recyclerView = findViewById(R.id.recyclerview_messages);
        messagesHandler = new MessagesHandler(GameActivity.this, recyclerView, roomRef, roomName, userData);

        gamePlayHandler = new GamePlayHandler(roomStoreRef, timerText, roomRef, userData, roomName, isAdmin, messagesHandler,this,usersNameMap);
        gamePlayHandler.SetStartListener(gameMakerHandler);

    }

    //gets and sets UserDetails - need to call only once
    private void UserDetails() {
        user = UserClient.getInstance().getUser();
        UserClient.getInstance().setCurrentRoom(roomName);
        UserClient.getInstance().setCurrentScore(score);
        userData = new PlayerViewer(user.getUserName(), user.getUserId());
    }


    private void SetScoreView() {
        teamsColorArray = GameActivity.this.getResources().getIntArray(R.array.TeamsColor);
        Log.d(TAG, "SetScoreView: the userTeam: " + userData.getTeam());
        CardView scoreCard = findViewById(R.id.score_card_view);
        scoreCard.setCardBackgroundColor(teamsColorArray[userData.getTeam()]);
    }

    private ArraySet<String> audioDownLoaded;//keep trak of what has been downloaded allready
    private void SetVoiceMessage() {
        audioHandler = new AudioHandler(this, userData.getId(), roomName, roomRef);//
        audioDownLoaded = new ArraySet<>();

        recordButton = findViewById(R.id.record_button);

        recordButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        audioHandler.startRecording();
                        recordButton.setImageResource(R.drawable.ic_mic_red_24dp);
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        audioHandler.stopRecording(audioTo);
                        recordButton.setImageResource(R.drawable.ic_mic_black_24dp);
                        return true; // if you want to handle the touch event
                }
                return false;
            }
        });
        roomRef.child(roomName).child("chat").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "CreateUserListener: updating The Players Data from database");
                for (DataSnapshot data : snapshot.getChildren()) {
                    if (!audioDownLoaded.contains(data.getKey())) {
                        audioDownLoaded.add(data.getKey());
                        String to = audioHandler.getName(data.getKey());
                        if (to.equals("all") || to.equals(Integer.toString(userData.getTeam())))//check that this is intended to me (to all or to team)
                            audioHandler.DownloadAudio(data.getValue().toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    //init teamsMap usersNameMap imageMap
    private void GetFireStoreData() {
        // Access a Cloud FireStore instance from your Activity
        db = FirebaseFirestore.getInstance();
        roomStoreRef = db.collection("Rooms").document(roomName);
        // get show all field - show all players on map or only my team
        roomStoreRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                showAll = task.getResult().getBoolean("showAll");
            }
        });

        teamsMap = new HashMap<>();
        usersNameMap = new HashMap<>();
        imageMap = new HashMap<>();

        roomStoreRef.collection("Players").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Log.d(TAG, "copying data from fireStore");
                    List<PlayerViewer> downloadInfoList = task.getResult().toObjects(PlayerViewer.class);
                    for (PlayerViewer playerInfo : downloadInfoList) {
                        if (playerInfo.getId().equals(userData.getId()))//set my team
                        {
                            userData.setTeam(playerInfo.getTeam());
                        }
                        teamsMap.put(playerInfo.getId(), playerInfo.getTeam());
                        usersNameMap.put(playerInfo.getId(), playerInfo.getName());
                        LoadImage(playerInfo.getId());
                    }
                } else {
                    Log.d(TAG, "get failed with", task.getException());
                    //retry?
                }
            }
        });
    }

    private void LoadImage(String id) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File myFile = new File(directory, id + ".jpg");
        if (myFile.exists()) {
            Log.d(TAG, "LoadImage: getting icon as int");
            imageMap.put(id, myFile.getAbsolutePath());
        } else {
            imageMap.put(id, "");
            Log.d(TAG, "LoadImage: no image for " + id);
        }
    }

    class AudioSpinnerClass implements AdapterView.OnItemSelectedListener
    {
        public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
        {
            audioTo = parent.getItemAtPosition(position).toString();
            if(audioTo.equals("team"))
            {
                audioTo = Integer.toString(userData.getTeam());
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }


    @Override
    protected void onStart() {
        mapView.onStart();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        DestroyHandlers();
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

    //leaving waiting Room
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Are you sure you want to leave this room?")
                .setCancelable(false)
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DestroyHandlers();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void DestroyHandlers()
    {
        gameDatabaseHandler.DestroyHendler();
        gameMakerHandler.DestroyHandler();
        gamePlayHandler.QuitGame();
    }

}
