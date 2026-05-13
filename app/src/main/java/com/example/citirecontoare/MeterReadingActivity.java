package com.example.citirecontoare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
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

    private androidx.camera.core.ImageCapture imageCapture;
    private androidx.camera.core.Camera camera;

    private ArrayList<Double> lastFetchedHistory = new ArrayList<>();
    public String[] monthNames = {"Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
            "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"};

    private static final String TAG = "MeterReadingActivity";

    private String currentReadingSource = "manual"; // Default este manual

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_reading);

        initializeUI();

        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        currentMonth = Calendar.getInstance().get(Calendar.MONTH);

        handleIntentData();
        updateDateDisplay();
        loadHouseDetails(houseNumber, zoneName);
        loadApometruDetails(houseNumber, currentYear, currentMonth);
        toggleEditMode(false);

        fetchHistoryAndRunAI();
    }

    private void initializeUI() {
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
        aiDividerView = findViewById(R.id.aiDividerView);
        btnVerifyAI = findViewById(R.id.btnVerifyAI);

        setupClickListeners();
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        previousYearButton.setOnClickListener(v -> { currentYear--; refresh(); });
        nextYearButton.setOnClickListener(v -> { currentYear++; refresh(); });

        readingDateEditText.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
                readingDateEditText.setText(String.format("%02d-%02d-%d", dayOfMonth, (monthOfYear + 1), year1));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
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

            editModeButton.setBackgroundResource(
                    isInEditMode ? R.drawable.baseline_done_outline_24 : R.drawable.baseline_edit_note_24
            );

            if (!isInEditMode) {
                saveHouseDetails();
                saveApometruDetails();
            }

            toggleEditMode(isInEditMode);

            // 🔥 DOAR O SINGURĂ DATĂ, SAFE
            String currentConsumStr = consumptionEditText.getText().toString().trim();
            if (!currentConsumStr.isEmpty() && !currentConsumStr.equals("0")) {
                try {
                    double consum = Double.parseDouble(currentConsumStr);
                    compareActualWithAI(consum);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Format consum invalid: " + currentConsumStr);
                }
            }
        });

        cameraOcrButton.setOnClickListener(v -> requestCameraPermission());
        runAnalyticsButton.setOnClickListener(v -> calculateAndSaveConsum());


        findViewById(R.id.btnShowChart).setOnClickListener(v -> {
            if (lastFetchedHistory.isEmpty()) {
                Toast.makeText(this, "Nu avem destule date pentru grafic!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ForecastingViewActivity.class);
            intent.putExtra("HISTORY_DATA", lastFetchedHistory);
            startActivity(intent);
        });

        btnVerifyAI.setOnClickListener(v -> {
            if (!isAIVerifiedOnce) {
                btnVerifyAI.setText("EȘTI SIGUR?");
                btnVerifyAI.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));
                isAIVerifiedOnce = true;
            } else {
                saveAIFeedback("confirmat_normal");
                btnVerifyAI.setText("CONFIRMAT ✅");
                btnVerifyAI.setEnabled(false);
                btnVerifyAI.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
                updateAIUI("✔ VALIDAT DE OPERATOR", android.graphics.Color.parseColor("#2E7D32"), "#F8FBFF");
            }

        });
    }

    // Pune codul ăsta undeva să-l rulezi o dată:


    private void refresh() {
        updateDateDisplay();
        loadHouseDetails(houseNumber, zoneName);
        loadApometruDetails(houseNumber, currentYear, currentMonth);
        fetchHistoryAndRunAI();
        currentReadingSource = "manual";
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
        resetAIUI();

        getMonthRef(houseNumber, year, month).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {

                Object indexObj = doc.get("Starea Apometrului");
                Number index = indexObj instanceof Number ? (Number) indexObj : null;
                meterIndexEditText.setText(index != null ? String.valueOf(index.longValue()) : "0");

                Object consumObj = doc.get("Consumatia mc");
                Number consum = consumObj instanceof Number ? (Number) consumObj : null;
                consumptionEditText.setText(consum != null ? String.valueOf(consum.longValue()) : "0");

                readingDateEditText.setText(doc.getString("Data citire"));

                if (consum != null && consum.doubleValue() > 0) {
                    compareActualWithAI(consum.doubleValue());
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Eroare încărcare detalii apometru", e);
            Toast.makeText(this, "Eroare la încărcarea citirii!", Toast.LENGTH_SHORT).show();
        });
    }

    private void resetAIUI() {
        isAIVerifiedOnce = false;
        btnVerifyAI.setEnabled(true);
        btnVerifyAI.setText("VALIDARE CONSUM");
        btnVerifyAI.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4169E1")));
        btnVerifyAI.setVisibility(View.GONE);

        textAIStatus.setText("Status: Se așteaptă date...");
        textAIStatus.setTextColor(android.graphics.Color.GRAY);
        aiDividerView.setBackgroundColor(android.graphics.Color.GRAY);

        meterIndexEditText.setBackgroundResource(R.drawable.border);

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
    private long parseLongFromUi(String value) throws NumberFormatException {
        if (value == null) throw new NumberFormatException("null");

        value = value.trim().replace(",", ".");

        double parsed = Double.parseDouble(value);
        return Math.round(parsed);
    }
    private void saveApometruDetails() {
        try {
            // Preluăm valorile proaspete din UI
            String indexStr = meterIndexEditText.getText().toString().trim();
            String consumStr = consumptionEditText.getText().toString().trim();

            if (indexStr.isEmpty() || consumStr.isEmpty()) {
                Toast.makeText(this, "Completează datele înainte de salvare!", Toast.LENGTH_SHORT).show();
                return;
            }

            long indexValue = parseLongFromUi(indexStr);
            long actualConsumption = parseLongFromUi(consumStr);
            String dateToSave = readingDateEditText.getText().toString().trim();

            if (dateToSave.isEmpty())
                dateToSave = new java.text.SimpleDateFormat("dd-MM-yyyy").format(new java.util.Date());

            // --- CONSTRUCȚIA DOCUMENTULUI ---
            Map<String, Object> data = new HashMap<>();
            data.put("schema_version", 2); // 🔥 Versiunea 2 (Master Level)
            data.put("Starea Apometrului", indexValue);
            data.put("Consumatia mc", actualConsumption);
            data.put("Data citire", dateToSave);
            data.put("timestamp", com.google.firebase.Timestamp.now());
            data.put("reading_source", currentReadingSource);

            // METRICI AI
            double aiPrediction = aiPredictedValue;
            double errorAbs = Math.abs(actualConsumption - aiPrediction);
            double errorPercent = (aiPrediction > 0.1) ? (errorAbs / aiPrediction) * 100 : 0;

            data.put("ai_prediction", aiPrediction);
            data.put("ai_error_abs", errorAbs);
            data.put("ai_error_percent", errorPercent);
            data.put("ai_status", (errorPercent > 20) ? "anomalie" : "normal");

            // METRICI OPERATOR
            if (RouteTracker.isTracking(this)) {
                long now = System.currentTimeMillis();
                long last = RouteTracker.getLastTimestamp(this);
                long routeStart = RouteTracker.getStartTime(this);
                if (last == 0) last = routeStart;

                long deltaSec = (now - last) / 1000;
                data.put("time_since_last_house_sec", deltaSec);
                data.put("route_elapsed_sec", (now - routeStart) / 1000);
                data.put("timing_valid", deltaSec < 600); // Sub 10 min e valid
                data.put("is_pro_reading", true);

                RouteTracker.saveLastTimestamp(this, now);
            } else {
                data.put("is_pro_reading", false);
            }

            // --- SINGURA SCRIERE ÎN FIRESTORE ---
            getMonthRef(houseNumber, currentYear, currentMonth)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Salvare reușită (v2)!", Toast.LENGTH_SHORT).show();
                        RouteTracker.updateLastHouse(this, "Numarul " + houseNumber);
                        currentReadingSource = "manual"; // Reset
                        meterIndexEditText.setBackgroundResource(R.drawable.border);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Eroare Firestore: " + e.getMessage()));

        } catch (Exception e) {
            Toast.makeText(this, "Eroare la procesarea cifrelor!", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            startCameraX();
        }
    }

    private void startCameraX() {
        setContentView(R.layout.activity_ocr_camera);



        androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                androidx.camera.lifecycle.ProcessCameraProvider cameraProvider =
                        androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this).get();


                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
                preview.setSurfaceProvider(((androidx.camera.view.PreviewView)findViewById(R.id.viewFinder)).getSurfaceProvider());

                imageCapture = new androidx.camera.core.ImageCapture.Builder()
                        .setCaptureMode(androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();


                androidx.camera.core.CameraSelector cameraSelector =
                        androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                camera.getCameraControl().setLinearZoom(0.35f);

                findViewById(R.id.btnFlashToggle).setOnClickListener(v -> toggleFlash());
                findViewById(R.id.btnCapture).setOnClickListener(v -> takePhotoAndProcess());

            } catch (Exception e) {
                Log.e(TAG, "Eroare pornire CameraX: " + e.getMessage());
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(this));
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraX();
            } else {
                Toast.makeText(this, "Permisiunea pentru cameră este necesară pentru OCR.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            boolean isFlashOn = camera.getCameraInfo().getTorchState().getValue() == androidx.camera.core.TorchState.ON;
            camera.getCameraControl().enableTorch(!isFlashOn);
        }
    }

    private void takePhotoAndProcess() {
        if (imageCapture == null) return;

        imageCapture.takePicture(androidx.core.content.ContextCompat.getMainExecutor(this),
                new androidx.camera.core.ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull androidx.camera.core.ImageProxy imageProxy) {
                        Bitmap bitmap = imageProxyToBitmap(imageProxy);
                        imageProxy.close();

                        if (bitmap == null) {
                            Toast.makeText(MeterReadingActivity.this, "Eroare la capturarea imaginii!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Bitmap croppedBitmap = cropToMeterWindow(bitmap);

                        setContentView(R.layout.activity_meter_reading);
                        initializeUI();

                        handleIntentData();
                        updateDateDisplay();
                        loadHouseDetails(houseNumber, zoneName);
                        resetAIUI();
                        fetchHistoryAndRunAI();

                        recognizeText(croppedBitmap);
                    }
                    @Override
                    public void onError(@NonNull androidx.camera.core.ImageCaptureException exception) {
                        Log.e(TAG, "Eroare captură OCR", exception);
                        Toast.makeText(MeterReadingActivity.this, "Eroare la capturarea imaginii!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Bitmap imageProxyToBitmap(androidx.camera.core.ImageProxy image) {
        androidx.camera.core.ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        java.nio.ByteBuffer buffer = plane.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void recognizeText(Bitmap bitmap) {
        Bitmap processed = preProcessBitmap(bitmap);

        InputImage image = InputImage.fromBitmap(processed, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image).addOnSuccessListener(result -> {
            boolean found = false;

            for (Text.TextBlock block : result.getTextBlocks()) {
                String rawText = block.getText();
                String finalIndex = sanitizeMeterIndex(rawText);

                if (!finalIndex.isEmpty()) {
                    if (!isInEditMode) {
                        isInEditMode = true;
                        toggleEditMode(true);
                        editModeButton.setBackgroundResource(R.drawable.baseline_done_outline_24);
                    }

                    meterIndexEditText.setText(finalIndex);
                    meterIndexEditText.setBackgroundColor(android.graphics.Color.parseColor("#FFF59D"));

                    currentReadingSource = "ocr";

                    calculateAndSaveConsum();

                    Toast.makeText(this, "Verifică indexul marcat cu galben!", Toast.LENGTH_LONG).show();
                    found = true;
                    break;
                }
            }

            if (!found) {
                Toast.makeText(this, "Nu am putut citi clar. Apropie camera!", Toast.LENGTH_SHORT).show();
            }

            recognizer.close();

        }).addOnFailureListener(e -> {
            Log.e(TAG, "ML Kit Error", e);
            Toast.makeText(this, "Eroare OCR!", Toast.LENGTH_SHORT).show();
            recognizer.close();
        });
    }

    private void calculateAndSaveConsum() {
        String currentIdxStr = meterIndexEditText.getText().toString().trim();
        if (currentIdxStr.isEmpty()) return;

        long currentIndex;

        try {
            currentIndex = (long) Double.parseDouble(currentIdxStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Index invalid!", Toast.LENGTH_SHORT).show();
            return;
        }

        int prevMonth = currentMonth - 1;
        int prevYear = currentYear;
        if (prevMonth < 0) {
            prevMonth = 11;
            prevYear--;
        }

        searchDepth = 0;
        findLastReadingRecursive(prevYear, prevMonth, currentIndex);
    }

    private int searchDepth = 0;
    private void findLastReadingRecursive(int year, int month, long currentIndex) {
        searchDepth++;
        if (searchDepth > 24) {
            finalizeConsumption(currentIndex, 0);
            return;
        }

        getMonthRef(houseNumber, year, month).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("Starea Apometrului")) {

                Object lastIndexObj = doc.get("Starea Apometrului");
                Number lastIndex = lastIndexObj instanceof Number ? (Number) lastIndexObj : null;

                if (lastIndex != null) {
                    finalizeConsumption(currentIndex, lastIndex.longValue());
                } else {
                    searchPreviousMonth(year, month, currentIndex);
                }

            } else {
                searchPreviousMonth(year, month, currentIndex);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Eroare căutare index anterior", e);
            finalizeConsumption(currentIndex, 0);
        });
    }

    private void searchPreviousMonth(int year, int month, long currentIndex) {
        int nextYear = year;
        int nextMonth = month - 1;

        if (nextMonth < 0) {
            nextMonth = 11;
            nextYear--;
        }

        findLastReadingRecursive(nextYear, nextMonth, currentIndex);
    }

    private void finalizeConsumption(long current, long last) {
        long result = Math.max(0, current - last);

        // Intrăm automat în Edit Mode ca operatorul să poată salva cu bifa
        if (!isInEditMode) {
            isInEditMode = true;
            toggleEditMode(true);
            editModeButton.setBackgroundResource(R.drawable.baseline_done_outline_24);
        }

        consumptionEditText.setText(String.valueOf(result));

        compareActualWithAI((double) result);

        Toast.makeText(this, "Consum calculat. Verifică și apasă bifa pentru salvare!", Toast.LENGTH_SHORT).show();
    }

    private void runAIForecast(List<Double> consumHistory) {
        aiPredictedValue = ForecastingEngine.predictNextConsumption(consumHistory);
        textPrediction.setText(String.format("Predicție AI: %.2f m³", aiPredictedValue));
        textAIStatus.setText("Trend: Analiză bazată pe ultimele " + consumHistory.size() + " luni");
    }

    private void compareActualWithAI(double consumIntrodus) {
        if (aiPredictedValue <= 0 || Double.isNaN(aiPredictedValue)) return;

        double diferentaAbsoluta = Math.abs(consumIntrodus - aiPredictedValue);
        double deviatieProcentuala = aiPredictedValue > 0.1
                ? (diferentaAbsoluta / aiPredictedValue) * 100
                : 0;

        int colorVerde = android.graphics.Color.parseColor("#2E7D32");
        int colorGalben = android.graphics.Color.parseColor("#FFC107");
        int colorRosu = android.graphics.Color.RED;

        if (deviatieProcentuala < 20 || diferentaAbsoluta < 3) {
            updateAIUI("✔ NORMAL", colorVerde, "#F8FBFF");
            btnVerifyAI.setVisibility(View.GONE);

        } else if (deviatieProcentuala >= 20 && deviatieProcentuala <= 50) {
            updateAIUI("⚠ ATIPIC (+" + (int)deviatieProcentuala + "%)", colorGalben, "#FFFDE7");
            btnVerifyAI.setVisibility(View.VISIBLE);

        } else {
            updateAIUI("🚨 ANOMALIE (+" + (int)deviatieProcentuala + "%)", colorRosu, "#FFF1F1");
            btnVerifyAI.setVisibility(View.VISIBLE);
        }
    }

    private void updateAIUI(String status, int mainColor, String bgColor) {
        textAIStatus.setText(status);
        textAIStatus.setTextColor(mainColor);
        aiDividerView.setBackgroundColor(mainColor);
        findViewById(R.id.cardAI).setBackgroundColor(android.graphics.Color.parseColor(bgColor));

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);

        float cornerRadiusInDp = 15f;
        float density = getResources().getDisplayMetrics().density;
        gd.setCornerRadius(cornerRadiusInDp * density);

        int colorWithAlpha;
        if (!isInEditMode) {
            colorWithAlpha = (android.graphics.Color.parseColor(bgColor) & 0x00FFFFFF) | 0x44000000;
            consumptionEditText.setAlpha(0.6f);
        } else {
            colorWithAlpha = android.graphics.Color.parseColor(bgColor);
            consumptionEditText.setAlpha(1.0f);
        }

        gd.setColor(colorWithAlpha);
        gd.setStroke(3, android.graphics.Color.parseColor("#4169E1"));
        consumptionEditText.setBackground(gd);
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
                if (doc.exists() && doc.get("Consumatia mc") != null) {
                    String feedback = doc.getString("ai_feedback");
                    if ("avarie".equals(feedback)) {
                        continue; // Ignorăm luna cu avarie
                    }
                    Object valueObj = doc.get("Consumatia mc");
                    Number value = valueObj instanceof Number ? (Number) valueObj : null;

                    if (value != null) {
                        lastFetchedHistory.add(value.doubleValue());
                    }
                }
            }

            java.util.Collections.reverse(lastFetchedHistory);

            // 🔥 LOGICA CURATĂ DE DECIZIE:
            if (lastFetchedHistory.size() >= 2) {
                // CAZUL 1: Avem destule date pentru Regresie
                runAIForecast(lastFetchedHistory);

                // Verificăm dacă avem deja ceva scris în EditText ca să comparăm live
                String currentConsumStr = consumptionEditText.getText().toString().trim();
                if (!currentConsumStr.isEmpty() && !currentConsumStr.equals("0")) {
                    try {
                        compareActualWithAI(Double.parseDouble(currentConsumStr));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Eroare parsare: " + currentConsumStr);
                    }
                }
            } else if (lastFetchedHistory.size() == 1) {
                // CAZUL 2: Fallback (Avem doar o lună, AI-ul doar repetă valoarea)
                runAIForecast(lastFetchedHistory);
                textAIStatus.setText("Trend: Date puține, predicție bazată pe o singură lună.");
            } else {
                // CAZUL 3: Zero date
                textPrediction.setText("AI: Date insuficiente");
                textAIStatus.setText("Nu există istoric (sau tot istoricul e marcat 'avarie').");
            }
        });
    }

    private Bitmap preProcessBitmap(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        Bitmap.Config config = source.getConfig() != null
                ? source.getConfig()
                : Bitmap.Config.ARGB_8888;

        Bitmap bitmap = Bitmap.createBitmap(width, height, config);

        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        android.graphics.Paint paint = new android.graphics.Paint();

        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        cm.setSaturation(0);

        float contrast = 1.5f;
        float brightness = -20f;
        cm.postConcat(new android.graphics.ColorMatrix(new float[] {
                contrast, 0, 0, 0, brightness,
                0, contrast, 0, 0, brightness,
                0, 0, contrast, 0, brightness,
                0, 0, 0, 1, 0
        }));

        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
        canvas.drawBitmap(source, 0, 0, paint);

        return bitmap;
    }

    private Bitmap cropToMeterWindow(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();

        // Definim o bandă orizontală (75% lățime, 22% înălțime)
        int cropWidth = (int) (width * 0.75);
        int cropHeight = (int) (height * 0.22);

        int startX = (width - cropWidth) / 2;
        int startY = (height - cropHeight) / 2;

        // Protecție să nu ieșim din cadrul imaginii
        startX = Math.max(0, startX);
        startY = Math.max(0, startY);

        return Bitmap.createBitmap(src, startX, startY, cropWidth, cropHeight);
    }
    private String sanitizeMeterIndex(String rawText) {
        if (rawText == null) return "";

        String cleanedText = rawText.trim();

        // 1. Dacă OCR-ul păstrează virgula/punctul, luăm doar partea dinainte.
        // Ex: "000010,48" -> "000010"
        if (cleanedText.contains(",")) {
            cleanedText = cleanedText.split(",")[0];
        } else if (cleanedText.contains(".")) {
            cleanedText = cleanedText.split("\\.")[0];
        }

        String raw = cleanedText.replaceAll("[^0-9]", "");

        if (raw.length() < 2) return "";

        // 2. Dacă e foarte lung, probabil a prins și zecimalele.
        // Ex: "00001048" -> "000010"
        if (raw.length() >= 6) {
            raw = raw.substring(0, raw.length() - 2);
        }

        try {
            return String.valueOf(Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            Log.e(TAG, "OCR invalid după sanitize: " + raw, e);
            return "";
        }
    }
}