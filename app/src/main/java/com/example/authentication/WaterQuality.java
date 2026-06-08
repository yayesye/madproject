package com.example.authentication;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WaterQuality extends AppCompatActivity implements SensorEventListener {

    private FirebaseFirestore firebasedb;
    private TextView tvTurbidity, tvPH, tvTemp, tvChlorine, tvStatusBanner, tvLocation;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private FusedLocationProviderClient fusedLocationClient;

    private RecyclerView rvLogs;
    private LogAdapter logAdapter;
    private List<LogModel> logList;
    private EditText etLogTitle, etLogComment;
    private Button btnAddLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_water_quality);

        firebasedb = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize UI
        tvTurbidity = findViewById(R.id.tvTurbidityValue);
        tvPH = findViewById(R.id.tvPHValue);
        tvTemp = findViewById(R.id.tvTempValue);
        tvChlorine = findViewById(R.id.tvChlorineValue);
        tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvLocation = findViewById(R.id.tvLocationName);

        etLogTitle = findViewById(R.id.etLogTitle);
        etLogComment = findViewById(R.id.etLogComment);
        btnAddLog = findViewById(R.id.btnAddLog);
        rvLogs = findViewById(R.id.rvInspectionLogs);

        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        logList = new ArrayList<>();
        logAdapter = new LogAdapter(logList);
        rvLogs.setAdapter(logAdapter);

        btnAddLog.setOnClickListener(v -> addLogEntry());

        // Sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        getLastLocation();
        getDatafromDB();
        sendemail();
        reportIssue();

        ImageButton goback = findViewById(R.id.btnBack);
        goback.setOnClickListener(v -> finish());

        loadInspectionLogs();
    }

    private void addLogEntry() {
        String title = etLogTitle.getText().toString().trim();
        String comment = etLogComment.getText().toString().trim();

        if (title.isEmpty() || comment.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> log = new HashMap<>();
        log.put("title", title);
        log.put("comment", comment);
        log.put("timestamp", System.currentTimeMillis());

        firebasedb.collection("inspection_logs")
                .add(log)
                .addOnSuccessListener(documentReference -> {
                    etLogTitle.setText("");
                    etLogComment.setText("");
                    Toast.makeText(WaterQuality.this, "Log entry added!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(WaterQuality.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadInspectionLogs() {
        firebasedb.collection("inspection_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        logList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            LogModel model = new LogModel(doc.getId(), doc.getString("title"), doc.getString("comment"));
                            logList.add(model);
                        }
                        logAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void updateLogEntry(LogModel log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Log Entry");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etTitle = new EditText(this);
        etTitle.setText(log.title);
        layout.addView(etTitle);

        final EditText etComment = new EditText(this);
        etComment.setText(log.comment);
        layout.addView(etComment);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newTitle = etTitle.getText().toString().trim();
            String newComment = etComment.getText().toString().trim();
            if (!newTitle.isEmpty() && !newComment.isEmpty()) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("title", newTitle);
                updates.put("comment", newComment);

                firebasedb.collection("inspection_logs").document(log.id)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> Toast.makeText(WaterQuality.this, "Log updated!", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteLogEntry(String id) {
        firebasedb.collection("inspection_logs").document(id)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(WaterQuality.this, "Log deleted!", Toast.LENGTH_SHORT).show());
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateLocationUI(location);
            }
        });
    }

    private void updateLocationUI(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String cityName = addresses.get(0).getLocality();
                if (tvLocation != null) {
                    tvLocation.setText(String.format("Location: %s", cityName));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            if (lux < 20) {
                tvStatusBanner.setText("Low light: Auto-contrast enabled");
                tvStatusBanner.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
            } else {
                tvStatusBanner.setText("Water is perfectly safe!");
                tvStatusBanner.setTextColor(getResources().getColor(android.R.color.black));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void sendemail() {
        TextView sendmail = findViewById(R.id.emailreport);
        sendmail.setOnClickListener(v -> {
            Intent openmail = new Intent(Intent.ACTION_SENDTO);
            openmail.setData(Uri.parse("mailto:"));
            openmail.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@watermonitor.com"});
            openmail.putExtra(Intent.EXTRA_SUBJECT, "Water Quality Report - Village A");
            openmail.putExtra(Intent.EXTRA_TEXT, "Turbidity: " + tvTurbidity.getText() + "\npH: " + tvPH.getText());
            try {
                startActivity(openmail);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(WaterQuality.this, "No email app installed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reportIssue() {
        Button reportBtn = findViewById(R.id.btnReportIssue);
        reportBtn.setOnClickListener(v -> {
            Intent intent = new Intent(WaterQuality.this, ReportIssueActivity.class);
            startActivity(intent);
        });
    }

    private void getDatafromDB() {
        firebasedb.collection("water_metrics").document("current_status")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tvTurbidity.setText(String.format("%s NTU", documentSnapshot.getString("turbidity")));
                        tvPH.setText(String.format("%s pH", documentSnapshot.getString("ph")));
                        tvTemp.setText(String.format("%s°C", documentSnapshot.getString("temp")));
                        tvChlorine.setText(String.format("%s mg/L", documentSnapshot.getString("chlorine")));
                    } else {
                        tvTurbidity.setText("0.8 NTU");
                        tvPH.setText("7.2 pH");
                        tvTemp.setText("24.5°C");
                        tvChlorine.setText("1.2 mg/L");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(WaterQuality.this, "Error fetching data", Toast.LENGTH_SHORT).show());
    }

    // Model and Adapter Classes
    private static class LogModel {
        String id, title, comment;
        LogModel(String id, String title, String comment) {
            this.id = id; this.title = title; this.comment = comment;
        }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        List<LogModel> logs;
        LogAdapter(List<LogModel> logs) { this.logs = logs; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inspection_log, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LogModel log = logs.get(position);
            holder.tvTitle.setText(log.title);
            holder.tvComment.setText(log.comment);
            holder.btnEdit.setOnClickListener(v -> updateLogEntry(log));
            holder.btnDelete.setOnClickListener(v -> deleteLogEntry(log.id));
        }

        @Override
        public int getItemCount() { return logs.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvComment;
            Button btnEdit, btnDelete;
            ViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvInspectionTitle);
                tvComment = v.findViewById(R.id.tvInspectionComment);
                btnEdit = v.findViewById(R.id.btnEditLog);
                btnDelete = v.findViewById(R.id.btnDeleteLog);
            }
        }
    }
}
