package edu.dadra.orecha;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.dadra.orecha.Main.ChatListFragment;
import edu.dadra.orecha.Main.ContactFragment;
import edu.dadra.orecha.Model.Users;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Toolbar toolbar;
    private AlertDialog dialog;

    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    public static Users currentUserData;

    private BottomNavigationView bottomNavigationView;
    private ImageView currentUserAvatar;
    private TextView mainTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        currentUserData = new Users();
        getCurrentUserData();

        initLayout();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        moveToProfileActivity();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment fragment;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    fragment = new ChatListFragment();
                    openFragment(fragment);
                    return true;
                case R.id.navigation_contact:
                    fragment = new ContactFragment();
                    openFragment(fragment);
                    return true;
            }
            return false;
        }
    };

    public void openFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.find_friend_option) {
            showAddFriendDialog();
        }
        if (item.getItemId() == R.id.change_password_option) {
            moveToChangePasswordActivity();
        }
        if (item.getItemId() == R.id.logout_option) {
            confirmLogout();
        }
        return true;
    }

    private void initLayout() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        openFragment(new ChatListFragment());

        toolbar = findViewById(R.id.main_toolbar);
        currentUserAvatar = findViewById(R.id.main_toolbar_icon);
        mainTitle = findViewById(R.id.main_toolbar_title);

        try {
            displayBottomNavigateBadge();
        } catch (NullPointerException ex) {
            Log.d(TAG, "Have no friendRequest");
        }
    }

    private void displayBottomNavigateBadge() {
        DocumentReference requestRef = db.collection("friendRequest").document(firebaseUser.getUid());
        requestRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (snapshot != null && snapshot.exists()) {
                    int unseen = snapshot.getLong("unseen").intValue();
                    BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.navigation_contact);
                    if (unseen > 0) {
                        badge.setVisible(true);
                    } else {
                        badge.setVisible(false);
                    }
                }
            }
        });
    }

    private void getCurrentUserData() {
        db.collection("users").document(firebaseUser.getUid())
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
                                        .placeholder(R.drawable.orange)
                                        .into(currentUserAvatar);
                            } else Glide.with(getApplicationContext())
                                    .load(R.drawable.orange)
                                    .placeholder(R.drawable.orange)
                                    .into(currentUserAvatar);

                            mainTitle.setText(currentUserData.getDisplayName());
                        }
                    }
                });
    }

    private void moveToProfileActivity() {
        currentUserAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profileIntent = new Intent(getApplicationContext(), ProfileActivity.class);
                profileIntent.putExtra("id", firebaseUser.getUid());
                profileIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(profileIntent);
            }
        });
    }

    private void showAddFriendDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.layout_add_friend, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle("Thêm Bạn");
        builder.setView(view);

        TextInputLayout addFriendEmailField = view.findViewById(R.id.textLayout_add_friend_by_email);

        builder.setPositiveButton("Thêm", null);
        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String friendEmail = addFriendEmailField.getEditText().getText().toString().trim();
                if (friendEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    findUser(friendEmail);
                } else {
                    addFriendEmailField.setError("Không hợp lệ");
                }
            }
        });
    }

    private void findUser (String friendEmail) {

        db.collection("users")
                .whereEqualTo("email", friendEmail)
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                Toast.makeText(getApplicationContext(), "Không tìm thấy", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            } else {
                                dialog.dismiss();
                                for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                    validateExistFriend(document);
                                }
                            }
                        }
                    }
                });
    }

    private void validateExistFriend(QueryDocumentSnapshot friend) {
        DocumentReference friendIdRef = db.collection("contacts").document(firebaseUser.getUid())
                .collection("userContacts").document(friend.getId());
        final String friendId = friend.getId();

        friendIdRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot snapshot) {
                if (snapshot.exists() || friendId.equals(firebaseUser.getUid())) {
                    Toast.makeText(getApplicationContext(), "Người này đã có trong danh bạ", Toast.LENGTH_SHORT).show();
                } else {
                    DocumentReference friendRequestRef = db.collection("friendRequest").document(friendId)
                            .collection("listOfFriendRequest").document(currentUserData.getId());
                    friendRequestRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(getApplicationContext(), "Hãy đợi đối phương đồng ý", Toast.LENGTH_SHORT).show();
                            } else {
                                DocumentReference myRequestRef = db.collection("friendRequest").document(currentUserData.getId())
                                        .collection("listOfFriendRequest").document(friendId);
                                myRequestRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            Toast.makeText(getApplicationContext(), "Họ đã gửi yêu cầu kết bạn\n Hãy đồng ý", Toast.LENGTH_SHORT).show();
                                        } else {
                                            sendFriendRequest(friendId);
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void sendFriendRequest(String friendId) {
        DocumentReference friendRequestRef = db.collection("friendRequest").document(friendId)
                .collection("listOfFriendRequest").document(currentUserData.getId());

        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("senderId", currentUserData.getId());
        requestInfo.put("senderName", currentUserData.getDisplayName());
        requestInfo.put("senderAvatar", currentUserData.getPhotoUrl());
        requestInfo.put("receiverId", friendId);
        requestInfo.put("state", "waiting");
        requestInfo.put("time", new Timestamp(new Date() ));

        friendRequestRef.set(requestInfo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        increaseUnseenCounter(friendId);
                        Toast.makeText(getApplicationContext(), "Đã gửi yêu cầu kết bạn", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void increaseUnseenCounter(String friendId) {
        DocumentReference requestRef = db.collection("friendRequest").document(friendId);
        Map<String, Object> unseenCounter = new HashMap<>();
        unseenCounter.put("unseen", FieldValue.increment(1));
        requestRef.set(unseenCounter, SetOptions.merge());
    }

    private void moveToChangePasswordActivity() {
        Intent changePasswordIntent = new Intent(getApplicationContext(), ChangePassword.class);
        changePasswordIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(changePasswordIntent);
    }

    private void logout() {
        mAuth.signOut();
        Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
        finish();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
                .setTitle("Xác nhận ?")
                .setNegativeButton("Hủy bỏ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Đăng xuất", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logout();
                    }
                })
                .show();
    }
}
