package org.shadoware.a1kmarroud;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ArroundLocationManager.ArrroundLocationListener {
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private FloatingActionButton goButton = null;
    private FloatingActionButton stopButton = null;
    private TextView distanceText = null;
    private TextView startText = null;
    private TextView locationText = null;

    private boolean mBounded;
    private ArroundService mServer;

    ArroundLocationManager locationManager;
    Location startLocation;
    Location runLocation;

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(MainActivity.this, "Service is disconnected", Toast.LENGTH_LONG).show();
            mServer.removeListener(MainActivity.this);
            mBounded = false;
            mServer = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(MainActivity.this, "Service is connected", Toast.LENGTH_LONG).show();
            mBounded = true;
            ArroundService.ArroundServiceBinder mLocalBinder = (ArroundService.ArroundServiceBinder) service;
            mServer = mLocalBinder.getServerInstance();
            mServer.addListener(MainActivity.this);

            startLocation = mServer.getStartLocation();
            runLocation = mServer.getRunLocation();

            updateTextLocation();
        }
    };


    @Override
    public void updateLocation(Location location) {
        runLocation = location;
        MainActivity.this.updateTextLocation();
    }

    public void updateTextLocation() {
        runOnUiThread(() -> {
            if (this.startLocation != null && this.runLocation != null) {
                distanceText.setText(getString(R.string.distanceLabel, (int) ArroundLocationManager.getDistance(startLocation, runLocation)));
            } else {
                distanceText.setText("");
            }
            if (this.startLocation != null) {
                startText.setText(getAddress(startLocation));
            } else {
                startText.setText("");
            }
            if (this.runLocation != null) {
                locationText.setText(getAddress(runLocation));
            } else {
                locationText.setText("");
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
        });

        locationManager = new ArroundLocationManager(this);
        locationManager.addListener((location) -> {
            if (!isMyServiceRunning(ArroundService.class)) {
                startLocation = location;
            }
            MainActivity.this.updateTextLocation();
        });
        locationManager.start();

        distanceText = findViewById(R.id.distance);
        startText = findViewById(R.id.start);
        locationText = findViewById(R.id.location);

        goButton = findViewById(R.id.goButton);
        stopButton = findViewById(R.id.stopButton);
        goButton.setOnClickListener(v -> {
            setStart(true);
        });
        stopButton.setOnClickListener(v -> {
            setStart(false);
        });
        setStart(isMyServiceRunning(ArroundService.class));
        MainActivity.this.updateTextLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServer != null) {
            unbindService(mConnection);
            mConnection.onServiceDisconnected(null);
        }
    }

    private void setStart(boolean start) {
        goButton.setVisibility(start ? View.GONE : View.VISIBLE);
        stopButton.setVisibility(!start ? View.GONE : View.VISIBLE);
        if (start) {
            startServer();
        } else {
            stopServer();
        }
    }

    private void startServer() {
        if (!mBounded) {
            Intent mIntent = new Intent(this, ArroundService.class);
            mIntent.putExtra("startLocation", startLocation);
            ContextCompat.startForegroundService(this, mIntent);
            bindService(mIntent, mConnection, BIND_AUTO_CREATE);
        }
    }

    private void stopServer() {
        if (mBounded) {
            mServer.stop();
            unbindService(mConnection);
            mConnection.onServiceDisconnected(null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private String getAddress(Location location) {
        Geocoder geocoder;
        List<Address> addresses;

        try {
            geocoder = new Geocoder(this, Locale.getDefault());

            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if (addresses.size() > 0 && addresses.get(0).getMaxAddressLineIndex() >= 0) {
                return addresses.get(0).getAddressLine(0);
            }
            return "Unknown";
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}