package com.example.authentication;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class WaterQuality extends AppCompatActivity {

    FirebaseFirestore firebasedb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_water_quality);

        // ni firebase declaration
        firebasedb = FirebaseFirestore.getInstance();


        // fetch dari firebase
        getDatafromDB();
        setDataToAll();

        // this is the implicit intent
        sendemail();

        //explicit intent
        reportIssue();



        // ni nak go back to the previous page do not touch
        ImageButton goback = findViewById(R.id.btnBack);
        goback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


    //implicit intent send email
    TextView sendmail;
    private void sendemail () {
        sendmail = findViewById(R.id.emailreport);
        sendmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openmail = new Intent(Intent.ACTION_SENDTO);
                openmail.setData(Uri.parse("mailto:"));
                openmail.putExtra(Intent.EXTRA_EMAIL, new String[]{"emir@gmail.com"});
                openmail.putExtra(Intent.EXTRA_SUBJECT, "Inaccurate Reading");
                openmail.putExtra(Intent.EXTRA_TEXT, "Hello, I am [NAME] and reporting to notify of Inaccurate Reading");
                try {
                    startActivity(openmail);
                } catch (ActivityNotFoundException e) {
                    // Gracefully handle the error if no email client is installed
                    Toast.makeText(WaterQuality.this, "no email app installed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    // ni butang hitam report issue kat bawah to go page dhila
    private void reportIssue() {
        Button reportisue = findViewById(R.id.btnReportIssue);
        reportisue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent report = new Intent(MainActivity.this, );
            }
        });
    }

    private void setDataToAll() {

    }

    private void getDatafromDB() {
//        firebasedb.collection("DAMIA")
//                .get("DAMIA")
//                .addOnCompleteListener()
    }





}
