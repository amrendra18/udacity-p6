package com.example.android.sunshine.app.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.example.android.sunshine.app.R;

/**
 * Created by Amrendra Kumar on 25/03/16.
 */
public class Utils {

    public static String getDay(Context context, int day) {
        Resources res = context.getResources();
        String[] weekdays = res.getStringArray(R.array.week_array);
        return weekdays[day];
    }

    public static String getMonth(Context context, int month) {
        Resources res = context.getResources();
        String[] months = res.getStringArray(R.array.month_array);
        return months[month];
    }

    public static int getWeatherIcon(int weatherId) {
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        Log.e("bdebug", "This should never happen");
        return -1;
    }
}
