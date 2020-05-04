package edu.dadra.orecha.Adapter;

import android.content.Context;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;

import edu.dadra.orecha.Model.Message;
import edu.dadra.orecha.R;

public class ChatAdapter extends FirestoreRecyclerAdapter<Message, ChatAdapter.ViewHolder> {

    private static final int MSG_TEXT_LEFT = 0;
    private static final int MSG_TEXT_RIGHT = 1;
    private static final int MSG_IMAGE_LEFT = 2;
    private static final int MSG_IMAGE_RIGHT = 3;

    private FirebaseStorage storage;

    private Context context;

    private FirebaseUser firebaseUser  = FirebaseAuth.getInstance().getCurrentUser();

    public ChatAdapter(@NonNull FirestoreRecyclerOptions<Message> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Message message) {
        if (message.getType().equals("text")) {
            holder.message.setText(message.getMessage());
        }
        if (message.getType().equals("image")) {
            Glide.with(context).load(storage.getReferenceFromUrl(message.getMessage()))
                    .fitCenter()
                    .into(holder.image);
        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        context = parent.getContext();
        storage = FirebaseStorage.getInstance();

        if(viewType == MSG_TEXT_RIGHT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sender, parent, false);
        }
        else if (viewType == MSG_TEXT_LEFT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_receiver, parent, false);
        }
        else if (viewType == MSG_IMAGE_RIGHT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_sender, parent, false);
        }
        else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_receiver, parent, false);
        }

        return new ViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        if(getItem(position).getType().equals("text")) {
            if (getItem(position).getSenderId().equals(firebaseUser.getUid())) {
                return MSG_TEXT_RIGHT;
            }
            else return MSG_TEXT_LEFT;
        }
        else {
            if (getItem(position).getSenderId().equals(firebaseUser.getUid())) {
                return MSG_IMAGE_RIGHT;
            }
            else return MSG_IMAGE_LEFT;
        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView  message;
        ImageView image;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.message_body);
            image = itemView.findViewById(R.id.message_image);

        }
    }

}