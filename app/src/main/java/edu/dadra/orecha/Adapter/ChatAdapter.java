package edu.dadra.orecha.Adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.storage.StorageReference;

import java.io.File;

import edu.dadra.orecha.FullScreenImageActivity;
import edu.dadra.orecha.Model.Message;
import edu.dadra.orecha.R;

public class ChatAdapter extends FirestoreRecyclerAdapter<Message, ChatAdapter.ViewHolder> {

    private static final int MSG_TEXT_LEFT = 1;
    private static final int MSG_TEXT_RIGHT = 2;
    private static final int MSG_IMAGE_LEFT = 3;
    private static final int MSG_IMAGE_RIGHT = 4;
    private static final int MSG_FILE_LEFT = 5;
    private static final int MSG_FILE_RIGHT = 6;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    private Context context;

    public ChatAdapter(@NonNull FirestoreRecyclerOptions<Message> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Message message) {
        if (message.getType().equals("text")) {
            holder.body.setText(message.getMessage());
            holder.body.setOnLongClickListener(v -> {
                showDeleteDialog(message);
                return true;
            });
        } else if (message.getType().equals("image") && !message.getMessage().equals("")) {
            RequestOptions options = new RequestOptions()
                    .fitCenter()
                    .error(R.drawable.error)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
            Glide.with(context)
                    .load(storage.getReferenceFromUrl(message.getMessage()))
                    .apply(options)
                    .into(holder.image);

            holder.image.setOnClickListener(v -> {
                Intent fullScreenImageIntent = new Intent(context, FullScreenImageActivity.class);
                fullScreenImageIntent.putExtra("imageUri", message.getMessage());
                context.startActivity(fullScreenImageIntent);
            });

            holder.image.setOnLongClickListener(v -> {
                showDeleteDialog(message);
                return true;
            });
        } else if (message.getType().equals("file") && !message.getMessage().equals("")) {
            StorageReference fileRef = storage.getReferenceFromUrl(message.getMessage());
            holder.fileName.setText(message.getFile().getName());
            holder.fileSize.setText(String.valueOf(message.getFile().getSizeInKB()));

            holder.download.setOnClickListener(v -> downloadFile(fileRef));
            holder.fileLayout.setOnLongClickListener(v -> {
                showDeleteDialog(message);
                return false;
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

        switch (viewType) {
            case MSG_TEXT_LEFT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_receiver, parent, false);
                break;
            case MSG_IMAGE_LEFT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_image_receiver, parent, false);
                break;
            case MSG_IMAGE_RIGHT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_image_sender, parent, false);
                break;
            case MSG_FILE_LEFT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_file_receiver, parent, false);
                break;
            case MSG_FILE_RIGHT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_file_sender, parent, false);
                break;
            default:    //text right
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_sender, parent, false);
        }

        return new ViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).getType().equals("text")) {
            if (getItem(position).getSenderId().equals(firebaseUser.getUid())) {
                return MSG_TEXT_RIGHT;
            } else return MSG_TEXT_LEFT;
        }
        else if (getItem(position).getType().equals("image")){
            if (getItem(position).getSenderId().equals(firebaseUser.getUid())) {
                return MSG_IMAGE_RIGHT;
            }
            else return MSG_IMAGE_LEFT;
        } else {
            if (getItem(position).getSenderId().equals(firebaseUser.getUid())) {
                return MSG_FILE_RIGHT;
            }
            else return MSG_FILE_LEFT;
        }

    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView body;
        ImageView image;
        TextView fileName, fileSize;
        ImageButton download;
        LinearLayout fileLayout;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.message_body);

            image = itemView.findViewById(R.id.message_image);      //image message

            fileName = itemView.findViewById(R.id.message_file_name);
            fileSize = itemView.findViewById(R.id.message_file_size);
            download = itemView.findViewById(R.id.message_download_file);
            fileLayout = itemView.findViewById(R.id.message_file_layout);
        }
    }

    private void showDeleteDialog(Message message) {
        View view = ((FragmentActivity)context).getLayoutInflater().inflate(R.layout.layout_chat_bottom_sheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(view);
        dialog.show();

        LinearLayout deleteMessage = dialog.findViewById(R.id.chat_delete_message);
        deleteMessage.setOnClickListener(v -> {
            deleteMessage(message);
            dialog.dismiss();
        });

    }

    private void deleteMessage(Message message) {
        DocumentReference myMessageRef = db
                .collection("messages").document(firebaseUser.getUid())
                .collection("messagesWith").document(message.getRoomId())
                .collection("messagesOfThisRoom").document(message.getId());

        if (message.getType().equals("text")) {
            myMessageRef.delete();
        }
        else if (message.getType().equals("image")) {
            myMessageRef.delete();
            StorageReference imageRef = storage.getReferenceFromUrl(message.getMessage());
            imageRef.delete();
        } else {
            myMessageRef.delete();
            StorageReference fileRef = storage.getReferenceFromUrl(message.getMessage());
            fileRef.delete();
        }
    }

    private void downloadFile(StorageReference fileRef) {
        File path = new File(Environment.getExternalStorageDirectory() + "/Orecha/");
        if(!path.exists()) {
            path.mkdirs();
        }
        File localFile = new File(path, fileRef.getName());

        fileRef.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> Toast.makeText(context, "Đã tải xuống\n Kiểm tra trong thư mục Orecha",
                        Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Lỗi\n" + e.getMessage(), Toast.LENGTH_LONG).show());
    }

}