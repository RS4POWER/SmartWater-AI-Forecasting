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

        readingDateEditText.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        // Formatăm frumos dd-MM-yyyy
                        String selectedDate = String.format("%02d-%02d-%d", dayOfMonth, (monthOfYear + 1), year1);
                        readingDateEditText.setText(selectedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });

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
        // PASUL 1: Curățăm ecranul ÎNAINTE de a cere date noi
        meterIndexEditText.setText("0");
        consumptionEditText.setText("0");
        readingDateEditText.setText("");
        anomalyWarningTextView.setVisibility(View.GONE);

        getMonthRef(houseNumber, year, month).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // PASUL 2: Dacă găsim date, le punem pe ecran
                Long index = doc.getLong("Starea Apometrului");
                meterIndexEditText.setText(index != null ? String.valueOf(index) : "0");

                Long consum = doc.getLong("Consumatia mc");
                consumptionEditText.setText(consum != null ? String.valueOf(consum) : "0");

                String dataCitire = doc.getString("Data citire");
                readingDateEditText.setText(dataCitire != null ? dataCitire : "");

                if (consum != null) checkConsumptionAnomaly(consum);
            } else {
                // PASUL 3: Dacă documentul NU există, lăsăm valorile pe 0 (deja setate mai sus)
                Log.d(TAG, "Nu există date pentru " + monthNames[month] + " " + year);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Eroare la încărcare: " + e.getMessage());
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
    // Modificăm semnătura: primim int monthIndex (0-11)
    private DocumentReference getMonthRef(long houseNumber, int year, int monthIndex) {
        // Ne asigurăm că indexul nu iese din limite (siguranță de Master)
        if (monthIndex < 0) monthIndex = 0;
        if (monthIndex > 11) monthIndex = 11;

        String monthName = monthNames[monthIndex]; // Transformăm 1 în "Februarie"

        return db.collection("zones").document(zoneName)
                .collection("numereCasa").document("Numarul " + houseNumber)
                .collection("consumApa").document(String.valueOf(year))
                .collection("lunile").document(monthName);
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
        String diameterStr = diameterEditText.getText().toString().trim();
        long diameterValue = 0;

        if (!diameterStr.isEmpty()) {
            try {
                diameterValue = Long.parseLong(diameterStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Diametrul trebuie să fie un număr!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("Apometru marca", brandEditText.getText().toString());
        data.put("Seria", serialEditText.getText().toString());
        data.put("Diametru apometru", diameterValue);
        data.put("Instalat la", installationDateEditText.getText().toString());

        // Folosim .set(..., merge) și aici pentru siguranță maximă
        db.collection("zones").document(zoneName).collection("numereCasa")
                .document("Numarul " + houseNumber)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Specificații apometru salvate!", Toast.LENGTH_SHORT).show());
    }

    private void saveApometruDetails() {
        String indexStr = meterIndexEditText.getText().toString().trim();
        String manualDate = readingDateEditText.getText().toString().trim();
        int actualYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

        // 1. Verificare an viitor (Păstrăm siguranța!)
        if (currentYear > actualYear) {
            Toast.makeText(this, "⚠️ Nu poți salva date pentru viitor!", Toast.LENGTH_LONG).show();
            return;
        }

        // 2. Validare Index (Nu lăsăm câmpuri goale)
        if (indexStr.isEmpty()) {
            Toast.makeText(this, "Introdu Indexul Apometrului!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Logica pentru DATA (Format dd-MM-yyyy + Default la azi)
        String dateToSave;
        if (manualDate.isEmpty()) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
            dateToSave = sdf.format(new java.util.Date());
        } else {
            dateToSave = manualDate;
        }

        try {
            long indexValue = Long.parseLong(indexStr);
            String consStr = consumptionEditText.getText().toString().trim();
            long consumptionValue = consStr.isEmpty() ? 0 : Long.parseLong(consStr);

            // 4. Pregătim datele pentru Firebase
            Map<String, Object> data = new HashMap<>();
            data.put("Starea Apometrului", indexValue);
            data.put("Consumatia mc", consumptionValue);
            data.put("Data citire", dateToSave);

            // 5. Salvarea efectivă cu .set(..., merge)
            getMonthRef(houseNumber, currentYear, currentMonth)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Salvare reușită: " + dateToSave, Toast.LENGTH_SHORT).show();
                        readingDateEditText.setText(dateToSave); // Punem data în UI dacă era gol
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Eroare Firebase: " + e.getMessage());
                        Toast.makeText(this, "Eroare permisiuni/rețea!", Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Te rugăm să introduci doar cifre!", Toast.LENGTH_LONG).show();
        }
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

    // Variabilă pentru a limita căutarea (să nu caute la infinit dacă e casă nouă)
    private int searchDepth = 0;

    private void calculateAndSaveConsum() {
        searchDepth = 0; // Resetăm adâncimea de căutare

        // Luăm indexul curent introdus de muncitor
        String currentIdxStr = meterIndexEditText.getText().toString().trim();
        if (currentIdxStr.isEmpty()) {
            Toast.makeText(this, "Introdu indexul actual întâi!", Toast.LENGTH_SHORT).show();
            return;
        }
        long currentIndex = Long.parseLong(currentIdxStr);

        // Începem căutarea în spate, pornind de la luna trecută
        int prevMonth = currentMonth - 1;
        int prevYear = currentYear;
        if (prevMonth < 0) { prevMonth = 11; prevYear--; }

        findLastReadingRecursive(prevYear, prevMonth, currentIndex);
    }

    private void findLastReadingRecursive(int year, int month, long currentIndex) {
        searchDepth++;
        // Limităm căutarea la ultimele 24 de luni (2 ani)
        if (searchDepth > 24) {
            finalizeConsumption(currentIndex, 0); // Nu am găsit nimic în 2 ani, presupunem 0
            return;
        }

        getMonthRef(houseNumber, year, month).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("Starea Apometrului")) {
                long lastIndex = doc.getLong("Starea Apometrului");
                finalizeConsumption(currentIndex, lastIndex);
            } else {
                // Nu am găsit în această lună, mergem o lună mai în spate
                int nextYear = year;
                int nextMonth = month - 1;
                if (nextMonth < 0) { nextMonth = 11; nextYear--; }
                findLastReadingRecursive(nextYear, nextMonth, currentIndex);
            }
        });
    }

    private void finalizeConsumption(long current, long last) {
        long result = Math.max(0, current - last);
        consumptionEditText.setText(String.valueOf(result));

        // Salvăm în Firebase
        getMonthRef(houseNumber, currentYear, currentMonth).update("Consumatia mc", result)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Consum calculat față de ultima citire găsită!", Toast.LENGTH_SHORT).show();
                    checkConsumptionAnomaly(result);
                });
    }
}