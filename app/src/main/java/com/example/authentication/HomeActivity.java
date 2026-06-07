package com.example.authentication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    FirebaseFirestore firebasedb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // firebase
        firebasedb = FirebaseFirestore.getInstance();

        // testing
        getDBdata();
        setUser();
        randomButton();

        // homepage nav
        waterqualitymore();
        waterusagemore();

        //implicit intent
        gotowebsite();

        //bottom nav
        bottomnav();
    }

    // Implicit intent - Open website
    TextView goweb;
    private void gotowebsite() {
        goweb = findViewById(R.id.firstTime);
        goweb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gowebsite = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yayesye"));
                startActivity(gowebsite);
            }
        });
    }

    // Bottom navigation
    private void bottomnav() {
        ImageButton nav1, nav2, nav3;

        nav1 = findViewById(R.id.nav1);
        nav2 = findViewById(R.id.nav2);
        nav3 = findViewById(R.id.nav3);

        // Home button
        nav1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Already on home
                Toast.makeText(HomeActivity.this, "Already on Home", Toast.LENGTH_SHORT).show();
            }
        });

        // Help/Questions button
        nav2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Help section coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        // Settings button - Link to Damia's Settings
        nav3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void getDBdata() {
        firebasedb.collection("auth")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("HOMEACTIVITY", document.getId() + "=>" + document.getData());
                            }
                        } else {
                            Log.w("HOMEACTIVITY", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public TextView setusername;
    private void setUser() {
        setusername = findViewById(R.id.UserName);
        // Get username from intent or Firebase
        String username = getIntent().getStringExtra("USERNAME");
        if (username != null && !username.isEmpty()) {
            setusername.setText(username);
        } else {
            setusername.setText("User");
        }
    }

    // Test button to insert data
    private void insertDBdata() {
        Map<String, Object> user = new HashMap<>();
        user.put("username", "emir");
        user.put("age", "18");

        firebasedb.collection("auth")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("FirestoreError", "Success! ID: " + documentReference.getId());
                        Toast.makeText(HomeActivity.this, "Data Inserted!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("FirestoreError", "Error writing document", e);
                        Toast.makeText(HomeActivity.this, "Failed to Insert Data!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void randomButton() {
        Button random = findViewById(R.id.testbutton);
        random.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertDBdata();
            }
        });
    }




    ImageButton waterqualitymore, waterusagemore;
    private void waterusagemore() {
        waterusagemore = findViewById(R.id.btnWaterUsageMore);
        waterusagemore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gotowaterusage = new Intent(HomeActivity.this, WaterUsageActivity.class);
                startActivity(gotowaterusage);
            }
        });
    }
    private void waterqualitymore() {
        waterqualitymore = findViewById(R.id.btnWaterQualityMore);
        waterqualitymore.setOnClickListener(view -> {
            Intent gotowaterquality = new Intent(HomeActivity.this, WaterQuality.class);
            startActivity(gotowaterquality);
        });
    }
}