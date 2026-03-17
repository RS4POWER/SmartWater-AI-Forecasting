package com.example.citirecontoare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;


import java.text.BreakIterator;

public class LoginActivity extends AppCompatActivity {

    EditText loginUsername, loginPassword;
    Button loginButton;
    TextView signupRedirectText;
    FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        loginUsername = findViewById(R.id.login_username);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = loginUsername.getText().toString().trim();
                String password = loginPassword.getText().toString().trim();

                if (email.isEmpty() && password.isEmpty())  {
                    Toast.makeText(LoginActivity.this, "Va rog completati adresa de email si parola.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (email.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Te rog introdu o adresă de email.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Te rog introdu o parolă.", Toast.LENGTH_SHORT).show();
                    return; // Intrerupe functia onClick pentru a evita continuarea operatiilor
                }


                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


                firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {

                                    if(user != null)
                                        if(user.isEmailVerified()) {
                                            startActivity(new Intent(LoginActivity.this, AfterLogIn.class));
                                            finish();
                                        }
                                        else
                                        {
                                            Toast.makeText(LoginActivity.this, "Verificati adresa de email.",
                                                    Toast.LENGTH_SHORT).show();
                                        }

                                } else {
                                    if (password.length() < 6) {
                                        Toast.makeText(LoginActivity.this, "Parola trebuie sa aiba cel putin 6 caractere.",
                                                Toast.LENGTH_LONG).show();
                                    } else {


                                        Toast.makeText(LoginActivity.this, "Autentificare esuata.",
                                                Toast.LENGTH_SHORT).show();
                                    }

                                }
                            }
                        });
            }
        });

        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });

        TextView resetPasswordTextView = findViewById(R.id.reset_password_text);
        resetPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
            }
        });



    }
}
