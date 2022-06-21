package com.lesson.weatherapplication.bootcomplete;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.lesson.weatherapplication.service.MyLocationService;

public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, MyLocationService.class);
        context.startService(service);
        Log.d("TEST", "Service loaded at start");
    }
}
