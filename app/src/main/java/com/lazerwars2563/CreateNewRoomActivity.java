package com.lazerwars2563;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class CreateNewRoomActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private String game_type;
    private String name;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_room);

        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();

        //set Spinner
        Spinner spinner = findViewById(R.id.game_types);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,R.array.gameTypes,android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(this);


        //button create click:
        findViewById(R.id.button_create).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //get String from txtinput
                EditText text = (EditText)findViewById(R.id.textInput);
                name = text.getText().toString();

                if(!checkIfNameLegal(name))
                {
                    return;
                }

                // Create a reference to the cities collection
                CollectionReference roomsRef = db.collection("Rooms");
                // Create a query against the collection.
                Query query = roomsRef.whereEqualTo("name", name);
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(!task.getResult().isEmpty())
                        {
                            Toast.makeText(CreateNewRoomActivity.this, "Name already exists",Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            //create data
                            Map<String, Object> room = new HashMap<>();
                            room.put("name",name);
                            room.put("game",game_type);
                            //insert to collection
                            db.collection("Rooms").document(name).set(room);
                        }
                    }
                });
            }
        });
    }

    private boolean checkIfNameLegal(String name) {
        if(name.length() == 0) {
            Toast.makeText(CreateNewRoomActivity.this, "Please fill the name text field",Toast.LENGTH_SHORT).show();
            return  false;
        }
        return  true;
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
         game_type = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
