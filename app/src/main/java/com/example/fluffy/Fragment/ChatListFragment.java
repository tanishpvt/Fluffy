package com.example.fluffy.Fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.example.fluffy.Adapter.AdapterChatlist;
import com.example.fluffy.GroupCreateActivity;
import com.example.fluffy.MainActivity;
import com.example.fluffy.Model.ModelChat;
import com.example.fluffy.Model.ModelChatlist;
import com.example.fluffy.Model.ModelUsers;
import com.example.fluffy.R;
import com.example.fluffy.SettingsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class ChatListFragment extends Fragment {


    //firebase auth
    FirebaseAuth firebaseAuth;

    RecyclerView recyclerView;
    List<ModelChatlist> chatlistList;
    List<ModelUsers> usersList;
    DatabaseReference reference;
    FirebaseUser currentUser;

    AdapterChatlist adapterChatlist;


    public ChatListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_chat_list, container, false);

        //init
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        recyclerView = view.findViewById(R.id.recyclerView);

        chatlistList = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Chatlist").child(currentUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatlistList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelChatlist chatlist = ds.getValue(ModelChatlist.class);
                    chatlistList.add(chatlist);
                }
                loadChats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return view;
    }

    private void loadChats() {
     usersList = new ArrayList<>();
     reference = FirebaseDatabase.getInstance().getReference("Users");
     reference.addValueEventListener(new ValueEventListener() {
         @Override
         public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
           usersList.clear();
           for (DataSnapshot ds: dataSnapshot.getChildren()){
               ModelUsers users = ds.getValue(ModelUsers.class);
               for (ModelChatlist chatlist: chatlistList){
                   if (users.getUid() != null && users.getUid().equals(chatlist.getId())){
                       usersList.add(users);
                       break;
                   }
               }
               //adapter
               adapterChatlist = new AdapterChatlist(getContext(), usersList);
               // set adapter
               recyclerView.setAdapter(adapterChatlist);
               // set lasst message
               for (int i=0; i<usersList.size();i++){
                   lastMessage(usersList.get(i).getUid());
               }

           }
         }

         @Override
         public void onCancelled(@NonNull DatabaseError databaseError) {

         }
     });
    }

    private void lastMessage(final String userId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
               String theLastMessage = "default";
               for (DataSnapshot ds: dataSnapshot.getChildren()){
                   ModelChat chat = ds.getValue(ModelChat.class);
                   if (chat==null){
                       continue;
                   }
                   String sender = chat.getSender();
                   String receiver = chat.getReceiver();
                   if (sender==null || receiver == null){
                       continue;
                   }
                   if (chat.getReceiver().equals(currentUser.getUid()) &&
                   chat.getSender().equals(userId) || chat.getReceiver().equals(userId) &&
                   chat.getSender().equals(currentUser.getUid())){
                       //instead of displaying url in message show "sent photo"
                       if (chat.getType().equals("image")){
                           theLastMessage = "Sent a photo";
                       }
                       else {
                           theLastMessage = chat.getMessage();
                       }

                   }
               }
               adapterChatlist.setLastMessageMap(userId, theLastMessage);
               adapterChatlist.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            //user is signed in stay here
            // set email of logged in user
            // mProfileTv.setText(user.getEmail());
        }
        else{
            //user not signed in, go to main activity
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true); // to show menu option in fragment
        super.onCreate(savedInstanceState);
    }

    /*inflate optiond menu*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //inflating menu
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);

        //hide addpost icon from this fragment
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);

        super.onCreateOptionsMenu(menu, inflater);
    }


    /*handle menu item clicks*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        else if (id==R.id.action_settings){
            //go to settings activity
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
        else if (id==R.id.action_create_group){
            //go to GroupCreateActivity activity
            startActivity(new Intent(getActivity(), GroupCreateActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}
