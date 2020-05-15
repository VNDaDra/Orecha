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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
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
import edu.dadra.orecha.Model.Friends;
import edu.dadra.orecha.Model.Message;
import edu.dadra.orecha.ProfileActivity;
import edu.dadra.orecha.R;

public class ChatListAdapter extends FirestoreRecyclerAdapter<Friends, ChatListAdapter.ViewHolder> {
    private static final String TAG = "ChatListAdapter";
    private Context context;

    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private String lastMessage, type;
    private Date date;
    private FirebaseStorage storage;

    public ChatListAdapter(@NonNull FirestoreRecyclerOptions<Friends> options) {
        super(options);
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
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

        getLastMessage(friend.getRoomId(), holder.lastMessageTextView, holder.time);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startChatRoom(friend);
            }
        });

        PopupMenu popup = new PopupMenu(context, holder.time);
        popup.inflate(R.menu.chat_list_menu);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.show_profile_option:
                        moveToProfileActivity(friend);
                        return true;
                    case R.id.delete_message_option:
                        confirmDelete(friend);
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
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        return new ViewHolder(view);
    }

    @Override
    public void onError(FirebaseFirestoreException e) {
        Log.d(TAG, Objects.requireNonNull(e.getMessage()));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView displayName, lastMessageTextView, time;
        ImageView avatar;
        ViewHolder(View itemView) {
            super(itemView);
            displayName = itemView.findViewById(R.id.textView_display_name);
            lastMessageTextView = itemView.findViewById(R.id.textView_last_message);
            time = itemView.findViewById(R.id.textView_time);
            avatar = itemView.findViewById(R.id.imageView_avatar);
        }
    }


    private void getLastMessage(String roomId, TextView lastMessageTextView, TextView time) {

        CollectionReference messagesRef = db.collection("messages").document(roomId)
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
                            lastMessageTextView.setVisibility(View.GONE);
                            time.setVisibility(View.GONE);
                        }
                        else if (type.equals("image")) {
                            lastMessageTextView.setText("[Hình ảnh]");
                            time.setText(formatDate(date));
                        }
                        else {
                            lastMessageTextView.setText(lastMessage);
                            time.setText(formatDate(date));
                        }

                        lastMessage = "";
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

    private void startChatRoom(Friends friend) {
        Intent chatIntent = new Intent(context, ChatActivity.class);
        chatIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        chatIntent.putExtra("roomId", friend.getRoomId());
        chatIntent.putExtra("displayName", friend.getDisplayName());
        chatIntent.putExtra("friendId", friend.getId());
        chatIntent.putExtra("friendAvatar", friend.getPhotoUrl());
        context.startActivity(chatIntent);
    }

    private void moveToProfileActivity(Friends friend) {
        Intent profileIntent = new Intent(context, ProfileActivity.class);
        profileIntent.putExtra("id", friend.getId());
        profileIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(profileIntent);
    }

    private void deleteMessage(Friends friend) {
        WriteBatch batch = db.batch();
        String roomId = friend.getRoomId();

        //Update chat information in MY contact collection
        DocumentReference friendIdRef = db.collection("contacts").document(firebaseUser.getUid())
                .collection("userContacts").document(friend.getId());
        batch.update(friendIdRef, "roomId", "");
        batch.update(friendIdRef, "hasChat", false);
        batch.update(friendIdRef, "lastMessageTime", null);

        //Update chat information in FRIEND contact collection
        DocumentReference myIdRef = db.collection("contacts").document(friend.getId())
                .collection("userContacts").document(firebaseUser.getUid());
        batch.update(myIdRef, "roomId", "");
        batch.update(myIdRef, "hasChat", false);
        batch.update(myIdRef, "lastMessageTime", null);

        //Delete room in rooms collection
        DocumentReference roomId_my_roomsCollection = db.collection("rooms").document(roomId);
        DocumentReference roomId_friend_roomsCollection = db.collection("rooms").document(roomId);
        batch.delete(roomId_my_roomsCollection);
        batch.delete(roomId_friend_roomsCollection);

        //Can't delete all messages of roomId because it has sub-collection **FireStore Limitation**
//        DocumentReference roomId_messageCollection = db.collection("message").document(roomId);
//        batch.delete(roomId_messageCollection);

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(context, "Đã xóa", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Không thể xóa", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete(Friends friend) {
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
                        deleteMessage(friend);
                    }
                })
                .show();
    }

}
