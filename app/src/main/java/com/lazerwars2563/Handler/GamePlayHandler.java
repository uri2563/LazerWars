package com.lazerwars2563.Handler;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

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
import com.google.firebase.firestore.ListenerRegistration;
import com.lazerwars2563.Activitys.ChooseRoomActivity;
import com.lazerwars2563.Activitys.WaitingRoomActivity;
import com.lazerwars2563.Class.Message;
import com.lazerwars2563.Class.PlayerViewer;

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
    private Map<String, String> usersNameMap;

    private long timeLeftMiliSeconds;

    private TextView timerText;
    private MessagesHandler messagesHandler;

    private int playersNum;//current Logged In players!

    public GamePlayHandler(DocumentReference roomStoreRef, TextView timerText, DatabaseReference roomRef, PlayerViewer userData, String roomName,
                           boolean isAdmin, MessagesHandler messagesHandler, Context context, Map<String, String> usersNameMap,GameDatabaseHandler gameDatabaseHandler) {
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

    private void SetReady()
    {
        roomRef.child(roomName).child("PlayersState").child(userData.getId()).setValue(true);
    }

    private ValueEventListener roomStateListener;
    private void ListenToRoomState() {
        roomStateListener = roomRef.child(roomName).child("RoomState").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentState = States.valueOf(snapshot.getValue().toString());
                Log.d(TAG, "ListenToRoomState: Game State Changed to: " + currentState.toString());
                if (currentState == States.START)
                {
                    if(startListener != null)
                    {
                        roomRef.removeEventListener(startListener);
                    }
                    StartGame();
                }
                else if(currentState == States.ADMIN_CHANGE)
                {
                    CreateAdmin();
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
        if(!isAdmin)
        {
            return;
        }
            startListener = roomRef.child(roomName).child("PlayersState").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(currentState ==  States.SETUP && snapshot.getChildrenCount() == usersNameMap.size())
                    {
                        Log.d(TAG,"SetStartListener: start game");
                        messagesHandler.SendMessage(new Message("all", "Start Game!!!"));
                        ChangeGameState(States.START);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
    }

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
            }
        });
    }

    public void QuitGame()
    {
        if(timer != null) {
            timer.DestroyTimer();
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

        if(playersNum == 1)
        {
            DestroyGame();
        }

        Intent intent = new Intent(context, ChooseRoomActivity.class);
        context.startActivity(intent);
    }

    private void DestroyGame() {
        roomRef.child(roomName).removeValue();
        roomStoreRef.delete();
    }
}
