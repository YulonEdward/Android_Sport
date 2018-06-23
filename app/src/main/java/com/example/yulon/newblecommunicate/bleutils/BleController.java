package com.example.yulon.newblecommunicate.bleutils;

import android.annotation.SuppressLint;
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.view.View;

import com.example.yulon.newblecommunicate.TestActivity;
import com.example.yulon.newblecommunicate.bleutils.callback.BleDeviceScanCallBack;
import com.example.yulon.newblecommunicate.bleutils.callback.ConnectCallback;
import com.example.yulon.newblecommunicate.bleutils.callback.ScanCallback;
import com.example.yulon.newblecommunicate.utils.HexUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.google.android.gms.internal.zzaqb.reset;

public class BleController{

    private static final String TAG = "BleController";

    private static BleController mBleController;
    private static Context mContext;

    private BluetoothManager mBlehManager;
    private BluetoothAdapter mBleAdapter;
    private BluetoothGatt mBleGatt;
    public static BluetoothGattCharacteristic mBleGattCharacteristic;
    private BleGattCallback mGattCallback;
    private BluetoothGattService bluetoothGattService;

    private boolean mScanning;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    //默认扫描时间：10s
    public static final int SCAN_TIME = 10000;
    //默认连接超时时间:10s
    private static final int CONNECTION_TIME_OUT = 10000;
    //获取到所有服务的集合
    private HashMap<String, Map<String, BluetoothGattCharacteristic>> servicesMap = new HashMap<>();

    //连接请求是否ok
    private boolean isConnectok = false;
    //是否是用户手动断开
    private boolean isMybreak = false;
    //連接結果的回調
    private ConnectCallback connectCallback;

    public int mConnectionState = STATE_DISCONNECTED;
    public static String mState;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

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

    /*
        D/Servie UUID :: 00001800-0000-1000-8000-00805f9b34fb
        D/Character UUID :: 00002a00-0000-1000-8000-00805f9b34fb
        E/BleController: 00002a00-0000-1000-8000-00805f9b34fb 的屬性為 可讀
                         00002a00-0000-1000-8000-00805f9b34fb 的屬性為 可寫
        D/Character UUID :: 00002a01-0000-1000-8000-00805f9b34fb
        E/BleController: 00002a01-0000-1000-8000-00805f9b34fb 的屬性為 可讀
        D/Character UUID :: 00002a04-0000-1000-8000-00805f9b34fb
        E/BleController: 00002a04-0000-1000-8000-00805f9b34fb 的屬性為 可讀

        D/Servie UUID :: 00001801-0000-1000-8000-00805f9b34fb
                         6e400001-b5a3-f393-e0a9-e50e24dcca9e
        D/Character UUID :: 6e400003-b5a3-f393-e0a9-e50e24dcca9e
        E/BleController: 6e400003-b5a3-f393-e0a9-e50e24dcca9e 的屬性為 通知屬性
        D/Character UUID :: 6e400002-b5a3-f393-e0a9-e50e24dcca9e
        E/BleController: 6e400002-b5a3-f393-e0a9-e50e24dcca9e 的屬性為 可寫
        D/NOTIFICATION UUID :: 6e400003-b5a3-f393-e0a9-e50e24dcca9e


     */


    //此属性一般不用修改
    private static final String BLUETOOTH_NOTIFY_D = "00002902-0000-1000-8000-00805f9b34fb";
//    private static final String BLUETOOTH_NOTIFY_D = "00001101-0000-1000-8000-00805F9B34FB";
    //TODO 以下uuid根据公司硬件改变
    public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_INDICATE = "0000000-0000-0000-8000-00805f9b0000";
    public static final String UUID_NOTIFY = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_WRITE = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_READ = "3f3e3d3c-3b3a-3938-3736-353433323130";
    private static final byte[] ENABLE_SENSOR = {0x01};
    private static String MAC_ADDRESS = "Mac_address";

    private static final Queue<Object> sWriteQueue = new ConcurrentLinkedQueue<Object>();
    private static boolean sIsWriting = false;


    public static synchronized BleController getInstance() {
        if (null == mBleController) {
            mBleController = new BleController();
        }
        return mBleController;
    }

    public BleController initble(Context context) {
        if (mContext == null) {
            mContext = context.getApplicationContext();
            mBlehManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (null == mBlehManager) {
                Log.e(TAG, "BluetoothManager init error!");
            }

            mBleAdapter = mBlehManager.getAdapter();
            if (null == mBleAdapter) {
                Log.e(TAG, "BluetoothManager init error!");
            }

            mGattCallback = new BleGattCallback();
        }
        return this;
    }

    private void broadcastUpdate(final String action) {
        Log.e(TAG,"mContent: " + mContext);
        final Intent intent = new Intent();
        intent.setAction(action);
        mContext.sendBroadcast(intent);
    }

    /**
     * 扫描设备
     *
     * @param time         指定扫描时间
     * @param scanCallback 扫描回调
     */
    public void ScanBle(int time, final boolean enable, final ScanCallback scanCallback) {
        if (!isEnable()) {
            mBleAdapter.enable();
            Log.e(TAG, "Bluetooth is not open!");
        }
        if (null != mBleGatt) {
            mBleGatt.close();
        }
        reset();
        final BleDeviceScanCallBack bleDeviceScanCallback = new BleDeviceScanCallBack(scanCallback);
        if (enable) {
            if (mScanning) return;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    //time后停止扫描
                    mBleAdapter.stopLeScan(bleDeviceScanCallback);
                    scanCallback.onSuccess();
                }
            }, time <= 0 ? SCAN_TIME : time);
            mScanning = true;
            mBleAdapter.startLeScan(bleDeviceScanCallback);
        } else {
            mScanning = false;
            mBleAdapter.stopLeScan(bleDeviceScanCallback);
        }
    }

    /**
     * 扫描设备
     * 默认扫描10s
     *
     * @param scanCallback
     */
    public void ScanBle(final boolean enable, final ScanCallback scanCallback) {
        ScanBle(SCAN_TIME, enable, scanCallback);
    }

    /**
     * 重置数据
     */
    private void reset() {
        isConnectok = false;
        servicesMap.clear();
    }

    /**
     * 连接设备
     *
     */

    public void Connect(final int connectionTimeOut, final String address, ConnectCallback connectCallback){

        if(mBleAdapter == null || address == null){
            Log.e(TAG,"No device found at this address : " + address);
            return;
        }
        BluetoothDevice remoteDevice = mBleAdapter.getRemoteDevice(address);
        if(remoteDevice == null){
            Log.w(TAG, "Device not found. Unable to connect.");
            return;
        }
        this.connectCallback = connectCallback;
        MAC_ADDRESS = address;
        mBleGatt = remoteDevice.connectGatt(mContext, false, mGattCallback);
        Log.e(TAG, "connecting mac-address:" + address);
        delayConnectResponse(connectionTimeOut);
    }

    /**
     * 连接设备
     *
     */

    public void Connect(final String address, ConnectCallback connectCallback) {
        Connect(CONNECTION_TIME_OUT, address, connectCallback);
    }

    /**
     * 超时断开
     *
     */

    private void delayConnectResponse(int connectionTimeOut){
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isConnectok && !isMybreak){
                    Log.e(TAG, "connect timeout");
                    disConnection();
                    reConnect();
                }else{
                    isMybreak = false;
                }
            }
        }, connectionTimeOut <= 0 ? CONNECTION_TIME_OUT : connectionTimeOut);
    }

    /**
     * 断开连接
     */
    private void disConnection(){
        if(null == mBleAdapter || null == mBleGatt){
            Log.e(TAG, "disconnection error maybe no init");
            return;
        }
        mBleGatt.disconnect();
        reset();
    }

    //-------------分割線-----------------//
    /**
     * 当前蓝牙是否打开
     */
    private boolean isEnable() {
        if (null != mBleAdapter) {
            return mBleAdapter.isEnabled();
        }
        return false;
    }

    /**
     * 蓝牙GATT连接及操作事件回调
     */

    private class BleGattCallback extends BluetoothGattCallback{
        @SuppressLint("WrongConstant")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;
            if(newState == BluetoothProfile.STATE_CONNECTED){
                Log.e("AAAAAAA","啟動服務發現 : " + mBleGatt.discoverServices());
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                final Intent intent = new Intent();
                intent.setAction(intentAction);
                intent.setFlags(1);
                mContext.sendBroadcast(intent);
                isMybreak = false;
//                isConnectok = true;
                mBleGatt.discoverServices();
                connSuccess();
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){ //斷開連接
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.d(TAG, String.valueOf(mConnectionState));
                final Intent intent = new Intent();
                intent.setAction(intentAction);
                intent.setFlags(2);
                mContext.sendBroadcast(intent);
                if(!isMybreak){
                    reConnect();
                }
                reset();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if(status == BluetoothGatt.GATT_SUCCESS && null != mBleGatt){
                Log.e(TAG,"成功發現服務");

                isConnectok = true;

                Log.d("haha", "onServicesDiscovered: " + "發現服務 : " + status);

                if(mBleGatt != null && isConnectok){
                    BluetoothGattService gattService = mBleGatt.getService(UUID.fromString(UUID_SERVICE));
                    BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(UUID.fromString(UUID_NOTIFY));

                    boolean b = mBleGatt.setCharacteristicNotification(characteristic, true);
                    if(b){

                        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                        for (BluetoothGattDescriptor descriptor : descriptors) {

                            boolean b1 = descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            if (b1) {
                                mBleGatt.writeDescriptor(descriptor);
                                startSend();
                                Log.d(TAG, "startRead: " + "監聽收數據");
                            }

                        }
                    }
                }

//                enableNotificationOfCharacteristic(true);
//
//                List<BluetoothGattService> services = mBleGatt.getServices();
//
//                for(int i = 0; i < services.size(); i++){
//                    bluetoothGattService = services.get(i);
//                    String serviceUuid = bluetoothGattService.getUuid().toString();
//                    Log.d("Servie UUID : ", serviceUuid);
//
//                    List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = bluetoothGattService.getCharacteristics();
//                    for(int j = 0; j < bluetoothGattCharacteristics.size(); j++){
//                        Log.d("Character UUID : ", bluetoothGattCharacteristics.get(j).getUuid().toString());
//                        int charaProp = bluetoothGattCharacteristics.get(j).getProperties();
//                        checkProp(charaProp, bluetoothGattCharacteristics.get(j).getUuid().toString());
////                        readCharacteristic(bluetoothGattCharacteristics.get(j));
//
//                    }
//
//                    if(bluetoothGattService.getUuid().equals(UUID.fromString(UUID_SERVICE))){
//                        mBleGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(UUID_NOTIFY));
//                        Log.d("NOTIFICATION UUID : ", String.valueOf(mBleGattCharacteristic.getUuid()));
//                        //Log.d("setNotification return", String.valueOf(setCharacteristicNotification(mBleGatt, mBleGattCharacteristic, true)));
//
//                    }
//                }

            }else{
                Log.e(TAG,"服務發現失敗，錯誤代碼為 : " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Log.e(TAG,"讀取成功 : " + String.valueOf(characteristic.toString()));

            if(status == BluetoothGatt.GATT_SUCCESS){

                String data = HexUtil.bytesToHexString(characteristic.getValue()); // 将字节转化为String字符串
                Log.e(TAG,"讀取成功 : " + HexUtil.convertStringToHex(characteristic.toString()));
                Log.i("onCharateristicRead", characteristic.toString());
                byte[] value = characteristic.getValue();
                String v = new String(value);
                Log.i("onCharacteristicRead", "Value: " + HexUtil.convertStringToHex(v));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.e(TAG,"寫入成功 : " + characteristic.getValue());
            }
            Log.v(TAG, "onCharacteristicWrite: " + status);

            sIsWriting = false;
            nextWrite();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //Log.v(TAG, "onCharacteristicChanged: " + characteristic.getUuid());
            Log.e(TAG, "onCharacteristicChanged: " + characteristic.getValue());
            byte[] value = characteristic.getValue();
            Log.d(TAG, "onCharacteristicChanged: " + value);
            String s0 = Integer.toHexString(value[0] & 0xFF);
            String s = Integer.toHexString(value[1] & 0xFF);
            Log.d(TAG, "onCharacteristicChanged: " + s0 + "、" + s);
            for (byte b : value) {
                Log.d(TAG, "onCharacteristicChanged: " + b);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.v(TAG, "onDescriptorWrite: " + status);
            sIsWriting = false;
            nextWrite();
        }
    }

    private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic characteristic, int offset) {
        Integer lowerByte = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1);

        return (upperByte << 8) + lowerByte;
    }

    private void enableNotificationOfCharacteristic(final boolean enable){

        UUID ServiceUUID = UUID.fromString(UUID_SERVICE);
        UUID CharaUUID = UUID.fromString(UUID_NOTIFY);
        if(!mBleGatt.equals(null)){
            BluetoothGattService service = mBleGatt.getService(ServiceUUID);
            if(service != null){
                BluetoothGattCharacteristic chara= service.getCharacteristic(CharaUUID);
                if(chara != null){
                    boolean success = mBleGatt.setCharacteristicNotification(chara,enable);
                    Log.e("success", "setCharactNotify: "+success);
                    BluetoothGattDescriptor descriptor = chara.getDescriptor(UUID.fromString(BLUETOOTH_NOTIFY_D));

                    if (descriptor != null){
                        if ((chara.getProperties()&BluetoothGattCharacteristic.PROPERTY_NOTIFY)==0) {
                            Log.e("success", "descriptor: "+(chara.getProperties()&BluetoothGattCharacteristic.PROPERTY_NOTIFY));
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        } else {
                            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                        }
                        SystemClock.sleep(200);
                        mBleGatt.writeDescriptor(descriptor);
                    }
                }
            }
        }
    }

    public void startSend(){
        if(mBleGatt != null && isConnectok){
            BluetoothGattService mGattService = mBleGatt.getService(UUID.fromString(UUID_SERVICE));
            BluetoothGattCharacteristic mGattCharacteristic = mGattService.getCharacteristic(UUID.fromString(UUID_WRITE));
            write(mGattCharacteristic);
//            byte[] bytes = new byte[2];
//            bytes[0] = 04;
//            bytes[1] = 01;
//            mGattCharacteristic.setValue(ENABLE_SENSOR);
//            mGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//            mBleGatt.writeCharacteristic(mGattCharacteristic);
        }
    }

    private synchronized void write(Object o) {
        if(sWriteQueue.isEmpty() && !sIsWriting) {
            doWrite(o);
        } else {
            sWriteQueue.add(o);
        }
    }

    private synchronized void nextWrite() {
        if(!sWriteQueue.isEmpty() && !sIsWriting) {
            doWrite(sWriteQueue.poll());
        }
    }

    private synchronized void doWrite(Object o) {
        if (o instanceof BluetoothGattCharacteristic) {
            sIsWriting = true;
            mBleGatt.writeCharacteristic(
                    (BluetoothGattCharacteristic) o);
        } else if (o instanceof BluetoothGattDescriptor) {
            sIsWriting = true;
            mBleGatt.writeDescriptor((BluetoothGattDescriptor) o);
        } else {
            nextWrite();
        }
    }

    private void checkProp(int mProp , String char_UUID){
        if((mProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0){
            Log.e(TAG, char_UUID + " 的屬性為 可讀");
        }
        if((mProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0){
            Log.e(TAG, char_UUID + " 的屬性為 可寫");
        }
        if((mProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0){
            Log.e(TAG, char_UUID + " 的屬性為 通知屬性");
        }
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBleAdapter == null || mBleGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBleGatt.readCharacteristic(characteristic);
    }

    public void readCharacteristic() {
//        BluetoothGattService service = mBleGatt.getService(UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"));
//        BluetoothGattCharacteristic characteristic= service.getCharacteristic(UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb"));
//        BluetoothGattCharacteristic characteristic= service.getCharacteristic(UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb"));
//        BluetoothGattCharacteristic characteristic= service.getCharacteristic(UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb"));

        BluetoothGattService service = mBleGatt.getService(UUID.fromString(UUID_SERVICE));
        BluetoothGattCharacteristic characteristic= service.getCharacteristic(UUID.fromString(UUID_NOTIFY));
        mBleGatt.readCharacteristic(characteristic);
        Log.d("PPPPPPPPPPPPPPP : ", String.valueOf(characteristic.getUuid()));
        if (mBleAdapter == null || mBleGatt == null){
//            BluetoothGattService service = mBleGatt.getService(UUID.fromString("00001801-0000-1000-8000-00805f9b34fb"));

//            mBleGatt.readCharacteristic(characteristic);
        }
    }

    // TODO 此方法断开连接或连接失败时会被调用。可在此处理自动重连,内部代码可自行修改，如发送广播
    private void reConnect() {
        if (connectCallback != null) {
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
//                    connectCallback.onConnFailed();
                    if(MAC_ADDRESS != null){
                        connectCallback.onReConn();
                        Connect(CONNECTION_TIME_OUT, MAC_ADDRESS, connectCallback);
                    }
                }
            });
        }

        Log.e(TAG, "Ble disconnect or connect failed!");
    }

    private void runOnMainThread(Runnable runnable) {
        if(isMainThread()){
            runnable.run();
        }else{
            if(mHandler != null){
                mHandler.post(runnable);
            }
        }
    }

    private boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    // TODO 此方法Notify成功时会被调用。可在通知界面连接成功,内部代码可自行修改，如发送广播
    private void connSuccess() {
        if (connectCallback != null) {
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    connectCallback.onConnSuccess();
                }
            });
        }
        Log.e(TAG, "Ble connect success!");
    }
}
