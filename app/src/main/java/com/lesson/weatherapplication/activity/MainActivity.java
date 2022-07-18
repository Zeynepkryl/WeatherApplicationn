package com.lesson.weatherapplication.activity;


import static cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory.TAG;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;

import android.content.BroadcastReceiver;
import android.content.ComponentName;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.gms.location.LocationServices;

import com.lesson.weatherapplication.R;
import com.lesson.weatherapplication.activity.adapter.WeatherModelAdapter;

import com.lesson.weatherapplication.broadcastreceiver.MyRecevier;
import com.lesson.weatherapplication.common.WidgetConstans;
import com.lesson.weatherapplication.data.dailymodel.Daily;
import com.lesson.weatherapplication.databinding.ActivityMainBinding;
import com.lesson.weatherapplication.data.model.NetworkStatusEnum;
import com.lesson.weatherapplication.service.MyLocationService;
import com.lesson.weatherapplication.viewmodel.WeatherViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private boolean isBound = false;
    private MyLocationService service;
    private boolean isInitialized = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MyLocationService.LocalBinder binder = (MyLocationService.LocalBinder) iBinder;
            service = binder.getService();
            service.setListener(cityName -> {
                mCityName = cityName;
                if (!isInitialized){
                    inWeatherDataCall();
                }
            });
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };
    ActivityMainBinding binding;
    private WeatherModelAdapter adapter;
    private WeatherViewModel viewModel;
    private BroadcastReceiver myReceiver = null;
    private List<Daily> dailyList;

    Long updateTime;
    Handler handler;
    Runnable runnable;
    Handler dateHandler;
    Runnable dateRunnable;

    FusedLocationProviderClient mFusedLocationClient;
    int PERMISSION_ID = 101;

    String mCityName;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> Log.d(TAG, "onActivityResult: "));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Window window = getWindow();
        WindowManager.LayoutParams winParams = window.getAttributes();
        winParams.flags &= ~WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        window.setAttributes(winParams);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        sharedPreferences = getSharedPreferences("com.lesson.weatherapp.Activity", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        updateTime = sharedPreferences.getLong("update", 60000);

        dailyList = new ArrayList<>();

        handler = new Handler();

        myReceiver = new MyRecevier(status -> {
            if (status == NetworkStatusEnum.Disconnected) {
                Dialog dialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                dialog.setContentView(R.layout.noo_internet_dialog);
                dialog.show();

                TextView settingsText = dialog.findViewById(R.id.trainagainTextView);
                settingsText.setOnClickListener(view -> {
                    Intent intent1 = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    activityLauncher.launch(intent1);
                });
            }
        });

        broadCastcall();
        getWeather();

        binding.settingsButton.setOnClickListener(view -> {
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(i);
        });

        dateRunnable = () -> {
            SimpleDateFormat format = new SimpleDateFormat("EEEE, dd LLLL HH:mm aaa ", Locale.ENGLISH);
            String currentDate = format.format(new Date());
            binding.dtText.setText(currentDate);
            dateHandler.postDelayed(dateRunnable, 100);
        };
        dateHandler = new Handler();
        dateHandler.post(dateRunnable);
    }


    @Override
    protected void onStart() {
        super.onStart();
        startMyService();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound && service != null) {
            unbindService(serviceConnection);
        }
    }

    @SuppressLint("SetTextI18n")
    private void inWeatherDataCall() {
        viewModel.getWeatherData(mCityName).observe(MainActivity.this, weatherModel -> {
            if (weatherModel != null && weatherModel.getWeather() != null) {
                binding.cityText.setText(weatherModel.getName() + " | " + weatherModel.getSys().getCountry());
                binding.tempText.setText((int) weatherModel.getMain().getTemp().doubleValue() + "°");
                binding.windText.setText(String.valueOf(weatherModel.getWind().getSpeed() + " km/h"));
                binding.humidityText.setText(String.valueOf(weatherModel.getMain().getHumidity() + " %"));
                binding.visibilityText.setText(String.valueOf(weatherModel.getVisibility() + " m"));
                binding.pressureText.setText(String.valueOf(weatherModel.getMain().getPressure() + " hPa"));
                binding.feelsText.setText((int) weatherModel.getMain().getFeelsLike().doubleValue() + "°");
                binding.descriptionText.setText(String.valueOf(weatherModel.getWind().getSpeed()));
                binding.conditionText.setText(String.valueOf(weatherModel.getWeather().get(0).getDescription()));

                Glide.with(this)
                        .load("http://openweathermap.org/img/w/" + weatherModel.getWeather().get(0).getIcon() + ".png")
                        .into(binding.idIVIcon);
                getWeatherData(updateTime);
                isInitialized = true;
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void getWeatherData(long updateTime) {

        handler.removeCallbacks(runnable);
        runnable = () -> viewModel.getWeatherData(mCityName).observe(MainActivity.this, weatherModel -> {
            if (weatherModel != null && weatherModel.getWeather() != null) {
                binding.cityText.setText(weatherModel.getName() + " | " + weatherModel.getSys().getCountry());
                binding.tempText.setText((int) weatherModel.getMain().getTemp().doubleValue() + "°");
                binding.windText.setText(String.valueOf(weatherModel.getWind().getSpeed() + " km/h"));
                binding.humidityText.setText(String.valueOf(weatherModel.getMain().getHumidity() + " %"));
                binding.visibilityText.setText(String.valueOf(weatherModel.getVisibility() + " m"));
                binding.pressureText.setText(String.valueOf(weatherModel.getMain().getPressure() + " hPa"));
                binding.feelsText.setText((int) weatherModel.getMain().getFeelsLike().doubleValue() + "°");
                binding.descriptionText.setText(String.valueOf(weatherModel.getWind().getSpeed()));
                binding.conditionText.setText(String.valueOf(weatherModel.getWeather().get(0).getDescription()));
                handler.postDelayed(runnable, updateTime);
            }
        });
        handler.postDelayed(runnable, updateTime);
    }


    private void getWeather() {

        adapter = new WeatherModelAdapter(MainActivity.this, dailyList);
        binding.recyclerWeather.setAdapter(adapter);
        binding.recyclerWeather.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        viewModel.getDaily().observe(this, dailies -> {
            dailyList.clear();
            dailyList.addAll(dailies);
            adapter.updateList(dailyList);
        });
    }

    private void broadCastcall() {
        registerReceiver(myReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
    }


    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isBound && service != null) {
                    unbindService(serviceConnection);
                }
                startMyService();
            }
        }
    }

    private void startMyService() {
        Intent intent = new Intent(this, MyLocationService.class);
        intent.setAction(WidgetConstans.ACTION_START_LOCATION_SERVICE);
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTime = sharedPreferences.getLong("update", 60000);
        inWeatherDataCall();
    }

}

