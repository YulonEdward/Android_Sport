package com.example.yulon.newblecommunicate;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yulon.newblecommunicate.command.Command;
import com.example.yulon.newblecommunicate.service.BleService;
import com.example.yulon.newblecommunicate.service.FirebaseService;
import com.example.yulon.newblecommunicate.service.GPSService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class BleDeviceControlActivity extends AppCompatActivity {

    private static final String TAG = TestActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final byte[] START_ENABLE_SENSOR = {0x45, 0x53};
    private static final byte[] STOP_ENABLE_SENSOR = {0x45, 0x50};
    private static final byte[] RESUME_ENABLE_SENSOR = {0x45, 0x52};
    private static final byte[] OVER_ENABLE_SENSOR = {0x45, 0x54};
    private static final byte[] BATTERY_ENABLE_SENSOR = {0x42, 0x47};
    private static final byte[] VERSION_ENABLE_SENSOR = {0x56, 0x4E};
    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private static int i_InitStepsData = 0;

    private boolean mAlreadyStartedService = false;

    private String mDeviceName;
    private String mDeviceAddress;
    public static String str_initStepsData = null;

    private TextView txtDeviceName;
    private TextView txtDeviceAdrress;
    private TextView txtDeviceState;
    private TextView txtDataSteps;
    private boolean bConnected = false;

    LocalBroadcastManager mLocalBroadcastManager, Ble_mLocalBroadcastManager;
    BroadcastReceiver mReceiver, Ble_mReceiver;
    IntentFilter filter, BleService_filter;
    Button btn_Start, btn_Stop, btn_Resume, btn_Over, btn_Battery, btn_Version;

    private FirebaseService mFirebaseService = null;
    private BleService mBleService = null;


    private ServiceConnection mBleServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder)
        {
            // TODO Auto-generated method stub
            mBleService = ((BleService.LocalBinder)serviceBinder).getService();

        }

        public void onServiceDisconnected(ComponentName name)
        {
            // TODO Auto-generated method stub
            Log.d(TAG, "onServiceDisconnected()" + name.getClassName());
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mFirebaseService = ((FirebaseService.LocalBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()" + name.getClassName());
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BleService.ACTION_GATT_CONNECTED.equals(action)){
                bConnected = true;
                updateConnectionState(R.string.connected);
                new CountDownTimer(3000, 1000){
                    @Override
                    public void onTick(long millisUntilFinished) {
                        Log.d(TAG, "seconds remaining: " + millisUntilFinished / 1000);
                    }

                    @Override
                    public void onFinish() {
                        mBleService.wirteCharacteristic(mDeviceAddress, START_ENABLE_SENSOR);
                    }
                }.start();

            }else if(BleService.ACTION_GATT_DISCONNECTED.equals(action)){
                bConnected = false;
                updateConnectionState(R.string.disconnected);

            }else if(BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){

            }else if(BleService.ACTION_DATA_AVAILABLE.equals(action)){

            }

        }

    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        initView();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        filter = new IntentFilter();
        filter.addAction(GPSService.ACTION_LOCATION_BROADCAST);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String latitude = intent.getStringExtra(GPSService.EXTRA_LATITUDE);
                String longitude = intent.getStringExtra(GPSService.EXTRA_LONGITUDE);
                String lastTime = intent.getStringExtra(GPSService.EXTRA_CURTIME);
//
//                if (latitude != null && longitude != null) {
//                    Toast.makeText(BleDeviceControlActivity.this, getString(R.string.msg_location_service_started) + "\n Latitude : " + latitude + "\n Longitude: " + longitude
//                            + "\n Time: " + lastTime, Toast.LENGTH_SHORT).show();
//                    if(mFirebaseService != null){
//                        mFirebaseService.Fire_locationData(latitude + "," + longitude);
//                        mFirebaseService.Fire_lastTimeData(lastTime);
//                    }
//                }

            }
        };

        mLocalBroadcastManager.registerReceiver(mReceiver, filter);

        Ble_mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        BleService_filter = new IntentFilter();
        BleService_filter.addAction(BleService.ACTION_BLESERVICE_BROADCAST);

        Ble_mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd");
                //取得現在時間
                Date dt=new Date();

                String str_Steps = intent.getStringExtra(BleService.EXTRA_STEPS);
                Log.d("str_Steps: ", str_Steps);
                if(mFirebaseService != null && mDeviceAddress != null){
                    String str_DataSteps = mFirebaseService.Firebase_GetStepsData(mDeviceAddress,  sdf.format(dt));
                    Log.d("str_Steps: ", mDeviceAddress);
                    Log.d("str_Steps: ",  sdf.format(dt));

                    if(str_Steps != null && str_DataSteps != null){
                        mFirebaseService.Fire_deviceStepsData(mDeviceAddress, sdf.format(dt), String.valueOf(Integer.parseInt(str_Steps) + Integer.parseInt(str_DataSteps)));
                        txtDataSteps.setText(String.valueOf(Integer.parseInt(str_Steps) + Integer.parseInt(str_DataSteps)));
//                        txtDataSteps.setText(str_DataSteps);

                    }
                }
            }
        };

        Ble_mLocalBroadcastManager.registerReceiver(Ble_mReceiver, BleService_filter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, FirebaseService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        Intent it = new Intent(this, BleService.class);
        bindService(it, mBleServiceConnection, BIND_AUTO_CREATE); //綁定Service
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        startStep1();
    }

    /**
     * Step 1: Check Google Play services
     */
    private void startStep1() {

        //Check whether this user has installed Google play service which is being used by Location updates.
        if (isGooglePlayServicesAvailable()) {

            //Passing null to indicate that it is executing for the first time.
            startStep2(null);

        } else {
            Toast.makeText(getApplicationContext(), R.string.no_google_playservice_available, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Step 2: Check & Prompt Internet connection
     */
    private Boolean startStep2(DialogInterface dialog) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            promptInternetConnect();
            return false;
        }

        if (dialog != null) {
            dialog.dismiss();
        }

        //Yes there is active internet connection. Next check Location is granted by user or not.

        if (checkPermissions()) { //Yes permissions are granted by the user. Go to the next step.
            startStep3();
        } else {  //No user has not granted the permissions yet. Request now.
            requestPermissions();
        }
        return true;
    }

    /**
     * Show A Dialog with button to refresh the internet state.
     */
    private void promptInternetConnect() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BleDeviceControlActivity.this);
        builder.setTitle(R.string.title_alert_no_intenet);
        builder.setMessage(R.string.msg_alert_no_internet);

        String positiveText = getString(R.string.btn_label_refresh);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                        //Block the Application Execution until user grants the permissions
                        if (startStep2(dialog)) {

                            //Now make sure about location permission.
                            if (checkPermissions()) {

                                //Step 2: Start the Location Monitor Service
                                //Everything is there to start the service.
                                startStep3();
                            } else if (!checkPermissions()) {
                                requestPermissions();
                            }

                        }
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Step 3: Start the Location Monitor Service
     */
    private void startStep3() {

        //And it will be keep running until you close the entire application from task manager.
        //This method will executed only once.

        if (!mAlreadyStartedService) {

            //Start location sharing service to app server.........
            Intent intent = new Intent(this, GPSService.class);
            startService(intent);

            mAlreadyStartedService = true;
            //Ends................................................
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        // mLocalBroadcastManager.unregisterReceiver(mReceiver);
        Ble_mLocalBroadcastManager.unregisterReceiver(Ble_mReceiver);
        mBleService.disconnect();
        mFirebaseService = null;
        unbindService(mServiceConnection); //解除綁定Service
        mBleService = null;
        unbindService(mBleServiceConnection); //解除綁定Service
    }

    private void initView(){
        txtDeviceAdrress = (TextView)findViewById(R.id.device_address);
        txtDeviceState = (TextView)findViewById(R.id.connection_state);
        txtDataSteps = (TextView)findViewById(R.id.txt_steps);
        btn_Start = (Button)findViewById(R.id.btn_startsport);
        btn_Stop = (Button)findViewById(R.id.btn_stopsport);
        btn_Resume = (Button)findViewById(R.id.btn_resumesport);
        btn_Over = (Button)findViewById(R.id.btn_oversport);
        btn_Battery = (Button)findViewById(R.id.btn_battery);
        btn_Version = (Button)findViewById(R.id.btn_version);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        txtDeviceAdrress.setText(mDeviceAddress);

        btn_Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bConnected){
                    mBleService.wirteCharacteristic(mDeviceAddress, START_ENABLE_SENSOR);
                }
            }
        });

        btn_Stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bConnected){
                    mBleService.wirteCharacteristic(mDeviceAddress, STOP_ENABLE_SENSOR);
                }
            }
        });

        btn_Resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bConnected){
                    mBleService.wirteCharacteristic(mDeviceAddress, RESUME_ENABLE_SENSOR);
                }
            }
        });

        btn_Over.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bConnected){
                    mBleService.wirteCharacteristic(mDeviceAddress, OVER_ENABLE_SENSOR);
                }
            }
        });

        btn_Battery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bConnected){
                    mBleService.wirteCharacteristic(mDeviceAddress, BATTERY_ENABLE_SENSOR);
                }
            }
        });

        btn_Version.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bConnected){
                    mBleService.wirteCharacteristic(mDeviceAddress, VERSION_ENABLE_SENSOR);
                }
            }
        });

    }


    private void updateConnectionState(final int resourceId){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtDeviceState.setText(resourceId);
            }
        });
    }

    /**
     * Return the availability of GooglePlayServices
     */
    public boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(this, status, 2404).show();
            }
            return false;
        }
        return true;
    }


    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState1 = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);

        int permissionState2 = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION);

        return permissionState1 == PackageManager.PERMISSION_GRANTED && permissionState2 == PackageManager.PERMISSION_GRANTED;

    }

    /**
     * Start permissions requests.
     */
    private void requestPermissions() {

        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        boolean shouldProvideRationale2 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION);


        // Provide an additional rationale to the img_user. This would happen if the img_user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale || shouldProvideRationale2) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(BleDeviceControlActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the img_user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(BleDeviceControlActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }


    /**
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If img_user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                startStep3();

            } else {
                // Permission denied.

                // Notify the img_user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the img_user for permission (device policy or "Never ask
                // again" prompts). Therefore, a img_user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // mLocalBroadcastManager.unregisterReceiver(mReceiver);
        Ble_mLocalBroadcastManager.unregisterReceiver(Ble_mReceiver);

    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_BLESERVICE_BROADCAST);
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


}
