package com.spectrochips.spectrumsdk.FRAMEWORK;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by ADMIN on 27-05-2019.
 */

public class BluetoothReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.e("Bluetooth off","call");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.e("Turning Bluetooth off...","call");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.e("Bluetooth on","call");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.e("Turning Bluetooth on...","call");
                    break;
            }
        }
    }
}
