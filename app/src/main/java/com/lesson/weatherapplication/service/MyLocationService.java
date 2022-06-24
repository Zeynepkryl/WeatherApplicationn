package com.lesson.weatherapplication.service;

import static com.lesson.weatherapplication.constans.Constans.BASE_URL;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.lesson.weatherapplication.R;
import com.lesson.weatherapplication.activity.MainActivity;
import com.lesson.weatherapplication.constans.Constans;
import com.lesson.weatherapplication.constans.WidgetConstans;
import com.lesson.weatherapplication.data.WeatherAPI;
import com.lesson.weatherapplication.model.Weather;
import com.lesson.weatherapplication.model.WeatherModel;
import com.lesson.weatherapplication.util.PreferencesConstants;
import com.lesson.weatherapplication.util.WidgetUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyLocationService extends Service {
    String cityName;
    Context context;
    AppWidgetManager appWidgetManager;
    int[] appWidgetIds;
    private static Retrofit retrofit;
    private final IBinder binder = new LocalBinder();

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null && locationResult.getLastLocation() != null) {
                double latitude = locationResult.getLastLocation().getLatitude();
                double longitude = locationResult.getLastLocation().getLongitude();
                System.out.println("Loc" + locationResult.getLastLocation().getLongitude());
                Log.d("LOCATION_UPDATE", latitude + ", " + longitude);

                cityName = getCityName(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                System.out.println("Current:" + cityName);

                for (int appWidgetId : appWidgetIds) {
                    Intent intent = new Intent(context, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, WidgetUtils.getWithMutability());

                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
                    views.setOnClickPendingIntent(R.id.widget_Root, pendingIntent);

                    dataRequest(views, appWidgetManager, context, appWidgetId, getCityNameFromPreferences(context));

                }
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startLocationService();
        context = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationService();
    }

    private void startLocationService() {
        String channelId = "location_notification_channel";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(),
                channelId
        );

        builder.setContentTitle("Location Service");
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setContentText("Running");
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null
                    && notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(
                        channelId,
                        "Location Service",
                        NotificationManager.IMPORTANCE_HIGH
                );
                notificationChannel.setDescription("This channel is used by location service ");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        startForeground(WidgetConstans.LOCATION_SERVICE_ID, builder.build());
    }

    private void stopLocationService() {
        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    private String getCityName(double latitude, double longitude) {
        String cityName = "Android";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            for (Address address : addresses) {
                if (address != null) {
                    String city = address.getAdminArea();
                    cityName = city;
                    if (city != null) {
                        cityName = city;
                    } else {
                        Log.d("TAG", "CITY NOT FOUND");
                        Toast.makeText(this, "City Not Found", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    private void dataRequest(RemoteViews views, AppWidgetManager appWidgetManager, Context context, int appWidgetId, String cityName) {
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

    private String getCityNameFromPreferences(Context context) {
        String defaultCityName = "Ankara";
        SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesConstants.PREFERENCES_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(PreferencesConstants.CITY_NAME, defaultCityName);
    }

    public class LocalBinder extends Binder {
        public MyLocationService getService() {
            return MyLocationService.this;
        }
    }
}