package com.lazerwars2563.Handler;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.lazerwars2563.Activitys.ChooseRoomActivity;
import com.lazerwars2563.Activitys.GameActivity;
import com.lazerwars2563.Activitys.ScoreBoardActivity;
import com.lazerwars2563.Class.Message;
import com.lazerwars2563.Class.PlayerViewer;
import com.lazerwars2563.R;
import com.lazerwars2563.util.UserClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GamePlayHandler
{
    private static String TAG = "GamePlayHandler";

    public enum States{SETUP,START, STOP, GAME, ADMIN_CHANGE, END};
    private States currentState = States.SETUP;
    private CustomTimer timer;

    private DatabaseReference roomRef;
    private DocumentReference roomStoreRef;

    private PlayerViewer userData;
    private String roomName;
    private boolean isAdmin;
    private Context context;
    private Activity activity;
    private Map<String, String> usersNameMap;

    private long timeLeftMiliSeconds;

    private TextView timerText;
    private MessagesHandler messagesHandler;
    private SerialServiceHandler serialServiceHandler;

    private int playersNum;//current Logged In players!

    private Map<String, Integer> teamsMap;
    private Map<String, String> idsMap;
    private Map<String,Boolean> onlineMap;

    private boolean withTeams;

    public GamePlayHandler(DocumentReference roomStoreRef, TextView timerText, DatabaseReference roomRef, PlayerViewer userData, String roomName,
                           boolean isAdmin, MessagesHandler messagesHandler, Context context, Map<String, String> usersNameMap,GameDatabaseHandler gameDatabaseHandler, SerialServiceHandler serialServiceHandler,
                           Map<String, Integer> teamsMap,  Map<String, String> idsMap, Activity activity) {
        Log.d(TAG, "Game time in milisec is: " + timeLeftMiliSeconds);
        this.roomRef = roomRef;
        this.userData = userData;
        this.roomName = roomName;
        this.isAdmin = isAdmin;
        this.timerText = timerText;
        this.messagesHandler = messagesHandler;
        this.context = context;
        this.usersNameMap = usersNameMap;
        this.roomStoreRef = roomStoreRef;
        this.serialServiceHandler = serialServiceHandler;
        this.teamsMap = teamsMap;
        this.idsMap = idsMap;
        this.activity = activity;

        withTeams = true;//Debug - change

        roomRef.child(roomName).child("RoomState").setValue(States.SETUP);
        AddToPlayerScore(userData.getId(),0);//score 0
        gameDatabaseHandler.StartScoreListener();

        //get time
        roomStoreRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                timeLeftMiliSeconds = (long)task.getResult().get("roundTime") * 60000;
                Log.d(TAG, "Game time in milisec is: " + timeLeftMiliSeconds);
                ListenToRoomState();
                SetReady();
            }
        });
    }

    public String getRoomName()
    {
        return roomName;
    }

    public Map<String,Boolean> getOnlineMap()
    {return onlineMap;}

    private void SetReady()
    {
        roomRef.child(roomName).child("PlayersState").child(userData.getId()).setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                StartConnectionSerialListener();
            }
        });
    }

    private ValueEventListener roomStateListener;
    private void ListenToRoomState() {
        roomStateListener = roomRef.child(roomName).child("RoomState").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentState = States.valueOf(snapshot.getValue().toString());
                Log.d(TAG, "ListenToRoomState: Game State Changed to: " + currentState.toString());
                if (currentState == States.START) {
                    StartGame();
                } else if (currentState == States.ADMIN_CHANGE) {
                    CreateAdmin();
                } else if (currentState == States.END) {
                    ((GameActivity)activity).DestroyHandlers(true);
                }
            }

            private void CreateAdmin() {
                Log.d(TAG, "CreateAdmin");
                roomRef.child(roomName).child("UsersLocation").orderByKey().limitToFirst(1)
                        .addListenerForSingleValueEvent(new ValueEventListener () {

                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){
                                    for (DataSnapshot user: dataSnapshot.getChildren()) {
                                        String id = user.getKey();
                                        Log.d(TAG,"CreateAdmin: user: " + user.getKey());
                                        if(id.equals(userData.getId()))
                                        {
                                            Log.d(TAG,"CreateAdmin: user: "+ id + " is admin now");
                                            isAdmin = true;
                                            ChangeGameState(States.GAME);
                                        }
                                    }
                                }

                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }

                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void AddToPlayerScore(final String id, final int adds)
    {
        roomRef.child(roomName).child("Scores").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    int scoreValue = snapshot.getValue(Integer.class);
                    roomRef.child(roomName).child("Scores").child(id).setValue(scoreValue + adds);
                }
                else {
                    roomRef.child(roomName).child("Scores").child(id).setValue(adds);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //admin only
    public void ChangeGameState(final States newtState) {
        if (!isAdmin) { return;}
        Log.d(TAG,"ChangeGameState: " + newtState.toString());
        roomRef.child(roomName).child("RoomState").setValue(newtState);
    }

    private ValueEventListener startListener;
    public void SetStartListener(GameMakerHandler gameMakerHandler)
    {
        StartPlayersNumListener(gameMakerHandler);
        onlineMap = new HashMap<>();

        startListener = roomRef.child(roomName).child("PlayersState").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(isAdmin && currentState ==  States.SETUP && snapshot.getChildrenCount() == usersNameMap.size())
                {
                    Log.d(TAG,"SetStartListener: start game");
                    messagesHandler.SendMessage(new Message("all", "Start Game!!!"));
                    ChangeGameState(States.START);
                }
                for (DataSnapshot userSnapshot: snapshot.getChildren()) {
                    onlineMap.put(userSnapshot.getKey(),(Boolean)userSnapshot.getValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //run after 10 seconds if didnt start yet
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isAdmin && currentState ==  States.SETUP)
                {
                    Log.d(TAG,"SetStartListener: start game");
                    messagesHandler.SendMessage(new Message("all", "Start Game!!!"));
                    ChangeGameState(States.START);
                }
            }
        }, 10000);
    }

    private void StartConnectionSerialListener() {
        serialServiceHandler.setHandler(messagesHandler,this,usersNameMap,idsMap);

        roomRef.child(roomName).child("PlayersState").child(userData.getId()).setValue(serialServiceHandler.isConnected());
        initArduino();

        serialServiceHandler.setListener(new SerialServiceHandler.ChangeListener() {
            @Override
            public void onChange() {
                Log.d(TAG,"StartConnectionSerialListener stage: "+serialServiceHandler.isConnected());
                roomRef.child(roomName).child("PlayersState").child(userData.getId()).setValue(serialServiceHandler.isConnected());
                initArduino();
            }
        });
    }

    private void initArduino()
    {
        if(serialServiceHandler.isConnected()) {
            Toast.makeText(context, "initArduino", Toast.LENGTH_SHORT).show();
            serialServiceHandler.SendData("StartGame" + teamsMap.toString());
        }
    }

    //keeps track on number of players connected
    private void StartPlayersNumListener(final GameMakerHandler gameMakerHandler) {
        Log.d(TAG,"StartPlayersNumListener: current Players loged in number");
        gameMakerHandler.setListener(new GameMakerHandler.ChangeListener() {
            @Override
            public void onChange() {
                playersNum = gameMakerHandler.getPlayersNum();
                Log.d(TAG,"StartPlayersNumListener: current Players loged in number: " + playersNum);
            }
        });
    }

    private void StartGame() {
        Log.d(TAG,"StartGame");
        //Start Timer
        timer = new CustomTimer(timeLeftMiliSeconds,timerText);
        timer.setListener(new CustomTimer.ChangeListener() {
            @Override
            public void onChange() {
                //end game
                //update arduino that the game ended
                ChangeGameState(States.END);
            }
        });
    }

    public void QuitGame(boolean ended)
    {
        if(timer != null) {
            timer.DestroyTimer();
        }

        if(startListener != null)
        {
            roomRef.removeEventListener(startListener);
        }

        try {
            roomRef.child(roomName).child("RoomState").removeEventListener(roomStateListener);
            roomRef.child(roomName).child("PlayersState").child(userData.getId()).setValue(false);

            roomRef.child(roomName).child("UsersLocation").child(userData.getId()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "QuitGame: deleted player: " + userData.getId());
                    if (isAdmin && playersNum > 1) {
                        //make another admin
                        ChangeGameState(States.ADMIN_CHANGE);
                    }
                }
            });
        }
        catch (Exception e)
        { Log.e(TAG,e.toString());}

        if(ended)
        {
            roomRef.child(roomName).child("Scores").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    HashMap<Integer, String> scores = new HashMap<>();
                    HashMap<Integer, Integer> teamsScores = new HashMap<>();

                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        String id = postSnapshot.getKey();
                        int score = postSnapshot.getValue(Integer.class);

                        scores.put(score, usersNameMap.get(id));//add player to score list

                        int team = teamsMap.get(id);
                        if (teamsScores.containsKey(team))//if team is in list
                        {
                            teamsScores.put(team, teamsScores.get(team) + score);//add to field
                        } else//if team isnt in list
                        {
                            teamsScores.put(team, score);//new field
                        }
                    }

                   if(playersNum == 1)
                    {
                            DestroyGame();
                    }

                    UserClient.getInstance().setScores(scores);
                    UserClient.getInstance().setTeamsScores(teamsScores);

                    Intent scoreBoardIntent = new Intent(context,ScoreBoardActivity.class);
                    scoreBoardIntent.putExtra("withTeams",withTeams);

                    context.startActivity(scoreBoardIntent);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
        else
        {
            if(playersNum == 1)
            {
                DestroyGame();
            }
            Intent intent = new Intent(context, ChooseRoomActivity.class);
            context.startActivity(intent);
        }

    }

    private void DestroyGame() {
        roomRef.child(roomName).removeValue();
        roomStoreRef.delete();
    }
}