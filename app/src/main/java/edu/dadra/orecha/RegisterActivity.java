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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private LinearLayout registerTopLayout, registerMidLayout;
    private TextInputLayout emailField, passwordField, rePasswordField;
    private Button registerButton;
    private TextView hintRegisterText;

    private AlertDialog progressDialog;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initLayout();

        displaySpannableString();

        initProgressDialog();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        registerButton.setOnClickListener(v -> {
            String authEmail = emailField.getEditText().getText().toString().trim();
            String authPassword = passwordField.getEditText().getText().toString().trim();
            createAccount(authEmail, authPassword);
        });

        registerTopLayout.setOnClickListener(v -> hideKeyboard());
        registerMidLayout.setOnClickListener(v -> hideKeyboard());

    }

    private void initLayout() {
        registerTopLayout = findViewById(R.id.register_top_layout);
        registerMidLayout = findViewById(R.id.register_mid_layout);
        emailField = findViewById(R.id.register_email);
        passwordField = findViewById(R.id.register_password);
        rePasswordField = findViewById(R.id.register_rePassword);
        registerButton = findViewById(R.id.register_button);
        hintRegisterText = findViewById(R.id.register_hint);
    }

    private void initProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.layout_loading_dialog);
        progressDialog = builder.create();
        progressDialog.dismiss();
    }

    private void displaySpannableString() {
        hintRegisterText.setMovementMethod(LinkMovementMethod.getInstance());
        hintRegisterText.setText(R.string.hint_login, TextView.BufferType.SPANNABLE);
        Spannable registerSpannable = (Spannable) hintRegisterText.getText();

        ClickableSpan registerClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                moveToLoginActivity();
            }
        };
        registerSpannable.setSpan(registerClickableSpan, 0, registerSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "Creating" + email);
        if(!validateForm()) {
            return;
        }

        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG,"Create account success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        addUserInformation(user);
                    } else {
                        Log.d(TAG,"Create account failed");
                        Toast.makeText(RegisterActivity.this, "Đăng kí thất bại\n" + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }

                });
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = emailField.getEditText().getText().toString().trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            emailField.setError("Không đúng định dạng email");
            valid = false;
        } else {
            emailField.setError(null);
        }

        String password = passwordField.getEditText().getText().toString().trim();
        if (TextUtils.isEmpty(password) || (password.length()<6)) {
            passwordField.setError("Không để trống và có ít nhất 6 kí tự");
            valid = false;
        } else {
            passwordField.setError(null);
        }

        String rePassword = rePasswordField.getEditText().getText().toString().trim();
        if (TextUtils.isEmpty(rePassword)) {
            rePasswordField.setError("Không để trống");
            valid = false;
        } else if (!TextUtils.equals(password, rePassword)) {
            rePasswordField.setError("Nhập lại mật khẩu chưa chính xác");
            valid = false;
        } else {
            rePasswordField.setError(null);
        }
        return valid;
    }

    private void addUserInformation(FirebaseUser user) {
        Map<String, Object> personalData = new HashMap<>();

        personalData.put("email", user.getEmail());
        personalData.put("id", user.getUid());
        personalData.put("displayName", user.getEmail().substring(0, user.getEmail().indexOf("@")));
        personalData.put("phone", "");
        personalData.put("photoUrl", "");

        db.collection("users").document(user.getUid())
                .set(personalData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getApplicationContext(), "Nhấn vào ảnh đại diện để thay đổi thông tin",
                            Toast.LENGTH_LONG).show();
                    moveToMainActivity();
                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Error writing document", e);
                    progressDialog.dismiss();
                });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void moveToLoginActivity() {
        finish();
    }

    private void moveToMainActivity() {
        Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
