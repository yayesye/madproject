package com.example.authentication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    // RecyclerViews
    private RecyclerView rvStations, rvTasks, rvUserReports;
    private StationAdapter stationAdapter;
    private TaskAdapter taskAdapter;
    private UserReportAdapter userReportAdapter;

    // Data lists
    private ArrayList<StationModel> stationList;
    private ArrayList<TaskModel> taskList;
    private ArrayList<Report> userReportList;

    // Input fields
    private EditText etStation, etPhone, etDate;
    private EditText etAlertTitle, etAlertMessage;
    private Button btnAddTask, btnSendAlert;
    private TextView tvTotal, tvAlerts, tvScheduledCount, tvPendingReports;

    // Firebase
    private FirebaseFirestore db;
    private CollectionReference stationsRef;
    private CollectionReference tasksRef;
    private CollectionReference reportsRef;
    private CollectionReference notificationsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        stationsRef = db.collection("water_stations");
        tasksRef = db.collection("maintenance_tasks");
        reportsRef = db.collection("reports");
        notificationsRef = db.collection("notifications");

        // Initialize UI
        initViews();

        // Setup RecyclerViews
        setupRecyclerViews();

        // Setup date picker
        setupDatePicker();

        // Setup realtime data listeners
        setupRealtimeData();

        // Setup button listeners
        setupButtonListeners();

        // Load user reports
        loadUserReports();

        seedStations();
    }
    private void seedStations() {
        CollectionReference stationsRef = db.collection("water_stations");

        StationModel station1 = new StationModel("station_001", "Kampung A Filtration", 2.1, 7.2, 26.5, 5.0, 6.5);
        StationModel station2 = new StationModel("station_002", "Kampung B Treatment", 3.5, 6.8, 27.0, 5.0, 6.5);
        StationModel station3 = new StationModel("station_003", "Kampung C Reservoir", 1.8, 7.5, 25.5, 5.0, 6.5);

        stationsRef.document("station_001").set(station1);
        stationsRef.document("station_002").set(station2);
        stationsRef.document("station_003").set(station3);

        Toast.makeText(this, "Seeding stations...", Toast.LENGTH_SHORT).show();
    }
    private void initViews() {
        rvStations = findViewById(R.id.rvFiltrationNetwork);
        rvTasks = findViewById(R.id.rvScheduledTasksList);
        rvUserReports = findViewById(R.id.rvUserReports);

        etStation = findViewById(R.id.etTargetStation);
        etPhone = findViewById(R.id.etSpecialistPhone);
        etDate = findViewById(R.id.etOperationDate);
        etAlertTitle = findViewById(R.id.etAlertTitle);
        etAlertMessage = findViewById(R.id.etAlertMessage);

        btnAddTask = findViewById(R.id.btnAddTask);
        btnSendAlert = findViewById(R.id.btnSendAlert);

        tvTotal = findViewById(R.id.tvTotalFilters);
        tvAlerts = findViewById(R.id.tvLiveAlerts);
        tvScheduledCount = findViewById(R.id.tvScheduled);
        tvPendingReports = findViewById(R.id.tvPendingReports);
    }

    private void setupRecyclerViews() {
        stationList = new ArrayList<>();
        rvStations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        stationAdapter = new StationAdapter(this, stationList);
        rvStations.setAdapter(stationAdapter);

        taskList = new ArrayList<>();
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, taskList);
        rvTasks.setAdapter(taskAdapter);

        userReportList = new ArrayList<>();
        rvUserReports.setLayoutManager(new LinearLayoutManager(this));
        userReportAdapter = new UserReportAdapter(userReportList);
        rvUserReports.setAdapter(userReportAdapter);
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                    etDate.setText(String.format(Locale.getDefault(), "%d/%d/%d", dayOfMonth, month + 1, year)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupRealtimeData() {
        // Load stations
        stationsRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            stationList.clear();
            int alertCount = 0;
            for (DocumentSnapshot doc : value.getDocuments()) {
                StationModel model = doc.toObject(StationModel.class);
                if (model != null) {
                    stationList.add(model);
                    if (model.getTurbidity() > model.getMaxTurbidityLimit() ||
                            model.getPh() < model.getMinPhLimit()) {
                        alertCount++;
                    }
                }
            }
            tvTotal.setText(String.valueOf(stationList.size()));
            tvAlerts.setText(String.valueOf(alertCount));
            stationAdapter.notifyDataSetChanged();
        });

        // Load tasks
        tasksRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            taskList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                TaskModel task = doc.toObject(TaskModel.class);
                if (task != null) {
                    task.setTaskId(doc.getId());
                    taskList.add(task);
                }
            }
            tvScheduledCount.setText(String.valueOf(taskList.size()));
            taskAdapter.notifyDataSetChanged();
        });
    }

    private void loadUserReports() {
        reportsRef.whereEqualTo("status", "Pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    userReportList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Report report = doc.toObject(Report.class);
                        if (report != null) {
                            report.setId(doc.getId());
                            userReportList.add(report);
                        }
                    }
                    tvPendingReports.setText(String.valueOf(userReportList.size()));
                    userReportAdapter.notifyDataSetChanged();
                });
    }

    private void setupButtonListeners() {
        // CREATE maintenance task
        btnAddTask.setOnClickListener(v -> {
            String station = etStation.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String date = etDate.getText().toString().trim();

            if (TextUtils.isEmpty(station) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(date)) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> task = new HashMap<>();
            task.put("targetStation", station);
            task.put("specialistPhone", phone);
            task.put("date", date);
            task.put("status", "Pending");
            task.put("createdAt", System.currentTimeMillis());

            tasksRef.add(task)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(this, "Task created successfully!", Toast.LENGTH_SHORT).show();
                        etStation.setText("");
                        etPhone.setText("");
                        etDate.setText("");
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // CREATE alert notification for users
        btnSendAlert.setOnClickListener(v -> {
            String title = etAlertTitle.getText().toString().trim();
            String message = etAlertMessage.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(message)) {
                Toast.makeText(this, "Please enter title and message", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("type", "alert");

            notificationsRef.add(notification)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(this, "Alert sent to all users!", Toast.LENGTH_SHORT).show();
                        etAlertTitle.setText("");
                        etAlertMessage.setText("");
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // Reports - Two buttons
        Button btnViewReport = findViewById(R.id.btnViewReport);
        Button btnExportReport = findViewById(R.id.btnExportReport);

        btnViewReport.setOnClickListener(v -> showScheduledTasksReport());
        btnExportReport.setOnClickListener(v -> exportReportViaEmail());

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Bottom navigation
        ImageButton navHome = findViewById(R.id.adminNavHome);
        ImageButton navSettings = findViewById(R.id.adminNavSettings);

        navHome.setOnClickListener(v -> Toast.makeText(this, "Already on Dashboard", Toast.LENGTH_SHORT).show());
        navSettings.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void showScheduledTasksReport() {
        if (taskList.isEmpty()) {
            Toast.makeText(this, "No scheduled tasks available", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append("═══════════════════════════════════\n");
        report.append("      SCHEDULED TASKS REPORT\n");
        report.append("═══════════════════════════════════\n");
        report.append("Date: ").append(new Date()).append("\n");
        report.append("Total Tasks: ").append(taskList.size()).append("\n\n");

        int taskNumber = 1;
        for (TaskModel task : taskList) {
            report.append(taskNumber++).append(". ").append(task.getTargetStation()).append("\n");
            report.append("   📞 Specialist: ").append(task.getSpecialistPhone()).append("\n");
            report.append("   📅 Date: ").append(task.getDate()).append("\n");
            report.append("   📌 Status: ").append(task.getStatus()).append("\n\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Scheduled Tasks Report")
                .setMessage(report.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void exportReportViaEmail() {
        if (taskList.isEmpty()) {
            Toast.makeText(this, "No tasks to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append("WATER FILTER SYSTEM - MAINTENANCE REPORT\n");
        report.append("=========================================\n");
        report.append("Generated: ").append(new Date()).append("\n\n");
        report.append("Total Scheduled Tasks: ").append(taskList.size()).append("\n\n");
        report.append("TASK DETAILS:\n");
        report.append("-------------\n");

        int taskNumber = 1;
        for (TaskModel task : taskList) {
            report.append(taskNumber++).append(". ").append(task.getTargetStation()).append("\n");
            report.append("   Specialist: ").append(task.getSpecialistPhone()).append("\n");
            report.append("   Date: ").append(task.getDate()).append("\n");
            report.append("   Status: ").append(task.getStatus()).append("\n\n");
        }

        report.append("\n--- End of Report ---\n");

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Water Filter System - Maintenance Report");
        emailIntent.putExtra(Intent.EXTRA_TEXT, report.toString());
        startActivity(Intent.createChooser(emailIntent, "Send Report"));
    }

    // ==================== INNER ADAPTER FOR USER REPORTS ====================

    class UserReportAdapter extends RecyclerView.Adapter<UserReportAdapter.ViewHolder> {
        private ArrayList<Report> reports;

        UserReportAdapter(ArrayList<Report> reports) {
            this.reports = reports;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Report report = reports.get(position);
            holder.tvIssueType.setText("📋 " + report.getIssueType());
            holder.tvDescription.setText(report.getDescription());
            holder.tvUserInfo.setText("👤 " + report.getUserId() + " | 📅 " + report.getTimestamp());
            holder.tvStatus.setText(report.getStatus());

            if ("Pending".equals(report.getStatus())) {
                holder.tvStatus.setBackgroundColor(0xFFFF9800);
            } else {
                holder.tvStatus.setBackgroundColor(0xFF4CAF50);
            }

            // DELETE report
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(AdminDashboardActivity.this)
                        .setTitle("Delete Report")
                        .setMessage("Delete report from " + report.getUserId() + "?")
                        .setPositiveButton("DELETE", (dialog, which) -> {
                            reportsRef.document(report.getId()).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(AdminDashboardActivity.this, "Report deleted", Toast.LENGTH_SHORT).show();
                                        sendNotificationToUser("Report Resolved",
                                                "Your report about '" + report.getIssueType() + "' has been reviewed.");
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(AdminDashboardActivity.this, "Delete failed", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            // RESOLVE report
            holder.btnResolve.setOnClickListener(v -> {
                reportsRef.document(report.getId()).update("status", "Resolved")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AdminDashboardActivity.this, "Marked as resolved", Toast.LENGTH_SHORT).show();
                            sendNotificationToUser("Report Resolved",
                                    "Your report about '" + report.getIssueType() + "' has been resolved.");
                        });
            });
        }

        private void sendNotificationToUser(String title, String message) {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("type", "notification");
            notificationsRef.add(notification);
        }

        @Override
        public int getItemCount() {
            return reports.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIssueType, tvDescription, tvUserInfo, tvStatus;
            Button btnDelete, btnResolve;

            ViewHolder(View v) {
                super(v);
                tvIssueType = v.findViewById(R.id.tvIssueType);
                tvDescription = v.findViewById(R.id.tvDescription);
                tvUserInfo = v.findViewById(R.id.tvUserInfo);
                tvStatus = v.findViewById(R.id.tvStatus);
                btnDelete = v.findViewById(R.id.btnDeleteReport);
                btnResolve = v.findViewById(R.id.btnResolveReport);
            }
        }
    }
}