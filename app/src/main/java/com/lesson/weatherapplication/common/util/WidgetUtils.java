package com.lesson.weatherapplication.common.util;

import static android.app.PendingIntent.FLAG_MUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.os.Build;

public class WidgetUtils {
    public static int getWithMutability() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return FLAG_MUTABLE;
        } else {
            return FLAG_UPDATE_CURRENT;
        }
    }
}

