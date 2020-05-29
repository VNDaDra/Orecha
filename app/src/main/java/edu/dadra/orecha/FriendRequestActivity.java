package edu.dadra.orecha;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import edu.dadra.orecha.Adapter.FriendRequestAdapter;
import edu.dadra.orecha.Model.FriendRequest;

public class FriendRequestActivity extends AppCompatActivity {

    private RecyclerView friendRequestRecyclerView;
    private FriendRequestAdapter friendRequestAdapter;

    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request_list);

        init();

        CollectionReference friendRequestRef = db.collection("friendRequest").document(firebaseUser.getUid())
                .collection("listOfFriendRequest");
        Query query = friendRequestRef.orderBy("time", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<FriendRequest> options = new FirestoreRecyclerOptions.Builder<FriendRequest>()
                .setQuery(query, FriendRequest.class).build();
        friendRequestAdapter = new FriendRequestAdapter(options);
        friendRequestAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
            }
        });
        friendRequestAdapter.notifyDataSetChanged();
        friendRequestRecyclerView.setAdapter(friendRequestAdapter);
    }

    private void init() {
        Toolbar toolbar = findViewById(R.id.friend_request_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        friendRequestRecyclerView = findViewById(R.id.friend_request_rv);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext(),
                LinearLayoutManager.VERTICAL, false);
        friendRequestRecyclerView.setLayoutManager(linearLayoutManager);
        db = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void setUnseenFriendRequestToZero () {
        DocumentReference requestRef = db.collection("friendRequest").document(firebaseUser.getUid());
        requestRef.update("unseen", 0);
    }

    @Override
    public void onStart() {
        super.onStart();
        friendRequestAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        friendRequestAdapter.stopListening();
        setUnseenFriendRequestToZero();
    }
}
