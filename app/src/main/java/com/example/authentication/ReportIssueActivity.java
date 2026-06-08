package com.example.authentication;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportIssueActivity extends AppCompatActivity implements SensorEventListener {

    private Spinner spinnerIssueType;
    private EditText editTextDescription;
    private Button btnSubmitReport, btnShareIssue;
    private TextView txtStatus;
    private FirebaseFirestore db;
    private String selectedIssueType = "";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Report Issue");
        }

        spinnerIssueType = findViewById(R.id.spinnerIssueType);
        editTextDescription = findViewById(R.id.editTextDescription);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        btnShareIssue = findViewById(R.id.btnShareIssue);
        txtStatus = findViewById(R.id.txtStatus);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize Sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        setupSpinner();
        setupSubmitButton();

        // Implicit Intent: Share issue details
        btnShareIssue.setOnClickListener(v -> {
            String desc = editTextDescription.getText().toString();
            if (desc.isEmpty()) desc = "I'm experiencing an issue with the water filter.";
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Water Issue Report: " + selectedIssueType);
            shareIntent.putExtra(Intent.EXTRA_TEXT, desc);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float acceleration = (float) Math.sqrt(x * x + y * y + z * z);
            long now = System.currentTimeMillis();

            if (acceleration > 15 && (now - lastShakeTime) > 2000) {
                lastShakeTime = now;
                editTextDescription.setText("");
                Toast.makeText(this, "Form cleared by shake!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.issue_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIssueType.setAdapter(adapter);

        spinnerIssueType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedIssueType = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedIssueType = "Other Issue";
            }
        });
    }

    private void setupSubmitButton() {
        btnSubmitReport.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        String description = editTextDescription.getText().toString().trim();

        if (description.isEmpty()) {
            editTextDescription.setError("Please describe the issue");
            editTextDescription.requestFocus();
            return;
        }

        // Disable button
        btnSubmitReport.setEnabled(false);
        btnSubmitReport.setText("Submitting...");
        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText("Submitting report...");

        // Create report data
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("issueType", selectedIssueType);
        reportData.put("description", description);
        reportData.put("userId", "User_" + System.currentTimeMillis());
        reportData.put("timestamp", timestamp);
        reportData.put("status", "Pending");
        reportData.put("createdAt", System.currentTimeMillis());

        // Save to Firestore under "reports" collection
        db.collection("reports")
                .add(reportData)
                .addOnSuccessListener(new OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.DocumentReference documentReference) {
                        txtStatus.setText("✓ Report submitted successfully!");
                        txtStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        editTextDescription.setText("");
                        spinnerIssueType.setSelection(0);
                        Toast.makeText(ReportIssueActivity.this, "Thank you for your report!", Toast.LENGTH_LONG).show();

                        btnSubmitReport.setEnabled(true);
                        btnSubmitReport.setText("Submit Report");

                        // Clear status after 3 seconds
                        txtStatus.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                txtStatus.setVisibility(View.GONE);
                            }
                        }, 3000);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        txtStatus.setText("✗ Failed: " + e.getMessage());
                        txtStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        Toast.makeText(ReportIssueActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

                        btnSubmitReport.setEnabled(true);
                        btnSubmitReport.setText("Submit Report");
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
