package com.lesson.weatherapplication.viewmodel;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.lesson.weatherapplication.constans.Constans;
import com.lesson.weatherapplication.dailymodel.Daily;
import com.lesson.weatherapplication.data.ApiClient;
import com.lesson.weatherapplication.data.WeatherAPI;
import com.lesson.weatherapplication.model.WeatherModel;
import com.lesson.weatherapplication.model.WeatherResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherViewModel extends AndroidViewModel {

    Context context;

    private MutableLiveData<WeatherModel> weatherModel;
    private MutableLiveData<List<Daily>> dailyWeather;

    public WeatherViewModel(Application context) {
        super(context);
    }

    public MutableLiveData<WeatherModel> getWeatherData(String city) {

        weatherModel = new MutableLiveData<>();

        WeatherAPI weatherAPI = ApiClient.createApiClient().create(WeatherAPI.class);
        weatherAPI.getWeather(city, Constans.API_KEY, Constans.METRIC).enqueue(new Callback<WeatherModel>() {
            @Override
            public void onResponse(@NonNull Call<WeatherModel> call, @NonNull Response<WeatherModel> response) {
                if (response.isSuccessful())
                    weatherModel.setValue(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<WeatherModel> call, @NonNull Throwable t) {

            }
        });
        return weatherModel;
    }

    public MutableLiveData<List<Daily>> getDaily() {
        dailyWeather = new MutableLiveData<>();
        WeatherAPI weatherAPI = ApiClient.createApiClient().create(WeatherAPI.class);
        weatherAPI.getDaily("0", "10", "10", Constans.API_KEY, "metric").enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                Log.e("Response", response.body().toString());
                if (response.body() != null && response.isSuccessful())
                    dailyWeather.postValue(response.body().daily);

            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
            }
        });
        return dailyWeather;
    }
}