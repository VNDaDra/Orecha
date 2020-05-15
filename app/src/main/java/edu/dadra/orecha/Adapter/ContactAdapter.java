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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.dadra.orecha.ChatActivity;
import edu.dadra.orecha.Model.Friends;
import edu.dadra.orecha.ProfileActivity;
import edu.dadra.orecha.R;

public class ContactAdapter extends FirestoreRecyclerAdapter<Friends, ContactAdapter.ViewHolder> {

    private final String TAG = "ContactAdapter";
    private Context context;

    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private FirebaseStorage storage;

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

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!friend.getHasChat()) {
                    createChatRoom(friend);

                } else {
                    startChatRoom(friend, friend.getRoomId());
                }
            }
        });

        PopupMenu popup = new PopupMenu(context, holder.menu);
        popup.inflate(R.menu.contact_menu);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
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
            }
        });
        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.show();
            }
        });

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        context = parent.getContext();
        db = FirebaseFirestore.getInstance();
        firebaseUser  = FirebaseAuth.getInstance().getCurrentUser();
        storage = FirebaseStorage.getInstance();
        return new ViewHolder(view);
    }

    @Override
    public void onError(FirebaseFirestoreException e) {
        Log.d(TAG, Objects.requireNonNull(e.getMessage()));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
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

    private void startChatRoom(Friends friend, String roomId) {
        Intent chatIntent = new Intent(context, ChatActivity.class);
        chatIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        chatIntent.putExtra("roomId", roomId);
        chatIntent.putExtra("displayName", friend.getDisplayName());
        chatIntent.putExtra("friendId", friend.getId());
        chatIntent.putExtra("friendAvatar", friend.getPhotoUrl());
        context.startActivity(chatIntent);
    }

    private void createChatRoom(Friends friend) {
        WriteBatch batch = db.batch();

        DocumentReference roomIdRef = db.collection("rooms").document();
        String roomId = roomIdRef.getId();

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("id", roomId);

        batch.set(roomIdRef, roomData);

        //Update chat information in MY contact collection
        DocumentReference friendIdRef = db.collection("contacts").document(firebaseUser.getUid())
                .collection("userContacts").document(friend.getId());
        batch.update(friendIdRef, "roomId", roomId);
        batch.update(friendIdRef, "hasChat", true);
        batch.update(friendIdRef, "lastMessageTime", null);

        //Update chat information in FRIEND contact collection
        DocumentReference myIdRef = db.collection("contacts").document(friend.getId())
                .collection("userContacts").document(firebaseUser.getUid());
        batch.update(myIdRef, "roomId", roomId);
        batch.update(myIdRef, "hasChat", true);
        batch.update(myIdRef, "lastMessageTime", null);

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                startChatRoom(friend, roomId);
                Log.d(TAG, "Create RoomId complete");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Create RoomId fail");
            }
        });
    }

    private void moveToProfileActivity(Friends friend) {
        Intent profileIntent = new Intent(context, ProfileActivity.class);
        profileIntent.putExtra("id", friend.getId());
        profileIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(profileIntent);
    }

    private void deleteFriend(Friends friend) {
        WriteBatch batch = db.batch();
        String roomId = friend.getRoomId();

        DocumentReference friendIdRef = db.collection("contacts").document(firebaseUser.getUid())
                .collection("userContacts").document(friend.getId());
        DocumentReference myIdRef = db.collection("contacts").document(friend.getId())
                .collection("userContacts").document(firebaseUser.getUid());

        try {
            DocumentReference roomIdRef = db.collection("rooms").document(roomId);
            batch.delete(roomIdRef);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "Have no room");
        }

        //Can't delete all messages of roomId because it has sub-collection **FireStore Limitation**
//        DocumentReference roomId_messagesCollection = db.collection("messages").document(roomId);
//        batch.delete(roomId_messagesCollection);

        batch.delete(friendIdRef);
        batch.delete(myIdRef);

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(context, "Đã xóa bạn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete(Friends friend) {
        new MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme)
                .setTitle("Xóa ?")
                .setMessage("Xóa cả tin nhắn với người này")
                .setNegativeButton("Hủy bỏ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteFriend(friend);
                    }
                })
                .show();
    }
}
