package edu.dadra.orecha.Main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

import edu.dadra.orecha.Adapter.ContactAdapter;
import edu.dadra.orecha.Model.Friends;
import edu.dadra.orecha.R;

public class ContactFragment extends Fragment {

    private static final String TAG = "ContactFragment";

    private View contactFragmentView;
    private RecyclerView contactRecyclerView;
    private LinearLayoutManager linearLayoutManager;
    ContactAdapter contactAdapter;

    private FirebaseUser user;
    private FirebaseFirestore db;
    private CollectionReference contactsRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        contactFragmentView = inflater.inflate(R.layout.fragment_contact, container, false);

        init();

        contactsRef = db.collection("contacts").document(user.getUid())
                .collection("userContacts");
        Query query = contactsRef.orderBy("email", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<Friends> options = new FirestoreRecyclerOptions.Builder<Friends>()
                .setQuery(query, Friends.class).build();
        contactAdapter = new ContactAdapter(options);
        contactAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                contactRecyclerView.scrollToPosition(contactAdapter.getItemCount() - 1);
            }
        });
        contactAdapter.notifyDataSetChanged();
        contactRecyclerView.setAdapter(contactAdapter);

        return contactFragmentView;
    }

    private void init() {
        contactRecyclerView = contactFragmentView.findViewById(R.id.rv_contact);
        linearLayoutManager = new LinearLayoutManager(Objects.requireNonNull(getActivity()).getApplicationContext(),
                LinearLayoutManager.VERTICAL, false);
        contactRecyclerView.setLayoutManager(linearLayoutManager);
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
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
