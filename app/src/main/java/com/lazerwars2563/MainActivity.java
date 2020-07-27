package com.lazerwars2563;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1997;
    private List<AuthUI.IdpConfig> providers;

    private FirebaseFirestore firebaseFirestore;

    private RecyclerView mFirestoreList;
    private FirestoreRecyclerAdapter adapter;

    //start menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mFirestoreList = findViewById(R.id.fireStoreList);

        //init providers
        providers  = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        themeAndLogo();

        //button:
        findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //change activity
                launchNewRoomActivity();
            }
        });

        //get Data from firebase
        ArrayList<Room> rooms = new ArrayList<>();
        rooms.add(new Room("uri","capture the flag"));//just for check
        rooms.add(new Room("razi","onePlayer"));//just for check
        rooms.add(new Room("uri","free for all"));//just for check

        RoomsRecViewAdapter adapter = new RoomsRecViewAdapter(this);
        adapter.setRooms(rooms);

        mFirestoreList.setAdapter(adapter);
        mFirestoreList.setLayoutManager(new GridLayoutManager(this,2));



/*

        //Query
        Query query = firebaseFirestore.collection("Rooms");

        //RecyclerOptions
        FirestoreRecyclerOptions<Room> options = new FirestoreRecyclerOptions.Builder<Room>()
                .setQuery(query, Room.class)
                .build();
        adapter = new FirestoreRecyclerAdapter<Room, RoomViewHolder>(options) {
            @NonNull
            @Override
            public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.roomcard,parent,false);
                return  new RoomViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull RoomViewHolder holder, int position, @NonNull Room model) {
                holder.room_name.setText(model.getName());
                holder.room_type.setText(model.getType());

            }
        };

        mFirestoreList.setHasFixedSize(false);
        mFirestoreList.setLayoutManager(new LinearLayoutManager(this));
        mFirestoreList.setAdapter(adapter);*/

    }

    private void launchNewRoomActivity() {

        Intent intent = new Intent(this, CreateNewRoomActivity.class);
        startActivity(intent);
    }

    //Menu option
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.LogOut:
                //LogOut
                AuthUI.getInstance()
                        .signOut(MainActivity.this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(MainActivity.this, "Logging out", Toast.LENGTH_SHORT).show();
                                themeAndLogo();
                            }
                        }).addOnFailureListener(new OnFailureListener() {//if failed to log out
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //set theme and logo for first page
    public void themeAndLogo() {
        // [START auth_fui_theme_logo]
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setLogo(R.drawable.logolazer)      // Set logo drawable
                        .setTheme(R.style.AppTheme)      // Set theme
                        .build(),
                RC_SIGN_IN);
        // [END auth_fui_theme_logo]
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                //show email in toast
                Toast.makeText(this, ""+user.getEmail(), Toast.LENGTH_SHORT).show();
            }
            else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                Toast.makeText(this, ""+response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

    }
/*
    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }
 */
}
