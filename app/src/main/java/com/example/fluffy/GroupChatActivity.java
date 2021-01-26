package com.example.fluffy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fluffy.Adapter.AdapterGroupChat;
import com.example.fluffy.Model.ModelGroupChat;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class GroupChatActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private  String groupId, myGroupRole="";

    private Toolbar toolbar;
    private ImageView groupIconIv;
    private ImageButton attachBtn,sendBtn;
    private TextView groupTitleTv;
    private EditText messageEt;
    private RecyclerView chatRv;

    private ArrayList<ModelGroupChat> groupChatList;
    private AdapterGroupChat adapterGroupChat;

    // permisssion constant
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;

    // image pick constant
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    // ARRAYS OF PERMISSION TO BE REQUESTED
    String[] cameraPermissions;
    String[] storagePermissions;

    //image picked will be samed in this uri
    Uri image_uri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        toolbar = findViewById(R.id.toolbar);
        groupIconIv = findViewById(R.id.groupIconIv);
        groupTitleTv = findViewById(R.id.groupTitleTv);
        attachBtn = findViewById(R.id.attachBtn);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendBtn);
        chatRv = findViewById(R.id.chatRv);

        setSupportActionBar(toolbar);

        //get the id of group
        Intent intent =getIntent();
        groupId = intent.getStringExtra("groupId");

        // init arrays of permission
        cameraPermissions =new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions =new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};


        firebaseAuth = FirebaseAuth.getInstance();
        loadGroupInfo();
        loadGroupMessages();
        loadMyGroupRole();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //GET TEXT FROM EDIT TEXT
                String message = messageEt.getText().toString().trim();
                //check if text is empety or not
                if (TextUtils.isEmpty(message)){
                    //text empty
                    Toast.makeText(GroupChatActivity.this, "Cannot send the empty message..", Toast.LENGTH_SHORT).show();

                }
                else {
                    // text not emepty
                    sendMessage(message);
                }
            }
        });
        attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pick image from camera/gallery
                showImagePickDialog();
            }
        });

    }
    private void showImagePickDialog() {
        // options(camera gallery) to show in dialog
        String[] options = {"Camera", "Gallery"};

        // dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image");
        // set options to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // item click handle
                if (which==0){
                    //camera clicked
                    //we need to check permission first
                    if (!checkCameraPermission()){
                        requestCameraPermission();
                    }
                    else {
                        pickFromCamera();

                    }
                }
                if (which==1){
                    // gallery clicked
                    if (!checkStoragePermission()){
                        requestStoragePermission();
                    }
                    else {
                        pickFromGallery();
                    }
                }
            }
        });

        // create and show dialog
        builder.create().show();
    }

    private void pickFromGallery() {
        // pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        //intent of picking image from device camera
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE,"GroupImageTitle");
        cv.put(MediaStore.Images.Media.DESCRIPTION,"GroupImageDescription");
        // put image uri
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

        //intent to start
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }


    private boolean checkStoragePermission(){
        // check if storage permission is enabled or not
        // retrun true if enabled
        //return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        // request runtime storaage permission
        ActivityCompat.requestPermissions(this,storagePermissions,STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        // check if storage permission is enabled or not
        // retrun true if enabled
        //return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission(){
        // request runtime storaage permission
        ActivityCompat.requestPermissions(this,cameraPermissions,CAMERA_REQUEST_CODE);
    }

    private void loadMyGroupRole() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("participants")
                .orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot ds: dataSnapshot.getChildren()){
                        myGroupRole = ""+ds.child("role").getValue();
                        //refresh menu item
                        invalidateOptionsMenu();
                    }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void loadGroupMessages() {
        //init list
        groupChatList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                     groupChatList.clear();
                     for (DataSnapshot ds: dataSnapshot.getChildren()){
                         ModelGroupChat model = ds.getValue(ModelGroupChat.class);
                         groupChatList.add(model);
                     }
                     //adapter
                        adapterGroupChat = new AdapterGroupChat(GroupChatActivity.this,groupChatList);
                     //set to recyclerview
                        chatRv.setAdapter(adapterGroupChat);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void sendMessage(String message) {

        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender",""+firebaseAuth.getUid());
        hashMap.put("message",""+ message);
        hashMap.put("timestamp",""+ timestamp);
        hashMap.put("type",""+ "text");    //txt/imxge/file

        // add in db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Messages").child(timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                          // message sent
                        //clear message
                        messageEt.setText("");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                  // message sending failed
                Toast.makeText(GroupChatActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGroupInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.orderByChild("groupId").equalTo(groupId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                       for (DataSnapshot ds: dataSnapshot.getChildren()){
                           String groupTitle =""+ds.child("groupTitle").getValue();
                           String groupDescription =""+ds.child("groupDescription").getValue();
                           String groupIcon =""+ds.child("groupIcon").getValue();
                           String timestamp =""+ds.child("timestamp").getValue();
                           String createdBy =""+ds.child("createdBy").getValue();

                           groupTitleTv.setText(groupTitle);
                           try {
                               Picasso.get().load(groupIcon).placeholder(R.drawable.ic_group_white).into(groupIconIv);
                           }
                           catch (Exception e){
                              groupIconIv.setImageResource(R.drawable.ic_group_white);
                           }
                       }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);

        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_logout).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);

        if (myGroupRole.equals("creator") || myGroupRole.equals("admin")){
            // im admin/creator, show add person options
            menu.findItem(R.id.action_add_participant).setVisible(true);
        }
        else {
            menu.findItem(R.id.action_add_participant).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_participant){
            Intent intent = new Intent(this, GroupParticipantAddActivity.class);
            intent.putExtra("groupId",groupId);
            startActivity(intent);
        }
        else if (id == R.id.action_groupinfo){
            Intent intent = new Intent(this, GroupInfoActivity.class);
            intent.putExtra("groupId",groupId);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        /*
         * this method will be called after piccking image from camera or gallery*/
        if (resultCode == RESULT_OK){

            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                // IMAGE IS PICKED FROM GALLERY, GET URI OF IMAGE
                image_uri= data.getData();
                sendImageMessage();
            }
            else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //image is picked from camera,get uri of image
                  sendImageMessage();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendImageMessage() {
        //progress dialog
        final ProgressDialog pd =new ProgressDialog(this);
        pd.setTitle("please wait");
        pd.setMessage("Sending Image");
        pd.setCanceledOnTouchOutside(false);
        pd.show();

        //filename and path in firebase storage
        String filenamePath = "ChatImages/" + ""+System.currentTimeMillis();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filenamePath);
        //upload image
        storageReference.putFile(image_uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //image uploaded,get url
                        Task<Uri> p_uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!p_uriTask.isSuccessful());
                        Uri p_downloadUri = p_uriTask.getResult();

                        if (p_uriTask.isSuccessful()){
                              //img url saved in db
                            String timestamp = String.valueOf(System.currentTimeMillis());

                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("sender",""+firebaseAuth.getUid());
                            hashMap.put("message",""+ p_downloadUri);
                            hashMap.put("timestamp",""+ timestamp);
                            hashMap.put("type",""+ "image");    //txt/imxge/file

                            // add in db
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
                            ref.child(groupId).child("Messages").child(timestamp)
                                    .setValue(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // message sent
                                            //clear message
                                            messageEt.setText("");
                                            pd.dismiss();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            pd.dismiss();
                                            // message sending failed
                                            Toast.makeText(GroupChatActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
             //failed uploading image
                Toast.makeText(GroupChatActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }
        });
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        /*this method called when user press allow or deny from permission dialog
         * here  we will handle permission cases(allowed & denied)*/

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                //pickinng from camera, first check if camera and storage permission allowed or not
                if (grantResults.length >0){
                    boolean  cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean  StorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && StorageAccepted){
                        // both  permission are granted
                        pickFromCamera();
                    }
                    else {
                        //camera or gallery both permission were denied
                        Toast.makeText(this, "Please enable camera & storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
                else {

                }

            }
            break;
            case STORAGE_REQUEST_CODE:{

                //pickinng from gallery, first check if storage permission allowed or not
                if (grantResults.length >0){
                    boolean  StorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (StorageAccepted){
                        // both  permission are granted
                        pickFromGallery();
                    }
                    else {
                        //camera or gallery both permission were denied
                        Toast.makeText(this, "Please enable storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
                else {

                }


            }
            break;
        }


    }

}
