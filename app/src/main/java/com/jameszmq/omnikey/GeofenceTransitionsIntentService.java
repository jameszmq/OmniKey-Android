package com.jameszmq.omnikey;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import static android.bluetooth.BluetoothAdapter.STATE_OFF;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class GeofenceTransitionsIntentService extends IntentService {

    OmniKeyService.OmniKeyServiceBinder binder;
    OmniKeyService service;
    ServiceConnection serviceConnection;

    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(this, OmniKeyService.class);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder mbinder) {
                binder = (OmniKeyService.OmniKeyServiceBinder) mbinder;
                service = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        bindService(intent, serviceConnection, 0);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            for (Geofence geofence : triggeringGeofences) {
                Log.d("Geofence","Enter Triggered: " + geofence.getRequestId());
                service.operationQueue.enqueue(Mapper.map(geofence.getRequestId(), true));
            }
        }
        else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            for (Geofence geofence : triggeringGeofences) {
                Log.d("Geofence","Exit Triggered: " + geofence.getRequestId());
                service.operationQueue.enqueue(Mapper.map(geofence.getRequestId(), false));
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }
}
