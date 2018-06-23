package com.example.yulon.newblecommunicate.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseService extends Service {

    private static final String TAG = FirebaseService.class.getSimpleName();

    public class LocalBinder extends Binder
    {
        public FirebaseService getService(){
            return FirebaseService.this;
        }
    }
    private LocalBinder mLocBin = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mLocBin;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public static void Fire_locationData(String str_location){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("location");

        myRef.setValue(str_location);
    }

    public static void Fire_lastTimeData(String str_lastTime){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("lastTime");

        myRef.setValue(str_lastTime);
    }
}
