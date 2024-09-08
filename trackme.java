package com.example.app_login;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

public class trackme extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private Marker selectedLocationMarker;

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trackme);

        Button btnLocation = findViewById(R.id.btnLocation);
        Button btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAndRequestSmsPermission();
            }
        });
        btnCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                markCurrentLocation();
            }
        });

        createLocationCallback();
        startLocationUpdates();
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // You can customize the map settings here

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Clear previous marker
                if (selectedLocationMarker != null) {
                    selectedLocationMarker.remove();
                }

                // Add a new marker at the clicked location
                selectedLocationMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Selected Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                // Fetch latitude and longitude
                double latitude = latLng.latitude;
                double longitude = latLng.longitude;

                Toast.makeText(trackme.this, "Selected Location: Lat " + latitude + ", Lng " + longitude, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY).build();
        locationRequest.setInterval(5000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null /* Looper */
        );
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocationUI(location);
                }
            }
        };
    }


    private void updateLocationUI(Location location) {
        if (location != null && mMap != null) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

            // Clear existing markers on the map
            mMap.clear();

            // Customize marker appearance
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(currentLocation)
                    .title("My Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

            // Add the marker to the map
            mMap.addMarker(markerOptions);

            // Move camera to the current location
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        }
    }


    private void checkAndRequestSmsPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, proceed to send SMS
            sendLiveLocation();
        } else {
            // Request the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to send SMS
                sendLiveLocation();
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied. Cannot send SMS.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendLiveLocation() {
        LatLng currentLocation = mMap.getCameraPosition().target; // Get the current camera position

        double latitude = currentLocation.latitude;
        double longitude = currentLocation.longitude;

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            firestore.collection("users")
                    .document(user.getUid())
                    .collection("members")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                String phoneNumber = document.getString("phone");
                                sendSmsWithLocation(phoneNumber, latitude, longitude);
                            }
                        } else {
                            Toast.makeText(trackme.this, "Error getting member details", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(trackme.this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSmsWithLocation(String phoneNumber, double latitude, double longitude) {
        String smsBody = "Check my live location: " +
                "http://maps.google.com/maps?q=" + latitude + "," + longitude;

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, smsBody, null, null);
            Toast.makeText(trackme.this, "Live location sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(trackme.this, "Failed to send SMS. Check permissions and try again.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    private void markCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                            // Clear existing markers on the map
                            mMap.clear();

                            // Customize marker appearance
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(currentLocation)
                                    .title("My Current Location")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                            // Add the marker to the map
                            mMap.addMarker(markerOptions);

                            // Move camera to the current location
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
                        } else {
                            Toast.makeText(trackme.this, "Location not available", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
