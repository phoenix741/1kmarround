package org.shadoware.a1kmarroud;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ArroundService.ArrroundLocationListener {
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private Marker startMarker = null;
    private Polygon zoneMarker = null;
    private Marker locationMarker = null;
    private FloatingActionButton goButton = null;
    private FloatingActionButton stopButton = null;
    private TextView distanceText = null;
    private TextView startText = null;
    private TextView locationText = null;

    private boolean mBounded;
    private ArroundService mServer;

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
            ArroundService.ArroundServiceBinder mLocalBinder = (ArroundService.ArroundServiceBinder)service;
            mServer = mLocalBinder.getServerInstance();

            mServer.addListener(MainActivity.this);
            setNewLocation(mServer.getStartLocation(), mServer.getLastLocation());
        }
    };

    @Override
    public void updateLocation(Location startLocation, Location lastLocation, float distance) {
        setNewLocation(startLocation, lastLocation);

        distanceText.setText("" + distance + " meters");
        startText.setText(getAddress(startLocation));
        locationText.setText(getAddress(lastLocation));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //inflate and create the map
        setContentView(R.layout.activity_main);

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's
        //tile servers will get you banned based on this string


        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        requestPermissionsIfNecessary(new String[] {
                // if you need to show the current location, uncomment the line below
                // Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        Intent arroundService = new Intent(this, ArroundService.class);
        startService(arroundService);

        goButton = findViewById(R.id.goButton);
        stopButton = findViewById(R.id.stopButton);
        goButton.setOnClickListener(v -> {
            mServer.start();
            goButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);
        });
        stopButton.setOnClickListener(v -> {
            mServer.stop();
            goButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(View.GONE);
        });

        distanceText = findViewById(R.id.distance);
        startText = findViewById(R.id.start);
        locationText = findViewById(R.id.location);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent mIntent = new Intent(this, ArroundService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    };

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    };


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

    void setNewLocation(Location startLocation, Location lastLocation) {
        IMapController mapController = map.getController();
        mapController.setZoom(15);
        GeoPoint startPoint = new GeoPoint(startLocation.getLatitude(), startLocation.getLongitude());
        GeoPoint lastPoint = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
        mapController.setCenter(startPoint);

        if (startPoint.getLongitude() < -180)
            startPoint.setLongitude(startPoint.getLongitude() + 360);
        if (startPoint.getLongitude() > 180)
            startPoint.setLongitude(startPoint.getLongitude() - 360);
        //latitude is a bit harder. see https://en.wikipedia.org/wiki/Mercator_projection
        if (startPoint.getLatitude() > 85.05112877980659)
            startPoint.setLatitude(85.05112877980659);
        if (startPoint.getLatitude() < -85.05112877980659)
            startPoint.setLatitude(-85.05112877980659);

        List<GeoPoint> circle = Polygon.pointsAsCircle(startPoint, 1000);
        if (zoneMarker == null) {
            zoneMarker = new Polygon(map);
        }
        zoneMarker.setPoints(circle);
        zoneMarker.setTitle("1KM");
        map.getOverlayManager().add(zoneMarker);

        if (locationMarker == null) {
            locationMarker = new Marker(map);
        }
        locationMarker.setPosition(lastPoint);
        locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        locationMarker.setIcon(getResources().getDrawable(R.drawable.person));
        map.getOverlays().add(locationMarker);

        if (startMarker == null) {
            startMarker = new Marker(map);
        }
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setIcon(getResources().getDrawable(R.drawable.marker_default_focused_base));
        map.getOverlays().add(startMarker);

        map.invalidate();
    }

    String getAddress(Location location) {
        Geocoder geocoder;
        List<Address> addresses;

        try {
            geocoder = new Geocoder(this, Locale.getDefault());

            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

            return addresses.get(0).getAddressLine(0);
        } catch (IOException e) {
            return e.getMessage();
        }
    }

}