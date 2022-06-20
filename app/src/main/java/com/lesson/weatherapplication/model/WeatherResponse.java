package com.lesson.weatherapplication.model;

import com.google.gson.annotations.SerializedName;
import com.lesson.weatherapplication.dailymodel.Daily;

import java.util.List;

public class WeatherResponse {
    @SerializedName("daily")
    public List<Daily> daily;
}
