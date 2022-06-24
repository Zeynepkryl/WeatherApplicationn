package com.lesson.weatherapplication.widget;

import android.app.ActivityManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.lesson.weatherapplication.R;

import com.lesson.weatherapplication.constans.WidgetConstans;
import com.lesson.weatherapplication.service.MyLocationService;


public class NewAppWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    }


    private boolean isLocationServiceRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service :
                    activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (MyLocationService.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    private void startLocationService(Context context) {
        if (!isLocationServiceRunning(context)) {
            Intent intent = new Intent(context, MyLocationService.class);
            intent.setAction(WidgetConstans.ACTION_START_LOCATION_SERVICE);
            context.startService(intent);
            Toast.makeText(context.getApplicationContext(), "Location Service Started", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onEnabled(Context context) {
        startLocationService(context);
    }

    @Override
    public void onDisabled(Context context) {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, NewAppWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        onUpdate(context, appWidgetManager, ids);

        appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_Root);
    }
}