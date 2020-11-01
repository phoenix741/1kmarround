package org.shadoware.a1kmarroud;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ArroundService extends Service implements TextToSpeech.OnInitListener {
    private static final String CHANNEL_DEFAULT_IMPORTANCE = "Running";
    private static final String TAG = "ArroundService";

    private boolean isStarted = false;

    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 50f;

    private static final int ARROUND_ID = 1;

    private Location mStartLocation;
    private Location mLastLocation;

    TextToSpeech textToSpeech;
    float lastDistance = 0;

    IBinder mBinder = new ArroundServiceBinder();

    private List<ArrroundLocationListener> listerner = new ArrayList<ArrroundLocationListener>();

    public interface ArrroundLocationListener {
        void updateLocation(Location startLocation, Location lastLocation, float distance);
    }

    public void addListener(ArrroundLocationListener l) {
        listerner.add(l);
    }

    public void removeListener(ArrroundLocationListener l) {
        listerner.remove(l);
    }

    private void callListener(Location startLocation, Location lastLocation, float distance) {
        for( ArrroundLocationListener l : listerner ) {
            l.updateLocation(startLocation, lastLocation, distance);
        }
    }

    private class LocationListener implements android.location.LocationListener {

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mStartLocation = new Location(provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            if (!isStarted) {
                mStartLocation.set(location);
            }
            mLastLocation.set(location);

            float distance = getDistance();
            callListener(mStartLocation, mLastLocation, distance);
            speakDistance();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class ArroundServiceBinder extends Binder {
        public ArroundService getServerInstance() {
            return ArroundService.this;
        }
    }

    public Location getStartLocation() {
        return mStartLocation;
    }

    public Location getLastLocation() {
        return mLastLocation;
    }

    public float getDistance() {
        float[] results = new float[1];
        Location.distanceBetween(mStartLocation.getLatitude(), mStartLocation.getLongitude(), mLastLocation.getLatitude(), mLastLocation.getLongitude(), results);
        float distance = results[0];
        return distance;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void start() {
        isStarted = true;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.ticker_text))
                        .build();

        startForeground(ARROUND_ID, notification);
    }

    public void stop() {
        isStarted = false;
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        createNotificationChannel();
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

        textToSpeech = new TextToSpeech(this, this);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }

        super.onDestroy();
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            NotificationChannel channel = new NotificationChannel(CHANNEL_DEFAULT_IMPORTANCE, name, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.FRANCE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("error", "This Language is not supported");
            }

            speakDistance();
        }
    }

    private void speakDistance() {
        int distance = (int)getDistance();
        if (Math.abs(lastDistance - distance) > 100 && isStarted) {
            String text = "" + distance + " metres.";
            if (distance > 1000) {
                text = "Alert: " + text + " Vous êtes en dehors de la zone.";
            } else if (distance > 900) {
                text = "Attention: " + text;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
            else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }

            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            lastDistance = distance;
        }
    }
}