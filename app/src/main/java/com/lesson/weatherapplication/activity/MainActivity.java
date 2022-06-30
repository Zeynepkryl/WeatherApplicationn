package com.lesson.weatherapplication.activity;

import static com.lesson.weatherapplication.common.PreferencesConstants.CITY_NAME;
import static cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory.TAG;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;
import com.lesson.weatherapplication.R;
import com.lesson.weatherapplication.activity.adapter.WeatherModelAdapter;

import com.lesson.weatherapplication.broadcastreceiver.MyRecevier;
import com.lesson.weatherapplication.data.dailymodel.Daily;
import com.lesson.weatherapplication.databinding.ActivityMainBinding;
import com.lesson.weatherapplication.data.model.NetworkStatusEnum;
import com.lesson.weatherapplication.service.MyLocationService;
import com.lesson.weatherapplication.viewmodel.WeatherViewModel;
import com.lesson.weatherapplication.widget.NewAppWidget;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private boolean isBound = false;
    private MyLocationService service;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MyLocationService.LocalBinder binder = (MyLocationService.LocalBinder) iBinder;
            service = binder.getService();
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
    ActivityResultLauncher<String> permissionLauncher;
    LocationManager locationManager;
    LocationListener locationListener;

    Handler handler;
    Runnable runnable;
    Handler dateHandler;
    Runnable dateRunnable;

    FusedLocationProviderClient mFusedLocationClient;
    int PERMISSION_ID = 101;

    String cityName;
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
        registerLauncher();
        getWeather();
        getLastLocation();

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
        Intent intent = new Intent(this, MyLocationService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
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
        viewModel.getWeatherData(cityName).observe(MainActivity.this, weatherModel -> {
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
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void getWeatherData(long updateTime) {

        handler.removeCallbacks(runnable);
        runnable = () -> viewModel.getWeatherData(cityName).observe(MainActivity.this, weatherModel -> {
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

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (isLocationEnabled()) {

                mFusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
                    Location location = task.getResult();
                    requestNewLocationData();
                    if (location != null) {
                        requestNewLocationData();

                        cityName = getcityName(location.getLatitude(), location.getLongitude());
                        inWeatherDataCall();
                        System.out.println("Get Last Location: " + cityName);

                        editor.putString(CITY_NAME, cityName);
                        editor.apply();

                        triggerWidgetOnUpdate();

                    }
                });
            } else {
                Toast.makeText(this, getString(R.string.location_warning_message), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }

    private void requestNewLocationData() {

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } else {
            requestPermissions();
        }
    }

    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            cityName = getcityName(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            inWeatherDataCall();
        }
    };

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    private boolean isLocationEnabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = location -> {
            System.out.println("Location:" + location.toString());
            cityName = getcityName(location.getLatitude(), location.getLongitude());
        };


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Snackbar.make(binding.getRoot(), "Permission needed", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                }).show();
            }
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private String getcityName(double latitude, double longitude) {
        String cityName = "Istanbul";
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
                        Toast.makeText(this, " City Not Found", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    private void registerLauncher() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    }

                } else {
                    Toast.makeText(MainActivity.this, "Permission Needed", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTime = sharedPreferences.getLong("update", 60000);
        inWeatherDataCall();
    }

    private void triggerWidgetOnUpdate() {
        Intent intent = new Intent(this, NewAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        int[] ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(getApplication(), NewAppWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        sendBroadcast(intent);
    }
}