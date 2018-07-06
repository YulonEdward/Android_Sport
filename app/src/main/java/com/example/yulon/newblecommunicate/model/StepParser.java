package com.example.yulon.newblecommunicate.model;

import android.util.Log;

public class StepParser {
    private int stepNum;
    private byte[] data;

    public StepParser(byte[] data){
        this.data = data;
        parseData();
    }

    private void parseData(){
        Log.d("StepParser : ", String.valueOf(data.length));
        if (data.length == 4) {
            stepNum = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
        }
    }

    public int getStepNum(){
        return stepNum;
    }
}
