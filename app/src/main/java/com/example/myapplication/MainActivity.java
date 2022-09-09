package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, OnMapReadyCallback {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private static final String TAG = "MainActivity";

    private ArrayList<Double> accList = new ArrayList<>();
    private ArrayList deltaAcc = new ArrayList<Double>();
    private ArrayList<Double> SMavg = new ArrayList<>();
    private HashMap<String, Integer> anomalies = new HashMap<>();

    double x = 0.0;
    double y = 0.0;
    private int cnt = 0;

    private GoogleMap googleMap;
    private static final int LOCATION_PERMISSION_CODE = 101;
    LocationService locationService = new LocationService();
    List<List<String>> gpsList = locationService.getGPSdata();
    private static final float ZOOM = 16.0f;

    private SensorManager sensorManager;
    Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //some default-made bullshit
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //gps
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= 23){
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }else {
                //Req Location Permission
                startService();
            }
        } else {
            //Start the Location Service
            startService();
        }

        // accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        //maps
        try {
            if (isLocationPermissionGranted()) {
                SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                supportMapFragment.getMapAsync(this);
            } else {
                requestLocationPermission();
            }
        } catch (Exception e) {
            Log.e(TAG, "onCreate", e);
        }

    }

    private void startService(){
        Intent intent = new Intent(MainActivity.this, LocationService.class);
        startService(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long time = System.currentTimeMillis();
        Log.d(TAG, "Time: " + Instant.ofEpochSecond(time) + " Z: " + sensorEvent.values[2]);
        accList.add((double) sensorEvent.values[2]);
        if (accList.size() >= 10) {
            int interval = accList.size() / 10;
            for (int i = 0; i < interval; i++) {
                double avg = 0.0;
                for (int j = 0; j < 10; j++) {
                    avg += accList.get(10 * i + j);
                }
                avg = avg/10;
                System.out.println("avg: " + avg);
                SMavg.add(avg);
                classify(avg);
            }
            //classify the anomalies based on the delta values we achieve during the trial on a bike
            //define a threshold
            //use a hashmap to record the number and types of anomalies
            //also mark it in the map at the same time
            cnt = 0;
            accList.clear();
        } else {
            System.out.println(accList.get(cnt));
            cnt++;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void classify (double avg){

        LatLng amsterdam = new LatLng(52.3676, 4.9041);
        googleMap.addMarker(new MarkerOptions().position(amsterdam).title("ams"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(amsterdam, ZOOM));

        double normal = 9.82;
        //System.out.println(avg);
        if (avg > (normal + 2.5) || avg < (normal - 2.5)) {
            double bigPotholeLatitude = Double.parseDouble(gpsList.get(gpsList.size() - 1).get(0));
            double bigPotholeLongitude = Double.parseDouble(gpsList.get(gpsList.size() - 1).get(1));
            if (anomalies.containsKey("Big Pothole")) {
                anomalies.replace("Big Pothole", anomalies.get("Big Pothole") + 1);
                System.out.println("big potty");
                LatLng bigPotHole = new LatLng(bigPotholeLatitude, bigPotholeLongitude);
                googleMap.addMarker(new MarkerOptions()
                        .position(bigPotHole)
                        .title("Big Pothole"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bigPotHole, ZOOM));

            } else {
                anomalies.put("Big Pothole", 1);
                System.out.println("big potty");
                LatLng bigPotHole = new LatLng(bigPotholeLatitude, bigPotholeLongitude);
                googleMap.addMarker(new MarkerOptions()
                        .position(bigPotHole)
                        .title("Big Pothole"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bigPotHole, ZOOM));
            }
        } else if (avg > (normal + 1) || avg < (normal - 1)) {
            double smallPotholeLatitude = Double.parseDouble(gpsList.get(gpsList.size() - 1).get(0));
            double smallPotholeLongitude = Double.parseDouble(gpsList.get(gpsList.size() - 1).get(1));
            if (anomalies.containsKey("Small Pothole")) {
                anomalies.replace("Small Pothole", anomalies.get("Small Pothole") + 1);
                System.out.println("small potty");
                LatLng smallPothole = new LatLng(smallPotholeLatitude, smallPotholeLongitude);
                googleMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        .position(smallPothole)
                        .title("Small Pothole"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(smallPothole, ZOOM));
            } else {
                anomalies.put("Small Pothole", 1);
                System.out.println("small potty");
                LatLng smallPothole = new LatLng(smallPotholeLatitude, smallPotholeLongitude);
                googleMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        .position(smallPothole)
                        .title("Small Pothole"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(smallPothole, ZOOM));
            }
        } else {
            System.out.println("normal");
        }
        System.out.println(anomalies);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startService();
                }else {
                    Toast.makeText(this, "Give me permission", Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        LatLng amsterdam = new LatLng(52.3676, 4.9041);
        googleMap.addMarker(new MarkerOptions().position(amsterdam).title("ams"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(amsterdam, ZOOM));
    }

    private boolean isLocationPermissionGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        else {
            return false;
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_CODE);
    }
}