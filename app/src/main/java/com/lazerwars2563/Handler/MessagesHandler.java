package com.lazerwars2563.Handler;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.lazerwars2563.Activitys.GameActivity;
import com.lazerwars2563.Class.Message;
import com.lazerwars2563.Class.PlayerViewer;
import com.lazerwars2563.R;
import com.lazerwars2563.adapters.MessageAdapter;

import java.util.ArrayList;

public class MessagesHandler {
    private static String TAG = "MessagesHandler";

    private DatabaseReference messageRef;
    private MessageAdapter mMessageAdapter;

    //get from activity
    private Context context;
    private RecyclerView recyclerView;
    private DatabaseReference roomRef;
    private String roomName;
    private PlayerViewer userData;

    public MessagesHandler(Context context, RecyclerView recyclerView, DatabaseReference roomRef, String roomName, PlayerViewer userData) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.roomRef = roomRef;
        this.roomName = roomName;
        this.userData = userData;

        SetMessages();
    }

    public void SendMessage(Message message) {
        Long tsLong = System.currentTimeMillis() / 1000;
        String ts = tsLong.toString();

        //set gameInfo
        roomRef.child(roomName).child("Messages").child(userData.getId() + "-" + ts).setValue(message);
    }

    private void SetMessages() {
        Log.d(TAG, "setUpRecyclerView ");
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        //linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        messageRef = roomRef.child(roomName).child("Messages");

        messageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<Message> arrayList = new ArrayList<>();
                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
                    if (IsMessageToMe(eventSnapshot.getValue(Message.class).getTo())) {
                        arrayList.add(eventSnapshot.getValue(Message.class));
                    }
                }
                mMessageAdapter = new MessageAdapter(arrayList, context, userData);
                recyclerView.setAdapter(mMessageAdapter);
                recyclerView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);//scroll to end
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private boolean IsMessageToMe(String t) {
        if (t.equals(userData.getId()) || t.equals(Integer.toString(userData.getTeam())) || t.equals("all")) {
            return true;
        }
        return false;
    }

}
