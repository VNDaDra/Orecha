package edu.dadra.orecha;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class FullScreenImageActivity extends AppCompatActivity {

    private FirebaseStorage storage;
    private StorageReference storageRef;

    private ImageView fullScreenImage;

    private String imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        Intent intent = getIntent();
        imageUri = intent.getStringExtra("imageUri");

        init();

        if (imageUri != null && !imageUri.equals("")) {
            RequestOptions options = new RequestOptions()
                    .fitCenter()
                    .error(R.drawable.error)
                    .priority(Priority.HIGH);
            Glide.with(this)
                    .load(storage.getReferenceFromUrl(imageUri))
                    .apply(options)
                    .into(fullScreenImage);
        }
    }

    private void init() {
        Toolbar toolbar = findViewById(R.id.fullscreen_image_toolbar);
        fullScreenImage = findViewById(R.id.full_screen_image);

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl(imageUri);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.fullscreen_image_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.download_option) {
            downloadImage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void downloadImage() {
        ActivityCompat.requestPermissions(FullScreenImageActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

        File path = new File("/storage/emulated/0/Download", "orecha");
        if(!path.exists()) {
            path.mkdirs();
        }
        File localFile = new File(path, storageRef.getName());

        storageRef.getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(getApplicationContext(), "Đã tải xuống", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Lỗi", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
