package com.lesson.weatherapplication.data;

import com.lesson.weatherapplication.model.WeatherModel;
import com.lesson.weatherapplication.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherAPI {

    @GET("data/2.5/weather")
    Call<WeatherModel>
    getWeather(@Query("q") String q, @Query("appid") String apikey, @Query("units") String units);

    @GET("data/2.5/onecall")
    Call<WeatherResponse>
    getDaily(@Query("lat") String lat, @Query("lon") String lon, @Query("exclude") String exclude, @Query("appid") String appid, @Query("units") String units);

}

