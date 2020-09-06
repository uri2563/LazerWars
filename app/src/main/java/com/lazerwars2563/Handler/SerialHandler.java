package com.lazerwars2563.Handler;

import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.lazerwars2563.Activitys.GameActivity;
import com.lazerwars2563.services.UsbService;

import java.lang.ref.WeakReference;

public class SerialHandler extends Handler {
    private final WeakReference<GameActivity> mActivity;

    public SerialHandler(GameActivity activity) {
        mActivity = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case UsbService.MESSAGE_FROM_SERIAL_PORT:
                String data = (String) msg.obj;
                //Debug: change...
                mActivity.get().userNameHeader.append(data);
                break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
        }
    }
}
