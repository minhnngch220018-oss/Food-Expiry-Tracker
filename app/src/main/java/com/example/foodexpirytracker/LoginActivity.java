package com.example.foodexpirytracker;

import android.content.Intent;
import android.view.LayoutInflater;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.ActionCodeSettings;

/*
 * Function: LoginActivity
 * Purpose: Authenticate users, navigate to registration, and handle password resets
 */
public class LoginActivity extends AppCompatActivity {

    /*
     * Function: onCreate
     * Purpose: Wire up login UI, validation, navigation, and reset password dialog
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText editEmail = findViewById(R.id.loginEmail);
        EditText editPassword = findViewById(R.id.loginPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView linkRegister = findViewById(R.id.linkRegister);
        TextView linkForgotPassword = findViewById(R.id.linkForgotPassword);
        TextView linkResendVerification = findViewById(R.id.linkResendVerification);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editEmail.getText().toString().trim();
                String password = editPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, R.string.hint_email, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(LoginActivity.this, R.string.invalid_email, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, R.string.empty_password, Toast.LENGTH_SHORT).show();
                    return;
                }

                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                if (user != null) {
                                    // Enforce re-verification after password reset if flagged for this email
                                    boolean forceReverify = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                                            .getBoolean("force_reverify_" + email, false);
                                    if (forceReverify) {
                                        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                                                .setUrl("https://food-expiry-tracker-f870f.firebaseapp.com")
                                                .setAndroidPackageName("com.example.foodexpirytracker", true, null)
                                                .setHandleCodeInApp(true)
                                                .build();
                                        FirebaseAuth.getInstance().useAppLanguage();
                                        user.sendEmailVerification(actionCodeSettings).addOnCompleteListener(vt -> {
                                            if (vt.isSuccessful()) {
                                                Toast.makeText(LoginActivity.this, R.string.verification_email_sent, Toast.LENGTH_SHORT).show();
                                            } else {
                                                Exception e = vt.getException();
                                                String msg = getString(R.string.verification_email_failed);
                                                if (e != null && e.getMessage() != null) {
                                                    msg = msg + ": " + e.getMessage();
                                                }
                                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                                            }
                                            // Clear flag and sign out to force user to verify again
                                            getSharedPreferences("auth_prefs", MODE_PRIVATE)
                                                    .edit()
                                                    .remove("force_reverify_" + email)
                                                    .apply();
                                            FirebaseAuth.getInstance().signOut();
                                        });
                                        Toast.makeText(LoginActivity.this, R.string.email_not_verified, Toast.LENGTH_SHORT).show();
                                        return; // Skip normal verified/unverified handling
                                    }
                                    if (user.isEmailVerified()) {
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        // Propagate optional clearDb extra to MainActivity
                                        boolean clearDb = getIntent() != null && getIntent().getBooleanExtra("clearDb", false);
                                        intent.putExtra("clearDb", clearDb);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        // Auto-send verification email on successful sign-in when unverified
                                        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                                                .setUrl("https://food-expiry-tracker-f870f.firebaseapp.com")
                                                .setAndroidPackageName("com.example.foodexpirytracker", true, null)
                                                .setHandleCodeInApp(true)
                                                .build();
                                        FirebaseAuth.getInstance().useAppLanguage();
                                        user.sendEmailVerification(actionCodeSettings).addOnCompleteListener(vt -> {
                                            if (vt.isSuccessful()) {
                                                Toast.makeText(LoginActivity.this, R.string.verification_email_sent, Toast.LENGTH_SHORT).show();
                                            } else {
                                                Exception e = vt.getException();
                                                String msg = getString(R.string.verification_email_failed);
                                                if (e != null && e.getMessage() != null) {
                                                    msg = msg + ": " + e.getMessage();
                                                }
                                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                                            }
                                            // Sign out so user must verify before accessing app
                                            FirebaseAuth.getInstance().signOut();
                                        });
                                        Toast.makeText(LoginActivity.this, R.string.email_not_verified, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                Exception e = task.getException();
                                if (e instanceof FirebaseAuthInvalidUserException || e instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(LoginActivity.this, R.string.wrong_account_or_password, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
        linkRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        linkForgotPassword.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(LoginActivity.this)
                    .inflate(R.layout.dialog_reset_password, null);
            EditText resetEmail = dialogView.findViewById(R.id.resetEmail);

            AlertDialog dialog = new AlertDialog.Builder(LoginActivity.this)
                    .setTitle(R.string.reset_password)
                    .setView(dialogView)
                    .setPositiveButton(R.string.reset_password, (d, which) -> {
                        String email = resetEmail.getText().toString().trim();
                        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            Toast.makeText(LoginActivity.this, R.string.invalid_email, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this, R.string.password_reset_email_sent, Toast.LENGTH_SHORT).show();
                                        // Require re-verification on next sign-in for this email
                                        getSharedPreferences("auth_prefs", MODE_PRIVATE)
                                                .edit()
                                                .putBoolean("force_reverify_" + email, true)
                                                .apply();
                                    } else {
                                        Toast.makeText(LoginActivity.this, R.string.password_reset_failed, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            dialog.show();
        });

        linkResendVerification.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString();

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(LoginActivity.this, R.string.invalid_email, Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, R.string.empty_password, Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                if (user.isEmailVerified()) {
                                    Toast.makeText(LoginActivity.this, R.string.email_already_verified, Toast.LENGTH_SHORT).show();
                                } else {
                                    ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                                            .setUrl("https://food-expiry-tracker-f870f.firebaseapp.com")
                                            .setAndroidPackageName("com.example.foodexpirytracker", true, null)
                                            .setHandleCodeInApp(true)
                                            .build();
                                    FirebaseAuth.getInstance().useAppLanguage();
                                    user.sendEmailVerification(actionCodeSettings).addOnCompleteListener(vt -> {
                                        if (vt.isSuccessful()) {
                                            Toast.makeText(LoginActivity.this, R.string.verification_email_sent, Toast.LENGTH_SHORT).show();
                                        } else {
                                            Exception e = vt.getException();
                                            String msg = getString(R.string.verification_email_failed);
                                            if (e != null && e.getMessage() != null) {
                                                msg = msg + ": " + e.getMessage();
                                            }
                                            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                            FirebaseAuth.getInstance().signOut();
                        } else {
                            Toast.makeText(LoginActivity.this, R.string.wrong_account_or_password, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}