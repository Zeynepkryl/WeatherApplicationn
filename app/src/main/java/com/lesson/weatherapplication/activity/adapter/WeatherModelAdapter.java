package com.lesson.weatherapplication.activity.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lesson.weatherapplication.data.dailymodel.Daily;
import com.lesson.weatherapplication.databinding.ItemDayDesignBinding;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WeatherModelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<Daily> weatherRVModalArrayList;

    public WeatherModelAdapter(Context context, List<Daily> weatherRVModalArrayList) {
        this.context = context;
        this.weatherRVModalArrayList = weatherRVModalArrayList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDayDesignBinding itemDayDesignBinding = ItemDayDesignBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new WeatherViewHolder(itemDayDesignBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Daily weatherItem = weatherRVModalArrayList.get(position);
        WeatherViewHolder viewHolder = (WeatherViewHolder) holder;

        ((WeatherViewHolder) holder).binding.maxTempTextView.setText((int) weatherItem.getTemp().getMax().doubleValue() + "°");
        ((WeatherViewHolder) holder).binding.minTempTextView.setText((int) weatherItem.getTemp().getMin().doubleValue() + "°");
        ((WeatherViewHolder) holder).binding.temp.setText(String.valueOf(weatherItem.getWeather().get(0).getDescription()));

        Glide.with(context)
                .load("http://openweathermap.org/img/w/" + weatherItem.getWeather().get(0).getIcon() + ".png")
                .into(((WeatherViewHolder) holder).binding.idIVCondition);

        long itemLong = weatherItem.getDt();
        java.util.Date d = new java.util.Date(itemLong * 1000L);
        String itemDateStr = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(d);
        ((WeatherViewHolder) holder).binding.dateText.setText(itemDateStr);
    }

    public void updateList(List<Daily> dailyList) {
        this.weatherRVModalArrayList = dailyList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (weatherRVModalArrayList == null)
            return 0;
        return weatherRVModalArrayList.size();

    }

    public static class WeatherViewHolder extends RecyclerView.ViewHolder {
        private ItemDayDesignBinding binding;

        public WeatherViewHolder(@NonNull ItemDayDesignBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
