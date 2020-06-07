package edu.dadra.orecha.Main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.Objects;

import edu.dadra.orecha.Adapter.ContactAdapter;
import edu.dadra.orecha.FriendRequestActivity;
import edu.dadra.orecha.Model.Friends;
import edu.dadra.orecha.R;

public class ContactFragment extends Fragment {

    private View contactFragmentView;
    private RecyclerView contactRecyclerView;
    private ContactAdapter contactAdapter;

    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;

    private LinearLayout friendRequest;
    private TextView unseenFriendRequest;
    private EditText searchBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        contactFragmentView = inflater.inflate(R.layout.fragment_contact, container, false);

        init();

        moveToFriendRequestActivity();

        displayUnseenFriendRequestBadge();

        CollectionReference contactsRef = db.collection("contacts").document(firebaseUser.getUid())
                .collection("userContacts");
        Query query = contactsRef.orderBy("email", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<Friends> defaultOptions = new FirestoreRecyclerOptions.Builder<Friends>()
                .setQuery(query, Friends.class).build();
        contactAdapter = new ContactAdapter(defaultOptions);
        contactAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
            }
        });
        contactAdapter.notifyDataSetChanged();
        contactRecyclerView.setAdapter(contactAdapter);

        contactRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboard();
                return false;
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    Query filterQuery = db.collection("contacts").document(firebaseUser.getUid())
                            .collection("userContacts")
                            .orderBy("displayName", Query.Direction.ASCENDING)
                            .startAt(s.toString().trim())
                            .endAt(s.toString().trim() + "\uf8ff");
                    FirestoreRecyclerOptions<Friends> filterOption = new FirestoreRecyclerOptions.Builder<Friends>()
                            .setQuery(filterQuery, Friends.class).build();
                    contactAdapter.updateOptions(filterOption);
                } else {
                    contactAdapter.updateOptions(defaultOptions);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return contactFragmentView;
    }

    private void init() {
        contactRecyclerView = contactFragmentView.findViewById(R.id.rv_contact);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(Objects.requireNonNull(getActivity()).getApplicationContext(),
                LinearLayoutManager.VERTICAL, false);
        contactRecyclerView.setLayoutManager(linearLayoutManager);

        initFirebase();
        initLayout();
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void initLayout() {
        searchBar = contactFragmentView.findViewById(R.id.contact_search_bar);
        friendRequest = contactFragmentView.findViewById(R.id.list_friend_request);
        unseenFriendRequest = contactFragmentView.findViewById(R.id.contact_unseen_friend_request);
        searchBar.clearFocus();
    }

    private void displayUnseenFriendRequestBadge() {
        DocumentReference requestRef = db.collection("friendRequest").document(firebaseUser.getUid());
        requestRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (snapshot != null && snapshot.exists()) {
                    int unseen = snapshot.getLong("unseen").intValue();
                    if (unseen > 0 && unseen < 9) {
                        unseenFriendRequest.setText(String.valueOf(unseen));
                        unseenFriendRequest.setVisibility(View.VISIBLE);
                    } else if (unseen > 9) {
                        unseenFriendRequest.setText("9+");
                        unseenFriendRequest.setVisibility(View.VISIBLE);
                    }
                    else {
                        unseenFriendRequest.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void moveToFriendRequestActivity() {
        friendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity().getApplicationContext(), FriendRequestActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
    }

    private void hideKeyboard() {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        searchBar.clearFocus();
    }

    @Override
    public void onStart() {
        super.onStart();
        contactAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        contactAdapter.stopListening();
    }

}
