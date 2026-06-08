package com.example.authentication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class NotificationCenterActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private ArrayList<NotificationItem> notificationList;
    private FirebaseFirestore db;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        db = FirebaseFirestore.getInstance();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        tvEmpty = findViewById(R.id.tvEmpty);

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        db.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        NotificationItem item = new NotificationItem();
                        item.id = doc.getId();
                        item.title = doc.getString("title");
                        item.message = doc.getString("message");
                        item.timestamp = doc.getLong("timestamp");
                        item.type = doc.getString("type");
                        if (item.type == null) item.type = "notification";
                        notificationList.add(item);
                    }
                    adapter.notifyDataSetChanged();

                    if (notificationList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvNotifications.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvNotifications.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Model class
    static class NotificationItem {
        String id, title, message, type;
        Long timestamp;
    }

    // Adapter with color coding
    class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private ArrayList<NotificationItem> list;

        NotificationAdapter(ArrayList<NotificationItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_center, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = list.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvMessage.setText(item.message);

            // COLOR CODING: Alert = Red, Notification = Yellow/Orange
            if ("alert".equals(item.type)) {
                holder.itemView.setBackgroundColor(0xFFFFEBEE); // Light red
                holder.tvTitle.setTextColor(0xFFD32F2F); // Red
                holder.tvType.setText("🔴 ALERT");
                holder.tvType.setTextColor(0xFFD32F2F);
            } else {
                holder.itemView.setBackgroundColor(0xFFFFF8E1); // Light yellow
                holder.tvTitle.setTextColor(0xFFFF9800); // Orange
                holder.tvType.setText("📢 NOTIFICATION");
                holder.tvType.setTextColor(0xFFFF9800);
            }

            // Delete button
            holder.btnDelete.setOnClickListener(v -> {
                db.collection("notifications").document(item.id).delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(NotificationCenterActivity.this, "Dismissed", Toast.LENGTH_SHORT).show();
                            loadNotifications();
                        });
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvType;
            Button btnDelete;
            ViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvNotifTitle);
                tvMessage = v.findViewById(R.id.tvNotifMessage);
                tvType = v.findViewById(R.id.tvNotifType);
                btnDelete = v.findViewById(R.id.btnDeleteNotif);
            }
        }
    }
}