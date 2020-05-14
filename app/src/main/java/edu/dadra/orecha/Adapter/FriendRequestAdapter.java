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
import com.google.firebase.storage.FirebaseStorage;

import java.util.Objects;

import javax.annotation.Nonnull;

import edu.dadra.orecha.Model.FriendRequest;
import edu.dadra.orecha.Model.Users;
import edu.dadra.orecha.R;

public class FriendRequestAdapter extends FirestoreRecyclerAdapter <FriendRequest, FriendRequestAdapter.ViewHolder> {

    private final String TAG = "FriendRequestAdapter";
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
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(holder.avatar);
        } else Glide.with(context)
                .load(R.drawable.ic_launcher_foreground)
                .placeholder(R.drawable.ic_launcher_foreground)
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
                updateMyContact(friendRequest);
                updateFriendContact(friendRequest);
                deleteFriendRequest(friendRequest);
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

    private void updateMyContact(FriendRequest friendRequest) {
        db.collection("users").document(friendRequest.getSenderId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        DocumentReference friendIdRef = db.collection("contacts").document(firebaseUser.getUid())
                                .collection("userContacts").document(friendRequest.getSenderId());

                        friendIdRef.set(document.getData())
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "updateMyContact error", e);
                                    }
                                });
                        friendIdRef.update("hasChat", false);
                        friendIdRef.update("roomId", "");
                    }
                }
            }
        });
    }

    private void updateFriendContact(FriendRequest friendRequest) {
        DocumentReference myIdRef = db.collection("contacts").document(friendRequest.getSenderId())
                .collection("userContacts").document(firebaseUser.getUid());

        myIdRef.set(currentUserData)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "updateFriendContact error", e);
                    }
                });
        myIdRef.update("hasChat", false);
        myIdRef.update("roomId", "");
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
