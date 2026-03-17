package com.example.citirecontoare;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import android.content.Intent;
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


public class HouseDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView ownerNameTextView, houseNumberTextView;
    private EditText marcaEditText, seriaEditText, diametruEditText, dataInstalareEditText,stareApometruEditText,consumMcEditText,datacitiriiEditText;
    private boolean isInEditMode = false;
    private ImageButton backButton,manualModifyButton,calculateConsumButton,  previousYearButton, nextYearButton,takePhotoButton;
    private  Button nextMonthButton,previousMonthButton;

    private int currentYear;
    private int currentMonth;
    private Long houseNumber;
    private String zoneName;
    boolean succes;

    public  String[] monthNames = {"Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
            "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_house_details);

        ownerNameTextView = findViewById(R.id.ownerNameTextView);
        houseNumberTextView = findViewById(R.id.houseNumberTextView);
        marcaEditText = findViewById(R.id.marcaEditText);
        seriaEditText = findViewById(R.id.seriaEditText);
        diametruEditText = findViewById(R.id.diametruEditText);
        dataInstalareEditText = findViewById(R.id.datainstalareEditText);
        manualModifyButton = findViewById(R.id.manualModifyButton);
        stareApometruEditText = findViewById(R.id.stareApometruEditText);
        consumMcEditText = findViewById(R.id.consumMcEditText);
        datacitiriiEditText = findViewById(R.id.datacitiriiEditText);
        backButton = findViewById(R.id.backButton);
        previousYearButton = findViewById(R.id.previousYearButton);
        nextYearButton = findViewById(R.id.nextYearButton);
        previousMonthButton = findViewById(R.id.previousMonthButton);
        nextMonthButton = findViewById(R.id.nextMonthButton);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        calculateConsumButton = findViewById(R.id.calculateConsumButton);

        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        currentMonth = Calendar.getInstance().get(Calendar.MONTH);


        updateDateDisplay();
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Închide activitatea curentă
            }

        });


        previousYearButton.setOnClickListener(v -> {
            currentYear--;
            updateDateDisplay();
            loadApometruDetails(houseNumber, currentYear, currentMonth);
            loadHouseDetails(houseNumber,zoneName);
        });

        nextYearButton.setOnClickListener(v -> {
            currentYear++;
            updateDateDisplay();
            loadApometruDetails(houseNumber, currentYear, currentMonth);
            loadHouseDetails(houseNumber,zoneName);
        });

        previousMonthButton.setOnClickListener(v -> {
            if (currentMonth > 0) {
                currentMonth--;
            } else {
                currentMonth = 11;
                currentYear--;
            }
            updateDateDisplay();
            loadApometruDetails(houseNumber, currentYear, currentMonth);
            loadHouseDetails(houseNumber,zoneName);
        });

        nextMonthButton.setOnClickListener(v -> {
            if (currentMonth < 11) {
                currentMonth++;
            } else {
                currentMonth = 0;
                currentYear++;
            }
            updateDateDisplay();
            loadApometruDetails(houseNumber, currentYear, currentMonth);
            loadHouseDetails(houseNumber,zoneName);
        });


        toggleEditMode(false);
        manualModifyButton.setOnClickListener(v -> {
            isInEditMode = !isInEditMode;

            // Actualizez iconița butonului
            if (isInEditMode) {
                manualModifyButton.setBackgroundResource(R.drawable.baseline_done_outline_24);
            } else {
                manualModifyButton.setBackgroundResource(R.drawable.baseline_edit_note_24);
                Log.d("ManualModifiyButon", "s-a apelat listener pentru manualModify");
                saveHouseDetails();
                saveApometruDetails();
            }
            toggleEditMode(isInEditMode);
        });


        // Obținere numarul casei, numele zonei  și numele proprietarului din intent
        Intent intent = getIntent();
         houseNumber = intent.getLongExtra("HOUSE_NUMBER", -1);
        zoneName = intent.getStringExtra("ZONE_NAME");
        if(houseNumber == -1) {
            Toast.makeText(this, "Numărul casei nu a fost transmis corect.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if(zoneName == null) {
            Toast.makeText(this, "Zona nu a fost transmisa corect.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        houseNumberTextView.setText(String.valueOf("NR "+houseNumber));

        loadHouseDetails(houseNumber,zoneName);
        loadApometruDetails(houseNumber, currentYear, currentMonth);


        if(houseNumber != -1) {
            loadApometruDetails(houseNumber, currentYear, currentMonth);
        } else {
            Toast.makeText(this, "Numărul casei nu a fost transmis corect.", Toast.LENGTH_SHORT).show();
            finish();
        }

        //OCR

        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCameraPermission();
            }
        });


        calculateConsumButton.setOnClickListener(v -> calculateAndSaveConsum());


    }

    private void loadHouseDetails(Long houseNumber, String zoneName) {
        DocumentReference houseRef = db.collection("zones")
                .document(zoneName)
                .collection("numereCasa")
                .document("Numarul " + houseNumber);

        houseRef.get().addOnSuccessListener(documentSnapshot -> {
            if(documentSnapshot.exists()) {
                // Actualizează interfața cu datele casei
                ownerNameTextView.setText(documentSnapshot.getString("Proprietar"));
                marcaEditText.setText(documentSnapshot.getString("Apometru marca"));
                seriaEditText.setText(documentSnapshot.getString("Seria"));
                diametruEditText.setText(documentSnapshot.getLong("Diametru apometru").toString());
                dataInstalareEditText.setText(documentSnapshot.getString("Instalat la"));

            } else {
                Toast.makeText(this, "Detalii despre casă nu au fost găsite.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Eroare la încărcarea detaliilor casei.", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadApometruDetails(Long houseNumber, int year, int month) {
        String monthName = monthNames[month];

        DocumentReference monthRef = db.collection("zones")
                .document(zoneName)
                .collection("numereCasa")
                .document("Numarul " + houseNumber)
                .collection("consumApa")
                .document(String.valueOf(year))
                .collection("lunile")
                .document(monthName);

        monthRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Verifică și tratează 'Starea Apometrului' ca număr
                Long stareaApometrului = documentSnapshot.getLong("Starea Apometrului");
                if (stareaApometrului != null) {
                    stareApometruEditText.setText(String.valueOf(stareaApometrului)); // Convertim la String
                } else {
                    stareApometruEditText.setText("Nedefinit");
                }

                consumMcEditText.setText(String.valueOf(documentSnapshot.getLong("Consumatia mc"))); // Convertim numarul la String
                datacitiriiEditText.setText(documentSnapshot.getString("Data citire"));

            } else {
                Toast.makeText(this, "Nu există înregistrări pentru " + monthName + " " + year, Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Eroare la încărcarea datelor pentru " + monthName + " " + year, Toast.LENGTH_SHORT).show();
        });




    }

    private DocumentReference getMonthRef(Long houseNumber, int year, int month) {
        if (month < 0) {
            year -= 1;
            month = 11; // Decembrie este luna 11 (0 indexat)
        }
        String monthName = monthNames[month];
        return db.collection("zones").document(zoneName).collection("numereCasa").document("Numarul " + houseNumber).collection("consumApa").document(String.valueOf(year)).collection("lunile").document(monthName);
    }




    private void toggleEditMode(boolean isInEditMode) {
        marcaEditText.setEnabled(isInEditMode);
        seriaEditText.setEnabled(isInEditMode);
        diametruEditText.setEnabled(isInEditMode);
        dataInstalareEditText.setEnabled(isInEditMode);
        stareApometruEditText.setEnabled(isInEditMode);
        consumMcEditText.setEnabled(isInEditMode);
        datacitiriiEditText.setEnabled(isInEditMode);

    }

    @SuppressLint("SuspiciousIndentation")
    private void saveHouseDetails() {

        String marca = marcaEditText.getText().toString();
        String seria = seriaEditText.getText().toString();
        String diametru = diametruEditText.getText().toString();
        String dataInstalare = dataInstalareEditText.getText().toString();

        if (!isValidNumber(diametru)) {
            succes = false;
            Toast.makeText(this, "Doar cifre sunt acceptate pentru diametru.", Toast.LENGTH_SHORT).show();
            return;
        }
        else {
            succes=true;
        }

        Long houseNumber = getIntent().getLongExtra("HOUSE_NUMBER", -1);
        DocumentReference houseRef = db.collection("zones").document(zoneName)
                .collection("numereCasa").document("Numarul " + houseNumber);

        Map<String, Object> houseDetails = new HashMap<>();
        houseDetails.put("Apometru marca", marca);
        houseDetails.put("Seria", seria);
        houseDetails.put("Diametru apometru", Long.parseLong(diametru)); // asigură-te că acesta este un număr
        houseDetails.put("Instalat la", dataInstalare);


        // Actualizare în Firestore
        houseRef.update(houseDetails)
                .addOnSuccessListener(aVoid -> {
                    if(succes!=false)
                    Toast.makeText(HouseDetailsActivity.this, "Datele au fost salvate cu succes!", Toast.LENGTH_SHORT).show();
                    isInEditMode = false;
                    toggleEditMode(false);

                })
                .addOnFailureListener(e -> Toast.makeText(HouseDetailsActivity.this, "Eroare la salvarea datelor.", Toast.LENGTH_SHORT).show());

        isInEditMode = false;
        toggleEditMode(false);

    }


    private void saveApometruDetails() {
        Log.d("saveApometru","s-a apelat saveApometruDetails()");
        Long houseNumber = getIntent().getLongExtra("HOUSE_NUMBER", -1);
        String stareApometru = stareApometruEditText.getText().toString();
        String consumMc = consumMcEditText.getText().toString();
        String dataCitirii = datacitiriiEditText.getText().toString();

        if (!isValidNumber(consumMc)) {
            succes=false;
            Toast.makeText(this, "Consumația trebuie să fie un număr valid.", Toast.LENGTH_SHORT).show();
            return;
        }
        else
        {
            succes=true;
        }

        double consumValue = Double.parseDouble(consumMc);
        long consumInt = (long) consumValue;

        DocumentReference monthRef = db.collection("zones")
                .document(zoneName)
                .collection("numereCasa")
                .document("Numarul " + houseNumber)
                .collection("consumApa")
                .document(String.valueOf(currentYear))
                .collection("lunile")
                .document(monthNames[currentMonth]);

        Map<String, Object> apometruDetails = new HashMap<>();
        apometruDetails.put("Starea Apometrului", Long.parseLong(stareApometru));
        apometruDetails.put("Consumatia mc", consumInt);
        apometruDetails.put("Data citire", dataCitirii);

        monthRef.update(apometruDetails)
                .addOnSuccessListener(aVoid -> {
                    isInEditMode = false;
                    toggleEditMode(false);
                })
                .addOnFailureListener(e -> Toast.makeText(HouseDetailsActivity.this, "Eroare la actualizarea datelor apometrului.", Toast.LENGTH_SHORT).show());
    }

private boolean isValidNumber(String numberStr) {
    try {
        double parsedNumber = Double.parseDouble(numberStr);
        String normalizedNumber = String.valueOf((long) parsedNumber);
        return normalizedNumber.equals(numberStr.replaceFirst("^0+(?!$)", ""));
    } catch (NumberFormatException e) {
        return false;
    }
}

    private void updateDateDisplay() {
        String monthName = monthNames[currentMonth];
        String displayText = monthName + ", " + currentYear;
        TextView dateTextView = findViewById(R.id.dateTextView);
        dateTextView.setText(displayText);
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private void requestCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Este necesara permisiunea de acces la camera pentru aceasta functionalitate", Toast.LENGTH_LONG).show();
            }
        }
    }

    //OCR
    private void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                Log.d("OCR", "Image captured and received in the app");
                recognizeText(imageBitmap);
            } else {
                Log.d("OCR", "No image data received");
            }
        } else {
            Log.d("OCR", "Camera activity did not return RESULT_OK");
        }
    }



    private void recognizeText(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(this::processTextRecognitionResult)
                .addOnFailureListener(e -> {
                    Log.e("OCR", "OCR Failed: " + e.getMessage());
                    Toast.makeText(HouseDetailsActivity.this, "OCR Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }




    private void processTextRecognitionResult(Text result) {
        String resultText = result.getText();
        Log.d("OCR", "OCR Result: " + resultText);
        for (Text.TextBlock block : result.getTextBlocks()) {
            String blockText = block.getText();
            Log.d("OCR", "Block text: " + blockText);
            if (blockText.matches("\\d+")) { // Regex pentru a verifica dacă textul este numeric
                stareApometruEditText.setText(blockText);
                Log.d("OCR", "Numeric text set in EditText: " + blockText);
                break;
            }
            else
            if (!blockText.matches("\\d+")) {
                Log.d("OCR", "Non-numeric text detected: " + blockText);
                loadApometruDetails(houseNumber, currentYear, currentMonth);
            }

        }
        if(resultText.isEmpty()) {
            Log.d("OCR", "No text recognized");
            //Toast.makeText(HouseDetailsActivity.this, "Nu s-a recunoscut consumul de pe contor.",
              //      Toast.LENGTH_SHORT).show();
            loadApometruDetails(houseNumber, currentYear, currentMonth);
        }
    }

    private void calculateAndSaveConsum() {
        DocumentReference currentMonthRef = getMonthRef(houseNumber, currentYear, currentMonth);
        DocumentReference previousMonthRef = getMonthRef(houseNumber, currentYear, currentMonth - 1);

        Task<DocumentSnapshot> currentMonthTask = currentMonthRef.get();
        Task<DocumentSnapshot> previousMonthTask = previousMonthRef.get();

        Tasks.whenAllSuccess(currentMonthTask, previousMonthTask).addOnSuccessListener(results -> {
            DocumentSnapshot currentDoc = (DocumentSnapshot) results.get(0);
            DocumentSnapshot previousDoc = (DocumentSnapshot) results.get(1);

            Long currentStareApometru = currentDoc.getLong("Starea Apometrului");
            Long previousStareApometru = previousDoc != null ? previousDoc.getLong("Starea Apometrului") : 0; // Assume 0 if null

            if (currentStareApometru != null && previousStareApometru != null) {
                Long consumMc = currentStareApometru - previousStareApometru;
                currentMonthRef.update("Consumatia mc", consumMc)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(HouseDetailsActivity.this, "Consumul a fost actualizat.", Toast.LENGTH_SHORT).show();
                            loadApometruDetails(houseNumber, currentYear, currentMonth);
                        })
                        .addOnFailureListener(e -> Toast.makeText(HouseDetailsActivity.this, "Eroare la actualizarea consumului.", Toast.LENGTH_SHORT).show());
            }

        }).addOnFailureListener(e -> {
            Toast.makeText(HouseDetailsActivity.this, "Eroare la accesarea datelor.", Toast.LENGTH_SHORT).show();
        });
    }


}


