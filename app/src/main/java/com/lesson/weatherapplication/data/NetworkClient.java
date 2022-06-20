package com.lesson.weatherapplication.data;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class NetworkClient {
    static OkHttpClient client = null;

    public static OkHttpClient createClient() {
        if (client == null)
            synchronized (NetworkClient.class) {
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
