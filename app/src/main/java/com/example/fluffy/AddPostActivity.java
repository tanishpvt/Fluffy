package com.example.fluffy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class AddPostActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    DatabaseReference userDbRef;
    ActionBar actionBar;

    // permisssion constant
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;

    // image pick constant
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    // ARRAYS OF PERMISSION TO BE REQUESTED
    String[] cameraPermissions;
    String[] storagePermissions;

    // views
    EditText titleEt, descriptionEt;
    ImageView imageIv;
    Button uploadBtn;

    //user info
    String name, email, uid, dp;

    //info of post to be edited
    String editTitle, editDescription, editImage;

    //image picked will be samed in this uri
    Uri image_rui = null;

    //progresss dialog bar
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Add New Post");
        // enable back button in action bar
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // init arrays of permission
        cameraPermissions =new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions =new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};

        pd = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        // init views
        titleEt = findViewById(R.id.pTitleEt);
        descriptionEt = findViewById(R.id.pDescriptionEt);
        imageIv = findViewById(R.id.pImageIv);
        uploadBtn = findViewById(R.id.pUploadBtn);


        //get data through intent from previous activities adapter
        Intent intent = getIntent();

        //get data and its type from intent(which i typed in intentfilter)
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type!=null){
            if ("text/plain".equals(type)){
                //text type data
                handleSendText(intent);
            }
            else if (type.startsWith("image")){
                //image type data
                handleSendImage(intent);
            }
        }

        final String isUpdateKey = ""+intent.getStringExtra("key");
        final String editPostId = ""+intent.getStringExtra("editPostId");
        // validate if we came here to update post i.e. came from adapterpost
        if (isUpdateKey.equals("editPost")){
           // update
            actionBar.setTitle("Update Post");
            uploadBtn.setText("Update");
            loadPostData(editPostId);
        }
        else{
            //add
            actionBar.setTitle("Add New Post");
            uploadBtn.setText("Upload");
        }

        actionBar.setSubtitle(email);

        //get some info of current user to include in post
        userDbRef =FirebaseDatabase.getInstance().getReference("Users");
        Query query = userDbRef.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
             for (DataSnapshot ds: dataSnapshot.getChildren()){
                 name = ""+ds.child("name").getValue();
                 email = ""+ds.child("email").getValue();
                 dp = ""+ds.child("image").getValue();
             }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



        //get image from camera/gallery on click
        imageIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show image pick dialog
                showImagePickDialog();
            }
        });

        //upload button click listener
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get data(title, discription) from edit texts
                String title = titleEt.getText().toString().trim();
                String description = descriptionEt.getText().toString().trim();
                if (TextUtils.isEmpty(title)){
                    Toast.makeText(AddPostActivity.this, "Enter title...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(description)){
                    Toast.makeText(AddPostActivity.this, "Enter Description...", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isUpdateKey.equals("editPost")){
                    beginUpdate(title, description, editPostId);

                }
                else {
                    uploadData(title, description);

                }


            }
        });

    }

    private void handleSendImage(Intent intent) {
   //handle the received image(uri)
        Uri imageURI = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageURI !=null){
            image_rui = imageURI;
            //set to imageview
            imageIv.setImageURI(image_rui);
        }
    }

    private void handleSendText(Intent intent) {
         //handle the received text
        String sharedText = intent.getStringExtra(Intent.EXTRA_STREAM);
        if (sharedText!=null){
            //set the description edit text
            descriptionEt.setText(sharedText);
        }
    } // we have to add firebase ofline feature

    private void beginUpdate(String title, String description, String editPostId) {
        pd.setMessage("Updating Post...");
        pd.show();
        if (!editImage.equals("noImage")){
            // with image
            updateWasWithImage(title, description, editPostId);
        }
        else if (imageIv.getDrawable() != null){
            // with image
            updateWithNowImage(title,description, editPostId);
        }
        else {
            //was without image, and still no image in image view
            updateWithoutImage(title, description, editPostId);
        }
    }

    private void updateWithoutImage(String title, String description, String editPostId) {

        HashMap<String, Object> hashMap = new HashMap<>();
        //put post info
        hashMap.put("uid",uid);
        hashMap.put("uName", name);
        hashMap.put("uEmail", email);
        hashMap.put("uDp",dp);
        hashMap.put("pLikes","0");
        hashMap.put("pComments","0");
        hashMap.put("pTitle",title);
        hashMap.put("pDescription",description);
        hashMap.put("pImage", "noImage");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        ref.child(editPostId)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        pd.dismiss();
                        Toast.makeText(AddPostActivity.this, "Updated...", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void updateWithNowImage(final String title, final String description, final String editPostId) {

        String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/"+ "post_"+timeStamp;

        //get image from  imageview
        Bitmap bitmap = ((BitmapDrawable)imageIv.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //image compress
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte[] data = baos.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        ref.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // IMAGE UPLOADED GET ITS URL
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());

                        String downloadUri = uriTask.getResult().toString();
                        if (uriTask.isSuccessful()){
                            //url is  received upload to firebase database

                            HashMap<String, Object> hashMap = new HashMap<>();
                            //put post info
                            hashMap.put("uid",uid);
                            hashMap.put("uName", name);
                            hashMap.put("uEmail", email);
                            hashMap.put("uDp",dp);
                            hashMap.put("pLikes","0");
                            hashMap.put("pComments","0");
                            hashMap.put("pTitle",title);
                            hashMap.put("pDescription",description);
                            hashMap.put("pImage",downloadUri);

                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                            ref.child(editPostId)
                                    .updateChildren(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            pd.dismiss();
                                            Toast.makeText(AddPostActivity.this, "Updated...", Toast.LENGTH_SHORT).show();

                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            pd.dismiss();
                                            Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //image not uploaded
                        pd.dismiss();
                        Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void updateWasWithImage(final String title, final String description, final String editPostId) {
        //post is with image, delete previous image first
        StorageReference mPictureRef= FirebaseStorage.getInstance().getReferenceFromUrl(editImage);
        mPictureRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //image deleted, upload new image
                        //for post image name, postid, publish time
                        String timeStamp = String.valueOf(System.currentTimeMillis());
                        String filePathAndName = "Posts/"+ "post_"+timeStamp;

                        //get image from  imageview
                        Bitmap bitmap = ((BitmapDrawable)imageIv.getDrawable()).getBitmap();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        //image compress
                        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
                        byte[] data = baos.toByteArray();

                        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
                        ref.putBytes(data)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                      // IMAGE UPLOADED GET ITS URL
                                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                        while (!uriTask.isSuccessful());

                                        String downloadUri = uriTask.getResult().toString();
                                        if (uriTask.isSuccessful()){
                                            //url is  received upload to firebase database

                                            HashMap<String, Object> hashMap = new HashMap<>();
                                            //put post info
                                            hashMap.put("uid",uid);
                                            hashMap.put("uName", name);
                                            hashMap.put("uEmail", email);
                                            hashMap.put("uDp",dp);
                                            hashMap.put("pTitle",title);
                                            hashMap.put("pDescription",description);
                                            hashMap.put("pImage",downloadUri);

                                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                            ref.child(editPostId)
                                                    .updateChildren(hashMap)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                         pd.dismiss();
                                                            Toast.makeText(AddPostActivity.this, "Updated...", Toast.LENGTH_SHORT).show();

                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            pd.dismiss();
                                                            Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //image not uploaded
                                        pd.dismiss();
                                        Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });


                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPostData(String editPostId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        // get detail of post using id of post
        Query fquery = reference.orderByChild("pId").equalTo(editPostId);
        fquery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
               for (DataSnapshot ds: dataSnapshot.getChildren()){
                   // get data
                   editTitle = ""+ds.child("pTitle").getValue();
                   editDescription = ""+ds.child("pDescription").getValue();
                   editImage = ""+ds.child("pImage").getValue();

                   //set data to views
                   titleEt.setText(editTitle);
                   descriptionEt.setText(editDescription);

                   //set image
                   if (!editImage.equals("noImage")){
                       try{
                           Picasso.get().load(editImage).into(imageIv);
                       }
                       catch (Exception e){

                       }
                   }

               }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void uploadData(final String title, final String description) {
        pd.setMessage("Publishing Post...");
        pd.show();

        //for post-image name, post-id,post-publish-time
        final String timeStamp = String.valueOf(System.currentTimeMillis());

        String filePathAndName = "Posts/" + "post_" + timeStamp;

         if (imageIv.getDrawable() !=null){
            //get image from  imageview
             Bitmap bitmap = ((BitmapDrawable)imageIv.getDrawable()).getBitmap();
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             //image compress
             bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
             byte[] data = baos.toByteArray();


             //post with image
             StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
             ref.putBytes(data)
                     .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                         @Override
                         public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // image is uploaded to firebase storage now get its url
                             Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                             while (!uriTask.isSuccessful());

                             String downloadUri = uriTask.getResult().toString();

                             if (uriTask.isSuccessful()){

                                 //url is received upload post to firebase database
                                 HashMap<Object, String> hashMap = new HashMap<>();
                                 hashMap.put("uid", uid);
                                 hashMap.put("uName", name);
                                 hashMap.put("uEmail", email);
                                 hashMap.put("uDp", dp);
                                 hashMap.put("pId", timeStamp);
                                 hashMap.put("pLikes","0");
                                 hashMap.put("pComments","0");
                                 hashMap.put("pTitle", title);
                                 hashMap.put("pDescription", description);
                                 hashMap.put("pImage", downloadUri);
                                 hashMap.put("pTime", timeStamp);
                                 
                                 //path to store post data
                                 DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                 //put data in this ref
                                 ref.child(timeStamp).setValue(hashMap)
                                         .addOnSuccessListener(new OnSuccessListener<Void>() {
                                             @Override
                                             public void onSuccess(Void aVoid) {
                                                 // added in database
                                                 pd.dismiss();
                                                 Toast.makeText(AddPostActivity.this, "Post Published", Toast.LENGTH_SHORT).show();
                                                 //reset views
                                                 titleEt.setText("");
                                                 descriptionEt.setText("");
                                                 imageIv.setImageURI(null);
                                                 image_rui = null;

                                                 //send notificaation
                                                 prepareNotification(""+timeStamp, // since we are using timestamp for post id
                                                         ""+name+" added new post",
                                                         ""+title+"\n"+description,
                                                         "PostNotification",
                                                         "POST"
                                                 );
                                                 
                                             }
                                         })
                                         .addOnFailureListener(new OnFailureListener() {
                                             @Override
                                             public void onFailure(@NonNull Exception e) {
                                                 // failed adding post in database
                                                 pd.dismiss();
                                                 Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                             }
                                         });
                             }
                         }
                     })
                     .addOnFailureListener(new OnFailureListener() {
                         @Override
                         public void onFailure(@NonNull Exception e) {
                             // failed uploading image
                        pd.dismiss();
                             Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                         }
                     });
         }
         else {
             //post without

             HashMap<Object, String> hashMap = new HashMap<>();
             hashMap.put("uid", uid);
             hashMap.put("uName", name);
             hashMap.put("uEmail", email);
             hashMap.put("uDp", dp);
             hashMap.put("pId", timeStamp);
             hashMap.put("pLikes","0");
             hashMap.put("pComments","0");
             hashMap.put("pTitle", title);
             hashMap.put("pDescription", description);
             hashMap.put("pImage", "noImage");
             hashMap.put("pTime", timeStamp);

             //path to store post data
             DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
             //put data in this ref
             ref.child(timeStamp).setValue(hashMap)
                     .addOnSuccessListener(new OnSuccessListener<Void>() {
                         @Override
                         public void onSuccess(Void aVoid) {
                             // added in database
                             pd.dismiss();
                             Toast.makeText(AddPostActivity.this, "Post Published", Toast.LENGTH_SHORT).show();
                             titleEt.setText("");
                             descriptionEt.setText("");
                             imageIv.setImageURI(null);
                             image_rui = null;

                             //send notificaation
                             prepareNotification(""+timeStamp, // since we are using timestamp for post id
                                     ""+name+" added new post",
                                     ""+title+"\n"+description,
                                     "PostNotification",
                                     "POST"
                             );

                         }
                     })
                     .addOnFailureListener(new OnFailureListener() {
                         @Override
                         public void onFailure(@NonNull Exception e) {
                             // failed adding post in database
                             pd.dismiss();
                             Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                         }
                     });
         }
    }

    private void prepareNotification(String pId, String title, String description, String notificationType, String notificationTopic){
        // prepare data for notification

        String NOTIFICATION_TOPIC = "/topics/" + notificationTopic; // topic must match with that the receiver subscribed to
        String NOTIFICATION_TITLE = title; // eg Tanish Mohanty added new post
        String NOTIFICATION_MESSAGE = description; // content of post
        String NOTIFICATION_TYPE = notificationType; // there are two notypication types chat and post, so to differintiate in firebasemessaging.java class

        // prepare json what to send and where to send
        JSONObject notificationJo = new JSONObject();
        JSONObject notificationBodyJo = new JSONObject();
        try {
            //what to send
            notificationBodyJo.put("notificationType", NOTIFICATION_TYPE);
            notificationBodyJo.put("sender", uid); // uid of current use/sender
            notificationBodyJo.put("pId",pId);//post id
            notificationBodyJo.put("pTitle", NOTIFICATION_TITLE);
            notificationBodyJo.put("pDescription",NOTIFICATION_MESSAGE);
            // where to send
            notificationBodyJo.put("to", NOTIFICATION_TOPIC);

            notificationBodyJo.put("data",notificationBodyJo); //combine data to be sent

        } catch (JSONException e) {
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        sendPostNotification(notificationJo);

    }

    private void sendPostNotification(JSONObject notificationJo) {
       //send volley object request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJo,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("FCM_RESPONSE", "onResponse: "+response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                     // error occured
                        Toast.makeText(AddPostActivity.this, ""+error.toString(), Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                //put required headers
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type","application/json");
                headers.put("Authorization","key=AAAAEOgfXek:APA91bFtwATG23ZCDVbAgVohbwfxs3IxQNO0whUCfSpmQKSmcmblU8SyAPmsmJxBsJVp9PziP9g1OqV0HEwF8kvTAoF6bmQYxMYfit4KEBMMW9fWtr6Rl4p0J75KhdaAF_ZCfuVBrhDD");

                return headers;
            }
        };
        // enqueue the volley request
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void showImagePickDialog() {
        // options(camera gallery) to show in dialog
        String[] options = {"Camera", "Gallery"};

        // dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from");
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
        cv.put(MediaStore.Images.Media.TITLE,"Temp pic");
        cv.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description");
        // put image uri
         image_rui = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

        //intent to start
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_rui);
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



    @Override
    protected void onStart() {
        super.onStart();
        checkUserStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserStatus();
    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            //user is signed in stay here
            // set email of logged in user
            // mProfileTv.setText(user.getEmail());
            email = user.getEmail();
            uid = user.getUid();
        }
        else{
            //user not signed in, go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // goto3 previous activity

        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }

    // handle premission results
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

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        /*
         * this method will be called after piccking image from camera or gallery*/
        if (resultCode == RESULT_OK){

            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                // IMAGE IS PICKED FROM GALLERY, GET URI OF IMAGE
                image_rui= data.getData();

                //set to imageview
                imageIv.setImageURI(image_rui);
            }
           else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                // IMAGE IS PICKED FROM CAMERA, GET URI OF IMAGE
                imageIv.setImageURI(image_rui);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
