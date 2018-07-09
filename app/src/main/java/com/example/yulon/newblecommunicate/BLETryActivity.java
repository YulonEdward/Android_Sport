package com.example.yulon.newblecommunicate;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yulon.newblecommunicate.adapter.ListViewAdspter;
import com.example.yulon.newblecommunicate.service.BleService;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class BLETryActivity extends AppCompatActivity {

    private static final String TAG = "BLETryActivity";
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 10;

    /**
     * 自定义的打开 Bluetooth 的请求码，与 onActivityResult 中返回的 requestCode 匹配。
     */
    private static final int REQUEST_CODE_BLUETOOTH_ON = 1313;

    /**
     * Bluetooth 设备可见时间，单位：秒
     */
    private static final int BLUETOOTH_DISCOVERABLE_DURATION = 250;
    //默认扫描时间：10s
    public static final int SCAN_TIME = 10000;

    private BleService mBleService = null;

    ListView mListView;
    private ListViewAdspter mListAdspter;

    private ProgressDialog progressDialog;

    BluetoothManager mBTManager;
    BluetoothAdapter mBTAdapter;
    BluetoothLeScanner mBletoothLeScanner;

    private String mDeviceAddress;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBleService = ((BleService.LocalBinder)service).getService();
            mBleService.initialize();
            if((mBleService.isBluetoothSupported()) && (!mBleService.isBluetoothEnabled())){
                turnOnBluetooth();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()" + name.getClassName());
            mBleService = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bletest);

        mListView = (ListView)findViewById(R.id.lvNewDevices);

        mBTManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBTAdapter = mBTManager.getAdapter();
        mBletoothLeScanner = mBTAdapter.getBluetoothLeScanner();

        initListview();

        Intent intent = new Intent(this, BleService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

    }

    private void initListview(){
        mListAdspter = new ListViewAdspter(BLETryActivity.this);
        mListView.setAdapter(mListAdspter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mListAdspter.getDevice(position);
                if(device == null){
                    return;
                }
                mDeviceAddress = device.getAddress();
                if(mBleService.connect(mDeviceAddress)){
                    Toast.makeText(BLETryActivity.this, "成功連接藍芽設備", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(BLETryActivity.this, BleDeviceControlActivity.class);
                    intent.putExtra(BleDeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                    intent.putExtra(BleDeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                    startActivity(intent);
                }else{
                    Toast.makeText(BLETryActivity.this, "無法連接，請重試", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGps();
    }

    /**
     *彈出系統框提示用戶開啟 Bluetooth
     */
    private void turnOnBluetooth(){
        //請求打開 bluetooth
        Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

        //設置 Bluetooth 設備可以被其他 Bluetooth 設備掃描到
        requestBluetoothOn.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

        //設置 bluetooth 設備可見時間
        requestBluetoothOn.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BLUETOOTH_DISCOVERABLE_DURATION);

        this.startActivityForResult(requestBluetoothOn, REQUEST_CODE_BLUETOOTH_ON);
    }

    /**
     * 掃描 bluetooth 設備
     */
    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
//            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            mListAdspter.addDevice(result.getDevice(), calculateAccuracy(-59, result.getRssi()));
        }
    };

    public void startScanning(){
        System.out.println("start scanning");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mBletoothLeScanner.startScan(bleScanCallback);
            }
        });
    }

    public void stopScanning(){
        System.out.println("stopping scanning");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mBletoothLeScanner.stopScan(bleScanCallback);
            }
        });
    }

    public void WaitProgressDialog(){
        progressDialog = ProgressDialog.show(BLETryActivity.this,
                "搜索中", "請等待10秒...",true);
        new Thread(new Runnable(){
            @Override
            public void run() {
                try{
                    Thread.sleep(SCAN_TIME);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                finally{
                    stopScanning();
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

    private static final double A_Value = 59; // A - 发射端和接收端相隔1米时的信号强度
    private static final double n_Value = 2.0; //  n - 环境衰减因子

    public static double getDistance(int rssi) { //根据Rssi获得返回的距离,返回数据单位为m
        int iRssi = Math.abs(rssi);
        double power = (iRssi - A_Value) / (10 * n_Value);
        return Math.pow(10, power);
    }

    protected static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBleService = null;
        unbindService(mServiceConnection); //解除綁定Service
        hideProgressDialog();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            mListAdspter.clear();
            startScanning();
            WaitProgressDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
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

    /**
     * 檢測 GPS 是否開啟
     */
    private boolean checkGPSIsOpen(){
        boolean isOpen;
        LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        isOpen = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isOpen;
    }

    /**
     * 跳轉 GPS 設定
     */
    private void openGPSSettings(){
        if(!checkGPSIsOpen()){
            //沒有打開則彈出對話框
            new AlertDialog.Builder(this).setTitle("提示").setMessage("當前應用程式需要打開定位功能。")
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setPositiveButton("設定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, REQUEST_CODE_ACCESS_COARSE_LOCATION);
                        }
                    }).setCancelable(false).show();
        }
    }
}
