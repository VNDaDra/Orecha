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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;

import java.util.Objects;

import javax.annotation.Nonnull;

import edu.dadra.orecha.Model.FriendRequest;
import edu.dadra.orecha.R;

public class FriendRequestAdapter extends FirestoreRecyclerAdapter <FriendRequest, FriendRequestAdapter.ViewHolder> {

    private final String TAG = "FriendRequestAdapter";
    private Context context;

    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private DocumentReference friendIdRef, myIdRef;
    private FirebaseStorage storage;

    public FriendRequestAdapter(@Nonnull FirestoreRecyclerOptions<FriendRequest> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, FriendRequest friendRequest) {
        holder.displayName.setText(friendRequest.getSenderId());

        if (!friendRequest.getSenderAvatar().equals("")) {
            Glide.with(context)
                    .load(storage.getReferenceFromUrl(friendRequest.getSenderAvatar()))
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(holder.avatar);
        } else Glide.with(context)
                .load(R.drawable.ic_launcher_foreground)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(holder.avatar);
    }

    @NonNull
    @Override
    public FriendRequestAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        context = parent.getContext();
        db = FirebaseFirestore.getInstance();
        firebaseUser  = FirebaseAuth.getInstance().getCurrentUser();
        storage = FirebaseStorage.getInstance();
        return new FriendRequestAdapter.ViewHolder(view);
    }

    @Override
    public void onError(@NonNull FirebaseFirestoreException e) {
        Log.d(TAG, Objects.requireNonNull(e.getMessage()));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView displayName;
        ImageView avatar;
        Button decline;
        Button accept;
        ViewHolder(View itemView) {
            super(itemView);
            displayName = itemView.findViewById(R.id.friend_request_display_name);
            avatar = itemView.findViewById(R.id.friend_request_avatar);
            decline = itemView.findViewById(R.id.friend_request_decline);
            accept = itemView.findViewById(R.id.friend_request_accept);
        }
    }
}
