package com.example.authentication;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminDashboard2Activity extends AppCompatActivity implements SensorEventListener {

    private TextView tvHeaderTitle, tvTurbidity, tvPh, tvTemperature, tvBatteryStatus, tvLightStatus;
    private EditText etMaxTurbidity, etMinPh;
    private Button btnUpdateThreshold, btnDeleteStation, btnCallSpecialist, btnShareReport;

    private FirebaseFirestore db;
    private DocumentReference stationDocRef;
    private String stationId, stationName;

    private SensorManager sensorManager;
    private Sensor lightSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard2);

        stationId = getIntent().getStringExtra("STATION_ID");
        stationName = getIntent().getStringExtra("STATION_NAME");

        tvHeaderTitle = findViewById(R.id.tvHeaderStationName);
        tvTurbidity = findViewById(R.id.tvLiveTurbidity);
        tvPh = findViewById(R.id.tvLivePH);
        tvTemperature = findViewById(R.id.tvLiveTemperature);
        tvBatteryStatus = findViewById(R.id.tvBatteryStatus);
        tvLightStatus = findViewById(R.id.tvLightStatus);
        etMaxTurbidity = findViewById(R.id.etMaxTurbidity);
        etMinPh = findViewById(R.id.etMinPh);
        btnUpdateThreshold = findViewById(R.id.btnUpdateThreshold);
        btnDeleteStation = findViewById(R.id.btnDeleteStation);
        btnCallSpecialist = findViewById(R.id.btnCallSpecialist);
        btnShareReport = findViewById(R.id.btnShareReport);

        if (stationName != null) {
            tvHeaderTitle.setText(stationName);
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        db = FirebaseFirestore.getInstance();
        if (stationId != null) {
            stationDocRef = db.collection("water_stations").document(stationId);
            attachLiveTelemetryListener();
            checkBatteryStatus();
        } else {
            Toast.makeText(this, "Error: Missing Station ID.", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnUpdateThreshold.setOnClickListener(v -> executeUpdateOperation());
        btnDeleteStation.setOnClickListener(v -> promptDeleteConfirmation());
        btnCallSpecialist.setOnClickListener(v -> callSpecialist());
        btnShareReport.setOnClickListener(v -> shareStationReport());
        findViewById(R.id.btnBack2).setOnClickListener(v -> finish());
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
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lightLevel = event.values[0];
            tvLightStatus.setText(String.format(Locale.getDefault(), "Light Sensor: %.1f lux", lightLevel));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void checkBatteryStatus() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batteryPct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        int status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
        boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL);

        tvBatteryStatus.setText(String.format(Locale.getDefault(), "Station Power: %d%% %s",
                batteryPct, isCharging ? "(Charging)" : "(Battery)"));
    }

    private void attachLiveTelemetryListener() {
        stationDocRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) return;
            if (documentSnapshot != null && documentSnapshot.exists()) {
                StationModel model = documentSnapshot.toObject(StationModel.class);
                if (model != null) {
                    tvTurbidity.setText(String.format(Locale.getDefault(), "Turbidity: %.1f NTU", model.getTurbidity()));
                    tvPh.setText(String.format(Locale.getDefault(), "pH Level: %.1f pH", model.getPh()));
                    tvTemperature.setText(String.format(Locale.getDefault(), "Temperature: %.1f°C", model.getTemperature()));

                    if (!etMaxTurbidity.hasFocus()) {
                        etMaxTurbidity.setText(String.valueOf(model.getMaxTurbidityLimit()));
                    }
                    if (!etMinPh.hasFocus()) {
                        etMinPh.setText(String.valueOf(model.getMinPhLimit()));
                    }
                }
            }
        });
    }

    private void executeUpdateOperation() {
        String maxTurbStr = etMaxTurbidity.getText().toString().trim();
        String minPhStr = etMinPh.getText().toString().trim();

        if (TextUtils.isEmpty(maxTurbStr) || TextUtils.isEmpty(minPhStr)) {
            Toast.makeText(this, "Threshold entries cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        double newMaxTurb = Double.parseDouble(maxTurbStr);
        double newMinPh = Double.parseDouble(minPhStr);

        Map<String, Object> updates = new HashMap<>();
        updates.put("maxTurbidityLimit", newMaxTurb);
        updates.put("minPhLimit", newMinPh);

        stationDocRef.update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Thresholds updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void promptDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Station")
                .setMessage("Delete " + stationName + "? This cannot be undone.")
                .setPositiveButton("DELETE", (dialog, which) -> executeDeleteOperation())
                .setNegativeButton("CANCEL", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void executeDeleteOperation() {
        stationDocRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Station deleted.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Delete failed.", Toast.LENGTH_SHORT).show());
    }

    private void callSpecialist() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Contact Specialist");

        final EditText input = new EditText(this);
        input.setHint("Enter phone number");
        builder.setView(input);

        builder.setPositiveButton("CALL", (dialog, which) -> {
            String phone = input.getText().toString().trim();
            if (!TextUtils.isEmpty(phone)) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phone));
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("SMS", (dialog, which) -> {
            String phone = input.getText().toString().trim();
            if (!TextUtils.isEmpty(phone)) {
                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                smsIntent.setData(Uri.parse("sms:" + phone));
                smsIntent.putExtra("sms_body", "Maintenance needed at " + stationName);
                startActivity(smsIntent);
            } else {
                Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void shareStationReport() {
        stationDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                StationModel model = documentSnapshot.toObject(StationModel.class);
                if (model != null) {
                    StringBuilder report = new StringBuilder();
                    report.append("STATION REPORT: ").append(stationName).append("\n");
                    report.append("Turbidity: ").append(model.getTurbidity()).append(" NTU\n");
                    report.append("pH Level: ").append(model.getPh()).append("\n");
                    report.append("Temperature: ").append(model.getTemperature()).append("°C\n");

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Station Report - " + stationName);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, report.toString());
                    startActivity(Intent.createChooser(shareIntent, "Share Report"));
                }
            }
        });
    }
}