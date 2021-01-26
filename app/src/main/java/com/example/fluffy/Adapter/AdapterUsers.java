package com.example.fluffy.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.fluffy.ChatActivity;
import com.example.fluffy.Model.ModelUsers;
import com.example.fluffy.R;
import com.example.fluffy.ThereProfileActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

public class AdapterUsers extends  RecyclerView.Adapter<AdapterUsers.MyHolder>{

    Context context;
    List<ModelUsers> usersList;

    //for getting current users id
    FirebaseAuth firebaseAuth;
    String myUid;

    //constructor
    public AdapterUsers(Context context, List<ModelUsers> usersList) {
        this.context = context;
        this.usersList = usersList;

        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // INFLATE LAYOUT(row_user.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.row_users,parent,false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        //get data
        final String hisUID = usersList.get(position).getUid();
        String userImage = usersList.get(position).getImage();
        String userName = usersList.get(position).getName();
        final String userEmail = usersList.get(position).getEmail();

        //setdata
        holder.mNameTv.setText(userName);
        holder.mEmailTv.setText(userEmail);
        try {
            Picasso.get().load(userImage)
                    .placeholder(R.drawable.ic_default_img)
                    .into(holder.mAvatarIv);
        }
        catch (Exception e){

        }

        holder.blockIv.setImageResource(R.drawable.ic_unblocked_green);
        //check if each user if is blocked or not
        checkIsBlocked(hisUID, holder,position);

        //handle item click
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // Toast.makeText(context, ""+userEmail, Toast.LENGTH_SHORT).show();

              /*click user from user list to start chatting/messaging
              * start activity by putting uid of receiver
              * we will use that UID to identify the user we are gonna chat*/



                // show dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                         if (which==0){
                             //profile clicked
                             /*click to go thereprofileactivity with uid, this uid is of clicked user
                              * which will be used to show user specific data/posts*/
                             Intent intent = new Intent(context, ThereProfileActivity.class);
                             intent.putExtra("uid",hisUID);
                             context.startActivity(intent);
                         }
                         if (which==1){
                             //chat clicked
                             /*click user from user list to start chatting/messaging
                              * start activity by putting uid of receiver
                              * we will use that UID to identify the user we are gonna chat*/
                            imBlockedORNot(hisUID);

                         }
                    }
                });
                builder.create().show();
            }
        });

        //click to block unblock user
        holder.blockIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             if (usersList.get(position).isBlocked()){
                 unBlockUser(hisUID);
             }
             else {
                 blockUser(hisUID);
             }
            }
        });
    }

    private void imBlockedORNot(final String hisUID){
        /*first check if sender(current user) is blocked by receiver or not
        * logic:- if uid of the sender (current user) exists in "blockedusers" of receiver than sender
        * (current user) is blocked otherwise not
        * if blocked then just display a mesage eg u r blocked by tht user , cant send message
        * if not blocked then simply start the chat activity*/
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUID).child("BlockedUsers").orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            if (ds.exists()){
                                Toast.makeText(context, "You are blocked by that user, can't send message", Toast.LENGTH_SHORT).show();
                                //blocked, dont proceed further
                                return;
                            }
                        }
                        // not blocked, start acticity
                        Intent intent =new Intent(context, ChatActivity.class);
                        intent.putExtra("hisUid",hisUID);
                        context.startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void checkIsBlocked(String hisUID, final MyHolder holder, final int position) {
        //check each user if blocked or not
        //if uid of the user exists in "blocked users" then that user is blocked otherwise not
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                      for (DataSnapshot ds: dataSnapshot.getChildren()){
                          if (ds.exists()){
                              holder.blockIv.setImageResource(R.drawable.ic_blocked_red);
                              usersList.get(position).setBlocked(true);
                          }
                      }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


    }

    private void blockUser(String hisUID) {
        //block the user, by adding uid to current users "blockedusers" node

        //put values in hashmap to put in database
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", hisUID);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(hisUID).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //blocked succesfully
                        Toast.makeText(context, "Blocked Successfully...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                      // failed to block
                        Toast.makeText(context, "Failed:"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unBlockUser(String hisUID) {
        //unblock the user, by adding uid to current users "blockedusers" node
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            if (ds.exists()){
                                ds.getRef().removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                               //unblocked successfully
                                                Toast.makeText(context, "Unblocked Successfully...", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //failed to unblock
                                                Toast.makeText(context, "Failed:"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    //view holder class
    class  MyHolder extends RecyclerView.ViewHolder{
        ImageView mAvatarIv,blockIv;
        TextView mNameTv, mEmailTv;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init views
            mAvatarIv = itemView.findViewById(R.id.avatarIv);
            blockIv = itemView.findViewById(R.id.blockIv);
            mNameTv = itemView.findViewById(R.id.nameTv);
            mEmailTv = itemView.findViewById(R.id.emailTv);

        }
    }
}
