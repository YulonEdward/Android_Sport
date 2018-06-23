package com.example.yulon.newblecommunicate;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.example.yulon.newblecommunicate.adapter.ListViewAdspter;
import com.example.yulon.newblecommunicate.bleutils.BleController;
import com.example.yulon.newblecommunicate.bleutils.callback.ConnectCallback;
import com.example.yulon.newblecommunicate.bleutils.callback.ScanCallback;

import junit.framework.Test;

public class MainActivity extends AppCompatActivity{

    ListView mListView;

    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 10;
    private ProgressDialog progressDialog;
    private BleController mBleController;
    private String mDeviceAddress;//当前连接的mac地址
    private ListViewAdspter mListAdspter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView)findViewById(R.id.lvNewDevices);

        initView();

    }

    private void initView(){
        mBleController = BleController.getInstance().initble(this);

        initListview();
    }

    private void initListview(){

        mListAdspter = new ListViewAdspter(MainActivity.this);
        mListView.setAdapter(mListAdspter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showProgressDialog("請稍後!","正在連接...");
                final BluetoothDevice device = mListAdspter.getDevice(position);
                if(device == null){
                    return;
                }
                mDeviceAddress = device.getAddress();
                mBleController.Connect(mDeviceAddress, new ConnectCallback(){

                    @Override
                    public void onConnSuccess() {
                        hideProgressDialog();
                        Intent intent = new Intent(MainActivity.this, TestActivity.class);
                        intent.putExtra(TestActivity.EXTRAS_DEVICE_NAME, device.getName());
                        intent.putExtra(TestActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                        startActivity(intent);
                    }

                    @Override
                    public void onConnFailed() {

                        Toast.makeText(MainActivity.this, "連接超時，請重試", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReConn() {
                        hideProgressDialog();
                        Toast.makeText(MainActivity.this, "重新連線中...", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void scanDevices(final boolean enable){
        mBleController.ScanBle(enable, new ScanCallback() {
            @Override
            public void onSuccess() {
                if (mListAdspter.mBleDevices.size() < 0) {
                    Toast.makeText(MainActivity.this, "為搜索到BLE設備", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onScanning(BluetoothDevice device, int rssi, byte[] scanRecord) {
                mListAdspter.addDevice(device, getDistance(rssi));
            }
        });
    }

    public void showProgressDialog(String title, String message){
        if(progressDialog == null){
            progressDialog = ProgressDialog.show(this, title, message, true, false);
        }else if(progressDialog.isShowing()){
            progressDialog.setTitle(title);
            progressDialog.setMessage(message);
        }
        progressDialog.show();
    }

    public void WaitProgressDialog(){
        progressDialog = ProgressDialog.show(MainActivity.this,
                "搜索中", "請等待10秒...",true);
        new Thread(new Runnable(){
            @Override
            public void run() {
                try{
                    Thread.sleep(mBleController.SCAN_TIME);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                finally{
                    progressDialog.dismiss();
                }
            }
        }).start();
    }

    public void ReConnectProgressDialog(){
        progressDialog = ProgressDialog.show(MainActivity.this,
                "重新連線中", "請等待10秒...",true);
        new Thread(new Runnable(){
            @Override
            public void run() {
                try{
                    Thread.sleep(mBleController.SCAN_TIME);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                finally{
                    progressDialog.dismiss();
                }
            }
        }).start();
    }

    public void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private static final double A_Value = 60; // A - 发射端和接收端相隔1米时的信号强度
    private static final double n_Value = 2.0; //  n - 环境衰减因子

    public static double getDistance(int rssi) { //根据Rssi获得返回的距离,返回数据单位为m
        int iRssi = Math.abs(rssi);
        double power = (iRssi - A_Value) / (10 * n_Value);
        return Math.pow(10, power);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGps();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            scanDevices(true);
            WaitProgressDialog();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        //mDeviceListFragment = DeviceListFragment.newInstance(null);
        return true;
    }


    /**
     * 开启位置权限
     */
    private void checkGps() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ACCESS_COARSE_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "位置權限已開啟", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "未開啟位置權限", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}

