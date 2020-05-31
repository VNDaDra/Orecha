package edu.dadra.orecha;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePassword extends AppCompatActivity {

    private static final String TAG = "ChangePasswordActivity";

    private FirebaseUser firebaseUser;

    private LinearLayout changePasswordLayout;
    private Button changePasswordButton;
    private TextInputLayout oldPasswordField, newPasswordField, newRetypePasswordField;

    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        init();

        changePasswordLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validate()) {
                    progressDialog.show();
                    changePassword();
                }
            }
        });
    }

    private void init() {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        Toolbar toolbar = findViewById(R.id.change_password_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        changePasswordLayout = findViewById(R.id.change_password_layout);
        changePasswordButton = findViewById(R.id.change_password_button);
        oldPasswordField = findViewById(R.id.change_password_old);
        newPasswordField = findViewById(R.id.change_password_new);
        newRetypePasswordField = findViewById(R.id.change_password_retype_new);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setView(R.layout.layout_loading_dialog);
        progressDialog = builder.create();
        progressDialog.dismiss();
    }

    private void changePassword() {     //Re-authentication then update password
        String email = firebaseUser.getEmail();
        String oldPassword = oldPasswordField.getEditText().getText().toString();
        AuthCredential credential = EmailAuthProvider.getCredential(email, oldPassword);
        firebaseUser.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        firebaseUser.updatePassword(newPasswordField.getEditText().getText().toString().trim())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        progressDialog.dismiss();
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "User password updated.");
                                            Toast.makeText(getApplicationContext(), "Thành công", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Log.d(TAG, "User password unable to update.");
                                            Toast.makeText(getApplicationContext(), "Không thành công\n" + task.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                });


    }

    private boolean validate() {
        boolean valid = true;

        oldPasswordField.setError(null);
        newPasswordField.setError(null);
        newRetypePasswordField.setError(null);

        String oldPassword = oldPasswordField.getEditText().getText().toString().trim();
        if (TextUtils.isEmpty(oldPassword)) {
            oldPasswordField.setError("Không để trống");
            valid = false;
        } else {
            oldPasswordField.setError(null);
        }

        String newPassword = newPasswordField.getEditText().getText().toString().trim();
        if (TextUtils.isEmpty(newPassword) || (newPassword.length()<6)) {
            newPasswordField.setError("Không để trống và có ít nhất 6 kí tự");
            valid = false;
        } else if (TextUtils.equals(oldPassword, newPassword)) {
            newPasswordField.setError("Trùng với mật khẩu cũ");
            valid = false;
        } else {
            newPasswordField.setError(null);
        }

        String newRetypePassword = newRetypePasswordField.getEditText().getText().toString().trim();
        if (TextUtils.isEmpty(newRetypePassword)) {
            newRetypePasswordField.setError("Không để trống");
            valid = false;
        } else if (!TextUtils.equals(newPassword, newRetypePassword)) {
            newRetypePasswordField.setError("Nhập lại mật khẩu chưa chính xác");
            valid = false;
        } else {
            newRetypePasswordField.setError(null);
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
