package com.lazerwars2563.Activitys;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.data.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lazerwars2563.Handler.GamePlayHandler;
import com.lazerwars2563.R;
import com.lazerwars2563.util.UserClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ScoreBoardActivity extends AppCompatActivity {


    private GamePlayHandler gamePlayHandler;
    private String roomName;
    private DatabaseReference roomRef;
    private Map<String, Integer> teamsMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_board);

        Bundle extras = getIntent().getExtras();
        boolean withTeams = extras.getBoolean("withTeams");

        HashMap<Integer, String> scores = UserClient.getInstance().getScores();
        HashMap<Integer, Integer> teamsScores = UserClient.getInstance().getTeamsScores();

        ArrayList<String> players = new ArrayList<>();

        int i = 1;
        for (Map.Entry<Integer, String> pair : scores.entrySet()) {
            players.add(i + ".   Score: " + pair.getKey() + "   name: " + pair.getValue());
            i++;
        }
        ArrayAdapter adapter1 = new ArrayAdapter(this, android.R.layout.simple_list_item_1, players);
        ListView listView_players = this.findViewById(R.id.players_list1);
        listView_players.setAdapter(adapter1);

        if (withTeams) {
            this.findViewById(R.id.teams_scores_board_title).setVisibility(View.VISIBLE);
            ArrayList<String> teams = new ArrayList<>();

            i = 1;
            String[] teamsNameArray = this.getResources().getStringArray(R.array.TeamsName);
            for (Map.Entry<Integer, Integer> pair : teamsScores.entrySet()) {
                teams.add(i + ".   Score: " + pair.getValue() + "   " + teamsNameArray[pair.getKey()]);
                i++;
            }
            ArrayAdapter adapter2 = new ArrayAdapter(this,android.R.layout.simple_list_item_1,teams);
            ListView listView_teams = (ListView)findViewById(R.id.teams_list1);
            listView_teams.setAdapter(adapter2);
        }
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
                        Intent intent = new Intent(ScoreBoardActivity.this, ChooseRoomActivity.class);
                        startActivity(intent);
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
}