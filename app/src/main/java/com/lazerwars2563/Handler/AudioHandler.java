package com.lazerwars2563.Handler;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.lazerwars2563.adapters.MessageAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

public class AudioHandler {
    private static String TAG = "AudioHandler";

    private Context context;
    private String id;
    private String roomName;

    private MediaRecorder recorder;
    private MediaPlayer mPlayer;

    private ArrayList<String> fileNames;
    private int fileCount;

    private StorageReference mStorageRef;
    private DatabaseReference roomRef;

    private MessageAdapter mMessageAdapter;


    public AudioHandler(Context context, String id, String roomName,DatabaseReference roomRef) {
        fileNames = new ArrayList<>();
        fileCount = 0;
        this.context = context;
        mStorageRef = FirebaseStorage.getInstance().getReference("Audio");
        this.roomRef = roomRef;
        this.id = id;
        this.roomName = roomName;
    }

    String CreateFileName()
    {
        ContextWrapper cw = new ContextWrapper(context.getApplicationContext());
        File directory = cw.getDir("audioDir", Context.MODE_PRIVATE);
        String file_name = directory.getAbsolutePath() + "/" + id + fileCount + ".3gp";
        fileNames.add(file_name);
        return file_name;
    }

    public void startRecording() {
        Log.d(TAG,"SetVoiceMessage: recording message");
        String fileName = CreateFileName();

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        recorder.start();
    }

    public String stopRecording(String to) {
        Log.d(TAG,"SetVoiceMessage: stop recording message");
        try {
            recorder.stop();
            recorder.release();
            recorder = null;

            return UploadAudio(to);
        }
        catch(Exception e){
            Log.e(TAG,"SetVoiceMessage:Error in stop recording message: " + e.toString());
            return "Error!";
    }
    }

    private String UploadAudio(final String to) {
        final String name = id + "-" + fileCount + ".3gp";
        StorageReference filePath = mStorageRef.child(roomName).child(name);
        Uri uri = Uri.fromFile(new File(fileNames.get(fileCount)));

        filePath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"UploadAudio: success");
                //get time

                Long tsLong = System.currentTimeMillis()/1000;
                String ts = tsLong.toString();

                //update all
                roomRef.child(roomName).child("chat").child(to + "-" + ts).setValue(name);
            }
        });
        //update number
        fileCount++;
        return name;
    }

    public static String getName(String name)
    {
        int iend = name.indexOf("-"); //this finds the first occurrence of "-"
        String subString ="Error: no -";
        if (iend != -1)
        {
            subString = name.substring(0 , iend);
        }
        return subString;
    }

    public void DownloadAudio(String filename)
    {
        String name = getName(filename);
        Log.d(TAG,"DownloadAudio: downloading: "+filename + ", user id: " + name);
        if(!name.equals(id))
        {
            StorageReference down = mStorageRef.child(roomName).child(filename);
            ContextWrapper cw = new ContextWrapper(context.getApplicationContext());
            File directory = cw.getDir("audioDir", Context.MODE_PRIVATE);
            final File file = new File(directory, name);
            down.getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    startPlaying(file.getAbsolutePath());
                }
            });
        }
    }

    public void startPlaying(String filePath) {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(filePath);
            mPlayer.prepare();
            mPlayer.start();
            Log.d(TAG,"startPlaying: started");
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    public void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }
}
