package com.jameszmq.omnikey;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

public class OmniKeyService extends Service {

    private final IBinder mBinder = new OmniKeyServiceBinder();
    private MainActivity activity;

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    BluetoothDevice bluetoothDevice;
    BluetoothGatt server;
    BluetoothGattService serverService;
    BluetoothGattCharacteristic characteristic;

    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    public OperationQueue operationQueue = new OperationQueue();

    public OmniKeyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        geofencingClient = LocationServices.getGeofencingClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("Service", "Service Destroyed");
    }

    public class OmniKeyServiceBinder extends Binder {
        OmniKeyService getService() {
            return OmniKeyService.this;
        }
    }

    public void scanDev() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void disconnectDev() {
        server.disconnect();
    }

    public void setCallBack(MainActivity context) {
        activity = context;
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice().getName() != null) {
                if (result.getDevice().getName().contains("MODULE")) {
                    if (activity != null) {
                        activity.pairSuccess();
                    }
                    bluetoothDevice = result.getDevice();
                    // Receives command responses and information from device server.
                    BluetoothGattCallback serverCallback = new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            super.onConnectionStateChange(gatt, status, newState);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                if (newState == STATE_CONNECTED) {
                                    server.discoverServices();
                                } else {
                                    Message message = activity.mHandler.obtainMessage();
                                    message.sendToTarget();
                                    server.close();
                                }
                            }
                        }

                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                            super.onServicesDiscovered(gatt, status);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                List<BluetoothGattService> services = gatt.getServices();
                                serverService = server.getService(services.get(2).getUuid());
                                List<BluetoothGattCharacteristic> characteristics = serverService.getCharacteristics();
                                characteristic = serverService.getCharacteristic(characteristics.get(0).getUuid());
                                server.setCharacteristicNotification(characteristic, true);
                                operationQueue.onConnection(server, characteristic);
                                operationQueue.onComplete();
                            }
                        }

                        @Override
                        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                            super.onCharacteristicWrite(gatt, characteristic, status);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                operationQueue.onComplete();
                            }
                        }

                        @Override
                        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                            super.onCharacteristicChanged(gatt, characteristic);
                            byte[] data = characteristic.getValue();
                            handleMsg(new String(data));
                        }
                    };

                    server = bluetoothDevice.connectGatt(
                            OmniKeyService.this,
                            false,
                            serverCallback);
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            btScanner.stopScan(leScanCallback);
                        }
                    });
                }
            }
        }
    };

    public void setGeofence(Location location, int which) {
        removeGeofence(which);
        Geofence geofence = new Geofence.Builder()
                .setRequestId(Integer.toString(which))
                .setCircularRegion(
                        location.getLatitude(),
                        location.getLongitude(),
                        300
                )
                .setExpirationDuration(NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        GeofencingRequest request = new GeofencingRequest.Builder()
                .addGeofence(geofence) // add a Geofence
                .build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        geofencingClient.addGeofences(request, getGeofencePendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("GeofenceClient", "geofence added.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(OmniKeyService.this, "Failed to add Geofence", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void removeGeofence(int which) {
        ArrayList<String> remove = new ArrayList<String>();
        remove.add(Integer.toString(which));
        geofencingClient.removeGeofences(remove);
        operationQueue.enqueue(Mapper.map(Integer.toString(which), false));
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    private void handleMsg(String message) {

    }

}
