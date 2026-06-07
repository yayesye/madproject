package com.example.authentication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Profile
    private EditText etUsername, etEmail;
    private EditText etCurrentPassword, etNewPassword, etConfirmNewPassword;

    // Preferences
    private CheckBox cbWaterAlert, cbMaintenanceAlert, cbSystemAlert;
    private TextView tvAppVersion, tvBuildDate;
    private Button btnUpdateProfile, btnChangePassword, btnSavePreferences;
    private Button btnClearCache, btnDeleteAccount;

    // Push Notification
    private EditText etNotifTitle, etNotifMessage;
    private Button btnSendNotification;
    private RecyclerView rvNotificationHistory;
    private TextView tvNoNotifications;

    private SharedPreferences sharedPreferences;

    // Notification Firestore Collection
    private ArrayList<NotificationModel> notificationList;
    private NotificationAdapter notificationAdapter;

    private static final String CHANNEL_ID = "water_alert_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);

        initViews();
        loadUserData();
        loadPreferences();
        setupClickListeners();
        setupNotificationChannel();
        requestNotificationPermission();
        loadNotifications();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        // Profile
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);

        // Preferences
        cbWaterAlert = findViewById(R.id.cbWaterAlert);
        cbMaintenanceAlert = findViewById(R.id.cbMaintenanceAlert);
        cbSystemAlert = findViewById(R.id.cbSystemAlert);
        tvAppVersion = findViewById(R.id.tvAppVersion);
        tvBuildDate = findViewById(R.id.tvBuildDate);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnSavePreferences = findViewById(R.id.btnSavePreferences);
        btnClearCache = findViewById(R.id.btnClearCache);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        // Push Notification
        etNotifTitle = findViewById(R.id.etNotifTitle);
        etNotifMessage = findViewById(R.id.etNotifMessage);
        btnSendNotification = findViewById(R.id.btnSendNotification);
        rvNotificationHistory = findViewById(R.id.rvNotificationHistory);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);

        tvAppVersion.setText("2.0.0");
        tvBuildDate.setText("June 2026");

        // Setup RecyclerView for notifications
        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notificationList, notification -> {
            // DELETE from Firestore
            db.collection("notifications").document(notification.id).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                        loadNotifications();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
        rvNotificationHistory.setLayoutManager(new LinearLayoutManager(this));
        rvNotificationHistory.setAdapter(notificationAdapter);
    }

    private void loadUserData() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String email = documentSnapshot.getString("email");
                            if (username != null) etUsername.setText(username);
                            if (email != null) etEmail.setText(email);
                        } else {
                            if (currentUser.getDisplayName() != null) etUsername.setText(currentUser.getDisplayName());
                            etEmail.setText(currentUser.getEmail());
                        }
                    })
                    .addOnFailureListener(e -> {
                        etEmail.setText(currentUser.getEmail());
                    });
        }
    }

    private void loadPreferences() {
        cbWaterAlert.setChecked(sharedPreferences.getBoolean("water_alert", true));
        cbMaintenanceAlert.setChecked(sharedPreferences.getBoolean("maintenance_alert", true));
        cbSystemAlert.setChecked(sharedPreferences.getBoolean("system_alert", true));
    }

    private void loadNotifications() {
        // READ from Firestore - Get all notifications ordered by timestamp
        db.collection("notifications")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        NotificationModel notification = new NotificationModel();
                        notification.id = document.getId();
                        notification.title = document.getString("title");
                        notification.message = document.getString("message");
                        notification.timestamp = document.getLong("timestamp");
                        notificationList.add(notification);
                    }
                    notificationAdapter.notifyDataSetChanged();

                    if (notificationList.isEmpty()) {
                        tvNoNotifications.setVisibility(View.VISIBLE);
                        rvNotificationHistory.setVisibility(View.GONE);
                    } else {
                        tvNoNotifications.setVisibility(View.GONE);
                        rvNotificationHistory.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners() {
        btnUpdateProfile.setOnClickListener(v -> updateProfile());
        btnChangePassword.setOnClickListener(v -> changePassword());
        btnSavePreferences.setOnClickListener(v -> savePreferences());
        btnClearCache.setOnClickListener(v -> clearAllNotifications());
        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
        btnSendNotification.setOnClickListener(v -> sendPushNotification());
    }

    private void updateProfile() {
        String newUsername = etUsername.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(newUsername)) {
            etUsername.setError("Username cannot be empty");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newEmail) || !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etEmail.setError("Valid email required");
            etEmail.requestFocus();
            return;
        }

        if (currentUser != null && !newEmail.equals(currentUser.getEmail())) {
            currentUser.verifyBeforeUpdateEmail(newEmail)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Verification email sent to " + newEmail, Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        String userId = currentUser.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newUsername);
        updates.put("email", newEmail);

        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void changePassword() {
        String currentPwd = etCurrentPassword.getText().toString().trim();
        String newPwd = etNewPassword.getText().toString().trim();
        String confirmPwd = etConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPwd)) {
            etCurrentPassword.setError("Current password required");
            etCurrentPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPwd) || newPwd.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            etNewPassword.requestFocus();
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            etConfirmNewPassword.setError("Passwords do not match");
            etConfirmNewPassword.requestFocus();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPwd);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    currentUser.updatePassword(newPwd)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(SettingsActivity.this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                                etCurrentPassword.setText("");
                                etNewPassword.setText("");
                                etConfirmNewPassword.setText("");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(SettingsActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SettingsActivity.this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                });
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("water_alert", cbWaterAlert.isChecked());
        editor.putBoolean("maintenance_alert", cbMaintenanceAlert.isChecked());
        editor.putBoolean("system_alert", cbSystemAlert.isChecked());
        editor.apply();

        Toast.makeText(this, "Preferences saved!", Toast.LENGTH_SHORT).show();
    }

    private void clearAllNotifications() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Notifications")
                .setMessage("Are you sure you want to clear ALL notification history? This cannot be undone.")
                .setPositiveButton("CLEAR ALL", (dialog, which) -> {
                    // DELETE all notifications from Firestore
                    db.collection("notifications").get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    document.getReference().delete();
                                }
                                Toast.makeText(this, "All notifications cleared!", Toast.LENGTH_SHORT).show();
                                loadNotifications();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to clear: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you absolutely sure? This action is permanent and cannot be undone. All your data will be lost.")
                .setPositiveButton("DELETE", (dialog, which) -> deleteAccount())
                .setNegativeButton("CANCEL", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteAccount() {
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Delete user data from Firestore first
        db.collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Then delete from Firebase Auth
                    currentUser.delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(SettingsActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                mAuth.signOut();
                                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(SettingsActivity.this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SettingsActivity.this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ==================== PUSH NOTIFICATION METHODS (Using Firestore) ====================

    private void sendPushNotification() {
        String title = etNotifTitle.getText().toString().trim();
        String message = etNotifMessage.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(message)) {
            Toast.makeText(this, "Please fill both title and message", Toast.LENGTH_SHORT).show();
            return;
        }

        // CREATE - Add new notification to Firestore
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", title);
        notificationData.put("message", message);
        notificationData.put("timestamp", System.currentTimeMillis());
        notificationData.put("sentBy", currentUser != null ? currentUser.getEmail() : "unknown");

        db.collection("notifications").add(notificationData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Notification sent successfully!", Toast.LENGTH_SHORT).show();
                    showPushNotification(title, message);

                    // Clear input fields
                    etNotifTitle.setText("");
                    etNotifMessage.setText("");

                    // Refresh the list
                    loadNotifications();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showPushNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Water Alert Notifications";
            String description = "Notifications for water availability and maintenance alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }
}