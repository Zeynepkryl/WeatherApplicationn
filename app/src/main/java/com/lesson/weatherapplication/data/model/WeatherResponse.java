package com.lesson.weatherapplication.data.model;

import com.google.gson.annotations.SerializedName;
import com.lesson.weatherapplication.data.dailymodel.Daily;

import java.util.List;

public class WeatherResponse {
    @SerializedName("daily")
    public List<Daily> daily;
}
