package edu.dadra.orecha.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import edu.dadra.orecha.Model.FriendRequest;
import edu.dadra.orecha.Model.Users;
import edu.dadra.orecha.R;

public class FriendRequestAdapter extends FirestoreRecyclerAdapter <FriendRequest, FriendRequestAdapter.ViewHolder> {

    private static final String TAG = "FriendRequestAdapter";
    private Context context;

    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private FirebaseStorage storage;

    private Users currentUserData;

    public FriendRequestAdapter(@Nonnull FirestoreRecyclerOptions<FriendRequest> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, FriendRequest friendRequest) {
        holder.displayName.setText(friendRequest.getSenderName());

        if (!friendRequest.getSenderAvatar().equals("")) {
            Glide.with(context)
                    .load(storage.getReferenceFromUrl(friendRequest.getSenderAvatar()))
                    .placeholder(R.drawable.orange)
                    .into(holder.avatar);
        } else Glide.with(context)
                .load(R.drawable.orange)
                .placeholder(R.drawable.orange)
                .into(holder.avatar);

        holder.declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteFriendRequest(friendRequest);
            }
        });

        holder.acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateContact(friendRequest);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        context = parent.getContext();
        db = FirebaseFirestore.getInstance();
        firebaseUser  = FirebaseAuth.getInstance().getCurrentUser();
        storage = FirebaseStorage.getInstance();

        getCurrentUserData();
        return new ViewHolder(view);
    }

    @Override
    public void onError(@NonNull FirebaseFirestoreException e) {
        Log.d(TAG, Objects.requireNonNull(e.getMessage()));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView displayName;
        ImageView avatar;
        Button declineButton;
        Button acceptButton;
        ViewHolder(View itemView) {
            super(itemView);
            displayName = itemView.findViewById(R.id.friend_request_display_name);
            avatar = itemView.findViewById(R.id.friend_request_avatar);
            declineButton = itemView.findViewById(R.id.friend_request_decline);
            acceptButton = itemView.findViewById(R.id.friend_request_accept);
        }
    }

    private void deleteFriendRequest(FriendRequest friendRequest) {
        DocumentReference friendRequestRef = db.collection("friendRequest").document(firebaseUser.getUid())
                .collection("listOfFriendRequest").document(friendRequest.getSenderId());
        friendRequestRef.delete();
    }

    private void updateContact(FriendRequest friendRequest) {
        db.collection("users").document(friendRequest.getSenderId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        WriteBatch batch = db.batch();

                        DocumentReference friendIdRef = db.collection("contacts").document(firebaseUser.getUid())
                                .collection("userContacts").document(friendRequest.getSenderId());
                        batch.set(friendIdRef, Objects.requireNonNull(document.getData()));

                        DocumentReference myIdRef = db.collection("contacts").document(friendRequest.getSenderId())
                                .collection("userContacts").document(firebaseUser.getUid());
                        batch.set(myIdRef, currentUserData);

                        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                createChatRoom(friendRequest);
                                deleteFriendRequest(friendRequest);
                            }
                        });
                    }
                }
            }
        });
    }

    private void createChatRoom(FriendRequest friendRequest) {
        WriteBatch batch = db.batch();

        DocumentReference roomIdRef = db.collection("rooms").document();
        String roomId = roomIdRef.getId();

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("id", roomId);

        batch.set(roomIdRef, roomData);

        //Update chat information in MY contact collection
        DocumentReference friendIdRef = db.collection("contacts").document(firebaseUser.getUid())
                .collection("userContacts").document(friendRequest.getSenderId());
        batch.update(friendIdRef, "roomId", roomId);
        batch.update(friendIdRef, "hasChat", true);
        batch.update(friendIdRef, "lastMessageTime", null);

        //Update chat information in FRIEND contact collection
        DocumentReference myIdRef = db.collection("contacts").document(friendRequest.getSenderId())
                .collection("userContacts").document(firebaseUser.getUid());
        batch.update(myIdRef, "roomId", roomId);
        batch.update(myIdRef, "hasChat", true);
        batch.update(myIdRef, "lastMessageTime", null);

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "Create RoomId complete");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Create RoomId fail");
            }
        });
    }

    private void getCurrentUserData() {
        db.collection("users").document(firebaseUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        currentUserData = document.toObject(Users.class);
                    }
                }
            }
        });
    }
}
