package edu.dadra.orecha.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.dadra.orecha.ChatActivity;
import edu.dadra.orecha.MainActivity;
import edu.dadra.orecha.Model.Friends;
import edu.dadra.orecha.Model.Users;
import edu.dadra.orecha.ProfileActivity;
import edu.dadra.orecha.R;

public class ContactAdapter extends FirestoreRecyclerAdapter<Friends, ContactAdapter.ViewHolder> {

    private static final String TAG = "ContactAdapter";
    private Context context;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Users currentUserData;

    public ContactAdapter(@NonNull FirestoreRecyclerOptions<Friends> options) {
        super(options);
    }

    public void onBindViewHolder(ViewHolder holder, int position, Friends friend) {
        holder.displayName.setText(friend.getDisplayName());

        if (!friend.getPhotoUrl().equals("")) {
            Glide.with(context)
                    .load(storage.getReferenceFromUrl(friend.getPhotoUrl()))
                    .placeholder(R.drawable.orange)
                    .into(holder.avatar);
        } else Glide.with(context)
                .load(R.drawable.orange)
                .placeholder(R.drawable.orange)
                .into(holder.avatar);

        holder.itemView.setOnClickListener(v -> {
            if (friend.getRoomId().equals("")) {
                createChatRoom(friend);
            } else {
                startChatRoom(friend.getId(), friend.getRoomId());
            }
        });

        PopupMenu popup = new PopupMenu(context, holder.menu);
        popup.inflate(R.menu.contact_menu);
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.show_profile_option:
                    moveToProfileActivity(friend);
                    return true;
                case R.id.delete_friend_option:
                    confirmDelete(friend);
                    return true;
                default:
                    return false;
            }
        });
        holder.menu.setOnClickListener(v -> popup.show());

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        context = parent.getContext();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        getCurrentUserData();
        return new ViewHolder(view);
    }

    @Override
    public void onError(FirebaseFirestoreException e) {
        Log.d(TAG, Objects.requireNonNull(e.getMessage()));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView displayName;
        ImageView avatar;
        ImageView menu;
        ViewHolder(View itemView) {
            super(itemView);
            displayName = itemView.findViewById(R.id.contact_display_name);
            avatar = itemView.findViewById(R.id.contact_avatar);
            menu = itemView.findViewById(R.id.contact_option_button);
        }
    }

    private void startChatRoom(String friendId, String roomId) {
        Intent chatIntent = new Intent(context, ChatActivity.class);
        chatIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        chatIntent.putExtra("roomId", roomId);
        chatIntent.putExtra("friendId", friendId);
        context.startActivity(chatIntent);
    }

    private void createChatRoom(Friends friend) {
        WriteBatch batch = db.batch();

        DocumentReference myRoomIdRef = db.collection("rooms").document(currentUserData.getId())
                .collection("userRooms").document();
        String roomId = myRoomIdRef.getId();

        Map<String, Object> myRoomData = new HashMap<>();
        myRoomData.put("roomId", roomId);
        myRoomData.put("friendId", friend.getId());
        myRoomData.put("lastMessageTime", null);

        DocumentReference friendRoomIdRef = db.collection("rooms").document(friend.getId())
                .collection("userRooms").document(roomId);

        Map<String, Object> friendRoomData = new HashMap<>();
        friendRoomData.put("roomId", roomId);
        friendRoomData.put("friendId", currentUserData.getId());
        friendRoomData.put("lastMessageTime", null);

        batch.set(myRoomIdRef, myRoomData);
        batch.set(friendRoomIdRef, friendRoomData);

        DocumentReference friendIdRef = db.collection("contacts").document(currentUserData.getId())
                .collection("userContacts").document(friend.getId());
        DocumentReference myIdRef = db.collection("contacts").document(friend.getId())
                .collection("userContacts").document(currentUserData.getId());
        batch.update(friendIdRef, "roomId", roomId);
        batch.update(myIdRef, "roomId", roomId);

        batch.commit().addOnCompleteListener(task -> {
            Log.d(TAG, "Create room complete");
            if (task.isSuccessful()) {
                startChatRoom(friend.getId(), roomId);
            }
        }).addOnFailureListener(e -> Log.d(TAG, "Create room fail"));
    }

    private void moveToProfileActivity(Friends friend) {
        Intent profileIntent = new Intent(context, ProfileActivity.class);
        profileIntent.putExtra("id", friend.getId());
        profileIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(profileIntent);
    }

    private void deleteFriend(Friends friend) {
        WriteBatch batch = db.batch();

        DocumentReference friendRef = db.collection("contacts").document(currentUserData.getId())
                .collection("userContacts").document(friend.getId());
        DocumentReference myRef = db.collection("contacts").document(friend.getId())
                .collection("userContacts").document(currentUserData.getId());

        batch.delete(friendRef);
        batch.delete(myRef);

        try {
            //Delete both rooms
            DocumentReference myRoomIdRef = db.collection("rooms").document(currentUserData.getId())
                    .collection("userRooms").document(friend.getRoomId());
            DocumentReference friendRoomIdRef = db.collection("rooms").document(friend.getId())
                    .collection("userRooms").document(friend.getRoomId());
            batch.delete(myRoomIdRef);
            batch.delete(friendRoomIdRef);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "Have no room");
        }

        batch.commit().addOnCompleteListener(task -> Toast.makeText(context, "Đã xóa bạn", Toast.LENGTH_SHORT).show());
    }

    private void deleteAllMessage(Friends friend) {
        CollectionReference myMessagesRef = db
                .collection("messages").document(currentUserData.getId())
                .collection("messagesWith").document(friend.getRoomId())
                .collection("messagesOfThisRoom");
        myMessagesRef.get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        for(QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            documentSnapshot.getReference().delete();
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });
        db.collection("messages").document(currentUserData.getId())
                .collection("messagesWith").document(friend.getRoomId()).delete();

        CollectionReference friendMessageRef = db
                .collection("messages").document(friend.getId())
                .collection("messagesWith").document(friend.getRoomId())
                .collection("messagesOfThisRoom");
        friendMessageRef.get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        for(QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            documentSnapshot.getReference().delete();
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });
        db.collection("messages").document(friend.getId())
                .collection("messagesWith").document(friend.getRoomId()).delete();
    }

    private void confirmDelete(Friends friend) {
        new MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme)
                .setTitle("Xóa ?")
                .setMessage("Xóa cả tin nhắn với người này")
                .setNegativeButton("Hủy bỏ", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Xóa", (dialog, which) -> {
                    try {
                        deleteAllMessage(friend);
                    } catch (Exception e) {
                       Log.d(TAG, "Have no message");
                    }
                    deleteFriend(friend);
                })
                .show();
    }

    private void getCurrentUserData() {
        currentUserData = MainActivity.currentUserData;
    }
}
