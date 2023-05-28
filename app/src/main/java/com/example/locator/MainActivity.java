package com.example.locator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private LocationManager locationManager;
    private final int REQUEST_FINE_LOCATION = 1234;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(() -> {
            try {
                int i = 0;
                while (true) {
                    Log.d("Locatorloop", "Location request: " + i);
                    request_location();
                    i++;
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }



    private void request_location() {
        final TextView textview = (TextView) findViewById(R.id.msg);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            String coordinates = "";

            if (location != null) {
                coordinates += String.format(location.getLatitude() + ";" + location.getLongitude());
            } else {
                coordinates += "Location fetch error";
            }

            textview.setText(coordinates);
            /**
             * send coordinates to server
             **/
            this.run_client_socket_thread(coordinates);

        });
    }


    private void  run_client_socket_thread(String coordinates) {
        new Thread(new Runnable(){
            String coordinates;
            public Runnable init(String coordinates) {
                this.coordinates = coordinates;
                return this;
            }
            public void run(){
                try {
                    Socket s = new Socket("192.168.1.61", 9999);

                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                    //BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                    //String resp = in.readLine();
                    //Log.d("Locator-server", resp);
                    out.println(this.coordinates);

                    //in.close();
                    out.close();
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.init(coordinates)).start();
    }


    @Override public void onLocationChanged(Location location) {
        String coordinates = String.format(location.getLatitude() + ";" + location.getLongitude());
        final TextView textview = (TextView) findViewById(R.id.msg);

        textview.setText(coordinates);

        this.run_client_socket_thread(coordinates);

        Log.d("LocatorCoordinates", coordinates);
        locationManager.removeUpdates(this);
    }


    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("gps", "Location permission granted");
                try {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    locationManager.requestLocationUpdates("gps", 0, 0, this);
                } catch (SecurityException ex) {
                    Log.d("gps", "Location permission did not work!");
                }
            }
        }
    }


    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }


}
