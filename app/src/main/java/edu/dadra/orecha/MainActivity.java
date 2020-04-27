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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Objects;

import edu.dadra.orecha.Main.ChatListFragment;
import edu.dadra.orecha.Main.ContactFragment;
import edu.dadra.orecha.Main.GroupListFragment;
import edu.dadra.orecha.Main.ViewPagerAdapter;
import edu.dadra.orecha.Model.Users;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    TabLayout tabLayout;
    ViewPager2 viewPager;
    ViewPagerAdapter viewPagerAdapter;
    Toolbar toolbar;
    private ArrayList<String> tabTitle = new ArrayList<>();

    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DocumentReference myIdRef, friendIdRef;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    public Users currentUserData;

    private ImageView currentUserAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        currentUserData = new Users();
        getCurrentUserData();

        initLayout();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPagerAdapter.addFragment(new ChatListFragment());
        viewPagerAdapter.addFragment(new GroupListFragment());
        viewPagerAdapter.addFragment(new ContactFragment());

        tabTitle.add("Chat");
        tabTitle.add("Nhóm");
        tabTitle.add("Bạn Bè");

        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setCurrentItem(0, true);

        new TabLayoutMediator(tabLayout, viewPager,
                new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                        tab.setText(tabTitle.get(position));
                    }
                }).attach();

        moveToProfileActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        getCurrentUserData();
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
        if (item.getItemId() == R.id.logout_option) {
            logout();
        }
        return true;
    }

    private void initLayout() {
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tabs);
        toolbar = findViewById(R.id.main_toolbar);
        currentUserAvatar = findViewById(R.id.main_toolbar_icon);
    }

    private void getCurrentUserData() {
        db.collection("users")
                .whereEqualTo("id", firebaseUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                currentUserData = document.toObject(Users.class);
                                if (!currentUserData.getPhotoUrl().equals("")) {
                                    Glide.with(getApplicationContext())
                                            .load(storage.getReferenceFromUrl(currentUserData.getPhotoUrl()))
                                            .placeholder(R.drawable.ic_launcher_foreground)
                                            .into(currentUserAvatar);
                                } else Glide.with(getApplicationContext())
                                        .load(R.drawable.ic_launcher_foreground)
                                        .placeholder(R.drawable.ic_launcher_foreground)
                                        .into(currentUserAvatar);

                            }
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

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String friendEmail = addFriendEmailField.getEditText().getText().toString().trim();
                if (friendEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    addFriend(friendEmail);
                    dialog.dismiss();
                } else {
                    addFriendEmailField.setError("Không hợp lệ");
                }
            }
        });
    }

    private void addFriend(String friendEmail) {

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
                                for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                    validateFriend(document);
                                }
                            }

                        }

                    }
                });
    }

    private void updateMyContact(QueryDocumentSnapshot doc) {
        friendIdRef = db.collection("contacts").document(firebaseUser.getUid())
                .collection("userContacts").document(doc.getId());

        friendIdRef.set(doc.getData())
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "updateMyContact error", e);
                    }
                });
        friendIdRef.update("hasChat", false);
        friendIdRef.update("roomId", "");
    }

    private void updateFriendContact(QueryDocumentSnapshot doc) {
        myIdRef = db.collection("contacts").document(doc.getId())
                .collection("userContacts").document(firebaseUser.getUid());

        myIdRef.set(currentUserData)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "updateFriendContact error", e);
                    }
                });
        myIdRef.update("hasChat", false);
        myIdRef.update("roomId", "");
    }

    private void validateFriend(QueryDocumentSnapshot doc) {
        friendIdRef = db.collection("contacts").document(firebaseUser.getUid())
                .collection("userContacts").document(doc.getId());

        friendIdRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (!document.exists() && !doc.getId().equals(firebaseUser.getUid())) {
                        updateMyContact(doc);
                        updateFriendContact(doc);
                        Toast.makeText(getApplicationContext(), "Thêm bạn thành công", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "addFriend Successful");
                    } else {
                        Toast.makeText(getApplicationContext(), "Người này đã có trong danh bạ", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "validateFiend: Failed");
                    }
                }
            }
        });
    }

    public void logout() {
        mAuth.signOut();
        Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
        finish();
    }
}
