package com.lazerwars2563.Activitys;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.collection.ArraySet;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.maps.android.clustering.ClusterManager;
import com.lazerwars2563.Class.AudioHandler;
import com.lazerwars2563.Class.ClusterMarker;
import com.lazerwars2563.Class.GeoSpot;
import com.lazerwars2563.Class.Message;
import com.lazerwars2563.Class.PlayerData;
import com.lazerwars2563.Class.PlayerViewer;
import com.lazerwars2563.Class.Room;
import com.lazerwars2563.adapters.MessageAdapter;
import com.lazerwars2563.adapters.RoomsAdapter;
import com.lazerwars2563.services.DataService;
import com.lazerwars2563.util.MyClusterManagerRenderer;
import com.lazerwars2563.util.UserClient;
import com.lazerwars2563.Class.UserDetails;
import com.lazerwars2563.R;
import com.lazerwars2563.util.ViewWeightAnimationWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lazerwars2563.util.Constants.MAPVIEW_BUNDLE_KEY;

public class GameActivity extends FragmentActivity implements OnMapReadyCallback, View.OnTouchListener {
    private static String TAG = "GameActivity";

    private boolean showAll = false;

    private ImageView dragView;
    float window_height;

    private boolean isAdmin;
    int score = 0;

    private RelativeLayout mMapContainer;
    private RelativeLayout mMainContainer;

    //fireStore db
    private FirebaseFirestore db;
    private DocumentReference roomStoreRef;

    //firebase realTime db
    private FirebaseDatabase database;
    private DatabaseReference roomRef;
    private DatabaseReference usersRef;
    private DatabaseReference messageRef;

    //players maps:
    private Map<String, Integer> teamsMap;
    private Map<String, String> usersNameMap;
    private Map<String, String> imageMap;

    private String roomName;
    private String gameType;

    private MapView mapView;
    private FusedLocationProviderClient fusedLocationProviderClient;
    //map
    private PlayerData myData;
    private GoogleMap mGoogleMap;
    private LatLngBounds mapBoundary;

    //over all map view window [MapZoom X MapZoom]
    private static final double MapZoom = 0.005;//smaller the value bigger the map zoom

    //cluster marker
    private ClusterManager clusterManager;
    private MyClusterManagerRenderer clusterManagerRenderer;
    private Map<String,ClusterMarker> clusterMarkers = new HashMap<>();//list of marker currently on the map

    private UserDetails user;
    private PlayerViewer userData;

    private Map<String,PlayerData> players = new HashMap<>();

    private ValueEventListener userListener;
    private Intent dataServiceIntent;

    private AudioHandler audioHandler;
    private String audioTo;

    private ImageButton recordButton;

    private RecyclerView recyclerView;
    private MessageAdapter mMessageAdapter;

    private int[] teamsColorArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);//dont allow rotation

        initGoogleMap(savedInstanceState);
        changeSize();

        //set spinner
        Spinner spinnerType = findViewById(R.id.audio_to_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,R.array.audio_to,android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(spinnerAdapter);
        spinnerType.setOnItemSelectedListener(new AudioSpinnerClass());

        //put all other actions onMapReady
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //set RealTime db
        database = FirebaseDatabase.getInstance();
        roomRef = database.getReference("Rooms");
        googleMap.setMyLocationEnabled(true);
        mGoogleMap = googleMap;

        //get game Data
        GetExtras();
        UserDetails();
        GetFireStoreData();
        myData = new PlayerData(userData.getId());

        SetVoiceMessage();

        UpdateUserData();//for the first time

        SetChat();
    }

    private void SetScoreView() {
        teamsColorArray = GameActivity.this.getResources().getIntArray(R.array.TeamsColor);
        Log.d(TAG,"SetScoreView: the userTeam: "+ userData.getTeam());
        CardView scoreCard = findViewById(R.id.score_card_view);
        scoreCard.setCardBackgroundColor(teamsColorArray[userData.getTeam()]);
    }

    private boolean IsMessageToMe(String t)
    {
        if(t.equals(userData.getId()) || t.equals(Integer.toString(userData.getTeam()))|| t.equals("all"))
        {
            return true;
        }
        return false;
    }

    private void SetChat() {
        Log.d(TAG,"setUpRecyclerView ");
        recyclerView = findViewById(R.id.recyclerview_messages);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        //linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        messageRef = roomRef.child(roomName).child("Messages");

        messageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<Message> arrayList = new ArrayList<>();
                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
                    if(IsMessageToMe(eventSnapshot.getValue(Message.class).getTo()))
                    {arrayList.add(eventSnapshot.getValue(Message.class));}
                }
                mMessageAdapter = new MessageAdapter(arrayList, GameActivity.this,userData);
                recyclerView.setAdapter(mMessageAdapter);
                recyclerView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);//scroll to end
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private ArraySet<String> audioDownLoaded;//keep trak of what has been downloaded allready
    private void SetVoiceMessage() {
        audioHandler = new AudioHandler(this,userData.getId(), roomName, roomRef);//
        audioDownLoaded = new ArraySet<>();

        recordButton = findViewById(R.id.record_button);

        recordButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
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
                    if(!audioDownLoaded.contains(data.getKey())) {
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

    //change map size
    private void changeSize()
    {
        mMapContainer = findViewById(R.id.relative_layout_map);
        mMainContainer = findViewById(R.id.relative_layout_main);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        window_height = displayMetrics.heightPixels;

        dragView = findViewById(R.id.dragView);
        dragView.setOnTouchListener(this);
    }

    //create the listener for the users data
    private void CreateUserListener()
    {
        Log.d(TAG,"CreateUserListener: UserListener initiated");
        usersRef = database.getReference("Rooms/"+roomName+"/users");
        userListener = usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG,"CreateUserListener: updating The Players Data from database");
                for (DataSnapshot data : snapshot.getChildren()) {
                    PlayerData player = data.getValue(PlayerData.class);
                    UpdataPlayersDataFromDb(player);
                }
                MoveMapMarkers();
                //Show UserData in view
            }

            private void MoveMapMarkers() {
                Log.d(TAG,"MoveMapMarkers: moving Markers");
                clusterManager.clearItems();
                mGoogleMap.clear();
                for (Map.Entry<String, ClusterMarker> marker : clusterMarkers.entrySet()) {
                    if(players.containsKey(marker.getKey()))
                        {
                        Log.d(TAG, "MoveMapMarkers: marker: "+ marker.getValue().toString());
                        PlayerData player = players.get(marker.getKey());
                        marker.getValue().setPosition(new LatLng(player.getGeoSpot().getLatitude(), player.getGeoSpot().getLongitude()));
                        marker.getValue().setSnippet("Score: " + marker.getValue());
                        Log.d(TAG, "MoveMapMarkers: after: "+ marker.getValue().toString());
                        ClusterMarker newMarker = new ClusterMarker(marker.getValue());

                        clusterManager.addItem(newMarker);//
                    }
                }
                clusterManager.cluster();
            }

            private void UpdataPlayersDataFromDb(PlayerData player) {
                //update players info
                players.put(player.getUserId(),player);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void SendMessage(Message message) {
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();

        //set gameInfo
        roomRef.child(roomName).child("Messages").child(userData.getId() + "-" + ts).setValue(message);
    }
    //gets the teams and user map from fireStore server
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
                if(task.isSuccessful() && task.getResult() != null)
                {
                    Log.d(TAG,"copying data from fireStore");
                    List<PlayerViewer> downloadInfoList = task.getResult().toObjects(PlayerViewer.class);
                    for (PlayerViewer playerInfo : downloadInfoList) {
                        if(playerInfo.getId().equals( userData.getId()))//set my team
                        {
                            userData.setTeam(playerInfo.getTeam());
                        }
                        teamsMap.put(playerInfo.getId(),playerInfo.getTeam());
                        usersNameMap.put(playerInfo.getId(),playerInfo.getName());
                        LoadImage(playerInfo.getId());
                    }
                    //create markers for players
                    addMapMarkers();

                    SetScoreView();

                    if(isAdmin)
                    {
                        SendMessage(new Message("all", "Start Game!!!"));
                    }
                }
                else
                {
                    Log.d(TAG,"get failed with", task.getException());
                    //retry?
                }
            }
        });
    }

    private void LoadImage(String id){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File myFile = new File(directory, id + ".jpg");
        if (myFile.exists()) {
            Log.d(TAG,"LoadImage: getting icon as int");
            imageMap.put(id,myFile.getAbsolutePath());
        }
        else {
            imageMap.put(id,"");
            Log.d(TAG,"LoadImage: no image for " + id);
        }
    }

    //gets and sets UserDetails - need to call only once
    private void UserDetails() {
        user = UserClient.getInstance().getUser();
        UserClient.getInstance().setCurrentRoom(roomName);
        UserClient.getInstance().setCurrentScore(score);
        userData = new PlayerViewer(user.getUserName(),user.getUserId());
    }
    //gets data sent by last activity
    private void GetExtras() {
        Bundle extras = getIntent().getExtras();
        roomName=extras.getString("name");
        gameType=extras.getString("game");
        isAdmin=extras.getBoolean("admin");
    }
    //starts the map view and prossess
    private void initGoogleMap(Bundle savedInstanceState)
    {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        Bundle mapViewBundle = null;
        if(savedInstanceState != null)
        {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        // Gets the MapView from the XML layout and creates it
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(GameActivity.this);
    }

    //updates in app and calls SaveUserDataInDb to save in db
    //to update Score just change score and call updateUserData
    private void UpdateUserData()
    {
        Log.d(TAG,"UpdateUserData Called");
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if(task.isSuccessful())
                {
                    Location location = task.getResult();
                    GeoSpot geoPoint = new GeoSpot(location.getLatitude(),location.getLongitude());

                    myData.setGeoSpot(geoPoint);
                    myData.setTimestamp(null);
                    myData.setScore(score);

                    UserClient.getInstance().setCurrentScore(score);
                    SetCameraView();

                    SaveUserDataInDb();
                }
            }
        });
    }

    //saves manually (only once) first data of the current user
    //and calls the CreateUserListener func
    //will start the DataService (calls startLocationService func)
    boolean firstInit = true;
    private void SaveUserDataInDb()
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
    //creates the map bounds
    private void SetCameraView()
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

    //set players makers
    //Debug - set Image and team Color
    private void addMapMarkers()
    {
        Log.d(TAG, "addMapMarkers: Creating Markers size: " + usersNameMap.size());
        if(mGoogleMap != null) {
            if (clusterManager == null) {
                clusterManager = new ClusterManager<ClusterMarker>(this.getApplicationContext(), mGoogleMap);
            }
            if(clusterManagerRenderer == null)
            {
                clusterManagerRenderer = new MyClusterManagerRenderer(
                        this,
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


    private void startDataService(){
        if(!isDataServiceRunning()){
            dataServiceIntent = new Intent(this, DataService.class);
//        this.startService(serviceIntent);
            Log.d(TAG,"startLocationService: StartLocationService");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){

                GameActivity.this.startForegroundService(dataServiceIntent);
            }else{
                startService(dataServiceIntent);
            }
        }
    }

    private boolean isDataServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("com.codingwithmitch.googledirectionstest.services.LocationService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.");
        return false;
    }
    //Debug check why not working
    private void RemoveListenersAndServices()
    {
        if(usersRef != null && userListener != null) {//Debug - needed?
            Log.d(TAG,"RemoveListenersAndServices: removing listeners");
            usersRef.removeEventListener(userListener);
        }
        if(isDataServiceRunning())
        {
            Log.d(TAG,"RemoveListenersAndServices: Stop services");
            stopService(dataServiceIntent);
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
        RemoveListenersAndServices();
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
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
}
