package com.lesson.weatherapplication.data;

import static com.lesson.weatherapplication.constans.Constans.BASE_URL;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    static Retrofit client = null;

    public static Retrofit createApiClient() {
        if (client == null)
            synchronized (ApiClient.class) {
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
