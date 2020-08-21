package com.lazerwars2563.Handler;

import android.app.Activity;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class CustomTimer {
    private static String TAG = "CustomTimer";
    private boolean finish = false;
    private ChangeListener listener;

    private TextView timerText;
    private CountDownTimer countDownTimer;
    private long timeLeftMiliSeconds;

    public CustomTimer(long timeLeftMiliSeconds, TextView timerText ) {
    this.timeLeftMiliSeconds = timeLeftMiliSeconds;
    this.timerText=timerText;
    StartTimer();
    }

    private void StartTimer() {
        Log.d(TAG,"starting timer");
        timerText.setVisibility(View.VISIBLE);//check that visible
        countDownTimer = new CountDownTimer(timeLeftMiliSeconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMiliSeconds = millisUntilFinished;
                UpdateTimer();
            }

            @Override
            public void onFinish() {
                setFinish(true);
            }
        }.start();
    }

    public void DestroyTimer()
    {
        if(countDownTimer != null)
        {
            countDownTimer.cancel();
        }
    }

    private void UpdateTimer() {
        int minutes = (int) timeLeftMiliSeconds/60000;
        int seconds = (int) (timeLeftMiliSeconds% 60000)/1000;

        String timeLeftText;

        timeLeftText = "" + minutes;
        timeLeftText += ":";
        if(seconds<10){
            timeLeftText += "0";
        }
        timeLeftText += seconds;

        timerText.setText(timeLeftText);
    }

    public boolean isFinshed() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
        if (listener != null) listener.onChange();
    }

    public ChangeListener getListener() {
        return listener;
    }

    public void setListener(ChangeListener listener) {
        this.listener = listener;
    }

    public interface ChangeListener {
        void onChange();
    }
}
