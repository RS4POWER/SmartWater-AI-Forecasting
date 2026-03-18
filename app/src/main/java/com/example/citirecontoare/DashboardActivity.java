package com.example.citirecontoare;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * DashboardActivity - Main navigation hub after authentication.
 * Refactored for Master's Dissertation (IT Sector).
 */
public class DashboardActivity extends AppCompatActivity {

    private Button primaryZoneButton;
    private Button secondaryZoneButton;
    private Button logoutButton;
    private TextView statusTextView;

    private static final String TAG = "DashboardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeUI();
        // Dezactivăm butoanele până se încarcă datele din Firebase
        primaryZoneButton.setEnabled(false);
        secondaryZoneButton.setEnabled(false);

        loadUserAssignedZones();
        setupClickListeners();
    }

    private void initializeUI() {
        statusTextView = findViewById(R.id.title_text_view);
        primaryZoneButton = findViewById(R.id.zoneButton1);
        secondaryZoneButton = findViewById(R.id.zoneButton2);
        logoutButton = findViewById(R.id.LogOff);

        logoutButton.setText("Log Off");
    }

    private void loadUserAssignedZones() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) return;

        String userUID = firebaseAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userUID);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String assignedZones = documentSnapshot.getString("zoneAsignate");
                if (assignedZones != null && !assignedZones.isEmpty()) {
                    String[] zones = assignedZones.split(";");

                    if (zones.length >= 1) {
                        primaryZoneButton.setText(zones[0]);
                        primaryZoneButton.setEnabled(true); // Activăm butonul
                    }
                    if (zones.length >= 2) {
                        secondaryZoneButton.setText(zones[1]);
                        secondaryZoneButton.setEnabled(true); // Activăm butonul
                    }
                } else {
                    statusTextView.setText("No assigned zones found.");
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching user data", e));
    }

    private void setupClickListeners() {

        primaryZoneButton.setOnClickListener(view -> {
            String zone = primaryZoneButton.getText().toString();
            // Verificăm dacă e încă în starea de încărcare
            if (zone.isEmpty() || zone.equals("Button") || !primaryZoneButton.isEnabled()) {
                Toast.makeText(this, "Se încarcă zonele, te rugăm așteaptă...", Toast.LENGTH_SHORT).show();
            } else {
                navigateToLocationList(zone);
            }
        });

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
        });


        secondaryZoneButton.setOnClickListener(view -> {
            String zone = secondaryZoneButton.getText().toString();
            if(!zone.isEmpty()) navigateToLocationList(zone);
        });
    }

    private void navigateToLocationList(String zoneName) {
        if (zoneName == null || zoneName.isEmpty()) return;
        Intent intent = new Intent(DashboardActivity.this, LocationListActivity.class);
        intent.putExtra("zoneName", zoneName);
        startActivity(intent);
    }
}