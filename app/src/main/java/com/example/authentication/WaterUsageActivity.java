package com.example.authentication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WaterUsageActivity extends AppCompatActivity {

    EditText etConsumption, etUsers, etHours;
    Button btnSave, btnUpdate;
    RecyclerView recyclerView;

    TextView txtDailyLiters, txtActiveUsers, txtUptimeHours;

    FirebaseFirestore db;
    ArrayList<WaterUsage> list;
    WaterUsageAdapter adapter;

    String selectedId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_usage);

        etConsumption = findViewById(R.id.etConsumption);
        etUsers = findViewById(R.id.etUsers);
        etHours = findViewById(R.id.etHours);
        btnSave = findViewById(R.id.btnSave);
        btnUpdate = findViewById(R.id.btnUpdate);
        recyclerView = findViewById(R.id.recyclerView);

        txtDailyLiters = findViewById(R.id.txtDailyLiters);
        txtActiveUsers = findViewById(R.id.txtActiveUsers);
        txtUptimeHours = findViewById(R.id.txtUptimeHours);


        // tambah go back button kat page ni
        ImageButton goback = findViewById(R.id.gobackbutton);
        goback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // ONLY THIS LINE CHANGED - Realtime DB to Firestore
        db = FirebaseFirestore.getInstance();

        list = new ArrayList<>();

        adapter = new WaterUsageAdapter(list, usage -> {
            selectedId = usage.id;
            etConsumption.setText(usage.consumption);
            etUsers.setText(usage.users);
            etHours.setText(usage.hours);
            updateTopCards(usage);
            Toast.makeText(this, "Record selected for update", Toast.LENGTH_SHORT).show();
        }, usage -> {
            // ONLY DELETE CHANGED - Firestore version
            db.collection("water_usage").document(usage.id).delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                            selectedId = "";
                            clearFields();
                            loadData();
                        } else {
                            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSave.setOnClickListener(v -> saveRecord());
        btnUpdate.setOnClickListener(v -> updateRecord());

        loadData();
    }

    private void saveRecord() {
        String consumption = etConsumption.getText().toString().trim();
        String users = etUsers.getText().toString().trim();
        String hours = etHours.getText().toString().trim();

        if (consumption.isEmpty() || users.isEmpty() || hours.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // ONLY ID GENERATION CHANGED - Firestore version
        String id = db.collection("water_usage").document().getId();

        Map<String, Object> usage = new HashMap<>();
        usage.put("id", id);
        usage.put("consumption", consumption);
        usage.put("users", users);
        usage.put("hours", hours);
        usage.put("timestamp", System.currentTimeMillis());

        // ONLY SAVE CHANGED - Firestore version
        db.collection("water_usage").document(id).set(usage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Record Saved", Toast.LENGTH_SHORT).show();
                        WaterUsage newUsage = new WaterUsage(id, consumption, users, hours);
                        updateTopCards(newUsage);
                        clearFields();
                        loadData();
                    } else {
                        Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateRecord() {
        String consumption = etConsumption.getText().toString().trim();
        String users = etUsers.getText().toString().trim();
        String hours = etHours.getText().toString().trim();

        if (selectedId.isEmpty()) {
            Toast.makeText(this, "Please select a record first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (consumption.isEmpty() || users.isEmpty() || hours.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // ONLY UPDATE CHANGED - Firestore version
        Map<String, Object> usage = new HashMap<>();
        usage.put("consumption", consumption);
        usage.put("users", users);
        usage.put("hours", hours);

        db.collection("water_usage").document(selectedId).update(usage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Record Updated", Toast.LENGTH_SHORT).show();
                        WaterUsage updatedUsage = new WaterUsage(selectedId, consumption, users, hours);
                        updateTopCards(updatedUsage);
                        selectedId = "";
                        clearFields();
                        loadData();
                    } else {
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadData() {
        // ONLY LOAD CHANGED - Firestore version
        db.collection("water_usage")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    list.clear();
                    WaterUsage latestUsage = null;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        WaterUsage usage = new WaterUsage();
                        usage.id = document.getString("id");
                        usage.consumption = document.getString("consumption");
                        usage.users = document.getString("users");
                        usage.hours = document.getString("hours");
                        list.add(usage);
                        latestUsage = usage;
                    }
                    if (latestUsage != null) {
                        updateTopCards(latestUsage);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Load failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTopCards(WaterUsage usage) {
        txtDailyLiters.setText(usage.consumption + " Liters");
        txtActiveUsers.setText("👥 Active User\n" + usage.users + " Pax");
        txtUptimeHours.setText("🕘 Uptime Hours\n" + usage.hours + " Hours");
    }

    private void clearFields() {
        etConsumption.setText("");
        etUsers.setText("");
        etHours.setText("");
    }
}