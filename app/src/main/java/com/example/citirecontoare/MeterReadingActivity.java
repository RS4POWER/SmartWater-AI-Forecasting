package com.example.citirecontoare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MeterReadingActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // UI Elements - Potrivite fix cu ID-urile din XML-ul tau verde
    private TextView ownerNameTextView, houseNumberTextView, currentDateTextView, anomalyWarningTextView;
    private EditText brandEditText, serialEditText, diameterEditText, installationDateEditText;
    private EditText meterIndexEditText, consumptionEditText, readingDateEditText;

    private ImageButton backButton, previousYearButton, nextYearButton;
    private ImageButton editModeButton, cameraOcrButton, runAnalyticsButton;
    private Button previousMonthButton, nextMonthButton;

    private boolean isInEditMode = false;
    private int currentYear, currentMonth;
    private Long houseNumber;
    private String zoneName;

    public String[] monthNames = {"Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
            "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"};

    private static final String TAG = "MeterReadingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_reading);

        // 1. Initializare UI (Stingem "rosul" prin legarea corecta a ID-urilor)
        ownerNameTextView = findViewById(R.id.ownerNameTextView);
        houseNumberTextView = findViewById(R.id.houseNumberTextView);
        currentDateTextView = findViewById(R.id.currentDateTextView);
        anomalyWarningTextView = findViewById(R.id.anomalyWarningTextView);

        brandEditText = findViewById(R.id.brandEditText);
        serialEditText = findViewById(R.id.serialEditText);
        diameterEditText = findViewById(R.id.diameterEditText);
        installationDateEditText = findViewById(R.id.installationDateEditText);
        meterIndexEditText = findViewById(R.id.meterIndexEditText);
        consumptionEditText = findViewById(R.id.consumptionEditText);
        readingDateEditText = findViewById(R.id.readingDateEditText);

        backButton = findViewById(R.id.backButton);
        previousYearButton = findViewById(R.id.previousYearButton);
        nextYearButton = findViewById(R.id.nextYearButton);
        previousMonthButton = findViewById(R.id.prevMonthButton); // ID-ul din noul XML
        nextMonthButton = findViewById(R.id.nextMonthButton);

        editModeButton = findViewById(R.id.editModeButton);
        cameraOcrButton = findViewById(R.id.cameraOcrButton);
        runAnalyticsButton = findViewById(R.id.runAnalyticsButton);

        // 2. Setup Data & Intent
        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        currentMonth = Calendar.getInstance().get(Calendar.MONTH);

        handleIntentData();
        updateDateDisplay();
        setupClickListeners();

        // 3. Load Initial Data
        loadHouseDetails(houseNumber, zoneName);
        loadApometruDetails(houseNumber, currentYear, currentMonth);
        toggleEditMode(false);
    } // AICI se inchide corect onCreate!

    private void handleIntentData() {
        Intent intent = getIntent();
        houseNumber = intent.getLongExtra("HOUSE_NUMBER", -1);
        zoneName = intent.getStringExtra("ZONE_NAME");
        if (houseNumber == -1 || zoneName == null) {
            Toast.makeText(this, "Error loading house data!", Toast.LENGTH_SHORT).show();
            finish();
        }
        houseNumberTextView.setText("NR " + houseNumber);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        previousYearButton.setOnClickListener(v -> { currentYear--; refresh(); });
        nextYearButton.setOnClickListener(v -> { currentYear++; refresh(); });

        previousMonthButton.setOnClickListener(v -> {
            if (currentMonth > 0) currentMonth--;
            else { currentMonth = 11; currentYear--; }
            refresh();
        });

        nextMonthButton.setOnClickListener(v -> {
            if (currentMonth < 11) currentMonth++;
            else { currentMonth = 0; currentYear++; }
            refresh();
        });

        editModeButton.setOnClickListener(v -> {
            isInEditMode = !isInEditMode;
            editModeButton.setBackgroundResource(isInEditMode ? R.drawable.baseline_done_outline_24 : R.drawable.baseline_edit_note_24);
            if (!isInEditMode) {
                saveHouseDetails();
                saveApometruDetails();
            }
            toggleEditMode(isInEditMode);
        });

        cameraOcrButton.setOnClickListener(v -> requestCameraPermission());
        runAnalyticsButton.setOnClickListener(v -> calculateAndSaveConsum());
    }

    private void refresh() {
        updateDateDisplay();
        loadHouseDetails(houseNumber, zoneName);
        loadApometruDetails(houseNumber, currentYear, currentMonth);
    }

    private void updateDateDisplay() {
        currentDateTextView.setText(monthNames[currentMonth] + ", " + currentYear);
    }

    // --- Firebase Logic ---

    private void loadHouseDetails(Long houseNumber, String zoneName) {
        db.collection("zones").document(zoneName).collection("numereCasa")
                .document("Numarul " + houseNumber).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        ownerNameTextView.setText(doc.getString("Proprietar"));
                        brandEditText.setText(doc.getString("Apometru marca"));
                        serialEditText.setText(doc.getString("Seria"));
                        diameterEditText.setText(String.valueOf(doc.get("Diametru apometru")));
                        installationDateEditText.setText(doc.getString("Instalat la"));
                    }
                });
    }

    private void loadApometruDetails(Long houseNumber, int year, int month) {
        getMonthRef(houseNumber, year, month).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Long index = doc.getLong("Starea Apometrului");
                meterIndexEditText.setText(index != null ? String.valueOf(index) : "0");

                Long consum = doc.getLong("Consumatia mc");
                consumptionEditText.setText(consum != null ? String.valueOf(consum) : "0");
                readingDateEditText.setText(doc.getString("Data citire"));

                // MASTER FEATURE: Verificam anomalii de consum
                if (consum != null) checkConsumptionAnomaly(consum);
            }
        });
    }

    private void checkConsumptionAnomaly(long currentConsum) {
        // Logica de predictie: daca consumul sare de 20mc, e alerta de avarie
        // Nota: Aici vei dezvolta in capitolul 5 algoritmul tau de cercetare
        if (currentConsum > 20) {
            anomalyWarningTextView.setVisibility(View.VISIBLE);
            anomalyWarningTextView.setText("⚠ Warning: High consumption detected (" + currentConsum + " m³)");
        } else {
            anomalyWarningTextView.setVisibility(View.GONE);
        }
    }

    private DocumentReference getMonthRef(Long houseNumber, int year, int month) {
        if (month < 0) { year--; month = 11; }
        return db.collection("zones").document(zoneName).collection("numereCasa")
                .document("Numarul " + houseNumber).collection("consumApa")
                .document(String.valueOf(year)).collection("lunile").document(monthNames[month]);
    }

    private void toggleEditMode(boolean mode) {
        brandEditText.setEnabled(mode);
        serialEditText.setEnabled(mode);
        diameterEditText.setEnabled(mode);
        installationDateEditText.setEnabled(mode);
        meterIndexEditText.setEnabled(mode);
        consumptionEditText.setEnabled(mode);
        readingDateEditText.setEnabled(mode);
    }

    private void saveHouseDetails() {
        Map<String, Object> data = new HashMap<>();
        data.put("Apometru marca", brandEditText.getText().toString());
        data.put("Seria", serialEditText.getText().toString());
        data.put("Diametru apometru", Long.parseLong(diameterEditText.getText().toString()));
        data.put("Instalat la", installationDateEditText.getText().toString());
        db.collection("zones").document(zoneName).collection("numereCasa").document("Numarul " + houseNumber).update(data);
    }

    private void saveApometruDetails() {
        Map<String, Object> data = new HashMap<>();
        data.put("Starea Apometrului", Long.parseLong(meterIndexEditText.getText().toString()));
        data.put("Consumatia mc", Long.parseLong(consumptionEditText.getText().toString()));
        data.put("Data citire", readingDateEditText.getText().toString());
        getMonthRef(houseNumber, currentYear, currentMonth).update(data);
    }

    // --- AI & OCR Section ---

    private void requestCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        } else { startCamera(); }
    }

    private void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bitmap bp = (Bitmap) data.getExtras().get("data");
            recognizeText(bp);
        }
    }

    private void recognizeText(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image).addOnSuccessListener(result -> {
            for (Text.TextBlock block : result.getTextBlocks()) {
                if (block.getText().matches("\\d+")) {
                    meterIndexEditText.setText(block.getText());
                    break;
                }
            }
        });
    }

    private void calculateAndSaveConsum() {
        getMonthRef(houseNumber, currentYear, currentMonth).get().addOnSuccessListener(curr -> {
            getMonthRef(houseNumber, currentYear, currentMonth - 1).get().addOnSuccessListener(prev -> {
                long c = curr.getLong("Starea Apometrului") != null ? curr.getLong("Starea Apometrului") : 0;
                long p = prev.exists() && prev.getLong("Starea Apometrului") != null ? prev.getLong("Starea Apometrului") : 0;
                long res = c - p;
                consumptionEditText.setText(String.valueOf(res));
                getMonthRef(houseNumber, currentYear, currentMonth).update("Consumatia mc", res);
                checkConsumptionAnomaly(res);
            });
        });
    }
}