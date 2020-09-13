package com.lazerwars2563.Handler;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.service.autofill.UserData;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

    private SerialServiceHandler serialServiceHandler;

    private final WeakReference<Activity> mActivity;
    private MessagesHandler messagesHandler;
    private GamePlayHandler gamePlayHandler;
    private Map<String, String> usersNameMap;
    private Map<String, String> idsMap;
    private UserDetails user;

    //firebase realTime db
    private DatabaseReference roomRef;

    private Context context;

    private boolean isUpdateArduinoId = false;

    public SerialHandler(Activity activity, SerialServiceHandler serialServiceHandler, Context context) {
        mActivity = new WeakReference<>(activity);
        user = UserClient.getInstance().getUser();
        this.serialServiceHandler = serialServiceHandler;
        this.context = context;

    }

    public void setHandlers(MessagesHandler messagesHandler, GamePlayHandler gamePlayHandler, Map<String, String> usersNameMap,Map<String, String> idsMap)
    {
        this.gamePlayHandler = gamePlayHandler;
        this.messagesHandler = messagesHandler;
        this.usersNameMap = usersNameMap;
        this.idsMap = idsMap;

        //set RealTime db
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        roomRef = database.getReference("Rooms");
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case UsbService.MESSAGE_FROM_SERIAL_PORT:

                String data = (String) msg.obj;
                if(data.equals(""))
                {
                    return;
                }

                String[] datas = data.split(" ");
                MessageType mType = GetMessageType(datas);

                Toast.makeText(context,"MessageType: " + mType.toString() ,Toast.LENGTH_SHORT).show();
                //Toast.makeText(context,"the first: " + datas[0] + " second: " + datas[1],Toast.LENGTH_SHORT).show();
                Toast.makeText(context,"the data...: " + data ,Toast.LENGTH_SHORT).show();//serialServiceHandler.SendData("id");

                if(mType == MessageType.ID)//setUp
                {
                    if(isUpdateArduinoId)
                    {
                        serialServiceHandler.SendData("ID;");
                    }
                    else
                    {
                        UserClient.getInstance().setGameId(datas[1]);
                        Toast.makeText(context,"the new name: " + datas[1] ,Toast.LENGTH_SHORT).show();
                        serialServiceHandler.SendData("hs");
                        isUpdateArduinoId = true;
                    }
                }
                else if(gamePlayHandler != null && messagesHandler != null && !data.equals(""))
                {
                    if(isUpdateArduinoId && mType == MessageType.HIT)//hit
                    {
                        //mActivity.get().userNameHeader.append(data);//

                    int score = Integer.parseInt(datas[1]);
                    String enemyArduinoId = datas[2];

                    Hit(enemyArduinoId,score);
                    }
                }

                break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
        }
    }

    private void Hit(String enemyArduinoId, int score) {
        String enemyId = idsMap.get(enemyArduinoId);

        //check that the enemy is online
        if (gamePlayHandler.getOnlineMap().get(enemyId)) {
            gamePlayHandler.AddToPlayerScore(user.getUserId(), score);
            messagesHandler.SendMessage(new com.lazerwars2563.Class.Message(user.getUserId(), "got hit from " + usersNameMap.get(enemyId) + "in the -"));

            messagesHandler.SendMessage(new com.lazerwars2563.Class.Message(enemyId, "Hit " + user.getUserName() + "and recived score: " + score));
            gamePlayHandler.AddToPlayerScore(enemyId, score);
        }
    }

    private static MessageType GetMessageType(String[] data) {
        if (data.length < 2)
        {
            return MessageType.UNKNOWN;
        }

        String prefix = data[0];
        if(prefix.equals("ID"))
        {
            return MessageType.ID;
        }else if(prefix.equals("HIT"))
        {
            return MessageType.HIT;
        }
        return MessageType.UNKNOWN;
    }
}
