package edu.dadra.orecha;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private Button loginButton;
    private TextView statusLogin, hintLoginText;
    private String authEmail, authPassword, hintString;
    private TextInputLayout emailField, passwordField;
    private AlertDialog.Builder builder;
    private AlertDialog progressDialog;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailField = findViewById(R.id.loginEmailEditText);
        passwordField = findViewById(R.id.loginPasswordEditText);
        loginButton = findViewById(R.id.loginButton);
        statusLogin = findViewById(R.id.statusLoginTextView);
        hintLoginText = findViewById(R.id.hintLoginTextView);

        mAuth = FirebaseAuth.getInstance();

        builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.layout_loading_dialog);
        progressDialog = builder.create();
        progressDialog.dismiss();

        hintString = "Chưa có tài khoản";
        hintLoginText.setMovementMethod(LinkMovementMethod.getInstance());
        hintLoginText.setText(hintString, TextView.BufferType.SPANNABLE);
        Spannable loginSpannable = (Spannable) hintLoginText.getText();

        ClickableSpan registerClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                moveToRegisterActivity();
            }
        };
        loginSpannable.setSpan(registerClickableSpan, 0, loginSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                authEmail = emailField.getEditText().getText().toString().trim();
                authPassword = passwordField.getEditText().getText().toString().trim();
                logIn(authEmail, authPassword);
            }
        });


    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            moveToMainActivity();
        }
    }

    private void logIn(String email, String password) {
        Log.d("signIn", email);
        if (!validateLoginForm()) {
            return;
        }

        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("Login", "logInWithEmail: success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            moveToMainActivity();
                        } else {
                            Log.d("Login", "logInWithEmail: fail");
                            Toast.makeText(LoginActivity.this, "Không thể xác nhận người dùng",
                                    Toast.LENGTH_SHORT).show();
                        }

                        if (!task.isSuccessful()) {
                            statusLogin.setText("Đăng nhập thất bại");
                        }
                        progressDialog.dismiss();
                    }
                });
    }

    private boolean validateLoginForm() {
        boolean valid = true;
        String email = emailField.getEditText().getText().toString().trim();
        String password = passwordField.getEditText().getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailField.setError("Không được để trống");
            valid = false;
        } else {
            emailField.setError(null);
        }
        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Không được để trống");
            valid = false;
        } else {
            passwordField.setError(null);
        }
        return valid;
    }

    private void moveToRegisterActivity() {
        Intent registerIntent = new Intent(getApplicationContext(), RegisterActivity.class);
        registerIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(registerIntent);
    }

    private void moveToMainActivity() {
        Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainIntent);
        finish();
    }
}
