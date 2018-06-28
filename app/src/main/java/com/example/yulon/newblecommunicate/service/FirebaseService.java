package com.example.yulon.newblecommunicate.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseService extends Service {

    private static final String TAG = FirebaseService.class.getSimpleName();
    final String[] str_DataSteps = {null};

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

    public static void Fire_deviceStepsData(String deviceAddress, String data_time, String data_Steps){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(deviceAddress).child(data_time).child("data_steps");

        myRef.setValue(String.valueOf(data_Steps));
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

    public String Firebase_GetStepsData(final String deviceAddress, final String data_time){

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(deviceAddress).child(data_time).child("data_steps");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                str_DataSteps[0] = dataSnapshot.getValue(String.class);
                Log.d(TAG, "Value is: " + str_DataSteps[0]);
                if(str_DataSteps[0] == null){
                    Fire_deviceStepsData(deviceAddress, data_time, "0");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });

        return str_DataSteps[0];
    }
}
