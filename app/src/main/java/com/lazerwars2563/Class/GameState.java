package com.lazerwars2563.Class;

import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.DocumentReference;
import com.lazerwars2563.services.CustomTimer;

public class GameState
{
    private enum States{START, STOP, END};
    private States currentState;
    private CustomTimer timer;
    private DatabaseReference roomRef;
    private PlayerViewer userData;
    private String roomName;
    private boolean isAdmin;

    public GameState(long timeLeftMinutes, DocumentReference roomStoreRef, TextView timerText, DatabaseReference roomRef, PlayerViewer userData, String roomName, boolean isAdmin) {
        this.currentState = States.START;
        this.roomRef = roomRef;
        this.userData = userData;
        this.roomName = roomName;
        this.isAdmin = true;
        long timeLeftMiliSeconds = timeLeftMinutes * 60000;
        //Start Timer
        timer = new CustomTimer(timeLeftMiliSeconds,timerText);
        timer.setListener(new CustomTimer.ChangeListener() {
            @Override
            public void onChange() {
                StartGame();
            }
        });
    }

    private void StartGame() {

    }

    private void SendMessage(Message message) {
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();

        //set gameInfo
        roomRef.child(roomName).child("Messages").child(userData.getId() + "-" + ts).setValue(message);
    }

}
