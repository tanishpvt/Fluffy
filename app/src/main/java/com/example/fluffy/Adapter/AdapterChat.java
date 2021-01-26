package com.example.fluffy.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fluffy.Model.ModelChat;
import com.example.fluffy.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder> {


    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    Context context;
    List<ModelChat> chatList;
    String imageUrl;

    FirebaseUser fUser;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layouts: row_chat_left.xml for receiver, row_chat_right.xml for sender
         if (viewType==MSG_TYPE_RIGHT){
             View view = LayoutInflater.from(context).inflate(R.layout.row_chart_right,parent,false);
             view.findViewById(R.id.profileIv).setVisibility(View.INVISIBLE);
             return new MyHolder(view);
         }
         else {
             View view = LayoutInflater.from(context).inflate(R.layout.row_chat_left,parent,false);
             return new MyHolder(view);
         }

    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        //getdata
        String message =chatList.get(position).getMessage();
        String timeStamp = chatList.get(position).getTimestamp();
        String type = chatList.get(position).getType();

        //convert time stamp to dd/mm/yyy hh:mm: am/pm
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(Long.parseLong(timeStamp));
       // String dateTime = DateFormat.format("dd/MM/YYYY hh:mm aa",cal).toString();
        String dateTime = DateFormat.format("dd/MM/yyy hh:mm aa",cal).toString();

        if (type.equals("text")){
            // text message
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageIv.setVisibility(View.GONE);

            holder.messageTv.setText(message);
        }
        else {
            //image message
            holder.messageTv.setVisibility(View.GONE);
            holder.messageIv.setVisibility(View.VISIBLE);


            Picasso.get().load(message).placeholder(R.drawable.ic_image_black).into(holder.messageIv);

        }


        //set data
        holder.messageTv.setText(message);
        holder.timeTv.setText(dateTime);
        try {
            Picasso.get().load(imageUrl).into(holder.profileIv);
        }
        catch (Exception e){

        }
        //click to show delete dialog
        holder.messageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show deleted message confirm dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure to delete this message?");
                //delete button
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       deleteMessage(position);
                    }
                });
                // cancel delete button
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                      // dismiss dialog
                        dialog.dismiss();
                    }
                });
                //create and show dialog
                builder.create().show();
            }
        });


        // set seen deleivered status of message
        if (position==chatList.size()-1){
            if (chatList.get(position).isSeen()){
                holder.isSeenTv.setText("Seen");
            }
            else {
                holder.isSeenTv.setText("Delivered");
            }
        }
        else {
            holder.isSeenTv.setVisibility(View.GONE);
        }

    }

    private void deleteMessage(int position) {
        final String myUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
      /*Logic
      * get timestamp of clicked message
      * compare the time stamp of the  clicked message with all message in chats
      * where both values matches delete that message*/
      String msgTimeStamp = chatList.get(position).getTimestamp();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = dbRef.orderByChild("timestamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    /*if u want to allow sender to delete only his message them
                    * compare sender value with current users uid
                    * if they match means its the message of sender that is trying to delete*/
                    if (ds.child("sender").getValue().equals(myUID)){
                        /*we can do one of two things here
                         * 1) remove the message from chats
                         * 2) set the value of message "This message was deleted..
                         *  so i will do whatever i want .."*/

                        //1)Remove from chats
                        ds.getRef().removeValue();

                       /* //2) set the value of message This message was deleted..
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("message", "This message was deleted..");
                        ds.getRef().updateChildren(hashMap); */

                        Toast.makeText(context, "message deleted..", Toast.LENGTH_SHORT).show();

                    }
                    else {
                        Toast.makeText(context, "You can delete only your messages..", Toast.LENGTH_SHORT).show();
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
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        //get currently signed in user
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (chatList.get(position).getSender().equals(fUser.getUid())){
            return MSG_TYPE_RIGHT;
        }
        else {
            return MSG_TYPE_LEFT;
        }
    }

    //view holder class

    class MyHolder extends RecyclerView.ViewHolder {

        // viewss
        ImageView profileIv, messageIv;
        TextView messageTv, timeTv, isSeenTv;
        LinearLayout messageLayout; //for click listner to show  delete


        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init views
            profileIv =itemView.findViewById(R.id.profileIv);
            messageIv =itemView.findViewById(R.id.messageIv);
            messageTv =itemView.findViewById(R.id.messageTv);
            timeTv =itemView.findViewById(R.id.timeTv);
            isSeenTv =itemView.findViewById(R.id.isSeenTv);
            messageLayout =itemView.findViewById(R.id.messageLayout);



        }
    }
}
