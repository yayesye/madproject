package com.example.authentication;

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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportIssueActivity extends AppCompatActivity {

    private Spinner spinnerIssueType;
    private EditText editTextDescription;
    private Button btnSubmitReport;
    private TextView txtStatus;
    private FirebaseFirestore db;
    private String selectedIssueType = "";

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
        txtStatus = findViewById(R.id.txtStatus);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        setupSpinner();
        setupSubmitButton();
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