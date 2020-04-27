package edu.dadra.orecha.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import javax.annotation.Nonnull;

import edu.dadra.orecha.ChatActivity;
import edu.dadra.orecha.Model.Friends;
import edu.dadra.orecha.Model.Message;
import edu.dadra.orecha.R;

public class ChatListAdapter extends FirestoreRecyclerAdapter<Friends, ChatListAdapter.ViewHolder> {
    private static final String TAG = "ChatListAdapter";
    private Context context;

    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseFirestore db;
    private String lastMessage;
    private Date date;

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
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(holder.avatar);
        } else Glide.with(context)
                .load(R.drawable.ic_launcher_foreground)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(holder.avatar);

        getLastMessage(friend.getRoomId(), holder.lastMessageTextView, holder.time);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startChatRoom(friend);
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
        storageReference = storage.getReference();
        return new ViewHolder(view);
    }

    @Override
    public void onError(FirebaseFirestoreException e) {
        Log.d(TAG, Objects.requireNonNull(e.getMessage()));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
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
        lastMessage = "blank";

        query.limit(1).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for(QueryDocumentSnapshot doc : task.getResult()) {
                                Message lastMesObj = doc.toObject(Message.class);
                                lastMessage = lastMesObj.getMessage();
                                date = lastMesObj.getTime().toDate();
                            }
                            if (lastMessage.equals("blank")) {
                                lastMessageTextView.setVisibility(View.GONE);
                                time.setVisibility(View.GONE);
                            } else {
                                lastMessageTextView.setText(lastMessage);
                                time.setText(dateFormat(date));
                            }

                            lastMessage = "blank";
                        }
                    }
                });
    }

    private String dateFormat(Date date) {
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

}
