package com.example.citirecontoare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;



public class AfterLogIn extends AppCompatActivity {
    private Button zoneButton1;
    private Button zoneButton2;
    private Button LogOff;

    private static final String TAG = "AfterLogin";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_log_in);

        TextView titleTextView = findViewById(R.id.title_text_view);
        zoneButton1 = findViewById(R.id.zoneButton1);
        zoneButton2 = findViewById(R.id.zoneButton2);

        LogOff = findViewById(R.id.LogOff);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        String userUID = firebaseAuth.getCurrentUser().getUid();
        Log.d(TAG, "-------------" + userUID);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userUID);

        userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    String zoneAsignate = documentSnapshot.getString("zoneAsignate");
                    Log.d(TAG, "Zone asignate: " + zoneAsignate);

                    if (zoneAsignate != null && !zoneAsignate.isEmpty()) {

                        String[] zoneArray = zoneAsignate.split(";");

                        if (zoneArray.length >= 1) {
                            zoneButton1.setText(zoneArray[0]);
                        }
                        if (zoneArray.length >= 2) {
                            zoneButton2.setText(zoneArray[1]);
                        }
                    } else {
                        titleTextView.setText("Nu există zone asignate pentru acest utilizator.");
                    }
                } else {
                    Log.d(TAG, "Document does not exist");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Error getting document: " + e);
            }
        });


        LogOff.setText("Log Off");

        LogOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(AfterLogIn.this, LoginActivity.class));
                finish();
            }
        });

        zoneButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AfterLogIn.this, HouseNumbersActivity.class);
                intent.putExtra("zoneName", zoneButton1.getText().toString());
                startActivity(intent);


            }
        });

        zoneButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AfterLogIn.this, HouseNumbersActivity.class);
                intent.putExtra("zoneName", zoneButton2.getText().toString());
                startActivity(intent);
            }
        });


    }
}
