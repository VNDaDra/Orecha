package edu.dadra.orecha;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
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
    private DocumentReference myMessagesRef, friendMessagesRef;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;

    private EditText messageField;
    private ImageButton sendButton, chooseImageButton, clearImageButton;
    private ImageView friendAvatar, previewImage;
    private TextView friendName;
    private ProgressBar sendImageProgress;

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

        displayMessages();

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
        sendImageProgress = findViewById(R.id.chat_send_progress);

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
        storageReference = storage.getReference("image_message").child(firebaseUser.getUid());
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
                                .placeholder(R.drawable.orange)
                                .into(friendAvatar);
                    } else Glide.with(getApplicationContext())
                            .load(R.drawable.orange)
                            .placeholder(R.drawable.orange)
                            .into(friendAvatar);
                } catch (NullPointerException npe) {
                    Log.d(TAG, Objects.requireNonNull(npe.getMessage()));
                }
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void displayMessages() {
        CollectionReference allMessageRef = db
                .collection("messages").document(firebaseUser.getUid())
                .collection("messagesWith").document(roomId)
                .collection("messagesOfThisRoom");
        Query query = allMessageRef.orderBy("time", Query.Direction.ASCENDING);

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
        chatRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboard();
                return false;
            }
        });
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
            messageField.setText("");
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
                    sendImageProgress.setVisibility(View.VISIBLE);

                    lastMessageTime = new Timestamp(new Date());
                    uploadImageToStorage();
                    setLastMessageTime();
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
                            sendImageProgress.setVisibility(View.GONE);
                            previewImage.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Lỗi "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            sendImageProgress.setProgress( (int) progress);
                        }
                    });
        }
    }

    private void sendImage(String imageRef) {
        WriteBatch batch = db.batch();
        myMessagesRef = db.collection("messages").document(firebaseUser.getUid())
                .collection("messagesWith").document(roomId).collection("messagesOfThisRoom")
                .document();

        String messageId = myMessagesRef.getId();
        Map<String, Object> messageInfo = new HashMap<>();

        messageInfo.put("id", messageId);
        messageInfo.put("roomId", roomId);
        messageInfo.put("senderId", firebaseUser.getUid());
        messageInfo.put("message", imageRef);
        messageInfo.put("time", new Timestamp(new Date() ));
        messageInfo.put("type", "image");

        batch.set(myMessagesRef, messageInfo);

        friendMessagesRef = db.collection("messages").document(friendId)
                .collection("messagesWith").document(roomId).collection("messagesOfThisRoom")
                .document(messageId);

        batch.set(friendMessagesRef, messageInfo);

        batch.commit()
                .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Không thể gửi tin nhắn",
                                Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                increaseUnseenCounter(friendId);
            }
        });
    }

    private void setLastMessageTime() {
        if (lastMessageTime != null) {
            db.collection("rooms").document(firebaseUser.getUid())
                    .collection("userRooms").document(roomId)
                    .update("lastMessageTime", lastMessageTime);
            db.collection("rooms").document(friendId)
                    .collection("userRooms").document(roomId)
                    .update("lastMessageTime", lastMessageTime);
        }
    }

    private void sendMessage(String message) {
        WriteBatch batch = db.batch();
        myMessagesRef = db.collection("messages").document(firebaseUser.getUid())
                .collection("messagesWith").document(roomId).collection("messagesOfThisRoom")
                .document();
        String messageId = myMessagesRef.getId();
        Map<String, Object> messageInfo = new HashMap<>();

        messageInfo.put("id", messageId);
        messageInfo.put("roomId", roomId);
        messageInfo.put("senderId", firebaseUser.getUid());
        messageInfo.put("message", message);
        messageInfo.put("time", new Timestamp(new Date() ));
        messageInfo.put("type", "text");

        batch.set(myMessagesRef, messageInfo);

        friendMessagesRef = db.collection("messages").document(friendId)
                .collection("messagesWith").document(roomId).collection("messagesOfThisRoom")
                .document(messageId);

        batch.set(friendMessagesRef, messageInfo);

        batch.commit()
            .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Không thể gửi tin nhắn",
                        Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                increaseUnseenCounter(friendId);
            }
        });
    }

    private void increaseUnseenCounter(String friendId) {
        DocumentReference requestRef = db
                .collection("messages").document(friendId)
                .collection("messagesWith").document(roomId);
        Map<String, Object> unseenCounter = new HashMap<>();
        unseenCounter.put("unseen", FieldValue.increment(1));
        requestRef.set(unseenCounter, SetOptions.merge());
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

    private void setUnseenMessageToZero () {
        DocumentReference requestRef = db
                .collection("messages").document(firebaseUser.getUid())
                .collection("messagesWith").document(roomId);
        requestRef.update("unseen", 0);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            messageField.clearFocus();
        }
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
        setUnseenMessageToZero();
    }
}
