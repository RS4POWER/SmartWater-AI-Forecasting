package com.example.citirecontoare;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationListActivity extends AppCompatActivity {

    private static final String TAG = "LocationListActivity";
    private ListView locationListView;
    private String selectedZone;

    // UI Elements pentru Tracking (Performanță)
    private CardView trackingCard;
    private TextView trackingInfoText;
    private Button stopTrackingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_list);

        selectedZone = getIntent().getStringExtra("zoneName");
        if (selectedZone == null || selectedZone.isEmpty()) {
            Log.e(TAG, "selectedZone is NULL!");
            finish();
            return;
        }

        setupUI();
        loadLocationsFromFirestore();
        updateTrackingUI(); // Verificăm dacă există un traseu în desfășurare
    }

    private void setupUI() {
        TextView zoneTitle = findViewById(R.id.textViewZoneName);
        zoneTitle.setText("Zone: " + selectedZone);

        locationListView = findViewById(R.id.listViewHouses);
        trackingCard = findViewById(R.id.trackingCard);
        trackingInfoText = findViewById(R.id.trackingInfoText);
        stopTrackingButton = findViewById(R.id.stopTrackingButton);

        ImageButton backButton = findViewById(R.id.buttonBack);
        backButton.setOnClickListener(v -> finish());

        Button exportButton = findViewById(R.id.buttonDownloadData);
        exportButton.setOnClickListener(v -> exportConsumptionData());

        Button perfButton = findViewById(R.id.buttonPerformance);
        perfButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PerformanceHistoryActivity.class);
            intent.putExtra("zoneName", selectedZone); // Trimitem zona ca să știm ce log-uri să tragem
            startActivity(intent);
        });

        // Listener pentru finalizarea traseului
        stopTrackingButton.setOnClickListener(v -> finalizeRoute());
    }

    private void loadLocationsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("zones")
                .document(selectedZone)
                .collection("numereCasa")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> houseNumbers = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        houseNumbers.add(document.getId());
                    }

                    // 1. SORTARE NUMERICĂ (Master Logic: 1, 2, 10...)
                    Collections.sort(houseNumbers, (s1, s2) -> {
                        int i1 = extractInt(s1);
                        int i2 = extractInt(s2);
                        return i1 - i2;
                    });

                    // 2. SETUP ADAPTER
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, R.layout.item_house, R.id.textViewHouseItem, houseNumbers);
                    locationListView.setAdapter(adapter);

                    // 3. ON CLICK LISTENER (Cu logică de Tracking)
                    locationListView.setOnItemClickListener((parent, view, position, id) -> {
                        String selectedHouse = houseNumbers.get(position);
                        handleHouseClick(selectedHouse);
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading locations", e));
    }

    private int extractInt(String s) {
        String num = s.replaceAll("\\D", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }

    private void handleHouseClick(String houseName) {
        if (!RouteTracker.isTracking(this)) {
            // Dacă nu avem traseu pornit, întrebăm userul
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Pornim monitorizarea?")
                    .setMessage("Vrei să pornești înregistrarea performanței de la " + houseName + "?")
                    .setPositiveButton("DA", (dialog, which) -> {
                        RouteTracker.startRoute(this, houseName);
                        updateTrackingUI();
                        navigateToMeterReading(houseName);
                    })
                    .setNegativeButton("Nu, doar citesc", (dialog, which) -> navigateToMeterReading(houseName))
                    .show();
        } else {
            // Dacă e deja pornit, mergem direct la citire
            navigateToMeterReading(houseName);
        }
    }

    private void navigateToMeterReading(String houseString) {
        // NOTĂM CASA CA FIIND ULTIMA VIZITATĂ (pentru casaSfarsit)
        RouteTracker.updateLastHouse(this, houseString);

        try {
            long houseNumber = Long.parseLong(houseString.replaceAll("\\D", ""));
            Intent intent = new Intent(this, MeterReadingActivity.class);
            intent.putExtra("HOUSE_NUMBER", houseNumber);
            intent.putExtra("ZONE_NAME", selectedZone);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Format casa invalid!", Toast.LENGTH_SHORT).show();
        }
    }

    // --- LOGICA DE PERFORMANCE TRACKING ---

    public void updateTrackingUI() {
        if (RouteTracker.isTracking(this)) {
            trackingCard.setVisibility(View.VISIBLE);
            trackingInfoText.setText("⏱ Pornit la: " + RouteTracker.getStartHouse(this));
        } else {
            trackingCard.setVisibility(View.GONE);
        }
    }

    private void finalizeRoute() {
        long startTime = RouteTracker.getStartTime(this);
        long endTime = System.currentTimeMillis();
        long durataMin = Math.round((endTime - startTime) / 60000.0);
        // Generăm un ID de document bazat pe dată (ex: 2026-03-18_17-12)
        String idDocument = new SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.getDefault()).format(new Date());
        String dataAfisare = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());

        Map<String, Object> logData = new HashMap<>();
        logData.put("dataFinalizare", dataAfisare);
        logData.put("casaInceput", RouteTracker.getStartHouse(this));
        logData.put("casaSfarsit", RouteTracker.getLastHouse(this)); // ACUM AVEM ȘI FINALUL!
        logData.put("durataMinute", durataMin);
        logData.put("operatorEmail", FirebaseAuth.getInstance().getCurrentUser().getEmail());

        FirebaseFirestore.getInstance()
                .collection("zones").document(selectedZone)
                .collection("logsPerformanta")
                .document(idDocument) // <--- AICI e magia: ID-ul nu mai e la întâmplare!
                .set(logData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Traseu finalizat: " + durataMin + " min", Toast.LENGTH_LONG).show();
                    RouteTracker.clearAll(this);
                    updateTrackingUI();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Eroare salvare!", Toast.LENGTH_SHORT).show());
    }

    private void exportConsumptionData() {
        Log.d(TAG, "Exporting data for: " + selectedZone);
        // Aici vei pune logica de export Excel/CSV în Capitolul 4
    }
}