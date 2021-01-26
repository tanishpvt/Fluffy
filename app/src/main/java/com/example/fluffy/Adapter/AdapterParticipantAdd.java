package com.example.fluffy.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fluffy.Model.ModelUsers;
import com.example.fluffy.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

public class AdapterParticipantAdd extends RecyclerView.Adapter<AdapterParticipantAdd.HolderParticipantAdd>{

    private Context context;
    private ArrayList<ModelUsers> usersList;
    private String groupId, myGroupRole;// creator/admin/participants

    public AdapterParticipantAdd(Context context, ArrayList<ModelUsers> usersList, String groupId, String myGroupRole) {
        this.context = context;
        this.usersList = usersList;
        this.groupId = groupId;
        this.myGroupRole = myGroupRole;
    }

    @NonNull
    @Override
    public HolderParticipantAdd onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout
        View view = LayoutInflater.from(context).inflate(R.layout.row_participant_add,parent,false);
        return new HolderParticipantAdd(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderParticipantAdd holder, int position) {
              //get data
        final ModelUsers modelUsers = usersList.get(position);
        String name = modelUsers.getName();
        String email = modelUsers.getEmail();
        String image = modelUsers.getImage();
        final String uid = modelUsers.getUid();

        // set data
        holder.nameTv.setText(name);
        holder.emailTv.setText(email);
        try{
            Picasso.get().load(image).placeholder(R.drawable.ic_default_img).into(holder.avatarIv);
        }
        catch (Exception e){
            holder.avatarIv.setImageResource(R.drawable.ic_default_img);
        }
        checkIfAlreadyExists(modelUsers, holder);
        //handle clicks
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Check if user already added or not
                * If added : show remove-participants/make-admin/removve-admin options(admin will not able to change role of creator)
                * if not added, show add participants options*/
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
                ref.child(groupId).child("participants").child(uid)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()){
                                    //user exists/ participants
                                    String hispreviousRole = ""+dataSnapshot.child("role").getValue();
                                    Log.e("tanish1","yes data enter");
                                    //options to display in dialog
                                    String[] options;

                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Choose Options");
                                    if (myGroupRole.equals("creator")){
                                        Log.e("tanish1","uner1");
                                        if (hispreviousRole.equals("admin")){
                                            // im creator, he is admin
                                            options = new String[]{"Remove Admin", "Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    //handle item clicks
                                                    if (which==0){
                                                        //remove admin clicked
                                                        removeAdmin(modelUsers);
                                                    }
                                                    else{
                                                        //remove user clicked
                                                        removeParticipant(modelUsers);
                                                    }
                                                }
                                            }).show();
                                        }
                                        else if (hispreviousRole.equals("participants")){
                                            Log.e("tanish1","more under");
                                            // im creator he is participants
                                            options = new String[]{"Make Admin","Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    //handle item clicks
                                                    if (which==0){
                                                        //make admin clicked
                                                        makeAdmin(modelUsers);
                                                    }
                                                    else{
                                                        //remove user clicked
                                                        removeParticipant(modelUsers);
                                                    }
                                                }
                                            }).show();
                                        }
                                    }
                                    else if (myGroupRole.equals("admin")){
                                        if (hispreviousRole.equals("creator")){
                                            //im admin, he is creator
                                            Toast.makeText(context, "Creator of Group...", Toast.LENGTH_SHORT).show();
                                        }
                                        else if (hispreviousRole.equals("admin")){
                                            //im admin, he is admin too
                                            options = new String[]{"Remove Admin", "Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    //handle item clicks
                                                    if (which==0){
                                                        //remove admin clicked
                                                        removeAdmin(modelUsers);
                                                    }
                                                    else{
                                                        //remove user clicked
                                                        removeParticipant(modelUsers);
                                                    }
                                                }
                                            }).show();
                                        }
                                        else if (hispreviousRole.equals("participants")){
                                            //im admin , he is participants
                                            options = new String[]{"Make Admin", "Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    //handle item clicks
                                                    if (which==0){
                                                        //make admin clicked
                                                        makeAdmin(modelUsers);
                                                    }
                                                    else{
                                                        //remove user clicked
                                                        removeParticipant(modelUsers);
                                                    }
                                                }
                                            }).show();
                                        }
                                    }
                                }
                                else {
                                         // user dosent exists/not-participant:add
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Add Participant")
                                            .setMessage("Add this user in this group?")
                                            .setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                  //add user
                                                    addParticipants(modelUsers);
                                                }
                                            })
                                            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            }).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }
        });
    }

    private void addParticipants(ModelUsers modelUsers) {
      //setup user data - add user in group
        String timestamp = ""+System.currentTimeMillis();
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", modelUsers.getUid());
        hashMap.put("role", "participants");
        hashMap.put("timestamp", ""+timestamp);

        //add that user in group>groupId>participants
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("participants").child(modelUsers.getUid()).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //added succesfully
                        Toast.makeText(context, "Added Successfully...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                       //failed adding up user in group
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void makeAdmin(ModelUsers modelUsers) {
     //setup data - change role
        //String timestamp = ""+System.currentTimeMillis();
        HashMap<String,Object>hashMap = new HashMap<>();
        hashMap.put("role","admin"); //roles: participant/admin/creator
        //update role in db
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("participants").child(modelUsers.getUid()).updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                    //make admin
                        Toast.makeText(context, "The user is now admin...", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //dailed making admin
                Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeParticipant(ModelUsers modelUsers) {
        //remove participants from group
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("participants").child(modelUsers.getUid()).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // remove succesfully
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //failed removing participnts
            }
        });

    }

    private void removeAdmin(ModelUsers modelUsers) {
        //setup data - remove admin = just change role
       // String timestamp = ""+System.currentTimeMillis();
        HashMap<String,Object>hashMap = new HashMap<>();
        hashMap.put("role","participants"); //roles: participant/admin/creator
        //update role in db
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("participants").child(modelUsers.getUid()).updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //make admin
                        Toast.makeText(context, "The user is no longer admin...", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //dailed making admin
                Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfAlreadyExists(ModelUsers modelUsers, final HolderParticipantAdd holder) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("participants").child(modelUsers.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                             //already exists
                            String hisRole = ""+dataSnapshot.child("role").getValue();
                            holder.statusTv.setText(hisRole);
                        }
                        else {
                            //dosen't exists
                            holder.statusTv.setText("");
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

    class HolderParticipantAdd extends RecyclerView.ViewHolder{

        private ImageView avatarIv;
        private TextView nameTv,emailTv,statusTv;

        public HolderParticipantAdd(@NonNull View itemView) {
            super(itemView);

            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            emailTv = itemView.findViewById(R.id.emailTv);
            statusTv = itemView.findViewById(R.id.statusTv);

        }
    }
}
