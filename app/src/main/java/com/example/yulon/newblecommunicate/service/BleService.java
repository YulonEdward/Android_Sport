package com.example.yulon.newblecommunicate.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.yulon.newblecommunicate.utils.HexUtil;
import com.example.yulon.newblecommunicate.utils.ParserUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BleService extends Service{

    private static final String TAG = "BleService";

    public BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBluetoothAdapter;
    private static String mBluetoothDeviceAddress;
    private BluetoothGatt mGatt = null;
    private BluetoothGattService mBluetoothGattService;
    public static BluetoothGattCharacteristic mBleGattCharacteristic;
    private int mConnectionState = STATE_DISCONNECTED;

    public static final String ACTION_BLESERVICE_BROADCAST = GPSService.class.getName() + "BleServiceBroadcast";
    public static final String EXTRA_STEPS = "extra_steps";

    private static final Queue<Object> sWriteQueue = new ConcurrentLinkedQueue<Object>();
    private static boolean sIsWriting = false;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    static final int MSG_DEVICE_DATA = 8;

    private static int i_OriginalSteps = 0;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

//    //默认扫描时间：10s
//    public static final int SCAN_TIME = 10000;
//    //默认连接超时时间:10s
//    private static final int CONNECTION_TIME_OUT = 10000;
//
//    public static final String KEY_MAC_ADDRESSES = "KEY_MAC_ADDRESSES";
//
//    private static final String DEVICE_NAME = "SensorTag";
    private static final UUID UUID_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID UUID_NOTIFY = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID UUID_WRITE = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID UUID_CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final byte[] ENABLE_SENSOR = {0x02,0x00};
    /**
     * Value used to enable notification for a client configuration descriptor
     */
    public static final byte[] ENABLE_NOTIFICATION_VALUE = {0x01, 0x00};
    /**
     * Value used to enable indication for a client configuration descriptor
     */
    public static final byte[] ENABLE_INDICATION_VALUE = {0x02, 0x00};
    /**
     * Value used to disable notifications or indicatinos
     */
    public static final byte[] DISABLE_NOTIFICATION_VALUE = {0x00, 0x00};

    private Map<String, BluetoothGatt> mBluetoothGattMap;

    private Map<String, BluetoothGattCharacteristic> mWriteCharacteristicMap = new HashMap<>();

    // 電池：42-61-74-74-3A-32-37-30-39
    // 版本：56-4E-3A-30-32-30-34-30-30-30-36

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void myMethod()
    {
        Log.d(TAG, "myMethod()");
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            Log.e("notify", "onCharacteristicChanged: "+ HexUtil.bytesToHexString(characteristic.getValue()));
            Log.e("notify", "onCharacteristicChanged: "+ characteristic.getValue());
            Log.e("notify", "onCharacteristicChanged: "+ ParserUtils.parse(characteristic));

            dealCallDatas(gatt, characteristic);

//            for(int i = 0; i < characteristic.getValue().length; i++){
//                Log.d("byte轉十六進制字串符 - 1", String.valueOf(characteristic.getValue()[i]));
//            }

            if (characteristic.getUuid().equals(UUID_NOTIFY)) {

//                ParserUtils.parse(characteristic.getValue());
////                Log.d(TAG, "Characteristic getValue: " + ParserUtils.parse(characteristic.getValue()));
//////                Log.d(TAG, "走路步數：" + characteristic.getValue()[1] + characteristic.getValue()[2] + characteristic.getValue()[3]);
////
////                int iSteps_thousand = HexUtil.toInt(new byte[]{characteristic.getValue()[1]}) * (int)Math.pow(250, 2);
////                int iSteps_hundred =  HexUtil.toInt(new byte[]{characteristic.getValue()[2]}) * 250;
////
////                Log.d(TAG, "走路步數_千步：" + iSteps_thousand);
////                Log.d(TAG, "走路步數_百步：" + iSteps_hundred);
////                Log.d(TAG, "走路步數：" + HexUtil.toInt(new byte[]{characteristic.getValue()[3]}));
////
////                int itotal_Steps = iSteps_thousand + iSteps_hundred + HexUtil.toInt(new byte[]{characteristic.getValue()[3]});
////
////                if(i_OriginalSteps != itotal_Steps){
////                    sendMessageToUI(String.valueOf(itotal_Steps - i_OriginalSteps));
////                    Log.d(TAG, "走路步數變化：" + String.valueOf(itotal_Steps - i_OriginalSteps));
////                    i_OriginalSteps = itotal_Steps;
////                }else{
////                    sendMessageToUI("0");
////                    Log.d(TAG, "走路步數沒有變化" );
////                }

//                sendMessageToUI(String.valueOf((int)(characteristic.getValue()[1] + characteristic.getValue()[2] + characteristic.getValue()[3])));

            }
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e("notify", "onDescriptorWrite: "+ HexUtil.bytesToHexString(characteristic.getValue()));
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;
            if(newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.e(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + mGatt.discoverServices());
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                reconnect();
                Log.e(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.e("notify", "onDescriptorWrite: "+ HexUtil.bytesToHexString(descriptor.getValue()));
            Log.e("notify", "onDescriptorWriteStatus: "+ status);

            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

                List<BluetoothGattService> services = mGatt.getServices();

                for(int i = 0; i < services.size(); i++){
                    mBluetoothGattService = services.get(i);
                    String serviceUuid = mBluetoothGattService.getUuid().toString();
                    Log.e("Servie UUID : ", serviceUuid);

                    List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = mBluetoothGattService.getCharacteristics();
                    for(int j = 0; j < bluetoothGattCharacteristics.size(); j++){
                        Log.e("Character UUID : ", bluetoothGattCharacteristics.get(j).getUuid().toString());
//                        enableNotification(true, bluetoothGattCharacteristics.get(j));
//                        enableIndication(true, bluetoothGattCharacteristics.get(j));
//                        enableNotification(mGatt, mBluetoothGattService.getUuid(), bluetoothGattCharacteristics.get(j).getUuid());
//                        setCharacteristicNotification(bluetoothGattCharacteristics.get(j), true);
                        setCharacteristicNotification(gatt.getDevice().getAddress(), bluetoothGattCharacteristics.get(j), true);
                        if(bluetoothGattCharacteristics.get(j).getUuid().toString().equals("6e400002-b5a3-f393-e0a9-e50e24dcca9e")){
                            mWriteCharacteristicMap.put(gatt.getDevice().getAddress(), bluetoothGattCharacteristics.get(j));
                        }
                    }

                    if(mBluetoothGattService.getUuid().equals(UUID.fromString(String.valueOf(UUID_SERVICE)))){
                        mBleGattCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString(String.valueOf(UUID_NOTIFY)));
                        Log.d("NOTIFICATION UUID : ", String.valueOf(mBleGattCharacteristic.getUuid()));
                        //Log.d("setNotification return", String.valueOf(setCharacteristicNotification(mBleGatt, mBleGattCharacteristic, true)));
//
//                        setCharacteristicNotification(mBleGattCharacteristic, true);
                    }
                }

            }else{
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
    };

    /**
     *  讀取 characteristic
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mGatt.readCharacteristic(characteristic);
    }

    /**
    *'  當前 Android 是否支持 Bluetooth
     */
    public boolean isBluetoothSupported(){
        return BluetoothAdapter.getDefaultAdapter() != null ? true : false;
    }

    /**
     *'  當前 Android 設備 bluetooth 是否開啟
     */
    public boolean isBluetoothEnabled(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter != null){
            return bluetoothAdapter.isEnabled();
        }
        return false;
    }

    public boolean initialize(){
        if(mBluetoothManager == null){
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if(mBluetoothManager == null){
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * 開啟 Notification
     */

    public void setCharacteristicNotification(String address,
                                              BluetoothGattCharacteristic characteristic, boolean enabled){
        if(mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null){
            Log.d(TAG, "BluetoothAdapter is null");
            return;
        }
        mBluetoothGattMap.get(address).setCharacteristicNotification(characteristic, enabled);

        if(characteristic.getDescriptors().size() > 0){
            List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
            for(BluetoothGattDescriptor descriptor : descriptors){
                if(descriptor != null){
                    if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0){
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    }else if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0){
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    }
                    mBluetoothGattMap.get(address).writeDescriptor(descriptor);
                }
            }
        }

    }

    /**
     *  連接 bluetooth 設備
     */
    public boolean connect(final String address){
        if(mBluetoothAdapter == null || address == null){
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect. ()
        if (mBluetoothGattMap == null) {
            mBluetoothGattMap = new HashMap<>();
        }

        //Previously connected device.  Try to reconnect.
        if(mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mGatt != null){
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if(mGatt.connect()){
                mConnectionState = STATE_CONNECTING;
                return true;
            }else{
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if(device == null){
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mGatt = device.connectGatt(this, false, mGattCallback);

        if(mGatt != null){
            mBluetoothGattMap.put(address, mGatt);
            Log.d(TAG, "Trying to create a new connection.");
            return true;
        }

        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTED;
        return true;
    }

    /**
     * 重新連線
     */
    private void reconnect(){
        if(mBluetoothDeviceAddress != null){
            connect(mBluetoothDeviceAddress);
        }
    }

    /**
     *
     */
    public void disconnect(){
        if(mBluetoothAdapter == null || mGatt == null){
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mGatt.disconnect();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public class LocalBinder extends Binder{
        public BleService getService(){
            return BleService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
    }

    private void broadcastUpdate(final String action){
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public boolean wirteCharacteristic(String address, byte[] value){

        Log.e("GGGGGGG", String.valueOf(value));
        Log.e("address", address);

        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattCharacteristic gattCharacteristic = mWriteCharacteristicMap.get(address);
        Log.d(TAG, gattCharacteristic.getUuid().toString());
        if(gattCharacteristic != null){
            try {
                if (UUID_WRITE.equals(gattCharacteristic.getUuid())) {
                    gattCharacteristic.setValue(value);
                    Log.d(TAG, address + " -- gattCharacteristic data:" + gattCharacteristic.getUuid());
                    boolean result = mBluetoothGattMap.get(address).writeCharacteristic(gattCharacteristic);
                    Log.d(TAG, address + " -- write data:" + Arrays.toString(value));
                    Log.d(TAG, address + " -- write result:" + result);
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void sendMessageToUI(String steps) {

        Log.d(TAG, "BleService Sending info...");

        Intent intent = new Intent(ACTION_BLESERVICE_BROADCAST);
        intent.putExtra(EXTRA_STEPS, steps);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic characteristic, int offset) {
        Integer lowerByte = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1);

        return (upperByte << 8) + lowerByte;
    }

    float[][] flaots = new float[7][30];

    private void dealCallDatas (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        //感測器數據
        byte[] value = characteristic.getValue();

        if(value[0] == 0x42){
            Log.e(TAG,"獲取電池電量。");
            int format = BluetoothGattCharacteristic.FORMAT_UINT8;
//            final int battery_level = characteristic.getIntValue(format, 0);
//            String data = HexUtil.bytes2HexString(characteristic.getValue());
//            final int battery_level = Integer.parseInt( data, 16);
            Log.d(TAG, "目前的電池電壓：" +  characteristic.getStringValue(0));
            Show_Battery("目前的電池電壓：" +  characteristic.getStringValue(0).substring(5,9) + " mV");
        }else if(value[0] == 0x56){
            Log.e(TAG,"獲取裝置目前版本。");
            Log.d(TAG, "目前的裝置版本：" + characteristic.getStringValue(0));
            Show_Version(characteristic.getStringValue(0).substring(3,11));
        }else{
            String data = HexUtil.bytes2HexString(characteristic.getValue());
            String count = data.substring(2,8);
            int stepCount = Integer.parseInt(count, 16);
            int itotal_Steps = stepCount;

            if(!sIsWriting){
                sendMessageToUI(String.valueOf(stepCount));
                Log.d(TAG, "Part1 目前鞋墊步數：" + stepCount);
                sIsWriting = true;
            }else{
                if(i_OriginalSteps != itotal_Steps){
                    sendMessageToUI(String.valueOf(itotal_Steps - i_OriginalSteps));
                    Log.d(TAG, "走路步數變化：" + String.valueOf(itotal_Steps - i_OriginalSteps));
                }else{
                    sendMessageToUI(String.valueOf(itotal_Steps - i_OriginalSteps));
                    Log.d(TAG, "走路步數沒有變化" );
                }
            }

            i_OriginalSteps = itotal_Steps;

            Log.e(TAG,"獲取裝置目前步數。");
            Log.d(TAG, "目前鞋墊步數：" + stepCount);
        }
    }

    private void Show_Battery(final String strBattery){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), strBattery, Toast.LENGTH_SHORT).show();
            }
        });
//        Toast.makeText(getApplicationContext(), strBattery, Toast.LENGTH_SHORT).show();
    }

    private void Show_Version(final String strVersion){
        Log.w(TAG, "字串內容 " + strVersion);
        final String[] splitStr = strVersion.split("0");
        for(String s : splitStr){
            Log.w(TAG, "字串內容 " + s);
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "目前的裝置版本："+splitStr[1]+"."+splitStr[2]+"."+splitStr[5], Toast.LENGTH_SHORT).show();
            }
        });
//        Toast.makeText(getApplicationContext(), strVersion, Toast.LENGTH_SHORT).show();
    }

}
