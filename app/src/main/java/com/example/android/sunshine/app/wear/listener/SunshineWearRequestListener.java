package com.example.android.sunshine.app.wear.listener;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.example.android.sunshine.app.utils.Debug;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Amrendra Kumar on 24/03/16.
 */
public class SunshineWearRequestListener extends WearableListenerService {
    private static final String INFO_REQUESTED_BY_WEAR = "/weather-info-requested-by-wear";
    private static final String INFO_REQUEST_KEY = "request_key";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Debug.c();
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                String path = dataEvent.getDataItem().getUri().getPath();
                Debug.e("onDataChanged : " + path, false);
                if (path.equals(INFO_REQUESTED_BY_WEAR)) {
/*                    Context context = getApplicationContext();
                    PreferenceManager.getInstance(context).removeKey(SunshineSyncAdapter
                            .LAST_LOW_REPORTED);
                    PreferenceManager.getInstance(context).removeKey(SunshineSyncAdapter
                            .LAST_HIGH_REPORTED);
                    PreferenceManager.getInstance(context).removeKey(SunshineSyncAdapter
                            .LAST_WEATHER_REPORTED);
                    PreferenceManager.getInstance(context).debug();*/
                    SunshineSyncAdapter.syncImmediately(this);

                }
            }
        }
    }
}
