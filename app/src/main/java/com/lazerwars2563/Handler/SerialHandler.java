package com.lazerwars2563.Handler;

import android.os.Handler;
import android.os.Message;
import android.service.autofill.UserData;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lazerwars2563.Activitys.GameActivity;
import com.lazerwars2563.Class.UserDetails;
import com.lazerwars2563.services.UsbService;
import com.lazerwars2563.util.UserClient;

import java.lang.ref.WeakReference;
import java.util.Map;

public class SerialHandler extends Handler {

    private enum MessageType{ID,HIT,UNKNOWN};

    private final WeakReference<GameActivity> mActivity;
    private MessagesHandler messagesHandler;
    private GamePlayHandler gamePlayHandler;
    private Map<String, String> usersNameMap;
    private UserDetails user;

    private boolean isUpdateArduinoId = false;

    //firebase realTime db
    private FirebaseDatabase database;
    private DatabaseReference roomRef;

    public SerialHandler(GameActivity activity) {
        mActivity = new WeakReference<>(activity);
        user = UserClient.getInstance().getUser();
    }

    public void setHandlers(MessagesHandler messagesHandler, GamePlayHandler gamePlayHandler,Map<String, String> usersNameMap)
    {
        this.gamePlayHandler = gamePlayHandler;
        this.messagesHandler = messagesHandler;
        this.usersNameMap = usersNameMap;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case UsbService.MESSAGE_FROM_SERIAL_PORT:

                String data = (String) msg.obj;
                String[] datas = data.split(" ");
                MessageType mType = GetMessageType(datas);

           /*     if(gamePlayHandler != null && messagesHandler != null && !data.equals(""))
                {
                    if(mType == MessageType.ID)//setUp
                    {
                        if(isUpdateArduinoId)
                        {
                            //can send data to Arduino that recived
                        }
                        else
                        {
                            database = FirebaseDatabase.getInstance();
                            roomRef = database.getReference("Rooms");
                            roomRef.child(gamePlayHandler.getRoomName()).child("ArduinoIds").child(datas[1]).setValue(user.getUserId()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    isUpdateArduinoId = true;
                                    ListenToId();
                                    //can send data to Arduino that recived
                                }
                            });
                        }
                    }
                    else if(isUpdateArduinoId && mType == MessageType.HIT)//hit
                    {
                        //mActivity.get().userNameHeader.append(data);//

                    int score = Integer.parseInt(datas[1]);
                    String enemyArduinoId = datas[2];

                    Hit(enemyArduinoId,score);
                    }
                }
*/
                break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
        }
    }

    private void ListenToId() {
    }

    private void Hit(String enemyArduinoId, int score) {
        
        gamePlayHandler.AddToPlayerScore(user.getUserId(),score);
        messagesHandler.SendMessage(new com.lazerwars2563.Class.Message(user.getUserId(),"got hit from - in the -"));

        //messagesHandler.SendMessage(new Message(enemy, "Hit "+ user.name + "and recived score: " + score));
        //gamePlayHandler.AddToPlayerScore(enemyId,score);*/
    }

    private static MessageType GetMessageType(String[] data) {
        if (data.length < 2)
        {
            return MessageType.UNKNOWN;
        }

        String prefix = data[0];
        if(prefix == "ID")
        {
            return MessageType.ID;
        }else if(prefix == "HIT")
        {
            return MessageType.HIT;
        }
        return MessageType.UNKNOWN;
    }
}
