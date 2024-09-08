package com.example.app_login;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class activity_safety_members extends AppCompatActivity {

    private static final int REQUEST_CALL = 1;
    private EditText editText, editText2;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore fstore;
    private String userID;
    private String contactId;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 123;
    private static final int LONG_PRESS_DURATION = 3000; // 3 seconds
    private boolean isVolumeDownPressed = false;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_members);

        editText = findViewById(R.id.editText);
        editText2 = findViewById(R.id.editText2);
        Button btnView = findViewById(R.id.btnView);
        Button button_contact = findViewById(R.id.Button_contact);
        Button btnAdd = findViewById(R.id.btnAdd);
        firebaseAuth = FirebaseAuth.getInstance();
        fstore = FirebaseFirestore.getInstance();

        ActivityResultLauncher<Intent> pickContactLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                handleSelectedContact(data);
                            }
                        }
                    }
                });

        button_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if the READ_CONTACTS permission is granted
                if (ContextCompat.checkSelfPermission(activity_safety_members.this, Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted, request it
                    ActivityCompat.requestPermissions(activity_safety_members.this, new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                } else {
                    // Permission has already been granted, proceed with contact picking
                    Intent pickContactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    pickContactLauncher.launch(pickContactIntent);
                }
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newEntry = editText.getText().toString();
                String newEntry1 = editText2.getText().toString();

                if (editText.length() != 0 && isValidPhoneNumber(newEntry1)) {
                    // Assuming you have a "members" collection under each user in Firestore
                    addDataToFirestore(newEntry, newEntry1);
                    editText.setText("");
                } else {
                    if (editText.length() == 0) {
                        Toast.makeText(activity_safety_members.this, "You must put something in the text field!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(activity_safety_members.this, "Please enter a valid phone number", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        btnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity_safety_members.this, activity_viewcontentlist.class);
                startActivity(intent);
            }
        });
    }


    private void handleSelectedContact(Intent data) {
        Uri contactUri = data.getData();
        String contactName = null;

        if (contactUri != null) {
            Cursor cursor = getContentResolver().query(
                    contactUri,
                    null,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int displayNameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                if (displayNameColumnIndex != -1) {
                    contactName = cursor.getString(displayNameColumnIndex);
                }

                // Close the cursor
                cursor.close();

                // Retrieve the contact phone number using the lookup URI
                String contactPhone = getContactPhoneNumber(contactUri);

                if (contactName != null && contactPhone != null) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        DocumentReference documentReference = fstore.collection("users")
                                .document(user.getUid())
                                .collection("members")
                                .document();

                        Map<String, Object> member = new HashMap<>();
                        member.put("name", contactName);
                        member.put("phone", contactPhone);

                        documentReference.set(member)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(activity_safety_members.this, "Contact Successfully Added!", Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(activity_safety_members.this, "Failed to add contact to Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        Toast.makeText(activity_safety_members.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity_safety_members.this, "Failed to retrieve contact information", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private String getContactPhoneNumber(Uri contactUri) {
        String phoneNumber = null;

        try {
            // Get the contact ID from the contact URI
            String[] projection = {ContactsContract.Contacts._ID};
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                if (contactIdIndex != -1) {
                    contactId = cursor.getString(contactIdIndex);
                }
                cursor.close();
            }

            // Query all phone numbers associated with the contact ID
            cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{contactId},
                    null
            );

            // Retrieve the first phone number if available
            if (cursor != null && cursor.moveToFirst()) {
                int phoneNumberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (phoneNumberIndex != -1) {
                    phoneNumber = cursor.getString(phoneNumberIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (phoneNumber == null) {
            Toast.makeText(this, "Failed to retrieve contact phone number", Toast.LENGTH_LONG).show();
        }

        return phoneNumber;
    }


    private void addDataToFirestore(String name, String phone) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
            DocumentReference documentReference = fstore.collection("users")
                    .document(user.getUid())
                    .collection("members")  // Change the collection name to "members"
                    .document();
            Map<String, Object> member = new HashMap<>();
            member.put("name", name);
            member.put("phone", phone);

            documentReference.set(member)
                    .addOnSuccessListener(aVoid -> Toast.makeText(activity_safety_members.this, "Data Successfully Inserted!", Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Toast.makeText(activity_safety_members.this, "Something went wrong :(", Toast.LENGTH_LONG).show());
        } else {
            Toast.makeText(activity_safety_members.this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return android.util.Patterns.PHONE.matcher(phoneNumber).matches() && phoneNumber.length() >= 10;
    }


   //  The onKeyDown method can be used to handle volume button presses
   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event) {
       if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
           isVolumeDownPressed = true;
           // Start a handler to check for long press
           new Handler().postDelayed(() -> {
               if (isVolumeDownPressed) {
                   // Long press detected, call the first contact in Firestore
                   callEmergencyNumber();
               }
               isVolumeDownPressed = false;
           }, LONG_PRESS_DURATION);

           return true; // Consume the event to prevent further propagation
       }

       return super.onKeyDown(keyCode, event);
   }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // Volume down button released, cancel the long press handler
            isVolumeDownPressed = false;
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private void callEmergencyNumber() {
        // Fetch emergency number from Firestore or activity_viewcontentlist
        // Replace "yourFirestoreCollection" with your actual Firestore collection name
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(user.getUid())
                    .collection("members")
                    .orderBy("phone") // You may need to specify an order based on your data
                    .limit(1)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String emergencyNumber = document.getString("phone"); // Replace "phone" with your actual field name

                            if (emergencyNumber != null && !emergencyNumber.isEmpty()) {
                                // Call the emergency number
                                Intent callIntent = new Intent(Intent.ACTION_CALL);
                                callIntent.setData(Uri.parse("tel:" + emergencyNumber));

                                if (ActivityCompat.checkSelfPermission(activity_safety_members.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(activity_safety_members.this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
                                } else {
                                    startActivity(callIntent);
                                }
                            }
                        }
                    });
        } else {
            Toast.makeText(activity_safety_members.this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

}
