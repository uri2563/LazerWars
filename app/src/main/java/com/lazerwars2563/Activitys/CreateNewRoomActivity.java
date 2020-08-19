package com.lazerwars2563.Activitys;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.lazerwars2563.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateNewRoomActivity extends AppCompatActivity {
    private String game_type;
    private boolean showAll;
    private String name;

    private long ready_time;
    private long round_time;

    private FirebaseFirestore db;


    private EditText readyTimeInput;
    private EditText roundTimeInput;
    private EditText text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_room);

        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();

        //set game type Spinner
        Spinner spinnerType = findViewById(R.id.game_types);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,R.array.gameTypes,android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(spinnerAdapter);
        spinnerType.setOnItemSelectedListener(new TypeSpinnerClass());

        //set game type Spinner
        Spinner spinnerShowAll = findViewById(R.id.showAll_options);
        ArrayAdapter<CharSequence> spinnerShowAdapter = ArrayAdapter.createFromResource(this,R.array.booleanOptions,android.R.layout.simple_spinner_item);
        spinnerShowAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerShowAll.setAdapter(spinnerShowAdapter);
        spinnerShowAll.setOnItemSelectedListener(new ShowSpinnerClass());

        readyTimeInput = findViewById(R.id.ready_time_input);
        roundTimeInput = findViewById(R.id.round_time_input);
        text = findViewById(R.id.name_input);

        //button create click:
        findViewById(R.id.button_create).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //get String from txtinput
                name = text.getText().toString();
                String sReady_time = readyTimeInput.getText().toString();
                String sRound_time = roundTimeInput.getText().toString();

                if(sReady_time.length() == 0 || sRound_time.length() == 0) {
                    Toast.makeText(CreateNewRoomActivity.this,"Please fill the time text field",Toast.LENGTH_SHORT).show();
                }
                else
                {
                    round_time = Long.parseLong(sRound_time);
                    ready_time = Long.parseLong(sReady_time);
                }

                if(IsInputValid())
                {
                    InitDataBase();
                }
            }
        });

    }

    private boolean IsInputValid() {
        String[] notAllowed = {".", "#", "$","[","]"};
        //name
        if (name.length() == 0)
        {
            Toast.makeText(CreateNewRoomActivity.this,"Please fill the name text field",Toast.LENGTH_SHORT).show();
            return false;
        }
        for(String c : notAllowed)
        {
            if(name.contains(c))
            {
                Toast.makeText(CreateNewRoomActivity.this,"name can not contain: . # $ [ ]",Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        //ready time
        if (ready_time > 1000)
        {
            Toast.makeText(CreateNewRoomActivity.this,"Getting ready time cant be longer then 1000 seconds",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(ready_time < 3)
        {
            Toast.makeText(CreateNewRoomActivity.this,"Getting ready time cant be shorter then 3 seconds",Toast.LENGTH_SHORT).show();
            return false;
        }
        //round time
        if(round_time > 120)
        {
            Toast.makeText(CreateNewRoomActivity.this,"round time cant be longer then 120 minutes",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(round_time < 1)
        {
            Toast.makeText(CreateNewRoomActivity.this,"Round time cant be shorter then 1 minute",Toast.LENGTH_SHORT).show();
            //return false; Debug
        }
        return true;
    }

    //will store the data and leave to the new room on succses
    private void InitDataBase() {
        //create data
        Map<String, Object> room = new HashMap<>();

        //put in room
        room.put("name",name);
        room.put("game",game_type);
        room.put("recruiting", true);
        room.put("startGame", false);
        room.put("roomExists", true);
        room.put("showAll", showAll);
        room.put("readyTime",ready_time);
        room.put("roundTime",round_time);

        //insert to collection
        db.collection("Rooms").document(name).set(room)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Intent intent = new Intent(CreateNewRoomActivity.this, WaitingRoomActivity.class);
                        intent.putExtra("name",name);
                        intent.putExtra("game",game_type);
                        intent.putExtra("admin", true);

                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CreateNewRoomActivity.this, "Faild to save data",Toast.LENGTH_SHORT).show();
                    }
                });
        //insert to collection

        db.collection("Rooms").document(name).collection("players");
    }

    class TypeSpinnerClass implements AdapterView.OnItemSelectedListener
    {
        public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
        {
            game_type = parent.getItemAtPosition(position).toString();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    class ShowSpinnerClass implements AdapterView.OnItemSelectedListener
    {
        public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
        {
            if(parent.getItemAtPosition(position).toString().equals("show all players"))
            {
                showAll = true;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

}
