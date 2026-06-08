package com.example.authentication;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SeedDataActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Add sample water stations
        addStation(db, "station_001", "Kampung A Filtration", 2.1, 7.2, 26.5, 5.0, 6.5);
        addStation(db, "station_002", "Kampung B Treatment", 3.5, 6.8, 27.0, 5.0, 6.5);
        addStation(db, "station_003", "Kampung C Reservoir", 1.8, 7.5, 25.5, 5.0, 6.5);

        // Add sample water metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("turbidity", "2.1");
        metrics.put("ph", "7.2");
        metrics.put("temp", "26.5");
        metrics.put("chlorine", "1.2");
        db.collection("water_metrics").document("current_status").set(metrics);

        Toast.makeText(this, "Sample data added!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void addStation(FirebaseFirestore db, String id, String name,
                            double turb, double ph, double temp, double maxTurb, double minPh) {
        StationModel station = new StationModel(id, name, turb, ph, temp, maxTurb, minPh);
        db.collection("water_stations").document(id).set(station);
    }
}