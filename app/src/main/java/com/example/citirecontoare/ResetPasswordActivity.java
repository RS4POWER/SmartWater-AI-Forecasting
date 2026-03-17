package com.example.citirecontoare;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    EditText emailEditText;
    Button resetPasswordButton;
    TextView backToLoginTextView;
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        firebaseAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.login_username);
        resetPasswordButton = findViewById(R.id.login_button);
        backToLoginTextView = findViewById(R.id.reset_password_text);

        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEditText.getText().toString().trim();
                if (!email.isEmpty()) {
                    firebaseAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(ResetPasswordActivity.this, "Verificați e-mailul pentru instrucțiuni de resetare a parolei.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(ResetPasswordActivity.this, "Trimiterea e-mailului de resetare a eșuat. Vă rugăm să vă verificați adresa de e-mail.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } else {
                    Toast.makeText(ResetPasswordActivity.this, "Te rugam sa introduci adresa ta de email.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        backToLoginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
