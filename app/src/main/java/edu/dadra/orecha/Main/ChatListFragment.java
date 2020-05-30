package edu.dadra.orecha.Main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
import edu.dadra.orecha.Model.Rooms;
import edu.dadra.orecha.R;

public class ChatListFragment extends Fragment {

    private final String TAG = "ChatListFragment";

    private View chatListFragmentView;
    private RecyclerView chatListRecyclerView;
    private ChatListAdapter chatListAdapter;

    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;

    private EditText searchBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        chatListFragmentView = inflater.inflate(R.layout.fragment_chatlist, container, false);

        init();
        searchBar = chatListFragmentView.findViewById(R.id.chat_list_search_bar);

        CollectionReference roomIdRef = db.collection("rooms").document(firebaseUser.getUid())
                .collection("userRooms");
        Query query = roomIdRef.orderBy("lastMessageTime", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<Rooms> defaultOptions = new FirestoreRecyclerOptions.Builder<Rooms>()
                .setQuery(query, Rooms.class).build();
        chatListAdapter = new ChatListAdapter(defaultOptions);
        chatListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
            }
        });

        chatListAdapter.notifyDataSetChanged();
        chatListRecyclerView.setAdapter(chatListAdapter);

        chatListRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboard();
                return false;
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    Query filterQuery = db.collection("rooms").document(firebaseUser.getUid())
                            .collection("userRooms")
                            .orderBy("displayName", Query.Direction.ASCENDING)
                            .startAt(s.toString().trim())
                            .endAt(s.toString().trim() + "\uf8ff");
                    FirestoreRecyclerOptions<Rooms> filterOption = new FirestoreRecyclerOptions.Builder<Rooms>()
                            .setQuery(filterQuery, Rooms.class).build();
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
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(Objects.requireNonNull(getActivity()).getApplicationContext(),
                LinearLayoutManager.VERTICAL, false);
        chatListRecyclerView.setLayoutManager(linearLayoutManager);
        db = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void hideKeyboard() {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
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
