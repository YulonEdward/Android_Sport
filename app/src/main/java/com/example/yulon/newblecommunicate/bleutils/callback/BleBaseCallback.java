package com.example.yulon.newblecommunicate.bleutils.callback;


import android.os.Handler;

public class BleBaseCallback {

    private String key;
    private Handler handler;

    public String getKey(){
        return key;
    }

    public void setKey(String key){
        this.key = key;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

}
