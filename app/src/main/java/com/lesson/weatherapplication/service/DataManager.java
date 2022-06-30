package com.lesson.weatherapplication.service;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lesson.weatherapplication.common.Constans;
import com.lesson.weatherapplication.data.WeatherAPI;
import com.lesson.weatherapplication.data.dailymodel.Daily;
import com.lesson.weatherapplication.data.model.WeatherModel;
import com.lesson.weatherapplication.data.model.WeatherResponse;
import com.lesson.weatherapplication.data.retrofit.ApiClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataManager {
    WeatherAPI weatherAPI = ApiClient.createApiClient().create(WeatherAPI.class);

    public LiveData<WeatherModel> getWeatherData(String city) {
        final MutableLiveData<WeatherModel> data = new MutableLiveData<>();
        weatherAPI.getWeather(city, Constans.API_KEY, Constans.METRIC).enqueue(new Callback<WeatherModel>() {
            @Override
            public void onResponse(@NonNull Call<WeatherModel> call, @NonNull Response<WeatherModel> response) {
                if (response.isSuccessful())
                    data.setValue(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<WeatherModel> call, @NonNull Throwable t) {
                data.setValue(null);
            }
        });
        return data;
    }

    public LiveData<List<Daily>> getDaily() {
        final MutableLiveData<List<Daily>> data = new MutableLiveData<>();
        weatherAPI.getDaily("0", "10", "10", Constans.API_KEY, "metric").enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.body() != null && response.isSuccessful())
                    data.setValue(response.body().daily);
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {

            }
        });
        return data;
    }
}