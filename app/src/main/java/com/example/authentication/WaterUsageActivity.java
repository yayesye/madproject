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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WaterUsageActivity extends AppCompatActivity {

    EditText etConsumption, etUsers, etHours;
    Button btnSave, btnUpdate;
    RecyclerView recyclerView;

    TextView txtDailyLiters, txtActiveUsers, txtUptimeHours, txtDateToday;
    android.widget.ProgressBar progressDaily, progressWeekly;

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
        txtDateToday = findViewById(R.id.txtDateToday);

        progressDaily = findViewById(R.id.progressDaily);
        progressWeekly = findViewById(R.id.progressWeekly);

        // Set today's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());
        if (txtDateToday != null) {
            txtDateToday.setText("Today : " + todayDate);
        }

        // Go back button
        ImageButton goback = findViewById(R.id.gobackbutton);
        goback.setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        list = new ArrayList<>();

        adapter = new WaterUsageAdapter(list, usage -> {
            selectedId = usage.id;
            etConsumption.setText(usage.consumption);
            etUsers.setText(usage.users);
            etHours.setText(usage.hours);
            Toast.makeText(this, "Record selected for update", Toast.LENGTH_SHORT).show();
        }, usage -> {
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

        String id = db.collection("water_usage").document().getId();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> usage = new HashMap<>();
        usage.put("id", id);
        usage.put("consumption", consumption);
        usage.put("users", users);
        usage.put("hours", hours);
        usage.put("timestamp", timestamp);

        db.collection("water_usage").document(id).set(usage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Record Saved", Toast.LENGTH_SHORT).show();
                        clearFields();
                        loadData();
                    } else {
                        Toast.makeText(this, "Save failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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

        Map<String, Object> usage = new HashMap<>();
        usage.put("consumption", consumption);
        usage.put("users", users);
        usage.put("hours", hours);

        db.collection("water_usage").document(selectedId).update(usage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Record Updated", Toast.LENGTH_SHORT).show();
                        selectedId = "";
                        clearFields();
                        loadData();
                    } else {
                        Toast.makeText(this, "Update failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadData() {
        // NO userId filter - shows ALL records
        db.collection("water_usage")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    list.clear();

                    int totalConsumption = 0;
                    int totalUsers = 0;
                    float totalHours = 0;
                    int recordCount = 0;

                    WaterUsage latestUsage = null;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        WaterUsage usage = new WaterUsage();
                        usage.id = document.getString("id");
                        usage.consumption = document.getString("consumption");
                        usage.users = document.getString("users");
                        usage.hours = document.getString("hours");
                        list.add(usage);

                        // Calculate totals for top cards
                        try {
                            totalConsumption += Integer.parseInt(usage.consumption);
                            totalUsers += Integer.parseInt(usage.users);
                            totalHours += Float.parseFloat(usage.hours);
                            recordCount++;
                        } catch (NumberFormatException e) {
                            // Skip if not a number
                        }

                        latestUsage = usage;
                    }

                    // Update top cards with latest OR calculated data
                    if (latestUsage != null) {
                        updateTopCards(latestUsage, totalConsumption, totalUsers, totalHours, recordCount);
                    } else {
                        // Default empty state
                        txtDailyLiters.setText("0 Liters");
                        txtActiveUsers.setText("👥 Active User\n0 Pax");
                        txtUptimeHours.setText("🕘 Uptime Hours\n0 Hours");
                        if (progressDaily != null) progressDaily.setProgress(0);
                        if (progressWeekly != null) progressWeekly.setProgress(0);
                    }

                    adapter.notifyDataSetChanged();

                    // Debug toast
                    Toast.makeText(this, "Loaded " + list.size() + " records", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTopCards(WaterUsage usage, int totalConsumption, int totalUsers, float totalHours, int recordCount) {
        // Daily - show latest record
        txtDailyLiters.setText(usage.consumption + " Liters");

        // Calculate daily progress (assuming 1000 Liters is max target)
        try {
            int consumptionInt = Integer.parseInt(usage.consumption);
            int dailyProgress = Math.min(consumptionInt * 100 / 1000, 100);
            if (progressDaily != null) progressDaily.setProgress(dailyProgress);
        } catch (NumberFormatException e) {
            if (progressDaily != null) progressDaily.setProgress(0);
        }

        // Active Users - show average or latest
        int avgUsers = recordCount > 0 ? totalUsers / recordCount : 0;
        txtActiveUsers.setText("👥 Active User\n" + avgUsers + " Pax");

        // Uptime Hours - show latest
        txtUptimeHours.setText("🕘 Uptime Hours\n" + usage.hours + " Hours");

        // Weekly progress - based on average consumption (target 5000 Liters per week)
        int weeklyTarget = 5000;
        int weeklyProgressValue = Math.min(totalConsumption * 100 / weeklyTarget, 100);
        if (progressWeekly != null) progressWeekly.setProgress(weeklyProgressValue);
    }

    private void clearFields() {
        etConsumption.setText("");
        etUsers.setText("");
        etHours.setText("");
    }
}