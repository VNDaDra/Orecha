package edu.dadra.orecha.Main;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Objects;

import edu.dadra.orecha.Adapter.ChatListAdapter;
import edu.dadra.orecha.Model.Friends;
import edu.dadra.orecha.R;

public class ChatListFragment extends Fragment {

    private static final String TAG = "ChatListFragment";

    private View chatListFragmentView;
    private RecyclerView chatListRecyclerView;
    private LinearLayoutManager linearLayoutManager;
    ChatListAdapter chatListAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private CollectionReference contactRef;

    private EditText searchBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        chatListFragmentView = inflater.inflate(R.layout.fragment_chatlist, container, false);

        init();
        searchBar = chatListFragmentView.findViewById(R.id.chat_list_search_bar);

        contactRef = db.collection("contacts").document(firebaseUser.getUid())
                .collection("userContacts");
        Query query = contactRef.whereEqualTo("hasChat", true)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<Friends> defaultOptions = new FirestoreRecyclerOptions.Builder<Friends>()
                .setQuery(query, Friends.class).build();
        chatListAdapter = new ChatListAdapter(defaultOptions);
        chatListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
            }
        });

        chatListAdapter.notifyDataSetChanged();
        chatListRecyclerView.setAdapter(chatListAdapter);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    Query filterQuery = db.collection("contacts").document(firebaseUser.getUid())
                            .collection("userContacts")
                            .whereEqualTo("hasChat", true)
                            .orderBy("displayName", Query.Direction.ASCENDING)
                            .startAt(s.toString().trim())
                            .endAt(s.toString().trim() + "\uf8ff");
                    FirestoreRecyclerOptions<Friends> filterOption = new FirestoreRecyclerOptions.Builder<Friends>()
                            .setQuery(filterQuery, Friends.class).build();
                    chatListAdapter.updateOptions(filterOption);
                } else {
                    chatListAdapter.updateOptions(defaultOptions);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return chatListFragmentView;
    }

    private void init() {
        chatListRecyclerView = chatListFragmentView.findViewById(R.id.rv_chat_list);
        linearLayoutManager = new LinearLayoutManager(Objects.requireNonNull(getActivity()).getApplicationContext(),
                LinearLayoutManager.VERTICAL, false);
        chatListRecyclerView.setLayoutManager(linearLayoutManager);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
    }

    @Override
    public void onStart() {
        super.onStart();
        chatListAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        chatListAdapter.stopListening();
    }


}
