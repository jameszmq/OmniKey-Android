package com.jameszmq.omnikey;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_OFF;

public class MainActivity extends AppCompatActivity {

    OmniKeyService.OmniKeyServiceBinder binder;
    OmniKeyService service;
    ServiceConnection serviceConnection;

    BroadcastReceiver broadcastReceiver;
    Handler mHandler;

    Button pair, unpair;

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    KeyListAdapter keyListAdapter;

    private FusedLocationProviderClient fusedLocationClient;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int KEY_NUM = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.list);
        keyListAdapter = new KeyListAdapter(this, KEY_NUM);
        recyclerView.setAdapter(keyListAdapter);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        pair = findViewById(R.id.pair_btn);
        unpair = findViewById(R.id.unpair_btn);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (!isBLESupport()) {
            Toast.makeText(this, "BLE is not supported on this device", Toast.LENGTH_SHORT).show();
        }
         broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if(state == BluetoothAdapter.STATE_TURNING_OFF || state == STATE_OFF) {
                        Toast.makeText(context, "Bluetooth is turned off.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        Intent intent = new Intent(this, OmniKeyService.class);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder mbinder) {
                binder = (OmniKeyService.OmniKeyServiceBinder) mbinder;
                service = binder.getService();
                service.setCallBack(MainActivity.this); // register
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        startService(intent);
        bindService(intent, serviceConnection, 0);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                unpairSuccess();
            }
        };
    }

    public void pairDevice(View v) {
        Toast.makeText(MainActivity.this, "Pairing Device", Toast.LENGTH_SHORT).show();
        service.scanDev();
    }

    public void unpairDevice(View v) {
        Toast.makeText(MainActivity.this, "Disconnecting from Device", Toast.LENGTH_SHORT).show();
        service.disconnectDev();
    }

    public void pairSuccess() {
        Toast.makeText(MainActivity.this, "Device successfully paired", Toast.LENGTH_SHORT).show();
        pair.setVisibility(View.INVISIBLE);
        unpair.setVisibility(View.VISIBLE);
    }

    public void unpairSuccess() {
        Toast.makeText(MainActivity.this, "Device successfully disconnected", Toast.LENGTH_SHORT).show();
        pair.setVisibility(View.VISIBLE);
        unpair.setVisibility(View.INVISIBLE);
    }

    private boolean isBLESupport() {
        return BluetoothAdapter.getDefaultAdapter() != null
                && this.getPackageManager().hasSystemFeature(getPackageManager().FEATURE_BLUETOOTH_LE);
    }

    public Location addFence(final int which) {

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION );
                }
            });
            builder.show();
        }
        else {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object
                                keyListAdapter.setLat(location.getLatitude(), which);
                                keyListAdapter.setLng(location.getLongitude(), which);
                                service.setGeofence(location, which);
                            }
                        }
                    });
        }
        return null;
    }

    public void removeFence(int which) {
        service.removeGeofence(which);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, OmniKeyService.class);
        stopService(intent);
        unbindService(serviceConnection);
        unregisterReceiver(broadcastReceiver);
    }
}
