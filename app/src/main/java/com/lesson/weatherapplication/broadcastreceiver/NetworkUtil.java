package com.lesson.weatherapplication.broadcastreceiver;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil {
    public static String getConnectionStatus(Context context) {
        String status = null;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                status = "Connected";
                return status;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                status = "MOBILE DATA ENABLED";
            }
        } else {
            return "Disconnected";
        }
        return status;
    }
}