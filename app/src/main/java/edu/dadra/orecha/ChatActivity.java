package edu.dadra.orecha;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.dadra.orecha.Adapter.ChatAdapter;
import edu.dadra.orecha.Model.FileMessage;
import edu.dadra.orecha.Model.Friends;
import edu.dadra.orecha.Model.Message;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private final int PICK_IMAGE_REQUEST = 1;
    private final int PICK_FILE_REQUEST = 2;

    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private DocumentReference myMessagesRef, friendMessagesRef;
    private FirebaseStorage storage;
    private StorageReference imageReference, fileReference;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;

    private EditText messageField;
    private ImageButton sendButton, chooseImageButton, chooseFileButton, clearImageButton, clearFileButton;
    private ImageView friendAvatar, previewImage;
    private TextView friendName, fileName, fileSize;
    private ProgressBar sendImageProgress, sendFileProgress;
    private CardView fileCardView;

    private String roomId, friendId;
    private Timestamp lastMessageTime;
    private Uri imagePath, filePath;

    private FileMessage currentFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        init();

        showFriendInformation();

        displayMessages();

        chooseImage();

        chooseFile();

        clearImageButton.setOnClickListener(v -> clearImage());

        clearFileButton.setOnClickListener(v -> clearFile());

        sendButtonListener();

        moveToProfileActivity();
    }

    private void init() {
        initLayout();
        initFirebase();
        initRecyclerView();
        getDataFromPreviousActivity();
        imagePath = null;
        filePath = null;
    }

    private void initLayout() {
        Toolbar toolbar = findViewById(R.id.chat_toolbar);
        messageField = findViewById(R.id.chat_message);

        previewImage =findViewById(R.id.chat_preview_image);
        clearImageButton = findViewById(R.id.chat_clear_image);
        sendImageProgress = findViewById(R.id.chat_send_image_progress);
        fileCardView = findViewById(R.id.chat_file_card_view);
        fileName = findViewById(R.id.chat_file_name);
        fileSize = findViewById(R.id.chat_file_size);
        sendFileProgress = findViewById(R.id.chat_send_file_progress);
        clearFileButton = findViewById(R.id.chat_clear_file);

        sendButton = findViewById(R.id.chat_send_iButton);
        chooseImageButton = findViewById(R.id.chat_choose_image_iButton);
        chooseFileButton = findViewById(R.id.chat_choose_file);

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
        imageReference = storage.getReference("image_message").child(firebaseUser.getUid());
        fileReference = storage.getReference("file_message").child(firebaseUser.getUid());
    }

    private void initRecyclerView () {
        chatRecyclerView = findViewById(R.id.chat_messages_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(linearLayoutManager);
    }

    private void showFriendInformation() {
        DocumentReference friendRef = db.collection("users").document(friendId);
        friendRef.addSnapshotListener((documentSnapshot, e) -> {
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
        chatRecyclerView.setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });
    }

    private void chooseImage() {
        chooseImageButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE_REQUEST);
        });
    }

    private void chooseFile() {
        chooseFileButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("*/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Chọn tập tin"), PICK_FILE_REQUEST);
        });
    }

    private void clearImage() {
        previewImage.setVisibility(View.GONE);
        clearImageButton.setVisibility(View.GONE);
        messageField.setVisibility(View.VISIBLE);
        imagePath = null;
    }

    private void clearFile() {
        fileCardView.setVisibility(View.GONE);
        messageField.setVisibility(View.VISIBLE);
        filePath = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null ) {
            imagePath = data.getData();
            if (getFileSizeInKB(imagePath) <= 5120) {
                messageField.setText("");
                messageField.setVisibility(View.INVISIBLE);
                previewImage.setVisibility(View.VISIBLE);
                clearImageButton.setVisibility(View.VISIBLE);
                previewImage.setImageURI(imagePath);
            } else {
                clearImage();
                Toast.makeText(getApplicationContext(), "Hãy chọn hình nhỏ hơn 5MB", Toast.LENGTH_LONG).show();
            }
        }
        else if(requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            filePath = data.getData();
            if(getFileSizeInKB(filePath) <= 51200) {
                messageField.setText("");
                messageField.setVisibility(View.INVISIBLE);

                currentFile = new FileMessage(getFileName(filePath), getFileExtension(filePath), getFileSizeInKB(filePath));
                fileCardView.setVisibility(View.VISIBLE);

                fileName.setText(currentFile.getName());
                fileSize.setText(String.valueOf(currentFile.getSizeInKB()));
            } else {
                clearFile();
                Toast.makeText(getApplicationContext(), "Hãy chọn tập tin nhỏ hơn 50MB", Toast.LENGTH_LONG).show();
            }
        }
    }

    private long getFileSizeInKB(Uri path) {
        Cursor returnCursor = getContentResolver().query(path,
                null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        return  returnCursor.getLong(sizeIndex) / 1024;
    }

    private String getFileName(Uri path) {
        String returnString, fullString;
        Cursor returnCursor = getContentResolver().query(path,
                null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        fullString = returnCursor.getString(nameIndex);
        if (fullString.length() > 20) {
            returnString = fullString.substring(0, 7) + "..." +
                    fullString.substring(fullString.length() - 10);
        } else {
            returnString = fullString;
        }
        return returnString;
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cR.getType(uri));
    }

    private void sendButtonListener() {
        sendButton.setOnClickListener(view -> {
            String currentMessage = messageField.getText().toString().trim();
            if (!currentMessage.equals("")) {
                lastMessageTime = new Timestamp(new Date());
                sendMessage(currentMessage);
            }

            if (currentMessage.equals("") && imagePath != null) {
                clearImageButton.setVisibility(View.GONE);
                sendImageProgress.setVisibility(View.VISIBLE);

                lastMessageTime = new Timestamp(new Date());
                uploadImageToStorage();
            }

            if (currentMessage.equals("") && filePath != null) {
                clearFileButton.setVisibility(View.GONE);
                sendFileProgress.setVisibility(View.VISIBLE);

                lastMessageTime = new Timestamp(new Date());
                uploadFileToStorage();
            }

            messageField.setVisibility(View.VISIBLE);
            messageField.setText("");
            imagePath = null;
            filePath = null;

        });
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
                .addOnFailureListener(e -> Toast.makeText(getApplicationContext(),
                        "Không thể gửi tin nhắn", Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(aVoid -> {
                    setLastMessageTime();
                    increaseUnseenMessage(friendId);
                });
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
                .addOnFailureListener(e -> Toast.makeText(getApplicationContext(),
                        "Không thể gửi tin nhắn", Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(aVoid -> {
                    setLastMessageTime();
                    increaseUnseenMessage(friendId);
                });
    }

    private void uploadImageToStorage() {
        if(imagePath != null) {

            StorageReference ref = imageReference.child(roomId)
                    .child(firebaseUser.getUid() + "_" + System.currentTimeMillis() + "." + getFileExtension(imagePath));
            ref.putFile(imagePath)
                    .addOnSuccessListener(taskSnapshot -> {
                        sendImage(ref.toString());
                        sendImageProgress.setVisibility(View.GONE);
                        previewImage.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> Toast.makeText(getApplicationContext(),
                            "Lỗi "+e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                .getTotalByteCount());
                        sendImageProgress.setProgress( (int) progress);
                    });
        }
    }

    private void sendFile(String fileRef) {
        WriteBatch batch = db.batch();
        myMessagesRef = db.collection("messages").document(firebaseUser.getUid())
                .collection("messagesWith").document(roomId).collection("messagesOfThisRoom")
                .document();

        String messageId = myMessagesRef.getId();
        Map<String, Object> messageInfo = new HashMap<>();

        messageInfo.put("id", messageId);
        messageInfo.put("roomId", roomId);
        messageInfo.put("senderId", firebaseUser.getUid());
        messageInfo.put("message", fileRef);
        messageInfo.put("time", new Timestamp(new Date() ));
        messageInfo.put("type", "file");

        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("name", currentFile.getName());
        fileInfo.put("extension", currentFile.getExtension().toUpperCase());
        fileInfo.put("sizeInKB", currentFile.getSizeInKB());

        messageInfo.put("file", fileInfo);

        batch.set(myMessagesRef, messageInfo);

        friendMessagesRef = db.collection("messages").document(friendId)
                .collection("messagesWith").document(roomId).collection("messagesOfThisRoom")
                .document(messageId);

        batch.set(friendMessagesRef, messageInfo);

        batch.commit()
                .addOnFailureListener(e -> Toast.makeText(getApplicationContext(),
                        "Không thể gửi tin nhắn", Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(aVoid -> {
                    setLastMessageTime();
                    increaseUnseenMessage(friendId);
                });
    }

    private void uploadFileToStorage() {
        if(filePath != null) {

            StorageReference ref = fileReference.child(roomId)
                    .child("file_" + System.currentTimeMillis() + "." + currentFile.getExtension());
            ref.putFile(filePath)
                    .addOnSuccessListener(taskSnapshot -> {
                        sendFile(ref.toString());
                        clearFile();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getApplicationContext(),
                            "Lỗi "+e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                .getTotalByteCount());
                        sendFileProgress.setProgress( (int) progress);
                    });
        }
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

    private void increaseUnseenMessage(String friendId) {
        DocumentReference requestRef = db
                .collection("messages").document(friendId)
                .collection("messagesWith").document(roomId);
        Map<String, Object> unseenCounter = new HashMap<>();
        unseenCounter.put("unseen", FieldValue.increment(1));
        requestRef.set(unseenCounter, SetOptions.merge());
    }

    private void moveToProfileActivity() {
        friendAvatar.setOnClickListener(v -> {
            Intent profileIntent = new Intent(getApplicationContext(), ProfileActivity.class);
            profileIntent.putExtra("id", friendId);
            profileIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(profileIntent);
        });

        friendName.setOnClickListener(v -> {
            Intent profileIntent = new Intent(getApplicationContext(), ProfileActivity.class);
            profileIntent.putExtra("id", friendId);
            profileIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(profileIntent);
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
