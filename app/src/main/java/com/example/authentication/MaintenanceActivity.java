package com.example.authentication;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MaintenanceAdapter adapter;
    private List<MaintenanceTask> taskList;
    private FirebaseFirestore db;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX = 0;
    private long lastShakeTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Maintenance");
        }

        recyclerView = findViewById(R.id.recyclerViewMaintenance);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskList = new ArrayList<>();
        adapter = new MaintenanceAdapter(taskList);
        recyclerView.setAdapter(adapter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        loadMaintenanceTasks();
    }

    private void loadMaintenanceTasks() {
        db.collection("maintenance_tasks")
                .get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        taskList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            MaintenanceTask task = document.toObject(MaintenanceTask.class);
                            task.setId(document.getId());
                            taskList.add(task);
                        }
                        adapter.notifyDataSetChanged();
                        updateStats();
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MaintenanceActivity.this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateStats() {
        int pending = 0;
        int completed = 0;

        for (MaintenanceTask task : taskList) {
            if ("Completed".equals(task.getStatus())) {
                completed++;
            } else {
                pending++;
            }
        }

        TextView tvPending = findViewById(R.id.tvPendingCount);
        TextView tvCompleted = findViewById(R.id.tvCompletedCount);

        if (tvPending != null) {
            tvPending.setText(String.valueOf(pending));
        }
        if (tvCompleted != null) {
            tvCompleted.setText(String.valueOf(completed));
        }
    }

    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = event.values[0];
                long now = System.currentTimeMillis();

                if (Math.abs(x - lastX) > 12 && (now - lastShakeTime) > 1000) {
                    lastShakeTime = now;
                    loadMaintenanceTasks();
                    Toast.makeText(MaintenanceActivity.this, "Refreshed!", Toast.LENGTH_SHORT).show();
                }
                lastX = x;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ==================== ADAPTER ====================

    public class MaintenanceAdapter extends RecyclerView.Adapter<MaintenanceAdapter.ViewHolder> {
        private List<MaintenanceTask> tasks;

        public MaintenanceAdapter(List<MaintenanceTask> tasks) {
            this.tasks = tasks;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_maintenance, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MaintenanceTask task = tasks.get(position);

            holder.tvTaskName.setText(task.getName());
            holder.tvDate.setText(task.getDate());
            holder.tvAssignedTo.setText(task.getAssignedTo());

            String statusText = task.getStatus();
            holder.tvStatus.setText(statusText);

            if ("Completed".equals(statusText)) {
                holder.tvStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                holder.btnCompleteTask.setEnabled(false);
                holder.btnCompleteTask.setText("Completed");
            } else {
                holder.tvStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
                holder.btnCompleteTask.setEnabled(true);
                holder.btnCompleteTask.setText("Mark Complete");
            }

            holder.btnCompleteTask.setOnClickListener(v -> {
                task.setStatus("Completed");
                db.collection("maintenance_tasks").document(task.getId())
                        .update("status", "Completed")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(v.getContext(), "Task completed!", Toast.LENGTH_SHORT).show();
                            notifyDataSetChanged();
                            updateStats();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(v.getContext(), "Failed to update", Toast.LENGTH_SHORT).show();
                        });
            });

            // Implicit Intent - Call technician
            holder.itemView.setOnLongClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:1800881234"));
                v.getContext().startActivity(intent);
                Toast.makeText(v.getContext(), "Calling technician...", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTaskName, tvDate, tvAssignedTo, tvStatus;
            Button btnCompleteTask;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTaskName = itemView.findViewById(R.id.tvTaskName);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnCompleteTask = itemView.findViewById(R.id.btnCompleteTask);
            }
        }
    }
}