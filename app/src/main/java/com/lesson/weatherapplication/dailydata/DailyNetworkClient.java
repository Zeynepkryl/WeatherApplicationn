package com.lesson.weatherapplication.dailydata;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class DailyNetworkClient {
    static OkHttpClient client = null;

    public static OkHttpClient createDaily() {
        if (client == null)
            synchronized (DailyNetworkClient.class) {
                if (client == null) {
                    client = new OkHttpClient().newBuilder()
                            .connectTimeout(5, TimeUnit.SECONDS)
                            .readTimeout(5, TimeUnit.SECONDS)
                            .writeTimeout(5, TimeUnit.SECONDS)
                            .build();
                }
            }

        return client;
    }
}
