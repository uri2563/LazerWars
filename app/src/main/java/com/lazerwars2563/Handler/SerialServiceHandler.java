package com.lazerwars2563.Handler;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.database.DatabaseReference;
import com.lazerwars2563.Activitys.CreateNewRoomActivity;
import com.lazerwars2563.Activitys.GameActivity;
import com.lazerwars2563.Activitys.WaitingRoomActivity;
import com.lazerwars2563.Class.PlayerViewer;
import com.lazerwars2563.R;
import com.lazerwars2563.services.UsbService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

public class SerialServiceHandler {
    public static final String TAG = "SerialServiceHandler";

    private UsbService usbService;
    private Context context;
    private SerialHandler serialHandler;
    private GameActivity gameActivity;

    private boolean connected = false;
    private SerialServiceHandler.ChangeListener listener;

    private boolean setUp;

    public SerialServiceHandler(Context context, Activity activity,boolean setUp) {
        Log.d(TAG,"SerialServiceHandler init");
        this.context = context;
        this.setUp = setUp;

        serialHandler = new SerialHandler(activity,this,context);
        if(!setUp)
        {
            gameActivity = (GameActivity) activity;
        }
    }

    public void setHandler(MessagesHandler messagesHandler, GamePlayHandler gamePlayHandler,Map<String, String> usersNameMap,Map<String, String> idsMap)
    {
        serialHandler.setHandlers(messagesHandler,gamePlayHandler,usersNameMap,idsMap);
    }

    public boolean SendData(String data)
    {
        if(usbService == null || !connected)
        {
            return false;
        }
        usbService.write(data.getBytes());
        return true;
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        context.registerReceiver(mUsbReceiver, filter);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    setConnation(true);
                    if(alertDialog != null && alertDialog.isShowing())
                    {
                        alertDialog.dismiss();
                    }
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    setConnation(false);
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    if(setUp)
                    {
                        ActionNoUsb("Connect","Return");
                    }
                    else
                    {
                        MakeAlarm();
                        ActionNoUsb("Connect","Leave");
                    }
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    setConnation(false);
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    if(setUp)
                    {
                        ActionNoUsb("Connect","Return");
                    }
                    else
                    {
                        MakeAlarm();
                        ActionNoUsb("Connect","Leave");
                    }
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    setConnation(false);
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void MakeAlarm()
    {
        //add alarm sound
        MediaPlayer ring= MediaPlayer.create(gameActivity, R.raw.alarm1);
        try {
            ring.start();
            // Get instance of Vibrator from current Context
            Vibrator v = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 600 milliseconds
            v.vibrate(600);
        } catch (Exception e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    AlertDialog alertDialog;
    private void ActionNoUsb(String positive, String negative)
    {
        if(alertDialog!= null && alertDialog.isShowing())
        {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Couldnt find usb connation, please connect to your suit and click Connect")
                .setCancelable(false)
                .setPositiveButton(positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(usbService != null)
                        {
                            usbService.findSerialPortDevice();
                        }
                        dialog.cancel();                    }
                })
                .setNegativeButton(negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!setUp) {
                            gameActivity.DestroyHandlers();
                        }
                        else
                        {
                            Toast.makeText(context,"ActionNoUsb: Dubag return", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        alertDialog = builder.create();
        alertDialog.show();
    }

    private final ServiceConnection usbConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(serialHandler);
           // Toast.makeText(context,"onServiceConnected",Toast.LENGTH_SHORT).show();
           // usbService.findSerialPortDevice();
            if(usbService != null) {
                setConnation(usbService.getSerialPortConnection());
                if (!usbService.getSerialPortConnection()) {
                    ActionNoUsb("Connect", "Leave");
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };
    private Intent startService = null;
    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            startService = new Intent(context, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            context.startService(startService);
        }
        Intent bindingIntent = new Intent(context, service);
        context.bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void OnPauseSerial()
            {
                context.unregisterReceiver(mUsbReceiver);
                context.unbindService(usbConnection);
            }

    public void OnResumeSerial()
    {
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
        /*if(usbService != null)
        {
            Toast.makeText(context,"OnResumeSerial 1",Toast.LENGTH_SHORT).show();
            setConnation(usbService.getSerialPortConnection());
            if(!usbService.getSerialPortConnection()) {
                Toast.makeText(context,"OnResumeSerial 2",Toast.LENGTH_SHORT).show();
                ActionNoUsb("Connect", "Leave");
            }
        }*/
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnation(boolean connected) {
        this.connected = connected;
        if (listener != null) listener.onChange();
    }

    public SerialServiceHandler.ChangeListener getListener() {
        return listener;
    }

    public void setListener(SerialServiceHandler.ChangeListener listener) {
        this.listener = listener;
    }

    public interface ChangeListener {
        void onChange();
    }
}
