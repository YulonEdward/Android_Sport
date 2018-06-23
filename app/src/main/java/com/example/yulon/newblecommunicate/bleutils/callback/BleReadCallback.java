package com.example.yulon.newblecommunicate.bleutils.callback;

import com.example.yulon.newblecommunicate.bleutils.exception.BleException;

public abstract class BleReadCallback extends BleBaseCallback{
    public abstract void onReadSuccess(byte[] data);

    public abstract void onReadFailture(BleException exception);
}
