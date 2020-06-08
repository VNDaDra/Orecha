package edu.dadra.orecha;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
    private TextView profileTitleName;
    private EditText profileEmail, profilePhone, profileName;
    private Button updateProfileButton, avatarDeclineButton, avatarAcceptButton;
    private ImageButton profileEditNameButton,  profileEditPhoneButton;

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
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
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

        profileEmail = findViewById(R.id.profile_email);
        profileName = findViewById(R.id.profile_name);
        profilePhone = findViewById(R.id.profile_phone);

        profileEditNameButton = findViewById(R.id.profile_edit_name);
        profileEditPhoneButton = findViewById(R.id.profile_edit_phone);

        updateProfileButton = findViewById(R.id.profile_update);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Friend view
        if (!userId.equals(firebaseUser.getUid())) {
            profileEmail.setTextColor(Color.BLACK);
            profileName.setTextColor(Color.BLACK);
            profilePhone.setTextColor(Color.BLACK);

            //Can't change friend information
            profileEditNameButton.setVisibility(View.INVISIBLE);
            profileEditPhoneButton.setVisibility(View.INVISIBLE);
            updateProfileButton.setVisibility(View.INVISIBLE);
        }
    }

    private void getUserData(String userId) {
        db.collection("users").document(userId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
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
                    }
                });
    }

    private void setFieldEditable() {
        profileEditNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileName.setEnabled(true);
                updateProfileButton.setEnabled(true);
            }
        });

        profileEditPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profilePhone.setEnabled(true);
                updateProfileButton.setEnabled(true);
            }
        });
    }

    private void avatarListener() {
        profileAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!userId.equals(firebaseUser.getUid())) {
                    viewAvatar();
                }
                else {
                    showViewImageDialog();
                }
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

        updateAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
                dialog.dismiss();
            }
        });
        viewAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewAvatar();
                dialog.dismiss();
            }
        });
    }

    private void viewAvatar() {
        Intent fullScreenImageIntent = new Intent(getApplicationContext(), FullScreenImageActivity.class);
        fullScreenImageIntent.putExtra("imageUri", userData.getPhotoUrl());
        startActivity(fullScreenImageIntent);
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
        avatarDeclineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        avatarAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                avatarDeclineButton.setVisibility(View.GONE);
                avatarAcceptButton.setVisibility(View.GONE);
                uploadAvatarToStorage();
            }
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
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            storage.getReferenceFromUrl(userData.getPhotoUrl()).delete();
                            updateAvatarUrlInDatabase(ref.toString());
                            Toast.makeText(getApplicationContext(), "Thành công", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Lỗi "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Đang tải lên "+(int)progress+"%");
                        }
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
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "updateAvatarUrlInDatabase fail");
                    }
                });

        db.collectionGroup("userContacts").whereEqualTo("id", firebaseUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                document.getReference().update("photoUrl", ref);
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void updateButtonListener() {
        updateProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkValidInput()) {
                    profileName.setEnabled(false);
                    profilePhone.setEnabled(false);
                    updateProfileButton.setEnabled(false);

                    updateUsersCollection();
                    updateContactsCollection();

                    getUserData(userId);
                }
            }
        });
    }

    private void updateUsersCollection() {
        userRef = db.collection("users").document(firebaseUser.getUid());
        userRef.update( "displayName", profileName.getText().toString().trim(),
                                    "phone", profilePhone.getText().toString().trim())
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "updateUsersCollection fail");
                    }
                });
    }

    private void updateContactsCollection() {
        db.collectionGroup("userContacts").whereEqualTo("id", firebaseUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                document.getReference().update("displayName", profileName.getText().toString().trim(),
                                        "phone", profilePhone.getText().toString().trim());
                            }
                            Toast.makeText(getApplicationContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
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
        topLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        bottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}
