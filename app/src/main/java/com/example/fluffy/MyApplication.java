package com.example.fluffy;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //enable firebase offline
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        //after enablingit data that is loaded will also be able to see offline
    }
}
