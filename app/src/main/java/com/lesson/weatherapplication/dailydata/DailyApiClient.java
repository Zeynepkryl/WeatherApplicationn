package com.lesson.weatherapplication.dailydata;

import static com.lesson.weatherapplication.constans.Constans.BASE_URL;

import com.lesson.weatherapplication.data.NetworkClient;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DailyApiClient {
    static Retrofit client = null;

    public static Retrofit createDailyApiClient() {
        if (client == null)
            synchronized (DailyApiClient.class) {
                if (client == null) {
                    client = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(NetworkClient.createClient())
                            .build();
                }
            }
        return client;
    }
}

