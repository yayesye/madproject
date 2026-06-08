package com.example.authentication;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportIssueActivity extends AppCompatActivity implements SensorEventListener {

    private Spinner spinnerIssueType;
    private EditText editTextDescription;
    private Button btnSubmitReport, btnShareIssue;
    private TextView txtStatus;

    private RecyclerView rvMyReports;
    private MyReportAdapter reportAdapter;
    private ArrayList<Report> myReportsList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String selectedIssueType = "";
    private String currentUserId;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Report Issue");
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "unknown";

        spinnerIssueType = findViewById(R.id.spinnerIssueType);
        editTextDescription = findViewById(R.id.editTextDescription);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        btnShareIssue = findViewById(R.id.btnShareIssue);
        txtStatus = findViewById(R.id.txtStatus);

        rvMyReports = findViewById(R.id.rvMyReports);
        rvMyReports.setLayoutManager(new LinearLayoutManager(this));
        myReportsList = new ArrayList<>();
        reportAdapter = new MyReportAdapter(myReportsList);
        rvMyReports.setAdapter(reportAdapter);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        setupSpinner();
        setupSubmitButton();
        loadMyReports();

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

        btnSubmitReport.setEnabled(false);
        btnSubmitReport.setText("Submitting...");
        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText("Submitting report...");

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("issueType", selectedIssueType);
        reportData.put("description", description);
        reportData.put("userId", currentUserId);
        reportData.put("timestamp", timestamp);
        reportData.put("status", "Pending");
        reportData.put("createdAt", System.currentTimeMillis());

        db.collection("reports")
                .add(reportData)
                .addOnSuccessListener(documentReference -> {
                    txtStatus.setText("✓ Report submitted successfully!");
                    txtStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    editTextDescription.setText("");
                    spinnerIssueType.setSelection(0);
                    Toast.makeText(ReportIssueActivity.this, "Thank you for your report!", Toast.LENGTH_LONG).show();

                    btnSubmitReport.setEnabled(true);
                    btnSubmitReport.setText("Submit Report");

                    txtStatus.postDelayed(() -> txtStatus.setVisibility(View.GONE), 3000);
                })
                .addOnFailureListener(e -> {
                    txtStatus.setText("✗ Failed: " + e.getMessage());
                    txtStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    Toast.makeText(ReportIssueActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSubmitReport.setEnabled(true);
                    btnSubmitReport.setText("Submit Report");
                });
    }

    private void loadMyReports() {
        db.collection("reports")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    myReportsList.clear();
                    for (var doc : value.getDocuments()) {
                        Report report = doc.toObject(Report.class);
                        if (report != null) {
                            report.setId(doc.getId());
                            myReportsList.add(report);
                        }
                    }
                    reportAdapter.notifyDataSetChanged();
                });
    }

    private void updateReport(Report report) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Report");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etDesc = new EditText(this);
        etDesc.setText(report.getDescription());
        layout.addView(etDesc);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newDesc = etDesc.getText().toString().trim();
            if (!newDesc.isEmpty()) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("description", newDesc);
                db.collection("reports").document(report.getId()).update(updates)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Report updated!", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteReport(String id) {
        db.collection("reports").document(id).delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Report deleted!", Toast.LENGTH_SHORT).show());
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Adapter for user's own reports
    class MyReportAdapter extends RecyclerView.Adapter<MyReportAdapter.ViewHolder> {
        private ArrayList<Report> reports;

        MyReportAdapter(ArrayList<Report> reports) {
            this.reports = reports;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Report report = reports.get(position);
            holder.tvIssue.setText(report.getIssueType());
            holder.tvDesc.setText(report.getDescription());
            holder.tvDate.setText(report.getTimestamp());

            String status = report.getStatus();
            holder.tvStatus.setText(status);
            if ("Pending".equals(status)) {
                holder.tvStatus.setBackgroundColor(0xFFFF9800);
            } else {
                holder.tvStatus.setBackgroundColor(0xFF4CAF50);
            }

            holder.btnEdit.setOnClickListener(v -> updateReport(report));
            holder.btnDelete.setOnClickListener(v -> deleteReport(report.getId()));
        }

        @Override
        public int getItemCount() { return reports.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIssue, tvDesc, tvDate, tvStatus;
            Button btnEdit, btnDelete;
            ViewHolder(View v) {
                super(v);
                tvIssue = v.findViewById(R.id.tvMyIssueType);
                tvDesc = v.findViewById(R.id.tvMyDescription);
                tvDate = v.findViewById(R.id.tvMyDate);
                tvStatus = v.findViewById(R.id.tvMyStatus);
                btnEdit = v.findViewById(R.id.btnEditMyReport);
                btnDelete = v.findViewById(R.id.btnDeleteMyReport);
            }
        }
    }
}