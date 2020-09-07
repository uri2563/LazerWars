package com.lazerwars2563.Activitys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.lazerwars2563.BuildConfig;
import com.lazerwars2563.Class.Room;
import com.lazerwars2563.Handler.SerialServiceHandler;
import com.lazerwars2563.R;
import com.lazerwars2563.adapters.RoomsAdapter;
import com.lazerwars2563.util.Constants;
import com.lazerwars2563.util.UserClient;


import java.io.File;

public class ChooseRoomActivity extends AppCompatActivity {
    private static String TAG = "ChooseRoomActivity";

    private FirebaseFirestore db;

    private RecyclerView recyclerView;

    private RoomsAdapter mAdapter;
    private SearchView mSearchView;

    private SerialServiceHandler serialServiceHandler;

    //start menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

                MenuItem item = menu.findItem(R.id.action_search);
                mSearchView = (SearchView) item.getActionView();
        setUpRecyclerView();
        return true;
    }

    //Menu option
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.LogOut:
                //LogOut
                AuthUI.getInstance()
                        .signOut(ChooseRoomActivity.this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                LogOut();
                            }
                        }).addOnFailureListener(new OnFailureListener()
                {//if failed to log out
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChooseRoomActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                return true;

            case R.id.profile:
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_room);
        //delete old files
        deletePlayerDirectory();

        serialServiceHandler = new SerialServiceHandler(ChooseRoomActivity.this,ChooseRoomActivity.this,true);

        //button:
        findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //change activity
                if(UserClient.getInstance().getGameId().equals("None"))
                {
                    Toast.makeText(ChooseRoomActivity.this,"Poor Usb connation, please wait and try agine if it isnt working please replug cable",Toast.LENGTH_SHORT).show();
                }
                else
                {
                    launchNewRoomActivity();
                }
            }
        });

}
    private void setUpRecyclerView() {
        db = FirebaseFirestore.getInstance();
        Log.d(TAG,"setUpRecyclerView ");
        recyclerView = findViewById(R.id.roomsList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(ChooseRoomActivity.this,2));

        Query query = db
                .collection("Rooms")
                .orderBy("name");
        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
                                      @Override
                                      public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                          if (e != null) {
                                              Log.w(TAG, "Listen failed.", e);
                                              return;
                                          }
                                          mAdapter = new RoomsAdapter(queryDocumentSnapshots.toObjects(Room.class), ChooseRoomActivity.this);
                                          recyclerView.setAdapter(mAdapter);
                                      }
                                  });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        serialServiceHandler.OnPauseSerial();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        serialServiceHandler.OnResumeSerial();
    }

    @Override
    public void onBackPressed() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Are you sure you want to LogOut?")
                .setCancelable(false)
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogOut();
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

    private void LogOut()
    {
        AuthUI.getInstance()
                .signOut(ChooseRoomActivity.this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        Intent intent = new Intent(ChooseRoomActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                });
    }

    private void launchNewRoomActivity() {

        Intent intent = new Intent(this, CreateNewRoomActivity.class);
        startActivity(intent);
    }
    //delete game files
    void deletePlayerDirectory() {
        //delete img
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        if (directory.isDirectory()) {
            for (File child : directory.listFiles()) {
                Log.d(TAG, "deletePlayerDirectory: delete image: " + child.getName());
                child.delete();
            }
        }
        //delete audio
        cw = new ContextWrapper(getApplicationContext());
        directory = cw.getDir("audioDir", Context.MODE_PRIVATE);
        if (directory.isDirectory()) {
            for (File child : directory.listFiles()) {
                Log.d(TAG, "deletePlayerDirectory: delete audio: " + child.getName());
                child.delete();
            }
        }

    }
}
