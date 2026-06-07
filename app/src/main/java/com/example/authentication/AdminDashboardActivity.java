package com.example.authentication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity implements SensorEventListener {

    private RecyclerView rvStations;
    private StationAdapter adapter;
    private ArrayList<StationModel> stationList;

    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private ArrayList<TaskModel> taskList;

    private EditText etStation, etPhone, etDate;
    private Button btnAddTask, btnLogout;
    private TextView tvTotal, tvAlerts, tvScheduledCount;

    private Button btnWaterQualityReport, btnMaintenanceReport, btnExportReport;
    private Button btnAlertSpecialist;

    private SensorManager sensorManager;
    private Sensor proximitySensor, lightSensor;
    private TextView tvBatteryStatus, tvProximityStatus, tvLightStatus, tvSensorMessage;

    private TextView tvFilterLifespan, tvMaintenanceAlert;
    private ProgressBar progressFilterLife;
    private int filterLifePercent = 100;
    private Handler filterHandler = new Handler();
    private Runnable filterRunnable;

    private FirebaseFirestore db;
    private CollectionReference stationsRef;
    private CollectionReference tasksRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        stationsRef = db.collection("water_stations");
        tasksRef = db.collection("maintenance_tasks");
        mAuth = FirebaseAuth.getInstance();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        rvStations = findViewById(R.id.rvFiltrationNetwork);
        rvTasks = findViewById(R.id.rvScheduledTasksList);
        etStation = findViewById(R.id.etTargetStation);
        etPhone = findViewById(R.id.etSpecialistPhone);
        etDate = findViewById(R.id.etOperationDate);
        btnAddTask = findViewById(R.id.btnAddTask);
        btnLogout = findViewById(R.id.btnLogout);
        tvTotal = findViewById(R.id.tvTotalFilters);
        tvAlerts = findViewById(R.id.tvLiveAlerts);
        tvScheduledCount = findViewById(R.id.tvScheduled);

        btnWaterQualityReport = findViewById(R.id.btnWaterQualityReport);
        btnMaintenanceReport = findViewById(R.id.btnMaintenanceReport);
        btnExportReport = findViewById(R.id.btnExportReport);
        btnAlertSpecialist = findViewById(R.id.btnAlertSpecialist);

        tvBatteryStatus = findViewById(R.id.tvBatteryStatus);
        tvProximityStatus = findViewById(R.id.tvProximityStatus);
        tvLightStatus = findViewById(R.id.tvLightStatus);
        tvSensorMessage = findViewById(R.id.tvSensorMessage);

        tvFilterLifespan = findViewById(R.id.tvFilterLifespan);
        tvMaintenanceAlert = findViewById(R.id.tvMaintenanceAlert);
        progressFilterLife = findViewById(R.id.progressFilterLife);

        stationList = new ArrayList<>();
        rvStations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new StationAdapter(this, stationList);
        rvStations.setAdapter(adapter);

        taskList = new ArrayList<>();
        rvTasks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        taskAdapter = new TaskAdapter(this, taskList);
        rvTasks.setAdapter(taskAdapter);
        rvTasks.setNestedScrollingEnabled(false);

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                    etDate.setText(String.format(Locale.getDefault(), "%d/%d/%d", dayOfMonth, month + 1, year)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        setupRealtimeDashboardMetrics();
        setupDatabaseInsertOperation();
        setupBatteryMonitoring();
        setupFilterLifespanSimulation();
        setupReportButtons();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> confirmLogout());
        btnAlertSpecialist.setOnClickListener(v -> sendAlertToSpecialist());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (filterRunnable != null) {
            filterHandler.removeCallbacks(filterRunnable);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];
            if (distance < 5 && distance > 0) {
                tvProximityStatus.setText("Proximity: Staff detected nearby");
                tvSensorMessage.setText("Refreshing data...");
                refreshStationData();
            } else {
                tvProximityStatus.setText("Proximity: Clear");
            }
        }
        else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lightLevel = event.values[0];
            tvLightStatus.setText(String.format(Locale.getDefault(), "Light Sensor: %.1f lux", lightLevel));
            if (lightLevel < 10) {
                tvSensorMessage.setText("Low light detected - Station may be closed");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void refreshStationData() {
        stationsRef.get().addOnSuccessListener(querySnapshot -> {
            int alertCount = 0;
            for (DocumentSnapshot doc : querySnapshot) {
                StationModel model = doc.toObject(StationModel.class);
                if (model != null && (model.getTurbidity() > model.getMaxTurbidityLimit() ||
                        model.getPh() < model.getMinPhLimit())) {
                    alertCount++;
                }
            }
            tvAlerts.setText(String.valueOf(alertCount));
            Toast.makeText(this, "Data refreshed by proximity sensor!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBatteryMonitoring() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batteryPct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        int status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
        boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL);

        tvBatteryStatus.setText(String.format(Locale.getDefault(), "Battery: %d%% %s",
                batteryPct, isCharging ? "(Charging)" : "(Battery)"));

        if (batteryPct < 15) {
            tvSensorMessage.setText("Low battery! Please charge device");
        }
    }

    private void setupFilterLifespanSimulation() {
        filterLifePercent = 100;
        progressFilterLife.setProgress(filterLifePercent);
        tvFilterLifespan.setText("Filter Lifespan: 100% Remaining");

        filterRunnable = new Runnable() {
            @Override
            public void run() {
                if (filterLifePercent > 0) {
                    filterLifePercent -= 1;
                    progressFilterLife.setProgress(filterLifePercent);
                    tvFilterLifespan.setText(String.format("Filter Lifespan: %d%% Remaining", filterLifePercent));

                    if (filterLifePercent <= 30 && filterLifePercent > 15) {
                        tvMaintenanceAlert.setText("Warning: Filter needs replacement soon!");
                        tvMaintenanceAlert.setTextColor(0xFFFF9800);
                    } else if (filterLifePercent <= 15) {
                        tvMaintenanceAlert.setText("URGENT: Filter replacement overdue!");
                        tvMaintenanceAlert.setTextColor(0xFFD32F2F);
                    } else {
                        tvMaintenanceAlert.setText("Filter is healthy");
                        tvMaintenanceAlert.setTextColor(0xFF4CAF50);
                    }
                    filterHandler.postDelayed(this, 1000);
                }
            }
        };
        filterHandler.postDelayed(filterRunnable, 5000);
    }

    private void sendAlertToSpecialist() {
        String specialistPhone = null;
        if (!taskList.isEmpty()) {
            specialistPhone = taskList.get(0).getSpecialistPhone();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Alert to Specialist");

        final EditText input = new EditText(this);
        if (specialistPhone != null) {
            input.setText(specialistPhone);
        }
        input.setHint("Enter specialist phone number");
        builder.setView(input);

        builder.setPositiveButton("SEND SMS", (dialog, which) -> {
            String phone = input.getText().toString().trim();
            if (!TextUtils.isEmpty(phone)) {
                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                smsIntent.setData(Uri.parse("sms:" + phone));
                smsIntent.putExtra("sms_body", "URGENT: Water filter requires maintenance! Filter at " + filterLifePercent + "%.");
                startActivity(smsIntent);
            } else {
                Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void setupReportButtons() {
        btnWaterQualityReport.setOnClickListener(v -> generateWaterQualityReport());
        btnMaintenanceReport.setOnClickListener(v -> generateMaintenanceReport());
        btnExportReport.setOnClickListener(v -> exportReportViaEmail());
    }

    private void generateWaterQualityReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== WATER QUALITY REPORT ===\n");
        report.append("Date: ").append(new java.util.Date()).append("\n\n");

        for (StationModel station : stationList) {
            report.append("Station: ").append(station.getName()).append("\n");
            report.append("  Turbidity: ").append(station.getTurbidity()).append(" NTU\n");
            report.append("  pH Level: ").append(station.getPh()).append("\n");
            report.append("  Status: ");
            if (station.getTurbidity() > station.getMaxTurbidityLimit() ||
                    station.getPh() < station.getMinPhLimit()) {
                report.append("ALERT - Unsafe\n");
            } else {
                report.append("Safe\n");
            }
            report.append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Water Quality Report")
                .setMessage(report.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void generateMaintenanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== MAINTENANCE REPORT ===\n");
        report.append("Date: ").append(new java.util.Date()).append("\n");
        report.append("Scheduled Tasks: ").append(taskList.size()).append("\n\n");

        for (TaskModel task : taskList) {
            report.append("Station: ").append(task.getTargetStation()).append("\n");
            report.append("  Specialist: ").append(task.getSpecialistPhone()).append("\n");
            report.append("  Date: ").append(task.getDate()).append("\n\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Maintenance Report")
                .setMessage(report.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void exportReportViaEmail() {
        StringBuilder report = new StringBuilder();
        report.append("WATER FILTER MONITORING REPORT\n");
        report.append("==============================\n\n");
        report.append("Generated: ").append(new java.util.Date()).append("\n\n");
        report.append("Total Stations: ").append(stationList.size()).append("\n");
        report.append("Active Alerts: ").append(tvAlerts.getText()).append("\n");
        report.append("Scheduled Tasks: ").append(taskList.size()).append("\n\n");
        report.append("Filter Lifespan: ").append(filterLifePercent).append("%\n");

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Water Filter System Report");
        emailIntent.putExtra(Intent.EXTRA_TEXT, report.toString());
        startActivity(Intent.createChooser(emailIntent, "Send Report"));
    }

    private void setupRealtimeDashboardMetrics() {
        stationsRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            stationList.clear();
            int totalCount = value.size();
            int dangerAlertsCount = 0;

            for (DocumentSnapshot doc : value.getDocuments()) {
                StationModel model = doc.toObject(StationModel.class);
                if (model != null) {
                    stationList.add(model);
                    if (model.getTurbidity() > model.getMaxTurbidityLimit() ||
                            model.getPh() < model.getMinPhLimit()) {
                        dangerAlertsCount++;
                    }
                }
            }
            tvTotal.setText(String.valueOf(totalCount));
            tvAlerts.setText(String.valueOf(dangerAlertsCount));
            adapter.notifyDataSetChanged();
        });

        tasksRef.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            taskList.clear();
            tvScheduledCount.setText(String.valueOf(value.size()));
            for (DocumentSnapshot doc : value.getDocuments()) {
                TaskModel task = doc.toObject(TaskModel.class);
                if (task != null) taskList.add(task);
            }
            taskAdapter.notifyDataSetChanged();
        });
    }

    private void setupDatabaseInsertOperation() {
        btnAddTask.setOnClickListener(v -> {
            String stationStr = etStation.getText().toString().trim();
            String phoneStr = etPhone.getText().toString().trim();
            String dateStr = etDate.getText().toString().trim();

            if (TextUtils.isEmpty(stationStr) || TextUtils.isEmpty(phoneStr) || TextUtils.isEmpty(dateStr)) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            String customTaskId = tasksRef.document().getId();
            HashMap<String, Object> taskMap = new HashMap<>();
            taskMap.put("taskId", customTaskId);
            taskMap.put("targetStation", stationStr);
            taskMap.put("specialistPhone", phoneStr);
            taskMap.put("date", dateStr);

            tasksRef.document(customTaskId).set(taskMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Task created successfully!", Toast.LENGTH_SHORT).show();
                        etStation.setText("");
                        etPhone.setText("");
                        etDate.setText("");
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("LOGOUT", (dialog, which) -> performLogout())
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}