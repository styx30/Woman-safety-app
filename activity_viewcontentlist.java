package com.example.app_login;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class activity_viewcontentlist extends AppCompatActivity {


    private ListView listView;
    private activity_customlist listAdapter;
    private ArrayList<String> theList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewcontentlist);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore fstore = FirebaseFirestore.getInstance();

        listView = findViewById(R.id.listviewsafemembers);


        // Enable multiple choice mode for the ListView
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Get the current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // Retrieve user details from Firestore based on the user's ID
            fstore.collection("users")
                    .document(user.getUid())
                    .collection("members")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            theList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Assuming your Firestore document fields are "name" and "phone"
                                String name = document.getString("name");
                                String phone = document.getString("phone");

                                // Add details to the list
                                theList.add("Name: " + name + ", Phone: " + phone);
                            }

                            // Display the list in the ListView using the custom adapter
                            listAdapter = new activity_customlist(this, theList);
                            listView.setAdapter(listAdapter);

                        } else {
                            Toast.makeText(this, "Error getting user details", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }



        // Set up item click listener for ListView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Toggle the selection state of the item
                listView.setItemChecked(position, !listView.isItemChecked(position));
            }
        });
    }



    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Get the document ID based on the position in theList
    private String getDocumentId(int position) {
        // Ensure theList and Firestore order match
        if (position >= 0 && position < theList.size()) {
            String selectedItem = theList.get(position);
            // Extract information from the selected item to get the corresponding document ID
            return getDocumentIdFromSelectedItem(selectedItem);
        }

        return null; // Return null or handle out-of-bounds positions based on your requirement
    }

    // Extract the document ID from the selected item
    private String getDocumentIdFromSelectedItem(String selectedItem) {
        // Assuming the format is "Name: [name], Phone: [phone]"

        // Split the selected item to extract relevant information
        String[] parts = selectedItem.split(", ");

        // Extracting name and phone information
        String name = parts[0].substring(parts[0].indexOf(":") + 2);
        String phone = parts[1].substring(parts[1].indexOf(":") + 2);

        return name + "_" + phone;
    }
}
