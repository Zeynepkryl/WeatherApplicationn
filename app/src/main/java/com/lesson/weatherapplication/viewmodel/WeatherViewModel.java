package com.lesson.weatherapplication.viewmodel;

import android.app.Application;
import android.content.Context;


import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.lesson.weatherapplication.data.dailymodel.Daily;
import com.lesson.weatherapplication.data.model.WeatherModel;
import com.lesson.weatherapplication.service.DataManager;

import java.util.List;


public class WeatherViewModel extends AndroidViewModel {
    Context context;
    DataManager dataManager = new DataManager();
    private LiveData<WeatherModel> weatherModel;
    private LiveData<List<Daily>> dailyWeather;

    public WeatherViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Daily>> getDaily() {
        LiveData<List<Daily>> data = dataManager.getDaily();
        if (data != null)
            dailyWeather = data;
        return dailyWeather;
    }

    public LiveData<WeatherModel> getWeatherData(String city) {
        LiveData<WeatherModel> weather = dataManager.getWeatherData(city);
        if (weather != null)
            weatherModel = weather;
        return weatherModel;
    }
}