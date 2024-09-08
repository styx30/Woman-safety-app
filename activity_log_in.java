package com.example.app_login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class activity_log_in extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private FirebaseAuth firebaseAuth;
    FirebaseFirestore fstore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        fstore = FirebaseFirestore.getInstance();


        emailEditText = findViewById(R.id.Email);
        passwordEditText = findViewById(R.id.password_toggle);
        Button loginButton = findViewById(R.id.button_id);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (!email.isEmpty() && !password.isEmpty()) {
                    // Call the login method
                    login(email, password);
                } else {
                    //empty fields
                    Toast.makeText(activity_log_in.this, "All fields must be filled", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void login(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(activity_log_in.this, "Both email and password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(activity_log_in.this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Login success
                            // You can add further actions here, like navigating to another activity
                            Toast.makeText(activity_log_in.this, "Login successful! Hello there!", Toast.LENGTH_SHORT).show();
                            // For example, you can start a new activity
                            Intent intent = new Intent(activity_log_in.this, activity_home_page.class);
                            startActivity(intent);
                            finish(); // Optional: Close the current activity to prevent the user from coming back with the back button
                        } else {
                            // If login fails, display a message to the user.
                            // You can handle different exceptions like invalid email, wrong password, etc.
                            // For simplicity, we'll show a toast with the exception message
                            if (task.getException() != null) {
                                if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                                    // User not found (email not registered)
                                    Toast.makeText(activity_log_in.this, "User not found. Please sign up first.", Toast.LENGTH_SHORT).show();
                                } else if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                                    // Incorrect password
                                    Toast.makeText(activity_log_in.this, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Other login failure
                                    Toast.makeText(activity_log_in.this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                });
    }


    public void signup(View view) {
        Intent intent=new Intent(activity_log_in.this,MainActivity.class);
        startActivity(intent);
    }
}
