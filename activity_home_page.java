package com.example.app_login;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class activity_home_page extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Initialize UI components
        ImageView imageView1 = findViewById(R.id.myprofile);
        ImageView imageView2 = findViewById(R.id.helpline_numbers);
        ImageView imageView3 = findViewById(R.id.trackme);
        ImageView imageView4 = findViewById(R.id.safety_members);
        Button panicButton = findViewById(R.id.panic);

        // Set click listeners for UI components
        imageView1.setOnClickListener(view -> openProfileActivity());
        imageView2.setOnClickListener(view -> openHelplineNumbersActivity());
        imageView3.setOnClickListener(view -> openTrackMeActivity());
        imageView4.setOnClickListener(view -> openSafetyMembersActivity());

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        panicButton.setOnClickListener(view -> sendEmergencySms());

    }

    private void openProfileActivity() {
        startActivity(new Intent(this, activity_my_profile.class));
    }

    private void openHelplineNumbersActivity() {
        startActivity(new Intent(this, activity_helpline_numbers.class));
    }

    private void openTrackMeActivity() {
        startActivity(new Intent(this, trackme.class));
    }

    private void openSafetyMembersActivity() {
        startActivity(new Intent(this, activity_safety_members.class));
    }

    private void sendEmergencySms() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            String userId = user.getUid();

            firestore.collection("users")
                    .document(userId)
                    .collection("members")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String name = document.getString("name");
                                String phoneNumber = document.getString("phone");
                                fetchAndSendSmsWithLocation(name, phoneNumber);
                            }
                        }
                    });
        }
    }

    private void fetchAndSendSmsWithLocation(String name, String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            sendSmsWithLocationAndSOS(name, phoneNumber, location);
                        }
                    });
        } else {
            // Handle the case where permissions are not granted
            // You may want to request permissions here
        }
    }

    private void sendSmsWithLocationAndSOS(String name, String phoneNumber, Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        String sosMessage = "Emergency! " + name + ", contact me ASAP! Here's My live location: " +
                "http://maps.google.com/maps?q=" + latitude + "," + longitude;

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, sosMessage, null, null);

        showToast("SOS signal sent to " + name);
    }

    private void showToast(String message) {
        Toast.makeText(activity_home_page.this, message, Toast.LENGTH_SHORT).show();
    }
}
