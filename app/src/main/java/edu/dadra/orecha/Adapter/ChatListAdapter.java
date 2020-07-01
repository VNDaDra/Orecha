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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import javax.annotation.Nonnull;

import edu.dadra.orecha.ChatActivity;
import edu.dadra.orecha.MainActivity;
import edu.dadra.orecha.Model.Message;
import edu.dadra.orecha.Model.Rooms;
import edu.dadra.orecha.Model.Users;
import edu.dadra.orecha.ProfileActivity;
import edu.dadra.orecha.R;

public class ChatListAdapter extends FirestoreRecyclerAdapter<Rooms, ChatListAdapter.ViewHolder> {
    private static final String TAG = "ChatListAdapter";
    private Context context;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String lastMessage, type;
    private Date date;

    private Users currentUserData;

    public ChatListAdapter(@NonNull FirestoreRecyclerOptions<Rooms> options) {
        super(options);
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
    }

    public void onBindViewHolder(@NonNull ViewHolder holder, int position, Rooms room) {
        displayFriendInformation(room.getFriendId(), holder);

        displayLastMessage(room.getRoomId(), holder);

        displayUnseenMessage(room.getRoomId(), holder);

        holder.itemView.setOnClickListener(v -> startChatRoom(room));

        PopupMenu popup = new PopupMenu(context, holder.time);
        popup.inflate(R.menu.chat_list_menu);
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.show_profile_option:
                    moveToProfileActivity(room);
                    return true;
                case R.id.delete_message_option:
                    confirmDelete(room);
                    return true;
                default:
                    return false;
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            popup.show();
            return true;
        });
    }

    @Nonnull
    @Override
    public ViewHolder onCreateViewHolder(@Nonnull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
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
        TextView displayName, lastMessage, time, unseen;
        ImageView avatar;
        ViewHolder(View itemView) {
            super(itemView);
            displayName = itemView.findViewById(R.id.textView_display_name);
            lastMessage = itemView.findViewById(R.id.textView_last_message);
            time = itemView.findViewById(R.id.textView_time);
            unseen = itemView.findViewById(R.id.textView_unseen);
            avatar = itemView.findViewById(R.id.imageView_avatar);
        }
    }

    private void displayFriendInformation(String friendId, ViewHolder holder) {
        DocumentReference friendRef = db.collection("users").document(friendId);
        friendRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.d(TAG, "Listen failed.", e);
                return;
            }
            Users friend = snapshot.toObject(Users.class);
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
        });
    }

    private void displayLastMessage(String roomId, ViewHolder holder) {
        CollectionReference messagesRef = db
                .collection("messages").document(currentUserData.getId())
                .collection("messagesWith").document(roomId)
                .collection("messagesOfThisRoom");
        Query query = messagesRef.orderBy("time", Query.Direction.DESCENDING);
        lastMessage = "";
        query.limit(1)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.d(TAG, "Listen failed.", e);
                        return;
                    }
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Message lastMesObj = doc.toObject(Message.class);
                        lastMessage = lastMesObj.getMessage();
                        type = lastMesObj.getType();
                        date = lastMesObj.getTime().toDate();
                    }
                    if (lastMessage.equals("")) {
                        holder.lastMessage.setVisibility(View.INVISIBLE);
                        holder.time.setVisibility(View.INVISIBLE);
                    }
                    else if (type.equals("image")) {
                        holder.lastMessage.setText(R.string.image_tag);
                        holder.time.setText(formatDate(date));
                    } else if (type.equals("file")) {
                        holder.lastMessage.setText(R.string.file_tag);
                        holder.time.setText(formatDate(date));
                    }
                    else {
                        holder.lastMessage.setText(lastMessage);
                        holder.time.setText(formatDate(date));
                    }

                    lastMessage = "";
                });
    }

    private void displayUnseenMessage(String roomId, ViewHolder holder) {
        DocumentReference requestRef = db
                .collection("messages").document(currentUserData.getId())
                .collection("messagesWith").document(roomId);
        requestRef.addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                int unseenCounter = snapshot.getLong("unseen").intValue();
                if (unseenCounter > 0 && unseenCounter < 9) {
                    holder.unseen.setText(String.valueOf(unseenCounter));
                    holder.unseen.setVisibility(View.VISIBLE);
                } else if (unseenCounter > 9) {
                    holder.unseen.setText("9+");
                    holder.unseen.setVisibility(View.VISIBLE);
                }
                else {
                    holder.unseen.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private String formatDate(Date date) {
        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/YYYY");

        if (sdf.format(date).equals(sdf.format(currentDate))) {
            sdf = new SimpleDateFormat("HH:mm");
        } else {
            sdf = new SimpleDateFormat("dd/MM");
        }
        return sdf.format(date);
    }

    private void startChatRoom(Rooms room) {
        Intent chatIntent = new Intent(context, ChatActivity.class);
        chatIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        chatIntent.putExtra("roomId", room.getRoomId());
        chatIntent.putExtra("friendId", room.getFriendId());
        context.startActivity(chatIntent);
    }

    private void moveToProfileActivity(Rooms room) {
        Intent profileIntent = new Intent(context, ProfileActivity.class);
        profileIntent.putExtra("id", room.getFriendId());
        profileIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(profileIntent);
    }

    private void deleteRoom(Rooms room) {
        WriteBatch batch = db.batch();

        //Delete room in rooms collection
        DocumentReference myRoomRef = db
                .collection("rooms").document(currentUserData.getId())
                .collection("userRooms").document(room.getRoomId());
        DocumentReference friendRoomRef = db
                .collection("rooms").document(room.getFriendId())
                .collection("userRooms").document(room.getRoomId());
        batch.delete(myRoomRef);
        batch.delete(friendRoomRef);

        DocumentReference friendRef = db
                .collection("contacts").document(currentUserData.getId())
                .collection("userContacts").document(room.getFriendId());
        DocumentReference myRef = db
                .collection("contacts").document(room.getFriendId())
                .collection("userContacts").document(currentUserData.getId());
        batch.update(friendRef, "roomId", "");
        batch.update(myRef, "roomId", "");

        batch.commit()
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Đã xóa", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Không thể xóa", Toast.LENGTH_SHORT).show());
    }

    private void deleteAllMessage(Rooms room) {
        CollectionReference myMessagesRef = db
                .collection("messages").document(currentUserData.getId())
                .collection("messagesWith").document(room.getRoomId())
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
                .collection("messagesWith").document(room.getRoomId()).delete();

        CollectionReference friendMessageRef = db
                .collection("messages").document(room.getFriendId())
                .collection("messagesWith").document(room.getRoomId())
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
        db.collection("messages").document(room.getFriendId())
                .collection("messagesWith").document(room.getRoomId()).delete();
    }

    private void confirmDelete(Rooms room) {
        new MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme)
                .setTitle("Xóa phòng chat?")
                .setMessage("Xóa cả tin nhắn của đối phương")
                .setNegativeButton("Hủy bỏ", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteAllMessage(room);
                    deleteRoom(room);
                })
                .show();
    }

    private void getCurrentUserData() {
        currentUserData = MainActivity.currentUserData;
    }

}
