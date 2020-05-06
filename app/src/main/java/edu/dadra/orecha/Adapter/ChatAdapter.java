package edu.dadra.orecha.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import edu.dadra.orecha.FullScreenImage;
import edu.dadra.orecha.Model.Message;
import edu.dadra.orecha.R;

public class ChatAdapter extends FirestoreRecyclerAdapter<Message, ChatAdapter.ViewHolder> {

    private static final int MSG_TEXT_LEFT = 0;
    private static final int MSG_TEXT_RIGHT = 1;
    private static final int MSG_IMAGE_LEFT = 2;
    private static final int MSG_IMAGE_RIGHT = 3;

    private FirebaseFirestore db;
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
            holder.message.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showDeleteDialog(message);
                    return true;
                }
            });
        }

        if (message.getType().equals("image") && !message.getMessage().equals("")) {
            RequestOptions options = new RequestOptions()
                    .fitCenter()
                    .error(R.drawable.error)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
            Glide.with(context)
                    .load(storage.getReferenceFromUrl(message.getMessage()))
                    .apply(options)
                    .into(holder.image);

            holder.image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent fullScreenImageIntent = new Intent(context, FullScreenImage.class);
                    fullScreenImageIntent.putExtra("imageUri", message.getMessage());
                    context.startActivity(fullScreenImageIntent);
                }
            });
            holder.image.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showDeleteDialog(message);
                    return true;
                }
            });
        }


    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        context = parent.getContext();
        db = FirebaseFirestore.getInstance();
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

    private void showDeleteDialog(Message message) {
        View view = ((FragmentActivity)context).getLayoutInflater().inflate(R.layout.layout_chat_bottom_sheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(view);
        dialog.show();

        LinearLayout deleteMessage = dialog.findViewById(R.id.chat_delete_message);
        deleteMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMessage(message);
                dialog.dismiss();
            }
        });

    }

    private void deleteMessage(Message message) {
        DocumentReference messageRef = db.collection("messages").document(message.getRoomId())
                .collection("messagesOfThisRoom").document(message.getId());
        if (message.getType().equals("text")) {
            messageRef.delete();
        }
        else {
            //LATER - delete image in storage
            messageRef.delete();
        }
    }

}