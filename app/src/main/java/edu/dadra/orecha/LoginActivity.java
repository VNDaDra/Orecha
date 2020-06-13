package edu.dadra.orecha;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private LinearLayout loginLayout;
    private Button loginButton, forgotPasswordButton;
    private TextView statusLogin, hintLoginText;
    private String authEmail, authPassword;
    private TextInputLayout emailField, passwordField;
    private AlertDialog progressDialog;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initLayout();

        displaySpannableString();

        initProgressDialog();

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                authEmail = emailField.getEditText().getText().toString().trim();
                authPassword = passwordField.getEditText().getText().toString().trim();
                login(authEmail, authPassword);
            }
        });

        forgotPasswordButton.setOnClickListener(v -> moveToForgotPasswordActivity());

        loginLayout.setOnClickListener(v -> hideKeyboard());

        mAuth = FirebaseAuth.getInstance();
    }

    private void initLayout() {
        loginLayout = findViewById(R.id.login_layout);
        emailField = findViewById(R.id.login_email);
        passwordField = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        forgotPasswordButton = findViewById(R.id.login_forgot_password);
        statusLogin = findViewById(R.id.login_status);
        hintLoginText = findViewById(R.id.login_hint);
    }

    private void displaySpannableString() {
        hintLoginText.setMovementMethod(LinkMovementMethod.getInstance());
        hintLoginText.setText(R.string.hint_register, TextView.BufferType.SPANNABLE);
        Spannable loginSpannable = (Spannable) hintLoginText.getText();

        ClickableSpan registerClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                moveToRegisterActivity();
            }
        };
        loginSpannable.setSpan(registerClickableSpan, 0, loginSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void initProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.layout_loading_dialog);
        progressDialog = builder.create();
        progressDialog.dismiss();
    }

    private void login(String email, String password) {
        Log.d(TAG, email);
        if (!validateLoginForm()) {
            return;
        }
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "loginWithEmail: success");
                        moveToMainActivity();
                    } else {
                        Log.d(TAG, "loginWithEmail: fail");
                        Toast.makeText(LoginActivity.this, "Không thể xác minh người dùng",
                                Toast.LENGTH_SHORT).show();
                        statusLogin.setText("Đăng nhập thất bại");
                    }
                    progressDialog.dismiss();
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

    private void moveToForgotPasswordActivity() {
        Intent ForgotPasswordIntent = new Intent(getApplicationContext(), ForgotPasswordActivity.class);
        ForgotPasswordIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(ForgotPasswordIntent);
    }

    private void moveToRegisterActivity() {
        Intent registerIntent = new Intent(getApplicationContext(), RegisterActivity.class);
        registerIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(registerIntent);
    }

    private void moveToMainActivity() {
        Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mainIntent);
        finish();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            moveToMainActivity();
        }
    }
}
