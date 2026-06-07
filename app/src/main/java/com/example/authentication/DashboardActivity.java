package com.example.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView tvOperatorEmail;
    private TextView tvTurbidityValue, tvFilterLifespan;
    private Button btnGoToAdmin;
    private Button btnSettings;
    private Button btnReportIssue;
    private Button btnMaintenance;
    private Button btnWaterUsage;      // ADD THIS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();

        tvOperatorEmail = findViewById(R.id.tvOperatorEmail);
        tvTurbidityValue = findViewById(R.id.tvTurbidityValue);
        tvFilterLifespan = findViewById(R.id.tvFilterLifespan);
        btnGoToAdmin = findViewById(R.id.btnGoToAdmin);
        btnSettings = findViewById(R.id.btnSettings);
        btnReportIssue = findViewById(R.id.btnReportIssue);
        btnMaintenance = findViewById(R.id.btnMaintenance);
        btnWaterUsage = findViewById(R.id.btnWaterUsage);  // ADD THIS

        FirebaseUser activeSessionUser = mAuth.getCurrentUser();
        if (activeSessionUser != null) {
            String authenticatedEmail = activeSessionUser.getEmail();
            tvOperatorEmail.setText(authenticatedEmail);
        } else {
            kickUserToAuthenticationScreen();
        }

        btnGoToAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        btnReportIssue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, ReportIssueActivity.class);
                startActivity(intent);
            }
        });

        btnMaintenance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, MaintenanceActivity.class);
                startActivity(intent);
            }
        });

        // ADD THIS - Water Usage button click listener
        btnWaterUsage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, WaterUsageActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btnRefreshMetrics).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DashboardActivity.this, "Syncing waterfilteringmonitor cloud database...", Toast.LENGTH_SHORT).show();
                tvTurbidityValue.setText("1.9 NTU (Excellent Quality)");
                tvFilterLifespan.setText("86% Efficiency Remaining");
            }
        });

        findViewById(R.id.btnSystemLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeUserSessionLogout();
            }
        });
    }

    private void executeUserSessionLogout() {
        mAuth.signOut();
        Toast.makeText(DashboardActivity.this, "Session Disconnected Successfully.", Toast.LENGTH_SHORT).show();
        kickUserToAuthenticationScreen();
    }

    private void kickUserToAuthenticationScreen() {
        Intent reverseIntent = new Intent(DashboardActivity.this, MainActivity.class);
        startActivity(reverseIntent);
        finish();
    }
}