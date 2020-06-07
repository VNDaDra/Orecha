package edu.dadra.orecha.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
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

    public void onBindViewHolder(ViewHolder holder, int position, Rooms room) {
        displayFriendInformation(room.getFriendId(), holder);

        displayLastMessage(room.getRoomId(), holder);

        displayUnseenMessage(room.getRoomId(), holder);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startChatRoom(room);
            }
        });

        PopupMenu popup = new PopupMenu(context, holder.time);
        popup.inflate(R.menu.chat_list_menu);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
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
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                popup.show();
                return true;
            }
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
        DocumentReference friendRef = db
                .collection("users").document(friendId);
        friendRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
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
            }
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
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {
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
                        }
                        else {
                            holder.lastMessage.setText(lastMessage);
                            holder.time.setText(formatDate(date));
                        }

                        lastMessage = "";
                    }
                });
    }

    private void displayUnseenMessage(String roomId, ViewHolder holder) {
        DocumentReference requestRef = db
                .collection("messages").document(currentUserData.getId())
                .collection("messagesWith").document(roomId);
        requestRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
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
            }
        });
    }

    private String formatDate(Date date) {
        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/YYYY");

        if (sdf.format(date).equals(sdf.format(currentDate))) {
            sdf = new SimpleDateFormat("HH:mm");
            return sdf.format(date);
        } else {
            sdf = new SimpleDateFormat("dd/MM");
            return sdf.format(date);
        }
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

//        CollectionReference myMessagesRef = db
//                .collection("messages").document(currentUserData.getId())
//                .collection("messagesWith").document(room.getRoomId())
//                .collection("messagesOfThisRoom");
//        DocumentReference friendMessagesRef = db.collection("messages").document(room.getRoomId())
//                .collection("messagesWith").document(currentUserData.getId());
//        batch.delete(myMessagesRef);
//        batch.delete(friendMessagesRef);

        batch.commit()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(context, "Đã xóa", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Không thể xóa", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmDelete(Rooms room) {
        new MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme)
                .setTitle("Xóa ?")
                .setMessage("Xóa cả tin nhắn của đối phương")
                .setNegativeButton("Hủy bỏ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteRoom(room);
                    }
                })
                .show();
    }

    private void getCurrentUserData() {
        currentUserData = MainActivity.currentUserData;
    }

}
