package edu.dadra.orecha;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.dadra.orecha.Adapter.ChatAdapter;
import edu.dadra.orecha.Model.Message;
import edu.dadra.orecha.Model.Friends;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private CollectionReference messagesRef;
    private FirebaseStorage storage;
    private StorageReference storageReference;


    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;

    private EditText messageField;
    private ImageButton sendButton;
    private ImageView friendAvatar;
    private TextView friendName;

    private String roomId, friendId;
    private Timestamp lastMessageTime;

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

        sendButtonListener();

        moveToProfileActivity();
    }

    private void initLayout() {
        Toolbar toolbar = findViewById(R.id.chat_toolbar);
        messageField = findViewById(R.id.editText_message);
        sendButton = findViewById(R.id.iButton_send);
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
        storageReference = storage.getReference();
    }

    private void initRecyclerView () {
        chatRecyclerView = findViewById(R.id.messages_view);
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
            }
        });
        chatRecyclerView.setAdapter(chatAdapter);
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
                } else {
                    messageField.setHint("Hãy nhập một tin nhắn");
                }
                messageField.setText("");
            }
        });
    }

    private void sendMessage(String message) {
        messagesRef = db.collection("messages").document(roomId)
                .collection("messagesOfThisRoom");
        Map<String, Object> messageInfo = new HashMap<>();

        messageInfo.put("senderId", firebaseUser.getUid());
        messageInfo.put("message", message);
        messageInfo.put("time", new Timestamp(new Date() ));

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
