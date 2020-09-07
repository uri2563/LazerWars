package com.lazerwars2563.Activitys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.lazerwars2563.Class.PlayerViewer;
import com.lazerwars2563.Class.TeamItem;
import com.lazerwars2563.Handler.CustomTimer;
import com.lazerwars2563.util.UserClient;
import com.lazerwars2563.Class.UserDetails;
import com.lazerwars2563.R;
import com.lazerwars2563.adapters.TeamAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.lazerwars2563.util.Constants.ERROR_DIALOG_REQUEST;
import static com.lazerwars2563.util.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

public class WaitingRoomActivity extends AppCompatActivity {
    private static final String TAG = "WaitingRoomActivity";
    private boolean permissionGranted = false;

    enum STATES
    {RECRUITING, CHOOSING, SHOW_TEAMS}

    static  int numberOfTeams = 2;//can be changed later - remember to update colors and strings
    //state of the page
    private STATES state;

    private Map<String, Integer> teamsList;
    private String[] teamsNameArray;
    private int[] teamsColorArray;

    //firestore
    private FirebaseFirestore db;
    private DocumentReference roomRef;
    //forestorage
    private StorageReference mStorageRef;

    //private PlayersCardRecViewAdapter adapter;
    private FirestoreRecyclerAdapter adapter;

    private UserDetails user;

    CustomTimer timer;
    private  long timeLeftMiliSeconds;

    private String room;
    private String roomType;

    private boolean admin;

    private Button startButton;
    private Button randomTeamButton;
    private TextView nameText;
    private TextView typeText;
    private TextView timerText;

    private ArrayList<TeamItem> teamList;
    private TeamAdapter teamAdapter;

    private Map<String, Boolean> playersImage;

    private ListenerRegistration waitingHandler;
    private ListenerRegistration playerListener;


    //leaving waiting Room
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Are you sure you want to leave this room?")
                .setCancelable(false)
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DeleteData();
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

    private void DeleteData() {
        if(admin)
        {
            //tell every player that the room is closed
            roomRef.update("roomExists", false).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    //delete room from db
                    roomRef.delete();
                }
            });
        }
        else {
            //delete player
        roomRef.collection("Players").document(user.getUserId()).delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            LeavePage();
                        }});
        }
    }

    private void LeavePage() {
        //if timer is running and you leave stop it
        if (state.equals(STATES.SHOW_TEAMS))
        {
            timer.DestroyTimer();
        }
        StopListeners();
        Intent intent = new Intent(WaitingRoomActivity.this, ChooseRoomActivity.class);
        startActivity(intent);
    }

    //kick non admin player
    private void

    kickPlayerFromDb(String userId)
    {
        //delete player
        roomRef.collection("Players").document(userId).delete()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(WaitingRoomActivity.this,"Error:" +e.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        state = STATES.RECRUITING;

        teamsList = new HashMap<>();

        //init firestore
        mStorageRef = FirebaseStorage.getInstance().getReference("Images");
        playersImage = new HashMap<>();//check that picture has been updated

        //get the details from intent
        GetExtras();

        //set text in text views
        InitViews();

        //init teams spinner (not initializing view yet)
        initTeamSpinnerList(numberOfTeams);

        //set dataBase data - user details and ref
        if(InitDataBase()) {//allow only if succses

            setUpRecyclerView();

            //only admin will be able to change game state
            if (!admin) {
                startButton.setVisibility(View.INVISIBLE);
            }

            //startButton click listener
            {
                startButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (state == STATES.RECRUITING) {
                            //change recruiting to false
                            roomRef.update("recruiting", false).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(WaitingRoomActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else if (state == STATES.CHOOSING) {
                            Toast.makeText(WaitingRoomActivity.this, "Starting the game!", Toast.LENGTH_SHORT).show();
                            InsertTeamsToDataBase(teamsList);

                            roomRef.update("startGame", true).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(WaitingRoomActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    }
                });
            }

            //randomTeamButton click listener
            {
                randomTeamButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RandomTeams();
                    }
                });
            }

            //listen to game status change
            {
                waitingHandler = roomRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(WaitingRoomActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            //if the room is closed
                            if (!(boolean) documentSnapshot.getData().get("roomExists")) {
                                Toast.makeText(WaitingRoomActivity.this, "The admin has closed the room", Toast.LENGTH_SHORT).show();
                                LeavePage();
                            }
                            //if the game stopped recruiting
                            if (!(boolean) documentSnapshot.getData().get("recruiting") && state.equals(STATES.RECRUITING)) {
                                ChangeToChooseState();
                            }
                            //if game started
                            else if ((boolean) documentSnapshot.getData().get("startGame") && state.equals(STATES.CHOOSING)) {
                                ChangeToShowState(documentSnapshot);
                            }
                        }
                    }

                    private void ChangeToChooseState() {
                        state = STATES.CHOOSING;
                        if (admin) {
                            randomTeamButton.setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    private void ChangeToShowState(DocumentSnapshot documentSnapshot) {
                        randomTeamButton.setVisibility(View.INVISIBLE);
                        startButton.setVisibility(View.INVISIBLE);
                        state = STATES.SHOW_TEAMS;
                        adapter.notifyDataSetChanged();
                        timeLeftMiliSeconds = (long) documentSnapshot.getData().get("readyTime") * 1000;
                        //StartTimer();
                        timer = new CustomTimer(timeLeftMiliSeconds, timerText);
                        timer.setListener(new CustomTimer.ChangeListener() {
                            @Override
                            public void onChange() {
                                startGame();
                            }
                        });
                    }
                });
            }

            //listen to player change
            {
                playerListener = roomRef.collection("Players").document(user.getUserId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(WaitingRoomActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!documentSnapshot.exists()) {
                            Toast.makeText(WaitingRoomActivity.this, "You where kicked from server", Toast.LENGTH_SHORT).show();
                            DeleteData();
                        }
                    }
                });
            }
        }
    }

    private boolean InitDataBase() {
        //get user details
        user = UserClient.getInstance().getUser();
        if(user == null)
        {
            Log.e(TAG,"null user!");
            return false;
        }

        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();
        roomRef = db.collection("Rooms").document(room);

        //set playersData in db
        Map<String, Object> players = new HashMap<>();
        players.put("name",user.getUserName());
        players.put("id",user.getUserId());
        players.put("team", 0);
        players.put("arduinoId", UserClient.getInstance().getGameId());

        roomRef.collection("Players").document(user.getUserId()).set(players);
        return true;
    }

    private ArrayList list;
    private int counter;
    private boolean random = false;
    private void RandomTeams() {
        //set list with all the wanted numbers
        int number_of_players = adapter.getItemCount();
        list = new ArrayList();
        counter = 0;
        int index = 0;
        while (counter < number_of_players)
        {
            index = index % numberOfTeams;
            list.add(index);
            index++;
            counter++;
        }
        Collections.shuffle(list);
        //init randomization
        counter = 0;
        random = true;
        adapter.notifyDataSetChanged();
    }

    private void InsertTeamsToDataBase(Map<String, Integer> teams) {
        for (Map.Entry<String,Integer> entry: teams.entrySet()) {
            roomRef.collection("Players").document(entry.getKey()).update("team",entry.getValue());
        }
    }

    //get extras from intent
    private void GetExtras() {
        Bundle extras = getIntent().getExtras();
        room = extras.getString("name");
        admin = extras.getBoolean("admin");
        roomType = extras.getString("game");
    }

    private void InitViews() {
        //titles
        nameText = (TextView) findViewById(R.id.nameText);
        nameText.setText(room);
        typeText = (TextView) findViewById(R.id.type_text);
        typeText.setText(roomType);

        //set random button
        startButton = findViewById(R.id.startButton);
        randomTeamButton = findViewById(R.id.randomButton);

        //set timer
        timerText = findViewById(R.id.timer_view);
    }

    private void initTeamSpinnerList(int teamsNumber) {
        teamList = new ArrayList<>();
        teamsNameArray = WaitingRoomActivity.this.getResources().getStringArray(R.array.TeamsName);
        teamsColorArray = WaitingRoomActivity.this.getResources().getIntArray(R.array.TeamsColor);

        for (int i = 0; i< teamsNumber; i++) {
            teamList.add(new TeamItem(teamsNameArray[i], teamsColorArray[i]));
        }
    }


    private void setTeamSpinner(final Spinner spinner, final PlayerViewer model)
    {
        teamAdapter = new TeamAdapter(this, teamList);
        spinner.setAdapter(teamAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                teamsList.put(model.getId(),position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void startGame()
    {
        Intent intent = new Intent(WaitingRoomActivity.this, GameActivity.class);
        intent.putExtra("name",room);
        intent.putExtra("game",roomType);
        intent.putExtra("admin",admin);
        //check how to pass map data!!!!
        StopListeners();
        startActivity(intent);
    }

    private void setUpRecyclerView() {
        // Create a query against the collection.
        Query query = roomRef.collection("Players");

        FirestoreRecyclerOptions<PlayerViewer> options = new FirestoreRecyclerOptions.Builder<PlayerViewer>()
                .setQuery(query,PlayerViewer.class)
                .build();

         adapter = new FirestoreRecyclerAdapter<PlayerViewer, PlayersHolder>(options) {
            @NonNull
            @Override
            public PlayersHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.player_view,parent,false);
                return new PlayersHolder(v);
            }

            @Override
            protected void onBindViewHolder(@NonNull final PlayersHolder holder, int position, @NonNull final PlayerViewer model) {
                try
                {
                    if(!playersImage.containsKey(model.getId()))//set image for first time
                    {
                        ImageUpdate(holder,model);
                    }
                    else if(playersImage.get(model.getId()))//has image
                    {
                        ImageFromPath(model.getId(), holder);
                    }
                    else//has no image
                        {
                            holder.playerImage.setImageResource(R.drawable.ic_warning_black_24dp);
                        }
                }
                catch (IOException e){
                    Log.d(TAG,"onBindViewHolder: couldn't load old image");
                }

                holder.textViewName.setText(model.getName());

                if(admin && !model.getId().equals(user.getUserId()))
                { holder.kickButton.setVisibility(View.VISIBLE); }

                //set choosing state
                if(state.equals(STATES.CHOOSING) && admin) {
                    holder.kickButton.setVisibility(View.INVISIBLE);
                    holder.spinnerTeams.setVisibility(View.VISIBLE);

                    //random button pressed
                    if (random)
                    {
                        final int num = (int) list.get(counter);
                        holder.spinnerTeams.post(new Runnable() {
                            @Override
                            public void run() {
                              holder.spinnerTeams.setSelection(num);
                            }
                        });
                        counter++;
                    }
                    if(counter >= adapter.getItemCount())
                    {
                        random = false;
                    }

                    setTeamSpinner(holder.spinnerTeams, model);
                }
                //show teams
                else if(state.equals(STATES.SHOW_TEAMS))
                {
                    holder.kickButton.setVisibility(View.INVISIBLE);
                    holder.spinnerTeams.setVisibility(View.INVISIBLE);
                    //changes card color by team!
                    holder.playerCardView.setCardBackgroundColor(teamsColorArray[model.getTeam()]);

                }

                holder.kickButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        kickPlayerFromDb(model.getId());
                    }
                });
            }

             private void ImageFromPath(String id, @NonNull final PlayersHolder holder) {
                 ContextWrapper cw = new ContextWrapper(getApplicationContext());
                 File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                 File myFile = new File(directory, id + ".jpg");
                 Bitmap myBitmap = BitmapFactory.decodeFile(myFile.getAbsolutePath());
                 holder.playerImage.setImageBitmap(myBitmap);
             }

             private void ImageUpdate(@NonNull final PlayersHolder holder, @NonNull final PlayerViewer model) throws IOException {
                StorageReference imgRef = mStorageRef.child(model.getId() + ".jpg");

                final long ONE_MEGABYTE = 1024 * 1024;
                imgRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes , 0, bytes.length);
                        holder.playerImage.setImageBitmap(bitmap);
                        SaveImage(bitmap, model);
                        playersImage.put(model.getId(), true);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        holder.playerImage.setImageResource(R.drawable.ic_warning_black_24dp);
                        playersImage.put(model.getId(), false);
                    }
                });
             }


             private void SaveImage(Bitmap bitmap,  @NonNull final PlayerViewer model) {
                 ContextWrapper cw = new ContextWrapper(getApplicationContext());
                 File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                 File file = new File(directory, model.getId() + ".jpg");
                     FileOutputStream fos = null;
                     try {
                         fos = new FileOutputStream(file);
                         bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                         fos.flush();
                         fos.close();
                         Log.d(TAG,"SaveImage: saved image at path: " +  file.toString());
                     } catch (java.io.IOException e) {
                         e.printStackTrace();
                     }
             }
         };

        RecyclerView recyclerView = findViewById(R.id.waitingList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }


    private class PlayersHolder extends RecyclerView.ViewHolder{
        TextView textViewName;
        Button kickButton;
        ImageView playerImage;
        Spinner spinnerTeams;
        CardView playerCardView;
        public PlayersHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textPlayerName);
            kickButton = itemView.findViewById(R.id.buttonKick);
            spinnerTeams = itemView.findViewById(R.id.spinner);
            playerImage = itemView.findViewById(R.id.playerImage);
            playerCardView = itemView.findViewById(R.id.player_card);
        }
    }

    ///get permissions:
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        getPermission();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if (!permissionGranted) {
                    getPermission();
                }
            }
        }
    }

    private static final int REQUEST_CODE = 331;
    private void getPermission() {
        Log.d(TAG,"requesting premissions");
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
        };
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[3]) == PackageManager.PERMISSION_GRANTED
                &&ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[2]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"given all premission");
            permissionGranted = true;
        }
        else {
            ActivityCompat.requestPermissions(WaitingRoomActivity.this, permissions,REQUEST_CODE);
        }
    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(WaitingRoomActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(WaitingRoomActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void buildAlertMessageNoGps() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final android.app.AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private boolean checkMapServices(){
        if(isServicesOK()){
            if(isMapsEnabled()){
                return true;
            }
        }
        return false;
    }

    private void StopListeners()
    {
        Log.d(TAG,"StopListeners: stop listening");
        waitingHandler.remove();
        playerListener.remove();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(adapter != null)
        {
            adapter.startListening();
        }
        Log.d(TAG,"onStart");
        if(checkMapServices() && !permissionGranted){
            getPermission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StopListeners();
        Log.d(TAG,"onDestroy");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(adapter != null)
        {
            adapter.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(adapter != null)
        {
            adapter.startListening();
        }
    }
}
