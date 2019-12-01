package com.example.homework2;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        SensorEventListener {


    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;
    List<Marker> markerList;
    FloatingActionButton fab1;
    FloatingActionButton fab2;
    SensorManager mSensorManager;
    Sensor mSensor;
    private long lastUpdate = -1;
    private TextView SensorValues;
    SensorEventListener g;
    boolean SensorStart = false;
    List<Double> LatLngList;
    final String MARKERS_JSON_FILE = "markers.json";





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        markerList = new ArrayList<>();
        LatLngList = new ArrayList<>();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        SensorValues = findViewById(R.id.textView);
        SensorValues.setVisibility(View.INVISIBLE);

        fab1 = findViewById(R.id.floatingActionButton1);
        fab2 = findViewById(R.id.floatingActionButton2);


        fab1.animate().translationY(fab1.getHeight() + 200f).setInterpolator(new LinearInterpolator()).start();
        fab2.animate().translationY(fab2.getHeight() + 200f).setInterpolator(new LinearInterpolator()).start();



        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            Toast.makeText(this,"No accelerometer",Toast.LENGTH_SHORT).show();
        }

        g = this;

        LatLngList.clear();
        markerList.clear();
        restoreFromJson();


    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        loadMarkers();
        LatLng latLng = new LatLng(52.4,16.9);
        CameraPosition cameraPos = new CameraPosition(latLng,5,0,0);
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPos));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getMinZoomLevel()));
    }


    @Override
    public void onMapLongClick(LatLng latLng) {

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude,latLng.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .alpha(0.8f)
                .title(String.format("Position:(%.2f, %.2f)",latLng.latitude,latLng.longitude)));

        LatLngList.add(latLng.latitude);
        LatLngList.add(latLng.longitude);
        markerList.add(marker);

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        CameraPosition cameraPos = mMap.getCameraPosition();
        if(cameraPos.zoom  < 14f)
            mMap.moveCamera(CameraUpdateFactory.zoomTo(14f));

        fab1.animate().translationY(0f).setInterpolator(new LinearInterpolator()).setDuration(500).start();
        fab2.animate().translationY(0f).setInterpolator(new LinearInterpolator()).setDuration(500).start();


        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SensorStart = !SensorStart;

                if (SensorStart == true) {
                    mSensorManager.registerListener(g, mSensor, 100000);
                    SensorValues.setVisibility(View.VISIBLE);
                }
                else {
                    mSensorManager.unregisterListener(g, mSensor);
                    SensorValues.setVisibility(View.INVISIBLE);
                }

            }
        });

        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fab1.animate().translationY(fab1.getHeight() + 200f).setInterpolator(new LinearInterpolator()).start();
                fab2.animate().translationY(fab2.getHeight() + 200f).setInterpolator(new LinearInterpolator()).start();
            }
        });

        return false;
    }



    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    if (gpsMarker != null)
                        gpsMarker.remove();

                    Location location = locationResult.getLastLocation();
                    gpsMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                            .alpha(0.8f)
                            .title("Current Location"));
                }
            }
        };
    }


    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastLocation = fusedLocationClient.getLastLocation();


        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();


    }


    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private  void stopLocationUpdates() {
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void zoomInClick(View v) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }
    public void zoomOutClick(View v) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        long timeMicro;

        if (lastUpdate == -1) {
            lastUpdate = event.timestamp;
            timeMicro = 0;
        } else {
            timeMicro = (event.timestamp - lastUpdate)/ 1000L;
            lastUpdate = event.timestamp;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Acceleration:\n");
        stringBuilder.append(String.format("x: %.4f ",event.values[0]));
        stringBuilder.append(String.format("y: %.4f ",event.values[1]));
        stringBuilder.append(String.format("z: %.4f ",event.values[2]));


        SensorValues.setText(stringBuilder.toString());


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private void saveMarkersToJson() {
        Gson gson = new Gson();
        String listJson = gson.toJson(LatLngList);
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(MARKERS_JSON_FILE,MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreFromJson() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson=  new Gson();
        String readJson;
        try {
            inputStream = openFileInput(MARKERS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) > 0){
                String tmp= String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0,n) : tmp;
                builder.append(substring);

            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<Double>>() {}.getType();
            List<Double> o = gson.fromJson(readJson,collectionType);
            if (o != null){
                LatLngList.clear();
                for(Double d : o) {
                    LatLngList.add(d);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        markerList.clear();
    }

    @Override
    protected void onDestroy() {
        saveMarkersToJson();
        super.onDestroy();
    }


    private void loadMarkers () {
        if(LatLngList.size() != 0)
            for (int i = 0;i <LatLngList.size();i+=2){
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(LatLngList.get(i),LatLngList.get(i+1)))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        .alpha(0.8f)
                        .title(String.format("Position:(%.2f, %.2f)",LatLngList.get(i),LatLngList.get(i+1))));
            }

    }


    public void clearMemory(View view) {
        LatLngList.clear();
        markerList.clear();
        mMap.clear();
    }
}
