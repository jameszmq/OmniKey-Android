package com.jameszmq.omnikey;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

public class OperationQueue {
    private Queue<String> queue;
    private String curr = null;
    BluetoothGatt server = null;
    BluetoothGattCharacteristic characteristic = null;

    public OperationQueue () {
        queue = new LinkedList<String>();
    }

    public void onConnection(BluetoothGatt server, BluetoothGattCharacteristic characteristic) {
        this.server = server;
        this.characteristic = characteristic;
    }

    public void enqueue(String number) {
        queue.add(number);
        Log.d("Queue", "Enqueued: " + number);
        if (server != null && curr == null) {
            curr = queue.poll();
            characteristic.setValue(number);
            server.writeCharacteristic(characteristic);
            Log.d("Queue", "Written: " + curr);
        }
    }

    public void onComplete() {
        curr = queue.poll();
        if (server != null && curr != null) {
            Log.d("Queue", "Dequeued: " + curr);
            characteristic.setValue(curr);
            server.writeCharacteristic(characteristic);
            Log.d("Queue", "Written: " + curr);
        }
    }

}
