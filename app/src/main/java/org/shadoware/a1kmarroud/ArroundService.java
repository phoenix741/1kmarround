package org.shadoware.a1kmarroud;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ArroundService extends Service implements TextToSpeech.OnInitListener, AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = "ArroundService";

    private ArroundNotificationManager notificationManager;
    private ArroundLocationManager locationManager;

    private PowerManager.WakeLock wakeLock = null;

    private TextToSpeech textToSpeech;
    private float lastDistance = 0;

    private Location startLocation;
    private Location runLocation;

    private final List<ArroundLocationManager.ArrroundLocationListener> listener = new ArrayList<>();

    private final IBinder mBinder = new ArroundServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class ArroundServiceBinder extends Binder {
        public ArroundService getServerInstance() {
            return ArroundService.this;
        }
    }

    public void addListener(ArroundLocationManager.ArrroundLocationListener l) {
        listener.add(l);
    }

    public void removeListener(ArroundLocationManager.ArrroundLocationListener l) {
        listener.remove(l);
    }

    private void callListener(Location location) {
        for (ArroundLocationManager.ArrroundLocationListener l : listener) {
            l.updateLocation(location);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");

        PowerManager powerService = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerService.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ArroundService::lock");
        wakeLock.acquire();

        if (intent != null) {
            Bundle extras = intent.getExtras();
            Location location = (Location) extras.get("startLocation");
            if (location != null) {
                this.startLocation = location;
            }
        }

        Notification notification = notificationManager.createNotification(0);
        startForeground(ArroundNotificationManager.ARROUND_ID, notification);

        return START_STICKY;
    }

    public void stop() {
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        notificationManager = new ArroundNotificationManager(this);
        locationManager = new ArroundLocationManager(this);
        locationManager.addListener(location -> {
            runLocation = location;
            callListener(location);
            notificationManager.notifyDistance(getDistance());
            speakDistance();
        });
        locationManager.start();
        textToSpeech = new TextToSpeech(this, this);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            speakDistance();
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

    }

    private void speakDistance() {
        int distance = (int) getDistance();
        if (Math.abs(lastDistance - distance) > 100) {
            int stringId;
            if (distance > 1000) {
                stringId = R.string.speaker_meters_alert;
            } else if (distance > 900) {
                stringId = R.string.speaker_meters_warn;
            } else {
                stringId = R.string.speaker_meters_info;
            }

            String text = getString(stringId, distance);
            speak(text);

            lastDistance = distance;
        }
    }

    private void speak(String text) {
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int res = audioManager.requestAudioFocus(this, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

        if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }

            audioManager.abandonAudioFocus(this);
        }
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public Location getRunLocation() {
        return runLocation;
    }

    public float getDistance() {
        if (startLocation != null && runLocation != null) {
            return ArroundLocationManager.getDistance(startLocation, runLocation);
        } else {
            return 0;
        }
    }
}