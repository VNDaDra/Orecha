package edu.dadra.orecha;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.dadra.orecha.Adapter.ChatAdapter;
import edu.dadra.orecha.Model.Friends;
import edu.dadra.orecha.Model.Message;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private final int PICK_IMAGE_REQUEST = 1;

    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private CollectionReference messagesRef;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;

    private EditText messageField;
    private ImageButton sendButton, chooseImageButton, clearImageButton;
    private ImageView friendAvatar, previewImage;
    private TextView friendName;

    private String roomId, friendId;
    private Timestamp lastMessageTime;
    private Uri filePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initLayout();

        getDataFromPreviousActivity();

        initFirebase();

        initRecyclerView();

        showFriendInformation();

        showMessages();

        chooseImage();

        clearImage();

        sendButtonListener();

        moveToProfileActivity();
    }

    private void initLayout() {
        Toolbar toolbar = findViewById(R.id.chat_toolbar);
        messageField = findViewById(R.id.chat_message);
        previewImage =findViewById(R.id.chat_preview_image);
        chooseImageButton = findViewById(R.id.chat_choose_image_iButton);
        clearImageButton = findViewById(R.id.chat_clear_image);
        sendButton = findViewById(R.id.chat_send_iButton);
        friendAvatar = findViewById(R.id.chat_toolbar_icon);
        friendName = findViewById(R.id.chat_toolbar_title);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void getDataFromPreviousActivity() {
        Intent intent = getIntent();
        roomId = intent.getStringExtra("roomId");
        friendId = intent.getStringExtra("friendId");
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference("image_message");
    }

    private void initRecyclerView () {
        chatRecyclerView = findViewById(R.id.chat_messages_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(linearLayoutManager);
    }

    private void showFriendInformation() {
        DocumentReference friendRef = db.collection("users").document(friendId);
        friendRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                try {
                    Friends friend = documentSnapshot.toObject(Friends.class);
                    friendName.setText(friend.getDisplayName());

                    if (!friend.getPhotoUrl().equals("")) {
                        Glide.with(getApplicationContext())
                                .load(storage.getReferenceFromUrl(friend.getPhotoUrl()))
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .into(friendAvatar);
                    } else Glide.with(getApplicationContext())
                            .load(R.drawable.ic_launcher_foreground)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .into(friendAvatar);
                } catch (NullPointerException npe) {
                    Log.d(TAG, Objects.requireNonNull(npe.getMessage()));
                }


            }
        });
    }

    private void showMessages() {
        messagesRef = db.collection("messages").document(roomId)
                .collection("messagesOfThisRoom");
        Query query = messagesRef.orderBy("time", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<Message> options = new FirestoreRecyclerOptions.Builder<Message>()
                .setQuery(query, Message.class).build();
        chatAdapter = new ChatAdapter(options);
        chatAdapter.registerAdapterDataObserver(    new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                chatRecyclerView.setHasFixedSize(true);
            }
        });
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void chooseImage() {
        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE_REQUEST);
            }
        });
    }

    private void clearImage() {
        clearImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previewImage.setVisibility(View.GONE);
                messageField.setVisibility(View.VISIBLE);
                filePath = null;
                clearImageButton.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null ) {
            filePath = data.getData();
            messageField.setVisibility(View.INVISIBLE);
            previewImage.setVisibility(View.VISIBLE);
            clearImageButton.setVisibility(View.VISIBLE);
            previewImage.setImageURI(filePath);
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cR.getType(uri));
    }

    private void sendButtonListener() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentMessage = messageField.getText().toString().trim();
                if (!currentMessage.equals("")) {
                    lastMessageTime = new Timestamp(new Date());
                    sendMessage(currentMessage);
                    setLastMessageTime();
                }

                if (currentMessage.equals("") && filePath != null) {

                    clearImageButton.setVisibility(View.GONE);
                    lastMessageTime = new Timestamp(new Date());
                    uploadImageToStorage();
                    setLastMessageTime();
                } else {
                    messageField.setHint("Hãy nhập một tin nhắn");
                }
                messageField.setVisibility(View.VISIBLE);
                messageField.setText("");
                filePath = null;

            }
        });
    }

    private void uploadImageToStorage() {
        if(filePath != null) {

            StorageReference ref = storageReference.child(roomId)
                    .child(firebaseUser.getUid() + "_" + System.currentTimeMillis() + "." + getFileExtension(filePath));
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            sendImage(ref.toString());
                            previewImage.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Lỗi "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void sendImage(String imageRef) {
        messagesRef = db.collection("messages").document(roomId)
                .collection("messagesOfThisRoom");
        Map<String, Object> messageInfo = new HashMap<>();

        messageInfo.put("senderId", firebaseUser.getUid());
        messageInfo.put("message", imageRef);
        messageInfo.put("time", new Timestamp(new Date() ));
        messageInfo.put("type", "image");

        messagesRef.add(messageInfo)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Không thể gửi tin nhắn",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void setLastMessageTime() {
        if (lastMessageTime != null) {
            db.collection("contacts").document(firebaseUser.getUid())
                    .collection("userContacts").document(friendId)
                    .update("lastMessageTime", lastMessageTime);
            db.collection("contacts").document(friendId)
                    .collection("userContacts").document(firebaseUser.getUid())
                    .update("lastMessageTime", lastMessageTime);
        }
    }

    private void sendMessage(String message) {
        messagesRef = db.collection("messages").document(roomId)
                .collection("messagesOfThisRoom");
        Map<String, Object> messageInfo = new HashMap<>();

        messageInfo.put("senderId", firebaseUser.getUid());
        messageInfo.put("message", message);
        messageInfo.put("time", new Timestamp(new Date() ));
        messageInfo.put("type", "text");

        messagesRef.add(messageInfo)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Không thể gửi tin nhắn",
                                Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void moveToProfileActivity() {
        friendAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profileIntent = new Intent(getApplicationContext(), ProfileActivity.class);
                profileIntent.putExtra("id", friendId);
                profileIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(profileIntent);
            }
        });

        friendName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profileIntent = new Intent(getApplicationContext(), ProfileActivity.class);
                profileIntent.putExtra("id", friendId);
                profileIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(profileIntent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        chatAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        chatAdapter.stopListening();
    }
}
