package com.example.android.sunshine.app.utils;

import android.content.Context;
import android.content.res.Resources;

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
}
