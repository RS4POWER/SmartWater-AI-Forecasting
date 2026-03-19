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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeterReadingActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

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
    private TextView textPrediction, textAIStatus;
    private double aiPredictedValue = 0;
    private View aiDividerView;
    private Button btnVerifyAI;
    private boolean isAIVerifiedOnce = false;

    private ArrayList<Double> lastFetchedHistory = new ArrayList<>();
    public String[] monthNames = {"Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
            "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"};

    private static final String TAG = "MeterReadingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_reading);

        // UI Initialization
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
        previousMonthButton = findViewById(R.id.prevMonthButton);
        nextMonthButton = findViewById(R.id.nextMonthButton);

        editModeButton = findViewById(R.id.editModeButton);
        cameraOcrButton = findViewById(R.id.cameraOcrButton);
        runAnalyticsButton = findViewById(R.id.runAnalyticsButton);

        textPrediction = findViewById(R.id.textPrediction);
        textAIStatus = findViewById(R.id.textAIStatus);

        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        currentMonth = Calendar.getInstance().get(Calendar.MONTH);

        aiDividerView = findViewById(R.id.aiDividerView);
        btnVerifyAI = findViewById(R.id.btnVerifyAI);

        handleIntentData();
        updateDateDisplay();
        setupClickListeners();

        loadHouseDetails(houseNumber, zoneName);
        loadApometruDetails(houseNumber, currentYear, currentMonth);
        toggleEditMode(false);

        findViewById(R.id.btnShowChart).setOnClickListener(v -> {
            if (lastFetchedHistory.isEmpty()) {
                Toast.makeText(this, "Nu avem destule date pentru grafic!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ForecastingViewActivity.class);
            intent.putExtra("HISTORY_DATA", lastFetchedHistory);
            startActivity(intent);
        });

        fetchHistoryAndRunAI();

        btnVerifyAI = findViewById(R.id.btnVerifyAI);

        btnVerifyAI.setOnClickListener(v -> {
            if (!isAIVerifiedOnce) {
                // Pasul 1: Double Check
                btnVerifyAI.setText("EȘTI SIGUR?");
                btnVerifyAI.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));
                isAIVerifiedOnce = true;
            } else {
                // Pasul 2: Salvare finală
                saveAIFeedback("confirmat_normal");
                btnVerifyAI.setText("CONFIRMAT ✅");
                btnVerifyAI.setEnabled(false); // Îl dezactivăm să nu mai poată apăsa
                btnVerifyAI.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));

                // Re-colorăm totul în verde pentru că muncitorul a zis că e OK
                updateAIUI("✔ VALIDAT DE OPERATOR", android.graphics.Color.parseColor("#2E7D32"), "#F8FBFF");
            }
        });
    }

    private void saveAIFeedback(String status) {
        getMonthRef(houseNumber, currentYear, currentMonth)
                .update("ai_feedback", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "AI a învățat acest consum pentru viitor!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Eroare feedback AI: " + e.getMessage()));
    }


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
            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        String selectedDate = String.format("%02d-%02d-%d", dayOfMonth, (monthOfYear + 1), year1);
                        readingDateEditText.setText(selectedDate);
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
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
            String currentConsum = consumptionEditText.getText().toString();
            if (!currentConsum.isEmpty() && !currentConsum.equals("0")) {
                compareActualWithAI(Double.parseDouble(currentConsum));
            }
        });

        cameraOcrButton.setOnClickListener(v -> requestCameraPermission());
        runAnalyticsButton.setOnClickListener(v -> calculateAndSaveConsum());


    }

    private void refresh() {
        updateDateDisplay();
        loadHouseDetails(houseNumber, zoneName);
        loadApometruDetails(houseNumber, currentYear, currentMonth);
        fetchHistoryAndRunAI();
    }

    private void updateDateDisplay() {
        currentDateTextView.setText(monthNames[currentMonth] + ", " + currentYear);
    }

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
        meterIndexEditText.setText("0");
        consumptionEditText.setText("0");
        readingDateEditText.setText("");

        // RESETĂM AI-UL LA NEUTRU când intrăm pe o lună nouă
        resetAIUI();

        getMonthRef(houseNumber, year, month).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Long index = doc.getLong("Starea Apometrului");
                meterIndexEditText.setText(index != null ? String.valueOf(index) : "0");
                Long consum = doc.getLong("Consumatia mc");
                consumptionEditText.setText(consum != null ? String.valueOf(consum) : "0");
                readingDateEditText.setText(doc.getString("Data citire"));

                // DOAR dacă avem un consum real (peste 0), rulăm AI-ul
                if (consum != null && consum > 0) {
                    compareActualWithAI((double) consum);
                }
            }
        });
    }

    // O metodă mică să curățăm cardul AI
    private void resetAIUI() {
        // 1. Resetăm variabila de control pentru Double Check
        isAIVerifiedOnce = false;

        // 2. Resetăm aspectul butonului de verificare
        btnVerifyAI.setEnabled(true);
        btnVerifyAI.setText("VALIDARE CONSUM");
        btnVerifyAI.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4169E1"))); // Înapoi la RoyalBlue
        btnVerifyAI.setVisibility(View.GONE); // Îl ascundem până când compareActualWithAI decide dacă e nevoie de el

        // 3. Resetăm restul textelor
        textAIStatus.setText("Status: Se așteaptă date...");
        textAIStatus.setTextColor(android.graphics.Color.GRAY);
        aiDividerView.setBackgroundColor(android.graphics.Color.GRAY);

        // 4. Resetăm câmpul de consum la designul neutru
        consumptionEditText.setAlpha(1.0f);
        consumptionEditText.setBackgroundResource(R.drawable.border);
        if (consumptionEditText.getBackground() != null) {
            consumptionEditText.getBackground().clearColorFilter();
        }
    }

    private DocumentReference getMonthRef(long houseNumber, int year, int monthIndex) {
        if (monthIndex < 0) monthIndex = 0;
        if (monthIndex > 11) monthIndex = 11;
        return db.collection("zones").document(zoneName)
                .collection("numereCasa").document("Numarul " + houseNumber)
                .collection("consumApa").document(String.valueOf(year))
                .collection("lunile").document(monthNames[monthIndex]);
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
        try {
            data.put("Diametru apometru", Long.parseLong(diameterEditText.getText().toString().trim()));
        } catch (Exception e) { data.put("Diametru apometru", 0); }
        data.put("Instalat la", installationDateEditText.getText().toString());

        db.collection("zones").document(zoneName).collection("numereCasa")
                .document("Numarul " + houseNumber)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Specificații salvate!", Toast.LENGTH_SHORT).show());
    }

    private void saveApometruDetails() {
        try {
            long indexValue = Long.parseLong(meterIndexEditText.getText().toString().trim());
            long consumptionValue = Long.parseLong(consumptionEditText.getText().toString().trim());
            String dateToSave = readingDateEditText.getText().toString().trim();

            if (dateToSave.isEmpty())
                dateToSave = new java.text.SimpleDateFormat("dd-MM-yyyy").format(new java.util.Date());

            // REPARAȚIE: Inițializăm Map-ul corect dintr-o dată
            Map<String, Object> data = new HashMap<>();
            data.put("Starea Apometrului", indexValue);
            data.put("Consumatia mc", consumptionValue);
            data.put("Data citire", dateToSave);

            // AICI legăm de restul sistemului: Salvăm timpul precis al citirii
            data.put("timestamp", com.google.firebase.Timestamp.now());

            getMonthRef(houseNumber, currentYear, currentMonth)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Salvare reușită!", Toast.LENGTH_SHORT).show();

                        // --- INTEGRARE CU PERFORMANCE TRACKING ---
                        // Notificăm RouteTracker că am terminat de citit această casă
                        RouteTracker.updateLastHouse(this, "Numarul " + houseNumber);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Eroare salvare: " + e.getMessage()));

        } catch (Exception e) {
            Toast.makeText(this, "Eroare la date! Verifică cifrele.", Toast.LENGTH_SHORT).show();
        }
    }

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
        String currentIdxStr = meterIndexEditText.getText().toString().trim();
        if (currentIdxStr.isEmpty()) return;
        long currentIndex = Long.parseLong(currentIdxStr);

        int prevMonth = currentMonth - 1;
        int prevYear = currentYear;
        if (prevMonth < 0) { prevMonth = 11; prevYear--; }

        searchDepth = 0;
        findLastReadingRecursive(prevYear, prevMonth, currentIndex);
    }

    private int searchDepth = 0;
    private void findLastReadingRecursive(int year, int month, long currentIndex) {
        searchDepth++;
        if (searchDepth > 24) { finalizeConsumption(currentIndex, 0); return; }

        getMonthRef(houseNumber, year, month).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("Starea Apometrului")) {
                finalizeConsumption(currentIndex, doc.getLong("Starea Apometrului"));
            } else {
                int nextYear = year, nextMonth = month - 1;
                if (nextMonth < 0) { nextMonth = 11; nextYear--; }
                findLastReadingRecursive(nextYear, nextMonth, currentIndex);
            }
        });
    }

    private void finalizeConsumption(long current, long last) {
        long result = Math.max(0, current - last);
        consumptionEditText.setText(String.valueOf(result));
        getMonthRef(houseNumber, currentYear, currentMonth).update("Consumatia mc", result)
                .addOnSuccessListener(aVoid -> compareActualWithAI((double) result));
    }

    private void runAIForecast(List<Double> consumHistory) {
        aiPredictedValue = ForecastingEngine.predictNextConsumption(consumHistory);
        textPrediction.setText(String.format("Predicție AI: %.2f m³", aiPredictedValue));
        textAIStatus.setText("Trend: Analiză bazată pe ultimele " + consumHistory.size() + " luni");
    }

    private void compareActualWithAI(double consumIntrodus) {
        if (aiPredictedValue <= 0) return;

        double diferentaAbsoluta = Math.abs(consumIntrodus - aiPredictedValue);
        double deviatieProcentuala = (diferentaAbsoluta / aiPredictedValue) * 100;

        // Resetăm culorile la default
        int colorVerde = android.graphics.Color.parseColor("#2E7D32");
        int colorGalben = android.graphics.Color.parseColor("#FFC107");
        int colorRosu = android.graphics.Color.RED;

        if (deviatieProcentuala < 20 || diferentaAbsoluta < 3) {
            // VERDE - Totul e ok
            updateAIUI("✔ NORMAL", colorVerde, "#F8FBFF");
        } else if (deviatieProcentuala >= 20 && deviatieProcentuala <= 50) {
            // GALBEN - Ceva e suspect
            updateAIUI("⚠ ATIPIC (+" + (int)deviatieProcentuala + "%)", colorGalben, "#FFFDE7");
        } else {
            // ROȘU - Probabil avarie
            updateAIUI("🚨 ANOMALIE (+" + (int)deviatieProcentuala + "%)", colorRosu, "#FFF1F1");
        }
    }
    private void updateAIUI(String status, int mainColor, String bgColor) {
        textAIStatus.setText(status);
        textAIStatus.setTextColor(mainColor);
        aiDividerView.setBackgroundColor(mainColor);
        findViewById(R.id.cardAI).setBackgroundColor(android.graphics.Color.parseColor(bgColor));

        // Creăm un GradientDrawable NOU de la zero pentru fundal
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);

        // NOTE: Aici pune corner radius-ul pe care l-ai avut în drawable/border
        // Am presupun că e 15dp ca în alte elemente, dar ajustează dacă e nevoie.
        float cornerRadiusInDp = 15f;
        float density = getResources().getDisplayMetrics().density;
        gd.setCornerRadius(cornerRadiusInDp * density);

        // LOGICA DESIGN: Viu sau Spălăcit (Faded)
        int colorWithAlpha;
        if (!isInEditMode) {
            // Non-Edit Mode: Fundal și Text mai transparente (spălăcite)
            colorWithAlpha = (android.graphics.Color.parseColor(bgColor) & 0x00FFFFFF) | 0x44000000; // 44 în hex e aprox 25% opacitate
            consumptionEditText.setAlpha(0.6f); // Textul devine și el un pic transparent
        } else {
            // Edit Mode: Fundal și Text în culori pline, vii
            colorWithAlpha = android.graphics.Color.parseColor(bgColor);
            consumptionEditText.setAlpha(1.0f); // Text plin
        }

        gd.setColor(colorWithAlpha);

        // Setăm CONTURUL RoyalBlue persistent
        gd.setStroke(3, android.graphics.Color.parseColor("#4169E1"));

        // Aplicăm drawable-ul NOU pe câmp
        consumptionEditText.setBackground(gd);

        // Setăm și culoarea textului în mod corespunzător
        consumptionEditText.setTextColor(mainColor);
    }

    private void fetchHistoryAndRunAI() {
        int monthsToBoard = 6;
        aiPredictedValue = 0;
        resetAIUI();
        List<Task<DocumentSnapshot>> tasks = new java.util.ArrayList<>();
        int tempMonth = currentMonth, tempYear = currentYear;

        for (int i = 0; i < monthsToBoard; i++) {
            tempMonth--;
            if (tempMonth < 0) { tempMonth = 11; tempYear--; }
            tasks.add(getMonthRef(houseNumber, tempYear, tempMonth).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            lastFetchedHistory.clear();
            for (Object res : results) {
                DocumentSnapshot doc = (DocumentSnapshot) res;
                if (doc.exists() && doc.contains("Consumatia mc")) {
                    lastFetchedHistory.add(doc.getDouble("Consumatia mc"));
                }
            }

            java.util.Collections.reverse(lastFetchedHistory);

            if (lastFetchedHistory.size() >= 2) {
                // 3. Calculăm predicția NOUĂ pentru luna curentă
                runAIForecast(lastFetchedHistory);

                // 4. Abia ACUM facem comparația cu consumul de pe ecran
                String currentConsumStr = consumptionEditText.getText().toString();
                if (!currentConsumStr.isEmpty() && !currentConsumStr.equals("0")) {
                    compareActualWithAI(Double.parseDouble(currentConsumStr));
                }
            } else {
                textPrediction.setText("AI: Date insuficiente");
                textAIStatus.setText("Sunt necesare minim 2 luni de istoric.");
            }
        });
    }
}