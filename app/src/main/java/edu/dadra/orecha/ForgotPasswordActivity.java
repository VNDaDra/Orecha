package edu.dadra.orecha;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private LinearLayout forgotPasswordLayout;
    private TextInputLayout emailField;
    private Button recoverButton;
    private TextView hint;
    private AlertDialog progressDialog;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initLayout();
        initFirebase();
        initProgressDialog();

        recoverButton.setOnClickListener(v -> {
            String email = emailField.getEditText().getText().toString().trim();
            if (validateEmail(email)) {
                sendRecoverEmail(email);
                progressDialog.show();
            }
        });

        forgotPasswordLayout.setOnClickListener(v -> hideKeyboard());
    }

    private void initLayout() {
        Toolbar toolbar = findViewById(R.id.forgot_password_toolbar);
        forgotPasswordLayout = findViewById(R.id.forgot_password_layout);
        emailField = findViewById(R.id.forgot_password_email);
        recoverButton = findViewById(R.id.forgot_password_button);
        hint = findViewById(R.id.forgot_password_hint);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.layout_loading_dialog);
        progressDialog = builder.create();
        progressDialog.dismiss();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void sendRecoverEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        hint.setVisibility(View.VISIBLE);
                        progressDialog.dismiss();
                        recoverButton.setEnabled(false);
                        emailField.clearFocus();
                        emailField.getEditText().setText("");
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Lỗi\n" + task.getException(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateEmail(String email) {
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            emailField.setError("Không được để trống");
            valid = false;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            emailField.setError("Không đúng định dạng email");
            valid = false;
        } else {
            emailField.setError(null);
        }

        return valid;
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}