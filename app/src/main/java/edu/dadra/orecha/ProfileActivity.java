package edu.dadra.orecha;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

import edu.dadra.orecha.Model.Users;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private final int PICK_IMAGE_REQUEST = 1;

    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private DocumentReference userRef;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private ImageView profileAvatar;
    private TextView profileTitleName;
    private EditText profileEmail, profilePhone, profileName;
    private Button updateProfileButton;
    private ImageButton profileEditNameButton, profileEditEmailButton, profileEditPhoneButton,
            profileDeclineButton, profileAcceptButton;

    private String userId;
    private Users currentUserData;
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

    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference("avatars");
    }

    private void initLayout() {
        profileAvatar = findViewById(R.id.profile_avatar);
        profileTitleName = findViewById(R.id.profile_title_name);
        profileDeclineButton = findViewById(R.id.profile_decline);
        profileAcceptButton = findViewById(R.id.profile_accept);

        profileEmail = findViewById(R.id.profile_email);
        profileName = findViewById(R.id.profile_name);
        profilePhone = findViewById(R.id.profile_phone);

        profileEditEmailButton = findViewById(R.id.profile_edit_email);
        profileEditNameButton = findViewById(R.id.profile_edit_name);
        profileEditPhoneButton = findViewById(R.id.profile_edit_phone);

        updateProfileButton = findViewById(R.id.profile_update);

        profileName.setEnabled(false);
        profileEmail.setEnabled(false);
        profilePhone.setEnabled(false);
        updateProfileButton.setEnabled(false);

        profileName.setText("");
        profileEmail.setText("");
        profilePhone.setText("");

        profileEditEmailButton.setVisibility(View.INVISIBLE); //User can't change email right now

        //Friend view
        if (!userId.equals(firebaseUser.getUid())) {
            profileEmail.setTextColor(Color.BLACK);
            profileName.setTextColor(Color.BLACK);
            profilePhone.setTextColor(Color.BLACK);
            //Can't change friend information
            profileAvatar.setEnabled(false);
            profileEditNameButton.setVisibility(View.GONE);
            profileEditPhoneButton.setVisibility(View.GONE);
            updateProfileButton.setVisibility(View.GONE);
        }
    }

    private void getUserData(String id) {
        db.collection("users").document(id)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.d(TAG, "Listen failed.", e);
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            currentUserData = snapshot.toObject(Users.class);
                            if (!currentUserData.getPhotoUrl().equals("")) {
                                Glide.with(getApplicationContext())
                                        .load(storage.getReferenceFromUrl(currentUserData.getPhotoUrl()))
                                        .placeholder(R.drawable.ic_launcher_foreground)
                                        .into(profileAvatar);
                            } else Glide.with(getApplicationContext())
                                    .load(R.drawable.ic_launcher_foreground)
                                    .placeholder(R.drawable.ic_launcher_foreground)
                                    .into(profileAvatar);

                            profileTitleName.setText(currentUserData.getDisplayName());
                            profileName.setText(currentUserData.getDisplayName());
                            profileEmail.setText(currentUserData.getEmail());
                            profilePhone.setText(currentUserData.getPhone());
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
                chooseImage();
            }
        });
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

            Glide.with(getApplicationContext())
                    .load(filePath)
                    .into(profileAvatar);
            profileDeclineButton.setVisibility(View.VISIBLE);
            profileAcceptButton.setVisibility(View.VISIBLE);

        }
    }

    private void confirmChangeAvatar() {
        profileDeclineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileDeclineButton.setVisibility(View.GONE);
                profileAcceptButton.setVisibility(View.GONE);
                if (!currentUserData.getPhotoUrl().equals("")) {
                    Glide.with(getApplicationContext())
                            .load(storage.getReferenceFromUrl(currentUserData.getPhotoUrl()))
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .into(profileAvatar);
                } else {
                    Glide.with(getApplicationContext())
                            .load(R.drawable.ic_launcher_foreground)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .into(profileAvatar);
                }
            }
        });

        profileAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadAvatarToStorage();
            }
        });
    }

    private void uploadAvatarToStorage() {
        if(filePath != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Tải lên");
            progressDialog.show();

            StorageReference ref = storageReference.child(userId + "." + getFileExtension(filePath));
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            profileDeclineButton.setVisibility(View.INVISIBLE);
                            profileAcceptButton.setVisibility(View.INVISIBLE);
                            progressDialog.dismiss();

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
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Profile", "updateAvatarUrlInDatabase successful");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Profile", "updateAvatarUrlInDatabase fail");
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
                            Log.d("ProfileActivity", "Error getting documents: ", task.getException());
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
                    profileEmail.setEnabled(false);
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
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Profile", "updateUsersCollection successful");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Profile", "updateUsersCollection fail");
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
                            Log.d("ProfileActivity", "Error getting documents: ", task.getException());
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

}
