package com.lesson.weatherapplication.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lesson.weatherapplication.data.model.NetworkStatusEnum;

public class MyRecevier extends BroadcastReceiver {
    private final OnBroadcastReceiverReceivedListener listener;

    public MyRecevier(OnBroadcastReceiverReceivedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String status = NetworkUtil.getConnectionStatus(context);
        if (status.equals("Disconnected") || status.isEmpty()) {
            listener.onReceived(NetworkStatusEnum.Disconnected);
        } else if (status.equals("")) {
            listener.onReceived(NetworkStatusEnum.Connected);
        }
    }

    public interface OnBroadcastReceiverReceivedListener {
        void onReceived(NetworkStatusEnum status);
    }
}
