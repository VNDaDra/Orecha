package edu.dadra.orecha;

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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputLayout emailField, passwordField, rePasswordField;
    private Button registerButton;
    private TextView statusRegister, hintRegisterText;
    private String hintString;

    private AlertDialog.Builder builder;
    private AlertDialog progressDialog;

    private String authEmail, authPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailField = findViewById(R.id.registerEmailEditText);
        passwordField = findViewById(R.id.registerPasswordEditText);
        rePasswordField = findViewById(R.id.registerRePasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        statusRegister = findViewById(R.id.registerStatusTextView);
        hintRegisterText = findViewById(R.id.hintRegisterTextView);

        builder = new AlertDialog.Builder(this);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(R.layout.layout_loading_dialog);
        progressDialog = builder.create();
        progressDialog.dismiss();

        mAuth = FirebaseAuth.getInstance();

        hintString = "Đã có tài khoản ? [Đăng nhập ngay]";
        int i1 = hintString.indexOf("[");
        int i2 = hintString.indexOf("]");
        hintRegisterText.setMovementMethod(LinkMovementMethod.getInstance());
        hintRegisterText.setText(hintString, TextView.BufferType.SPANNABLE);
        Spannable registerSpannable = (Spannable) hintRegisterText.getText();

        ClickableSpan registerClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                moveToLoginActivity();
            }
        };
        registerSpannable.setSpan(registerClickableSpan, i1+1, i2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authEmail = emailField.getEditText().getText().toString();
                authPassword = passwordField.getEditText().getText().toString();
                createAccount(authEmail, authPassword);
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
//         Check if user is signed in (non-null) and update UI accordingly.
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if (currentUser != null) {
//            startActivity(new Intent(getApplicationContext(), MainActivity.class));
//        }
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "Creating" + email);
        if(!validateForm()) {
            return;
        }

        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG,"Create success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            addUserInformation(user);
                            Toast.makeText(RegisterActivity.this, "Đăng kí thành công",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG,"Create failed");
                            Toast.makeText(RegisterActivity.this, "Đăng kí thất bại! Vui lòng kiểm tra lại thông tin",
                                    Toast.LENGTH_SHORT).show();
                        }

                        progressDialog.dismiss();


                    }
                });
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = emailField.getEditText().getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailField.setError("Không để trống");
            valid = false;
        } else {
            emailField.setError(null);
        }

        String password = passwordField.getEditText().getText().toString();
        if (TextUtils.isEmpty(password) || (password.length()<6)) {
            passwordField.setError("Không để trống và có ít nhất 6 kí tự");
            valid = false;
        } else {
            passwordField.setError(null);
        }

        String rePassword = rePasswordField.getEditText().getText().toString();
        if (TextUtils.isEmpty(rePassword)) {
            rePasswordField.setError("Không để trống");
            valid = false;
        } else {
            rePasswordField.setError(null);
        }

        if (!TextUtils.equals(password, rePassword)) {
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
        personalData.put("displayName", user.getEmail());
        personalData.put("phone", "");
        personalData.put("photoUrl", "");

        db.collection("users").document(user.getUid())
                .set(personalData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });

    }

    private void moveToLoginActivity() {
        Intent mainIntent = new Intent(getApplicationContext(), LoginActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }



}
