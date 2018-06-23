package com.example.yulon.newblecommunicate.bleutils.callback;

public interface ConnectCallback {

    /**
     *  获得通知之后
     */

    void onConnSuccess();

    /**
     * 断开或连接失败
     */
    void onConnFailed();

    /**
    *重新連線
     */

    void onReConn();
}
