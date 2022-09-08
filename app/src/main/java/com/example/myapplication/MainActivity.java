package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private GoogleMap map;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private static final int LOCATION_PERMISSION_CODE = 101;

    private static final String TAG = "MainActivity";

    private ArrayList<Double> accList = new ArrayList<>();
    private ArrayList deltaAcc = new ArrayList<Double>();
    private ArrayList<Double> SMavg = new ArrayList<>();

    double x = 0.0;
    double y = 0.0;
    private int cnt = 0;

    private SensorManager sensorManager;
    Sensor accelerometer;

    Context way;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        // accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);


        //map
        if (isLocationPermissionGranted()) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
        else {
            requestLocationPermission();
        }
    }

    private void startService(){
        Intent intent = new Intent(MainActivity.this, LocationService.class);
        startService(intent);
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long time = System.currentTimeMillis();
        Log.d(TAG, "Time: " + Instant.ofEpochSecond(time) + " X: " + sensorEvent.values[0] + " Y: " + sensorEvent.values[1] + " Z: " + sensorEvent.values[2]);
        accList.add((double) sensorEvent.values[2]);

        if (accList.size() > 1) {
            x = (double) accList.get(cnt);
            y = (double) accList.get(cnt - 1);
            System.out.println("delta = " + (x - y));
            double interval = Math.floor(accList.size() / 5);
            if (interval >= 1) {
                for (int i = 0; i < interval; i++) {
                    double avg = 0.0;
                    for (int j = 0; j < 5; j++) {
                        avg += accList.get(5 * i + j);
                    }
                    System.out.println("avg: " + avg / 5);
                    SMavg.add(avg / 5);
                }
            }
            System.out.println("Moving avg: " + SMavg);

//            for (int i = 0; i < SMavg.size(); i++) {
//                double normal = 0.0;
//                for (int j = 0; j < 5; j++) {
//                    normal += SMavg.get(j);
//                }
//                normal = normal/5;
//
//            }
            //classify the anomalies based on the delta values we achieve during the trial on a bike
            //define a threshold
            //use a hashmap to record the number and types of anomalies
            //also mark it in the map at the same time
            cnt++;
        } else {
            System.out.println(accList.get(cnt));
            cnt++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        float zoomLevel = 16.0f;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }

        LocationService ls = new LocationService();
        while (true) {
            List<List<String>> list = ls.getGPSdata();
            for (List<String> l : list) {
                if (l.get(2).equals("0")) {
                    LatLng latLng = new LatLng(Double.parseDouble(l.get(0)), Double.parseDouble(l.get(1)));
                    map.addMarker(new MarkerOptions().position(latLng).title("Bump"));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
                    l.set(2, "1");
                }
                else {
                    continue;
                }
            }
        }
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