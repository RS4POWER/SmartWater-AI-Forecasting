package com.example.citirecontoare;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private TextView userNameText;

    private TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        userNameText = findViewById(R.id.user_name_text);
        welcomeText = findViewById(R.id.welcome_text);

        // Verificăm dacă utilizatorul este autentificat
        if (firebaseAuth.getCurrentUser() != null) {
            // Utilizatorul este autentificat, afișăm numele său
            String emailName = firebaseAuth.getCurrentUser().getEmail();
            userNameText.setText(emailName);

        } else {
            // Dacă utilizatorul nu este autentificat, îl redirecționăm la pagina de login
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish(); // Terminăm activitatea curentă pentru a preveni revenirea la această pagină
        }

        // Setăm acțiunea pentru butonul de logout
        Button logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Deautentificăm utilizatorul
                firebaseAuth.signOut();
                // Redirecționăm utilizatorul către pagina de login
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish(); // Terminăm activitatea curentă pentru a preveni revenirea la această pagină
            }
        });
    }
}
