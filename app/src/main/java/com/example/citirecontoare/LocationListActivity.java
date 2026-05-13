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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private String csvDataPendingSave = "";

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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 123 && resultCode == RESULT_OK && data != null) {
            android.net.Uri uri = data.getData();
            if (uri != null) {
                try (android.os.ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                     java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(pfd.getFileDescriptor())) {

                    fileOutputStream.write(csvDataPendingSave.getBytes());
                    Toast.makeText(this, "Fișier salvat cu succes!", Toast.LENGTH_LONG).show();

                } catch (java.io.IOException e) {
                    Log.e(TAG, "Eroare la salvarea locală", e);
                    Toast.makeText(this, "Eroare la salvare!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    private double safeDouble(DocumentSnapshot doc, String field) {
        Double value = doc.getDouble(field);
        return value != null ? value : 0.0;
    }

    private long safeLong(DocumentSnapshot doc, String field) {
        Long value = doc.getLong(field);
        return value != null ? value : 0L;
    }

    private String safeString(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value : "N/A";
    }

    private boolean safeBoolean(DocumentSnapshot doc, String field) {
        Boolean value = doc.getBoolean(field);
        return value != null && value;
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Pregătire context (Luna și Anul pentru care facem exportul)
        String currentMonth = new MeterReadingActivity().monthNames[Calendar.getInstance().get(Calendar.MONTH)];
        String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

        StringBuilder csvData = new StringBuilder();
        // ⚠️ ANTET NOU: Am adăugat Data_Citire și Perioada
        csvData.append("Casa;Proprietar;Data_Citire;Perioada;SchemaV;Index;Consum;AI_Predictie;AI_Eroare_%;AI_Status;Sursa;Timp_Casa_Sec;Timp_Total_Sec;Timing_Valid\n");

        db.collection("zones").document(selectedZone)
                .collection("numereCasa").get()
                .addOnSuccessListener(houses -> {
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot houseDoc : houses) {
                        tasks.add(houseDoc.getReference().collection("consumApa")
                                .document(currentYear).collection("lunile")
                                .document(currentMonth).get());
                    }

                    Tasks.whenAllComplete(tasks).addOnSuccessListener(results -> {
                        for (int i = 0; i < tasks.size(); i++) {
                            DocumentSnapshot consumption = (DocumentSnapshot) tasks.get(i).getResult();
                            String houseId = houses.getDocuments().get(i).getId();
                            String owner = safeString(houses.getDocuments().get(i), "Proprietar");

                            if (consumption != null && consumption.exists()) {
                                // 🕒 EXTRACȚIE TIMP
                                String dataCitire = safeString(consumption, "Data citire"); // Ziua efectivă (ex: 07-05-2026)
                                String perioada = currentMonth + " " + currentYear; // Contextul (ex: Mai 2026)

                                long schema = safeLong(consumption, "schema_version");
                                double index = safeDouble(consumption, "Starea Apometrului");
                                double consum = safeDouble(consumption, "Consumatia mc");
                                double aiPred = safeDouble(consumption, "ai_prediction");
                                double aiErr = safeDouble(consumption, "ai_error_percent");
                                String aiStat = safeString(consumption, "ai_status");
                                String source = safeString(consumption, "reading_source");
                                long tCasa = safeLong(consumption, "time_since_last_house_sec");
                                long tTotal = safeLong(consumption, "route_elapsed_sec");
                                boolean valid = safeBoolean(consumption, "timing_valid");

                                // ✍️ SCRIERE RÂND (Atenție la ordine: am adăugat %s;%s pentru dată și perioadă)
                                csvData.append(String.format(Locale.US, "%s;%s;%s;%s;%d;%.2f;%.2f;%.2f;%.2f%%;%s;%s;%d;%d;%b\n",
                                        houseId, owner, dataCitire, perioada, schema, index, consum, aiPred, aiErr, aiStat, source, tCasa, tTotal, valid));
                            }
                        }

                        this.csvDataPendingSave = csvData.toString();

                        new android.app.AlertDialog.Builder(this)
                                .setTitle("Export Raport - " + selectedZone)
                                .setMessage("Datele au fost colectate. Cum dorești să le salvezi?")
                                .setPositiveButton("Trimite (Share)", (dialog, which) -> shareCSV(this.csvDataPendingSave))
                                .setNegativeButton("Salvează local", (dialog, which) -> triggerSaveAs(selectedZone))
                                .setNeutralButton("Anulează", null)
                                .show();
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Eroare la citirea listei de case!", Toast.LENGTH_SHORT).show());
    }
    private void triggerSaveAs(String zoneName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "Raport_" + zoneName + ".csv");

        // Codul 123 este un ID pe care îl verificăm în onActivityResult
        startActivityForResult(intent, 123);
    }
    private void shareCSV(String data) {
        try {
            // 1. Creăm fișierul în folderul de "Files" al aplicației
            java.io.File path = getExternalFilesDir(null);
            java.io.File file = new java.io.File(path, "Raport_" + selectedZone + ".csv");

            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            out.write(data.getBytes());
            out.close();

            // 2. Obținem un URI securizat (aici folosim FileProvider)
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    file);

            // 3. Pregătim Intent-ul de trimitere
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Raport Consum: " + selectedZone);
            intent.putExtra(Intent.EXTRA_STREAM, uri);

            // Acordăm permisiune temporară de citire aplicației care va primi fișierul (ex: WhatsApp/Gmail)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Trimite raportul prin..."));

        } catch (java.io.IOException e) {
            Log.e(TAG, "Eroare la generarea fișierului CSV", e);
            Toast.makeText(this, "Eroare la scrierea fișierului!", Toast.LENGTH_SHORT).show();
        }
    }
}