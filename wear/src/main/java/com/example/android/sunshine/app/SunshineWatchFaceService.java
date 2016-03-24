/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */


/*
Help taken from
http://code.tutsplus.com/tutorials/creating-an-android-wear-watch-face--cms-23718
https://medium.com/@moyinoluwa/developing-for-android-wear-a-noob-s-perspective-de47c4686ffb#.z87hk2ecf
 */
public class SunshineWatchFaceService extends CanvasWatchFaceService {
    public static final String TAG = "bdebug";
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFaceService.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFaceService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }


    private class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {
        private static final String INFO_FOR_WEAR = "/weather-info-for-wear";
        private static final String INFO_HIGH = "high";
        private static final String INFO_LOW = "low";
        private static final String INFO_WEATHER = "weatherId";
        private static final String INFO_KEY = "key";
        private static final String INFO_REQUESTED_BY_WEAR = "/weather-info-requested-by-wear";
        private static final String INFO_REQUEST_KEY = "request_key";
        GoogleApiClient mGoogleApiClient;

        final Handler mUpdateTimeHandler = new EngineHandler(this);

        boolean mRegisteredTimeZoneReceiver = false;
/*        Paint mBackgroundPaint;
        Paint mTextPaint;*/

        // paints
        Paint mBackgroundPaint;
        Paint mCurveAreaPaint;
        Paint mTextTimePaint;
        Paint mTextAmPmPaint;
        Paint mTextTimeSecPaint;
        Paint mTextDatePaint;
        Paint mTextTempHighPaint;
        Paint mTextTempLowPaint;

        // offsets
        float mXOffSetTime;
        float mYOffSetTime;

        boolean mAmbient;
        boolean mBurnInProtection;

        // temperature strings
        String high = "0";
        String low = "0";
        String weatherId = "0";

        Time mTime;

        public Engine() {
            initGoogleApiClient();
        }

        private void initGoogleApiClient() {
            if (null == mGoogleApiClient) {
                mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(Wearable.API)
                        .build();
            }
        }

        private void requestDeviceForWeatherInfo() {
            Log.i(TAG, "requestDeviceForWeatherInfo");
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(INFO_REQUESTED_BY_WEAR);
            long time = System.currentTimeMillis();
            putDataMapRequest.getDataMap().putString(INFO_REQUEST_KEY, Long.toString(time));
            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Error: parcel company didnot pickup parcel");
                            } else {
                                Log.i(TAG, "data item picked up");
                            }
                        }
                    });
        }

        private void connectGoogleClient() {
            mGoogleApiClient.connect();
        }

        private void disconnectGoogleClient() {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                Wearable.DataApi.removeListener(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }
        }

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        int mTapCount;

        /**
         * Whether the display supports fewer bits for each getColor in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Context context = getApplicationContext();
            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = SunshineWatchFaceService.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(getColor(R.color.primary_light));
            mCurveAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCurveAreaPaint.setColor(getColor(R.color.primary_dark));

            // time paints
            mTextTimePaint = createTextPaint(getColor(R.color.primary_dark), Typeface.DEFAULT_BOLD);
            mTextAmPmPaint = createTextPaint(getColor(R.color.primary_dark), Typeface.DEFAULT);
            mTextTimeSecPaint = createTextPaint(getColor(R.color.primary_dark), Typeface.DEFAULT);

            // date paints
            mTextDatePaint = createTextPaint(getColor(R.color.digital_text), Typeface.DEFAULT);

            // temp high low paints
            mTextTempLowPaint = createTextPaint(getColor(R.color.accent), Typeface.DEFAULT);
            mTextTempHighPaint = createTextPaint(getColor(R.color.accent), Typeface.DEFAULT);

            mTime = new Time();
        }

        private int getColor(int id) {
            return ContextCompat.getColor(getApplicationContext(), id);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                connectGoogleClient();
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
                disconnectGoogleClient();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffSetTime = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_time_round : R.dimen.digital_x_offset_time);
            mYOffSetTime = resources.getDimension(isRound
                    ? R.dimen.digital_y_offset_time_round : R.dimen.digital_y_offset_time);
            float textSizeTime = resources.getDimension(isRound
                    ? R.dimen.digital_text_time_size_round : R.dimen.digital_text_time_size);

            mTextTimePaint.setTextSize(textSizeTime);
            mTextTimeSecPaint.setTextSize(textSizeTime * 0.6f);
            mTextAmPmPaint.setTextSize(textSizeTime * 0.6f);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextTimePaint.setAntiAlias(!inAmbientMode);
                    mTextTempLowPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = SunshineWatchFaceService.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
                float l = bounds.left - bounds.width() / 2;
                float t = bounds.centerY() + 20f;
                float r = bounds.right + bounds.width() / 2;
                float b = bounds.centerY() + bounds.height();
                RectF rectF = new RectF(l, t, r, b);
                canvas.drawArc(rectF, 0, 360, false, mCurveAreaPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            String timeText = String.format("%d:%02d", mTime.hour, mTime.minute);

            String secText = String.format("%02d", mTime.second);

            float timeTextLen = mTextTimePaint.measureText(timeText);
            float secTextLen = mTextTimeSecPaint.measureText(secText);

            mXOffSetTime = bounds.centerX() - timeTextLen / 2;
            canvas.drawText(timeText, mXOffSetTime, mYOffSetTime, mTextTimePaint);
            canvas.drawText(secText, mXOffSetTime + timeTextLen + 2, mYOffSetTime,
                    mTextTimeSecPaint);


        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.e(TAG, "Error: onConnectionFailed");
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.e(TAG, "Info: onConnected");
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            requestDeviceForWeatherInfo();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.e(TAG, "Error: onConnectionSuspended");
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            Log.i(TAG, "Info: onDataChanged");
            for (DataEvent dataEvent : dataEventBuffer) {
                if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                    String path = dataEvent.getDataItem().getUri().getPath();
                    Log.i(TAG, "path : " + path);
                    if (path.equals(INFO_FOR_WEAR)) {
                        low = dataMap.getString(INFO_LOW);
                        high = dataMap.getString(INFO_HIGH);
                        weatherId = Integer.toString(dataMap.getInt(INFO_WEATHER));
                        Log.i(TAG, "lowTemp " + low + "highTemp " + high + "weatherId " + weatherId);
                        invalidate();
                    }
                }
            }
        }
    }
}
