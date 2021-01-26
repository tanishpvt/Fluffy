package com.example.fluffy;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentTransaction;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fluffy.Fragment.ChatListFragment;
import com.example.fluffy.Fragment.GroupChatsFragment;
import com.example.fluffy.Fragment.HomeFragment;
import com.example.fluffy.Fragment.NotificationsFragment;
import com.example.fluffy.Fragment.ProfileFragment;
import com.example.fluffy.Fragment.UsersFragment;
import com.example.fluffy.Notification.Token;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

public class DashboardActivity extends AppCompatActivity {

    //firebase auth
    FirebaseAuth firebaseAuth;



    //id for creation of channel
    private static final String ID="some_id";
    private static final String NAME="FirebaseAPP";
    ActionBar actionBar;

    String mUID;

    private BottomNavigationView navigationView;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

       // createNotificationChannel();

        // action bar and its title
        actionBar = getSupportActionBar();
        actionBar.setTitle("profile");

        //init
        firebaseAuth = FirebaseAuth.getInstance();

        // bottom navigation
         navigationView =findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        // home fragment transacstion(default, on start)
        actionBar.setTitle("Home"); // change acyion bar title
        HomeFragment fragment1 = new HomeFragment();
        FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
        ft1.replace(R.id.content,fragment1,"");
        ft1.commit();

        checkUserStatus();


    }

    @Override
    protected void onResume() {
        checkUserStatus();
        super.onResume();
    }

    public void updateToken(String token){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        ref.child(mUID).setValue(mToken);

    }

  /*  @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        //setting user to particular category
        FirebaseMessaging.getInstance().subscribeToTopic("nonuser")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Done";
                        if (!task.isSuccessful()) {
                            msg = "Error";
                        }
                        Toast.makeText(DashboardActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            NotificationChannel notificationChannel = new NotificationChannel(ID
                    ,NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(getString(R.string.CHANNEL_DESCRIPTION));
            notificationChannel.setShowBadge(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);


            if (defaultSoundUri != null) {
                AudioAttributes att = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                notificationChannel.setSound(defaultSoundUri, att);
            }

            notificationManager.createNotificationChannel(notificationChannel);



            Toast.makeText(this, "created", Toast.LENGTH_SHORT).show();
        }
    }

   */

    private  BottomNavigationView.OnNavigationItemSelectedListener selectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    // handle item click
                    switch (menuItem.getItemId()){
                        case R.id.nav_home:
                            // home fragment transacstion
                            actionBar.setTitle("Home"); // change acyion bar title
                            HomeFragment fragment1 = new HomeFragment();
                            FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                            ft1.replace(R.id.content,fragment1,"");
                            ft1.commit();
                            return true;


                        case R.id.nav_profile:
                            // profile fragment transacstion
                            actionBar.setTitle("Profile"); // change acyion bar title
                            ProfileFragment fragment2 = new ProfileFragment();
                            FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                            ft2.replace(R.id.content,fragment2,"");
                            ft2.commit();
                            return true;


                        case R.id.nav_users:
                            // users fragment transacstion
                            actionBar.setTitle("Users"); // change acyion bar title
                            UsersFragment fragment3 = new UsersFragment();
                            FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
                            ft3.replace(R.id.content,fragment3,"");
                            ft3.commit();
                            return true;

                        case R.id.nav_chat:
                            // users fragment transacstion
                            actionBar.setTitle("Chats"); // change acyion bar title
                            ChatListFragment fragment4 = new ChatListFragment();
                            FragmentTransaction ft4 = getSupportFragmentManager().beginTransaction();
                            ft4.replace(R.id.content,fragment4,"");
                            ft4.commit();
                            return true;

                        case R.id.nav_more:
                            showMoreOptions();
                            return true;
                    }
                    return false;
                }
            };

    private void showMoreOptions() {
       //popup menu to show more options
        PopupMenu popupMenu = new PopupMenu(this, navigationView, Gravity.END);
        //item to show in menu
        popupMenu.getMenu().add(Menu.NONE,0,0,"Notifications");
        popupMenu.getMenu().add(Menu.NONE,1,1,"Group Chats");

        //menu clicks
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == 0){
                    //notification clicked
                    // notification fragment transacstion
                    actionBar.setTitle("Notifications"); // change acyion bar title
                    NotificationsFragment fragment5 = new NotificationsFragment();
                    FragmentTransaction ft5 = getSupportFragmentManager().beginTransaction();
                    ft5.replace(R.id.content,fragment5,"");
                    ft5.commit();
                }
                else if (id == 1){
                    //group chats clicked
                    // notification fragment transacstion
                    actionBar.setTitle("Group Chats"); // change acyion bar title
                    GroupChatsFragment fragment6 = new GroupChatsFragment();
                    FragmentTransaction ft6 = getSupportFragmentManager().beginTransaction();
                    ft6.replace(R.id.content,fragment6,"");
                    ft6.commit();
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            //user is signed in stay here
            // set email of logged in user
           // mProfileTv.setText(user.getEmail());
            mUID = user.getUid();

            // save uid of currently signed in user in shared prefrences
            SharedPreferences sp = getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID", mUID);
            editor.apply();

            //update token
            updateToken(FirebaseInstanceId.getInstance().getToken());
        }
        else{
            //user not signed in, go to main activity
            startActivity(new Intent(DashboardActivity.this,MainActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    protected void onStart() {
        // check on start of app
        checkUserStatus();
        super.onStart();
    }

}
