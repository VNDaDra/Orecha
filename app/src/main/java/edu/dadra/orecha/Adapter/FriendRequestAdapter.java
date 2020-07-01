package edu.dadra.orecha.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
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

import edu.dadra.orecha.MainActivity;
import edu.dadra.orecha.Model.FriendRequest;
import edu.dadra.orecha.Model.Users;
import edu.dadra.orecha.R;

public class FriendRequestAdapter extends FirestoreRecyclerAdapter <FriendRequest, FriendRequestAdapter.ViewHolder> {

    private static final String TAG = "FriendRequestAdapter";
    private Context context;

    private FirebaseFirestore db;
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

        holder.declineButton.setOnClickListener(v -> deleteFriendRequest(friendRequest));

        holder.acceptButton.setOnClickListener(v -> {
            holder.acceptButton.setEnabled(false);
            updateContact(friendRequest, holder);
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        context = parent.getContext();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        getCurrentUserData();
        return new ViewHolder(view);
    }

    @Override
    public void onError(@NonNull FirebaseFirestoreException e) {
        Log.d(TAG, Objects.requireNonNull(e.getMessage()));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
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
        DocumentReference friendRequestRef = db
                .collection("friendRequest").document(currentUserData.getId())
                .collection("listOfFriendRequest").document(friendRequest.getSenderId());
        friendRequestRef.delete();
    }

    private void updateContact(FriendRequest friendRequest, ViewHolder holder) {
        db.collection("users").document(friendRequest.getSenderId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            WriteBatch batch = db.batch();

                            DocumentReference friendIdRef = db
                                    .collection("contacts").document(currentUserData.getId())
                                    .collection("userContacts").document(friendRequest.getSenderId());
                            batch.set(friendIdRef, Objects.requireNonNull(document.getData()));

                            DocumentReference myIdRef = db
                                    .collection("contacts").document(friendRequest.getSenderId())
                                    .collection("userContacts").document(currentUserData.getId());
                            batch.set(myIdRef, currentUserData);

                            batch.commit().addOnCompleteListener(task1 -> {
                                if(task1.isSuccessful()){
                                    createChatRoom(friendRequest);
                                    deleteFriendRequest(friendRequest);
                                } else {
                                    holder.acceptButton.setEnabled(true);
                                    Toast.makeText(context, "LỖI\nVui lòng thử lại", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
    }

    private void createChatRoom(FriendRequest friendRequest) {
        WriteBatch batch = db.batch();

        DocumentReference myRoomIdRef = db
                .collection("rooms").document(currentUserData.getId())
                .collection("userRooms").document();
        String roomId = myRoomIdRef.getId();

        Map<String, Object> myRoomData = new HashMap<>();
        myRoomData.put("roomId", roomId);
        myRoomData.put("displayName", friendRequest.getSenderName());
        myRoomData.put("friendId", friendRequest.getSenderId());
        myRoomData.put("lastMessageTime", null);

        DocumentReference friendRoomIdRef = db
                .collection("rooms").document(friendRequest.getSenderId())
                .collection("userRooms").document(roomId);

        Map<String, Object> friendRoomData = new HashMap<>();
        friendRoomData.put("roomId", roomId);
        friendRoomData.put("displayName", currentUserData.getDisplayName());
        friendRoomData.put("friendId", currentUserData.getId());
        friendRoomData.put("lastMessageTime", null);

        batch.set(myRoomIdRef, myRoomData);
        batch.set(friendRoomIdRef, friendRoomData);

        DocumentReference friendIdRef = db
                .collection("contacts").document(currentUserData.getId())
                .collection("userContacts").document(friendRequest.getSenderId());
        DocumentReference myIdRef = db
                .collection("contacts").document(friendRequest.getSenderId())
                .collection("userContacts").document(currentUserData.getId());
        batch.update(friendIdRef, "roomId", roomId);
        batch.update(myIdRef, "roomId", roomId);

        batch.commit()
                .addOnCompleteListener(task -> Log.d(TAG, "Create room complete"))
                .addOnFailureListener(e -> Log.d(TAG, "Create room fail"));
    }

    private void getCurrentUserData() {
        currentUserData = MainActivity.currentUserData;
    }
}
