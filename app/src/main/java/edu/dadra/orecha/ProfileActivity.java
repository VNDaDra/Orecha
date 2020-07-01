package edu.dadra.orecha;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.InputStream;

import edu.dadra.orecha.Model.Users;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private final int PICK_IMAGE_REQUEST = 1;

    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private DocumentReference userRef;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private LinearLayout topLayout, bottomLayout;
    private ImageView profileAvatar;
    private TextView profileTitleName, verifyStatus;
    private EditText profileEmail, profilePhone, profileName;
    private Button updateProfileButton, avatarDeclineButton, avatarAcceptButton, sendVerifyEmailButton;
    private ImageButton editNameButton,  editPhoneButton, refresh;

    private String userId;
    private Users userData;
    private Uri filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Intent intent = getIntent();
        userId = intent.getStringExtra("id");

        initFirebase();
        initLayout();
        getUserData(userId);

        setFieldEditable();
        avatarListener();
        updateButtonListener();

        confirmChangeAvatar();

        layoutListener();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.setLanguageCode("vi");
        firebaseUser = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference("avatars");
    }

    private void initLayout() {
        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        topLayout = findViewById(R.id.profile_top_layout);
        bottomLayout = findViewById(R.id.profile_bottom_layout);

        profileAvatar = findViewById(R.id.profile_avatar);
        profileTitleName = findViewById(R.id.profile_title_name);
        avatarDeclineButton = findViewById(R.id.profile_avatar_decline);
        avatarAcceptButton = findViewById(R.id.profile_avatar_accept);

        verifyStatus = findViewById(R.id.profile_verify_status);
        sendVerifyEmailButton = findViewById(R.id.profile_send_verify);
        refresh = findViewById(R.id.profile_refresh_verify);

        profileEmail = findViewById(R.id.profile_email);
        profileName = findViewById(R.id.profile_name);
        profilePhone = findViewById(R.id.profile_phone);

        editNameButton = findViewById(R.id.profile_edit_name);
        editPhoneButton = findViewById(R.id.profile_edit_phone);

        updateProfileButton = findViewById(R.id.profile_update);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!firebaseUser.isEmailVerified()) {
            displaySendVerifyEmailButton();
        } else {
            sendVerifyEmailButton.setVisibility(View.GONE);
            verifyStatus.setText(R.string.verified_email);
            verifyStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.greenA700));
        }

        //Friend view
        if (!userId.equals(firebaseUser.getUid())) {
            profileEmail.setTextColor(Color.BLACK);
            profileName.setTextColor(Color.BLACK);
            profilePhone.setTextColor(Color.BLACK);

            verifyStatus.setVisibility(View.GONE);
            sendVerifyEmailButton.setVisibility(View.GONE);

            editNameButton.setVisibility(View.INVISIBLE);
            editPhoneButton.setVisibility(View.INVISIBLE);
            updateProfileButton.setVisibility(View.INVISIBLE);
        }
    }

    private void displaySendVerifyEmailButton() {
        sendVerifyEmailButton.setVisibility(View.VISIBLE);
        sendVerifyEmailButton.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), "Kiểm tra email để xác thực", Toast.LENGTH_LONG).show();
            sendVerifyEmail();
            countdown();
            refresh.setVisibility(View.VISIBLE);
            refresh.setOnClickListener(v1 -> refreshVerifyStatus());
        });
    }

    private void getUserData(String userId) {
        db.collection("users").document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.d(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        userData = snapshot.toObject(Users.class);
                        if (!userData.getPhotoUrl().equals("")) {
                            Glide.with(getApplicationContext())
                                    .load(storage.getReferenceFromUrl(userData.getPhotoUrl()))
                                    .placeholder(R.drawable.orange)
                                    .into(profileAvatar);
                        } else Glide.with(getApplicationContext())
                                .load(R.drawable.orange)
                                .placeholder(R.drawable.orange)
                                .into(profileAvatar);

                        profileTitleName.setText(userData.getDisplayName());
                        profileName.setText(userData.getDisplayName());
                        profileEmail.setText(userData.getEmail());
                        profilePhone.setText(userData.getPhone());
                    }
                });
    }

    private void setFieldEditable() {
        editNameButton.setOnClickListener(v -> {
            profileName.setEnabled(true);
            updateProfileButton.setEnabled(true);
        });

        editPhoneButton.setOnClickListener(v -> {
            profilePhone.setEnabled(true);
            updateProfileButton.setEnabled(true);
        });
    }

    private void avatarListener() {
        profileAvatar.setOnClickListener(v -> {
            if (!userId.equals(firebaseUser.getUid())) {
                viewAvatar();
            }
            else {
                showViewImageDialog();
            }
        });
    }

    private void showViewImageDialog() {
        View view = View.inflate(getApplicationContext(),R.layout.layout_profile_bottom_sheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);
        dialog.show();

        LinearLayout updateAvatar = dialog.findViewById(R.id.profile_update_avatar);
        LinearLayout viewAvatar = dialog.findViewById(R.id.profile_view_avatar);

        updateAvatar.setOnClickListener(v -> {
            chooseImage();
            dialog.dismiss();
        });
        viewAvatar.setOnClickListener(v -> {
            viewAvatar();
            dialog.dismiss();
        });
    }

    private void viewAvatar() {
        if (!userData.getPhotoUrl().equals("") && userData.getPhotoUrl() != null) {
            Intent fullScreenImageIntent = new Intent(getApplicationContext(), FullScreenImageActivity.class);
            fullScreenImageIntent.putExtra("imageUri", userData.getPhotoUrl());
            startActivity(fullScreenImageIntent);
        } else {
            Toast.makeText(getApplicationContext(), "Đây là ảnh mặc định", Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh đại diện"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null ) {
            filePath = data.getData();
            if (getImageSize() <= 5120) {
                Glide.with(getApplicationContext())
                        .load(filePath)
                        .into(profileAvatar);
                avatarDeclineButton.setVisibility(View.VISIBLE);
                avatarAcceptButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getApplicationContext(), "Hãy chọn hình nhỏ hơn 5MB", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private int getImageSize() {
        InputStream fileInputStream;
        int imageSize = 0;
        try {
            fileInputStream = getApplicationContext().getContentResolver().openInputStream(filePath);
            imageSize = fileInputStream.available() / 1024;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageSize;
    }

    private void confirmChangeAvatar() {
        avatarDeclineButton.setOnClickListener(v -> {
            avatarDeclineButton.setVisibility(View.GONE);
            avatarAcceptButton.setVisibility(View.GONE);
            if (!userData.getPhotoUrl().equals("")) {
                Glide.with(getApplicationContext())
                        .load(storage.getReferenceFromUrl(userData.getPhotoUrl()))
                        .placeholder(R.drawable.orange)
                        .into(profileAvatar);
            } else {
                Glide.with(getApplicationContext())
                        .load(R.drawable.orange)
                        .placeholder(R.drawable.orange)
                        .into(profileAvatar);
            }
        });

        avatarAcceptButton.setOnClickListener(v -> {
            avatarDeclineButton.setVisibility(View.GONE);
            avatarAcceptButton.setVisibility(View.GONE);
            uploadAvatarToStorage();
        });
    }

    private void uploadAvatarToStorage() {
        if(filePath != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Tải lên");
            progressDialog.show();

            StorageReference ref = storageReference.child(userId + "_" +
                    System.currentTimeMillis() + "." + getFileExtension(filePath));
            ref.putFile(filePath)
                    .addOnSuccessListener(taskSnapshot -> {
                        if (!userData.getPhotoUrl().equals("")) {
                            storage.getReferenceFromUrl(userData.getPhotoUrl()).delete();
                        }
                        progressDialog.dismiss();
                        updateAvatarUrlInDatabase(ref.toString());
                        Toast.makeText(getApplicationContext(), "Thành công", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Lỗi "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                .getTotalByteCount());
                        progressDialog.setMessage("Đang tải lên "+(int)progress+"%");
                    });
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cR.getType(uri));
    }

    private void updateAvatarUrlInDatabase(String ref) {
        userRef = db.collection("users").document(firebaseUser.getUid());
        userRef.update( "photoUrl", ref)
                .addOnFailureListener(e -> Log.d(TAG, "updateAvatarUrlInDatabase fail"));

        db.collectionGroup("userContacts").whereEqualTo("id", firebaseUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().update("photoUrl", ref);
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });
    }

    private void updateButtonListener() {
        updateProfileButton.setOnClickListener(v -> {
            if (checkValidInput()) {
                profileName.setEnabled(false);
                profilePhone.setEnabled(false);
                updateProfileButton.setEnabled(false);

                updateUsersCollection();
                updateContactsCollection();

                getUserData(userId);
            }
        });
    }

    private void updateUsersCollection() {
        userRef = db.collection("users").document(firebaseUser.getUid());
        userRef.update( "displayName", profileName.getText().toString().trim(),
                                    "phone", profilePhone.getText().toString().trim())
                .addOnFailureListener(e -> Log.d(TAG, "updateUsersCollection fail"));
    }

    private void updateContactsCollection() {
        db.collectionGroup("userContacts").whereEqualTo("id", firebaseUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().update("displayName", profileName.getText().toString().trim(),
                                    "phone", profilePhone.getText().toString().trim());
                        }
                        Toast.makeText(getApplicationContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });
    }

    private boolean checkValidInput() {
        boolean valid = true;
        if (TextUtils.isEmpty(profileName.getText().toString().trim())) {
            valid = false;
            profileName.setError("Không để trống");
        } else profileName.setError(null);

        return valid;
    }

    private void layoutListener() {
        topLayout.setOnClickListener(v -> hideKeyboard());

        bottomLayout.setOnClickListener(v -> hideKeyboard());
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void sendVerifyEmail() {
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), "Kiểm tra email của bạn để tiến hành xác thực",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Lỗi! Vui lòng thử lại sau",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void countdown() {
        sendVerifyEmailButton.setEnabled(false);
        new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                sendVerifyEmailButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
                sendVerifyEmailButton.setText("" + (millisUntilFinished / 1000));
            }

            public void onFinish() {
                refreshVerifyStatus();
                sendVerifyEmailButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                sendVerifyEmailButton.setText("Gửi lại");
                sendVerifyEmailButton.setEnabled(true);
            }
        }.start();
    }

    private void refreshVerifyStatus() {
        firebaseUser.reload();
        if (firebaseUser.isEmailVerified()) {
            refresh.setVisibility(View.GONE);
            sendVerifyEmailButton.setVisibility(View.GONE);
            verifyStatus.setText(R.string.verified_email);
            verifyStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.greenA700));
        }
    }

}
