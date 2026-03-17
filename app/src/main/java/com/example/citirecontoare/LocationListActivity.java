package com.example.citirecontoare;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * LocationListActivity - Displays all house numbers/locations for a selected zone.
 */
public class LocationListActivity extends AppCompatActivity {

    private static final String TAG = "LocationListActivity";
    private ListView locationListView;
    private String selectedZone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_list);

        // PRELUARE DATĂ
        selectedZone = getIntent().getStringExtra("zoneName");

        // VERIFICARE CRITICĂ (Să nu mai dea NullPointerException)
        if (selectedZone == null || selectedZone.isEmpty()) {
            Log.e(TAG, "selectedZone is NULL or Empty!");
            finish(); // Închidem activitatea imediat
            return;
        }

        setupUI();
        loadLocationsFromFirestore();
    }

    private void setupUI() {
        TextView zoneTitle = findViewById(R.id.textViewZoneName);
        if (zoneTitle != null) zoneTitle.setText("Zone: " + selectedZone);

        locationListView = findViewById(R.id.listViewHouses);

        ImageButton backButton = findViewById(R.id.buttonBack);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        Button exportButton = findViewById(R.id.buttonDownloadData);
        if (exportButton != null) {
            exportButton.setOnClickListener(v -> exportConsumptionData());
        }
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

                    // ADAUGĂM UN ADAPTER CUSTOM pentru design de Master
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            this, R.layout.item_house, R.id.textViewHouseItem, houseNumbers);
                    locationListView.setAdapter(adapter);

                    locationListView.setOnItemClickListener((parent, view, position, id) -> {
                        String selectedHouse = houseNumbers.get(position);
                        navigateToMeterReading(selectedHouse);
                    });
                });
    }

    private void navigateToMeterReading(String houseString) {
        // FIX PENTRU CRASH: Extragem doar cifrele din "Numarul 13" -> "13"
        try {
            String numericOnly = houseString.replaceAll("[^0-9]", "");
            Long houseNumber = Long.parseLong(numericOnly);

            Intent intent = new Intent(this, MeterReadingActivity.class);
            intent.putExtra("HOUSE_NUMBER", houseNumber);
            intent.putExtra("ZONE_NAME", selectedZone);
            startActivity(intent);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Eroare la procesarea numarului casei: " + houseString);
            Toast.makeText(this, "Format numar casa invalid!", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportConsumptionData() {
        Log.d(TAG, "Exporting data for: " + selectedZone);
    }
}