package com.example.app_login;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class activity_my_profile extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private TextView textViewUsername, textViewEmail, textViewUsername1;
    private ImageView profileImageView;
    private Uri imageUri; // To store the selected image URI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        // Initialize TextView and ImageView widgets
        textViewUsername = findViewById(R.id.id_username);
        textViewUsername1 = findViewById(R.id.id_username1);
        textViewEmail = findViewById(R.id.id_email);
        profileImageView = findViewById(R.id.id_profile_pic);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // If not authenticated, redirect to the login screen
            startActivity(new Intent(getApplicationContext(), activity_log_in.class));
            finish();
        } else {
            // Fetch user data from Firestore and update TextView widgets
            fetchUserData();
        }
    }
        // Fetch user data from Firestore and update TextView widgets


    private void fetchUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        if (user != null) {
            String userId = user.getUid();

            firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null) {
                                // Fetch the username and email from Firestore
                                String username = document.getString("Name");
                                String email = document.getString("Email_ID");

                                // Set the fetched data to the TextView widgets
                                textViewUsername.setText(username);
                                textViewUsername1.setText(username);
                                textViewEmail.setText(email);
                            }
                        }
                    });
        }
    }

    // Method to handle profile picture selection
    public void selectProfilePicture(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    // Method to handle the result of the profile picture selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                profileImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logout(View view) {
        FirebaseAuth.getInstance().signOut(); // Sign out
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();

    }
}
