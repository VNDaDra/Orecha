package edu.dadra.orecha;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.storage.FirebaseStorage;

public class FullScreenImage extends AppCompatActivity {

    FirebaseStorage storage;
    String imageUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ImageView fullScreenImage = findViewById(R.id.full_screen_image);
        Intent intent = getIntent();
        storage = FirebaseStorage.getInstance();
        imageUri = intent.getStringExtra("imageUri");
        if (imageUri != null && !imageUri.equals("")) {
            RequestOptions options = new RequestOptions()
                    .fitCenter()
                    .error(R.drawable.error)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .priority(Priority.HIGH);
            Glide.with(this)
                    .load(storage.getReferenceFromUrl(imageUri))
                    .apply(options)
                    .into(fullScreenImage);
        }
    }
}
