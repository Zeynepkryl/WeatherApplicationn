package com.lesson.weatherapplication.widget;

import static android.content.Context.MODE_PRIVATE;

import static com.lesson.weatherapplication.common.Constans.BASE_URL;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.lesson.weatherapplication.R;

import com.lesson.weatherapplication.activity.MainActivity;
import com.lesson.weatherapplication.common.Constans;
import com.lesson.weatherapplication.common.PreferencesConstants;
import com.lesson.weatherapplication.common.WidgetConstans;
import com.lesson.weatherapplication.common.util.WidgetUtils;
import com.lesson.weatherapplication.data.WeatherAPI;
import com.lesson.weatherapplication.data.model.Weather;
import com.lesson.weatherapplication.data.model.WeatherModel;
import com.lesson.weatherapplication.service.MyLocationService;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class NewAppWidget extends AppWidgetProvider {

    private static Retrofit retrofit;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, WidgetUtils.getWithMutability());

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
            views.setOnClickPendingIntent(R.id.widget_Root, pendingIntent);

            dataRequest(views, appWidgetManager, context, appWidgetId, getCityNameFromPreferences(context));

        }
    }


    private boolean isLocationServiceRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service :
                    activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (MyLocationService.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    private void startLocationService(Context context) {
        if (!isLocationServiceRunning(context)) {
            Intent intent = new Intent(context, MyLocationService.class);
            intent.setAction(WidgetConstans.ACTION_START_LOCATION_SERVICE);
            context.startService(intent);
            Toast.makeText(context.getApplicationContext(), "Location Service Started", Toast.LENGTH_LONG).show();
        }
    }

    private void dataRequest(RemoteViews views, AppWidgetManager appWidgetManager, Context
            context, int appWidgetId, String cityName) {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(logging);

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        WeatherAPI service = retrofit.create(WeatherAPI.class);

        Call<WeatherModel> weatherCall = service.getWeather(cityName, Constans.API_KEY, Constans.METRIC);

        AppWidgetTarget weatherIcon = new AppWidgetTarget(context, R.id.widgetCondition, views, appWidgetId);

        weatherCall.enqueue(new Callback<WeatherModel>() {
            @Override
            public void onResponse(@NonNull Call<WeatherModel> call, @NonNull Response<WeatherModel> response) {
                if (response.isSuccessful()) {
                        assert response.body() != null;
                        Weather todayWeather = response.body().getTodayWeather();
                        views.setTextViewText(R.id.widgetDescription, response.body().getWeather().get(0).getDescription());
                        views.setTextViewText(R.id.widgetminTemp, ((int) response.body().getMain().getTempMin().doubleValue() + "°"));
                        views.setTextViewText(R.id.widgetmaxTemp, ((int) response.body().getMain().getTempMax().doubleValue() + "°"));
                        views.setTextViewText(R.id.widgetTemp, ((int) response.body().getMain().getTemp().doubleValue() + "°"));
                        views.setTextViewText(R.id.widgetcityText, response.body().getName());

                        Glide.with(context.getApplicationContext())
                                .asBitmap()
                                .load(todayWeather.getIconUrl())
                                .into(weatherIcon);
                }
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }

            @Override
            public void onFailure(@NonNull Call<WeatherModel> call, @NonNull Throwable t) {
            }
        });
    }

    @Override
    public void onEnabled(Context context) {
        startLocationService(context);
    }

    @Override
    public void onDisabled(Context context) {

    }

    private String getCityNameFromPreferences(Context context) {
        String defaultCityName = "Ankara";
        SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesConstants.PREFERENCES_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(PreferencesConstants.CITY_NAME, defaultCityName);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, NewAppWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        onUpdate(context, appWidgetManager, ids);

        appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_Root);
    }

}