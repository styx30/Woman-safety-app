package com.example.app_login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText, emailEditText, confirmPasswordEditText;
    private FirebaseAuth firebaseAuth;
    Button button1;
    FirebaseFirestore fstore;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button1 = findViewById(R.id.button_id);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        fstore = FirebaseFirestore.getInstance();
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password_toggle);
        emailEditText = findViewById(R.id.Email);
        confirmPasswordEditText = findViewById(R.id.password_toggle1);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                if (!username.isEmpty() && !email.isEmpty() && !password.isEmpty() && !confirmPassword.isEmpty()) {
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailEditText.setError("Please enter a valid email address");
                        return;
                    }

                    if (password.equals(confirmPassword)) {
                        if (password.length() < 6) {
                            passwordEditText.setError("Password should be at least 6 characters");
                            return;
                        }

                        // Call the signUp method
                        signUp(email, password);
                    } else {
                        // Passwords do not match
                        Toast.makeText(MainActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Handle empty fields
                    // You can show a toast or set an error on the EditText fields
                    Toast.makeText(MainActivity.this, "All fields must be filled", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void signUp(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    userID = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
                    DocumentReference documentReference = fstore.collection("users").document(userID);
                    Map<String, Object> user = new HashMap<>();
                    user.put("Name", usernameEditText.getText().toString());
                    user.put("Email_ID", emailEditText.getText().toString());

                    documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "onSuccess: user profile is created for " + userID);
                        }
                    });

                    // Sign up success
                    // You can add further actions here, like navigating to another activity

                    // Create an Intent to go to the activity_log_in
                    Intent intent = new Intent(MainActivity.this, activity_log_in.class);
                    startActivity(intent);
                    finish(); // Optional: Close the current activity to prevent the user from coming back with the back button

                    Toast.makeText(MainActivity.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                } else {
                    // If sign in fails, display a message to the user.
                    // You can handle different exceptions like weak password, email already in use, etc.
                    // For simplicity, we'll show a toast with the exception message

                    try {
                        throw Objects.requireNonNull(task.getException());
                    } catch (FirebaseAuthUserCollisionException e) {
                        // If the email is already in use
                        Toast.makeText(MainActivity.this, "Email already in use. Please use a different email.", Toast.LENGTH_SHORT).show();
                    } catch (FirebaseAuthWeakPasswordException e) {
                        // If the password is too weak
                        Toast.makeText(MainActivity.this, "Weak password. Please use a stronger password.", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        // If sign up fails for some other reason
                        Toast.makeText(MainActivity.this, "Authentication failed: Please try again", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    public void login(View view) {
        Intent intent = new Intent(MainActivity.this, activity_log_in.class);
        startActivity(intent);
    }
}
