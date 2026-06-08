package com.example.authentication;

import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    FirebaseFirestore firebasedb;
    private RecyclerView rvNotes;
    private NotesAdapter notesAdapter;
    private List<NoteModel> noteList;
    private EditText etQuickNote;
    private Button btnAddNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        firebasedb = FirebaseFirestore.getInstance();

        // UI components
        etQuickNote = findViewById(R.id.etQuickNote);
        btnAddNote = findViewById(R.id.btnAddNote);
        rvNotes = findViewById(R.id.rvQuickNotes);

        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList);
        rvNotes.setAdapter(notesAdapter);

        btnAddNote.setOnClickListener(v -> addQuickNote());

        setUser();
        waterqualitymore();
        waterusagemore();
        gotowebsite();
        bottomnav();
        loadDashboardSummary();
        loadQuickNotes();
    }

    private void addQuickNote() {
        String content = etQuickNote.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Please enter a note", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> note = new HashMap<>();
        note.put("content", content);
        note.put("timestamp", System.currentTimeMillis());

        firebasedb.collection("quick_notes")
                .add(note)
                .addOnSuccessListener(documentReference -> {
                    etQuickNote.setText("");
                    Toast.makeText(HomeActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(HomeActivity.this, "Failed to save note", Toast.LENGTH_SHORT).show());
    }

    private void loadQuickNotes() {
        firebasedb.collection("quick_notes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        noteList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            NoteModel model = new NoteModel(doc.getId(), doc.getString("content"));
                            noteList.add(model);
                        }
                        notesAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void updateNote(NoteModel note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Note");
        final EditText input = new EditText(this);
        input.setText(note.content);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newContent = input.getText().toString().trim();
            if (!newContent.isEmpty()) {
                firebasedb.collection("quick_notes").document(note.id)
                        .update("content", newContent)
                        .addOnSuccessListener(aVoid -> Toast.makeText(HomeActivity.this, "Note updated!", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteNote(String id) {
        firebasedb.collection("quick_notes").document(id)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(HomeActivity.this, "Note deleted!", Toast.LENGTH_SHORT).show());
    }

    private void loadDashboardSummary() {
        TextView tvTurbidity = findViewById(R.id.tvTurbidity);
        TextView tvPH = findViewById(R.id.tvPH);
        TextView tvUsagePercent = findViewById(R.id.tvUsagePercent);
        android.widget.ProgressBar progressBar = findViewById(R.id.progressWaterUsage);

        firebasedb.collection("water_metrics").document("current_status")
                .addSnapshotListener((value, error) -> {
                    if (value != null && value.exists()) {
                        String turb = value.getString("turbidity");
                        String ph = value.getString("ph");
                        if (turb != null) tvTurbidity.setText(String.format("%s NTU", turb));
                        if (ph != null) tvPH.setText(String.format("%s pH", ph));
                    }
                });

        firebasedb.collection("water_usage")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        DocumentSnapshot doc = value.getDocuments().get(0);
                        String consumptionStr = doc.getString("consumption");
                        if (consumptionStr != null) {
                            try {
                                int consumption = Integer.parseInt(consumptionStr.replaceAll("[^0-9]", ""));
                                int percent = (consumption * 100) / 1000;
                                if (percent > 100) percent = 100;
                                tvUsagePercent.setText(String.format(Locale.getDefault(), "%d%%", percent));
                                if (progressBar != null) progressBar.setProgress(percent);
                            } catch (Exception e) {
                                tvUsagePercent.setText("70%");
                            }
                        }
                    }
                });
    }

    private void gotowebsite() {
        TextView goweb = findViewById(R.id.firstTime);
        goweb.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yayesye"));
            startActivity(intent);
        });
    }

    private void bottomnav() {
        LinearLayout llHome = findViewById(R.id.llHome);
        LinearLayout llAlerts = findViewById(R.id.llAlerts);
        LinearLayout llReport = findViewById(R.id.llReport);
        LinearLayout llSettings = findViewById(R.id.llSettings);

        llHome.setOnClickListener(v -> {
            Toast.makeText(HomeActivity.this, "Already on Home", Toast.LENGTH_SHORT).show();
        });

        llAlerts.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, NotificationCenterActivity.class);
            startActivity(intent);
        });

        llReport.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReportIssueActivity.class);
            startActivity(intent);
        });

        llSettings.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void setUser() {
        TextView setusername = findViewById(R.id.UserName);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            firebasedb.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null && !username.isEmpty()) {
                                setusername.setText(username + "!");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        String intentName = getIntent().getStringExtra("USERNAME");
                        setusername.setText(intentName != null && !intentName.isEmpty() ? intentName : "User");
                    });
        } else {
            String username = getIntent().getStringExtra("USERNAME");
            setusername.setText(username != null && !username.isEmpty() ? username : "User");
        }
    }

    private void waterusagemore() {
        findViewById(R.id.btnWaterUsageMore).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, WaterUsageActivity.class)));
    }

    private void waterqualitymore() {
        findViewById(R.id.btnWaterQualityMore).setOnClickListener(view ->
                startActivity(new Intent(HomeActivity.this, WaterQuality.class)));
    }

    // Model and Adapter Classes
    private static class NoteModel {
        String id, content;
        NoteModel(String id, String content) { this.id = id; this.content = content; }
    }

    private class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {
        List<NoteModel> notes;
        NotesAdapter(List<NoteModel> notes) { this.notes = notes; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NoteModel note = notes.get(position);
            holder.tvContent.setText(note.content);
            holder.btnEdit.setOnClickListener(v -> updateNote(note));
            holder.btnDelete.setOnClickListener(v -> deleteNote(note.id));
        }

        @Override
        public int getItemCount() { return notes.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvContent;
            Button btnEdit, btnDelete;
            ViewHolder(View v) {
                super(v);
                tvContent = v.findViewById(R.id.tvNoteContent);
                btnEdit = v.findViewById(R.id.btnEditNote);
                btnDelete = v.findViewById(R.id.btnDeleteNote);
            }
        }
    }
}