package com.spectrochips.spectrumsdk.FRAMEWORK;

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
import android.os.Message;
import android.util.Log;
import android.os.Build;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


public class UartService extends Service {
    Context context;
    private final static String TAG = UartService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.nordicsemi.nrfUART.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.nordicsemi.nrfUART.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.nordicsemi.nrfUART.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.nordicsemi.nrfUART.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.nordicsemi.nrfUART.EXTRA_DATA";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART =
            "com.nordicsemi.nrfUART.DEVICE_DOES_NOT_SUPPORT_UART";
    public final static String  ACTION_DATA_AVAILABLE_DATA="com.nordicsemi.nrfUART.ACTION_DATA_AVAILABLE";
    public static final UUID TX_POWER_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");//("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");//("0000ffe0-0000-1000-8000-00805f9b34fb");//("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    public static final int GATT_WRITE_TIMEOUT = 10000; // Milliseconds
    public static UUID UUID_HM_RX_TX = null;

    private int notiCount;

    BluetoothGattCharacteristic wCharacterstic;
    private volatile boolean mBusy = false; // Write/read pending response

    public void fillContext(Context context1) {
        context = context1;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
              String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                Log.e(TAG, "Connected to GATT server.");
                Log.e(TAG, "Attempting to start service discovery:");
               new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothGatt=gatt;
                       gatt.discoverServices();
                    }
                });
                broadcastUpdate(intentAction);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                SCConnectionHelper.getInstance().isConnected = false;
                SCConnectionHelper.getInstance().disconnectWithPeripheral();
                if ( SCConnectionHelper.getInstance().scanDeviceInterface == null) {
                } else {
                    SCConnectionHelper.getInstance().scanDeviceInterface.onFailureForConnection("Device DisConnected");
                }
                broadcastUpdate(intentAction);
            }
            notiCount = 0;
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e("onservice","call"+status);
            List<BluetoothGattService> ser=mBluetoothGatt.getServices();
            for(int i=0;i<ser.size();i++){
                BluetoothGattService  service  = ser.get(i);
                if(service.getUuid().equals(RX_SERVICE_UUID)){
                    List<BluetoothGattCharacteristic> charectersticArray =service.getCharacteristics();
                    for(int j=0;j<charectersticArray.size();j++){
                        BluetoothGattCharacteristic    characteristic = charectersticArray.get(j);
                        if(characteristic.getUuid().equals(TX_CHAR_UUID)){
                            Log.e("Called Matched","Charecterstic");
                            wCharacterstic=characteristic;
                            mBluetoothGatt.setCharacteristicNotification(characteristic,true);
                            SCConnectionHelper.getInstance().isConnected = true;
                            if ( SCConnectionHelper.getInstance().scanDeviceInterface == null) {
                            } else {
                                Log.e("connect","call"+mBluetoothGatt.getServices());
                                SCConnectionHelper.getInstance().scanDeviceInterface.onSuccessForConnection("Device Connected");
                            }
                        }
                    }
                }
                Log.e("chara","call"+ser.get(i).getCharacteristics().get(0).getUuid());
                Log.e("uuid","call"+ser.get(i).getUuid());
                // broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
            Log.e("enable","call"+mBluetoothGatt.getServices());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt);
                // startActivity(new Intent(getApplicationContext(), SCFilesViewController.class));
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscoveredreceived: " + status);
            }

        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //  broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            Log.e(TAG, "Received TX: "  + notiCount + " - " + characteristic.getStringValue(0) );
            notiCount++;
            if(SCTestAnalysis.getInstance().testDataInterface !=null){
                SCTestAnalysis.getInstance(). testDataInterface.gettingData(characteristic.getValue());
            }
            //  broadcastUpdate(ACTION_DATA_AVAILABLE_DATA, characteristic);
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            mBusy = false;
        }
        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            mBusy = false;
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite: " + descriptor.getUuid().toString());
            mBusy = false;
        }
    };
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
            try {
                //  String s= new String(characteristic.getValue(), "UTF-8");
                // Log.e("utfstring","call"+s);
                final byte[] txValue = characteristic.getValue();
                //Log.e("ReceivedBytes", "call" + txValue.length);
                String text = new String(characteristic.getValue(),"UTF-8");
                Log.e("ReceivedBytes", "call" + text);
                // SCTestAnalysis.getInstance(). socketDidReceiveMessage(text, SCTestAnalysis.getInstance().requestCommand);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            notiCount++;
            /*if(SCTestAnalysis.getInstance().testDataInterface !=null){
                SCTestAnalysis.getInstance(). testDataInterface.gettingData(characteristic.getValue());
            }*/
            // intent.putExtra(EXTRA_DATA, characteristic.getValue());
        } else {

        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        UartService getService() {
            return UartService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();


    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter =mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public boolean connect(final BluetoothDevice device) {
        if (mBluetoothAdapter == null || device == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && device.getAddress().equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        /*final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(bl);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }*/
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        /*if (Build.VERSION.SDK_INT < 23) {
            Log.d(TAG, "Trying to create a new connection.");
            mBluetoothGatt = device.connectGatt(context, false,mGattCallback);
        }else{
            Log.d(TAG, "Trying to higher create a new connection.");
            mBluetoothGatt = device.connectGatt(context, false, mGattCallback,BluetoothDevice.TRANSPORT_LE);
        }*/
        mBluetoothGatt = device.connectGatt(context, false,mGattCallback);
        mBluetoothDeviceAddress = device.getAddress();
        mConnectionState = STATE_CONNECTING;
        return true;
    }
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
         mBluetoothDeviceAddress=null;
       //  mBluetoothGatt=null;
        Log.v("disconnectmethod", "mBluetoothGatt closed");
    }
    public void close() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.disconnect();
        mBluetoothGatt = null;
        Log.v("closemethod", "mBluetoothGatt closed");
    }


    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

    }



    public void enableTXNotification() {
        List<BluetoothGattService> ser=mBluetoothGatt.getServices();
        for(int i=0;i<ser.size();i++){
            Log.e("chara","call"+ser.get(i).getCharacteristics());
        }
        Log.e("enable","call"+mBluetoothGatt.getServices());
        if (mBluetoothGatt == null) {
            showMessage("mBluetoothGatt null" + mBluetoothGatt);
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }


        BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Tx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        try {
            mBluetoothGatt.setCharacteristicNotification(TxChar, true);
            BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }

    public BluetoothGattCharacteristic getRXChar() {
        BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
        showMessage("mBluetoothGatt null" + mBluetoothGatt);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return null;
        }
        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        if (RxChar == null) {
            showMessage("Rx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return null;
        }

        return RxChar;
    }

    public BluetoothGatt getGatt()
    {
        return mBluetoothGatt;
    }

    public boolean writeRXCharacteristic(BluetoothGattCharacteristic characteristic) {
        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean waitIdle(int timeout) {
        timeout /= 5;
        while (--timeout > 0) {
            if (mBusy)
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            else
                break;
        }

        return timeout > 0;
    }

    public void writeRXCharacteristic(String value) {
        if (wCharacterstic == null) {
            showMessage("sample Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }else {
            value=value+"\n";
            //String text = decodeUTF8(txValue);
            Log.e("writeRXCharacteristic","call"+value);
            Log.e("charaValue","call"+value+wCharacterstic.getWriteType());
            wCharacterstic.setWriteType(wCharacterstic.getWriteType());
            wCharacterstic.setValue(value);//("$CAL#\n");
            boolean status = mBluetoothGatt.writeCharacteristic(wCharacterstic);
            Log.e("chsrstatus","call"+status);
            if (status) {
                //mBluetoothGatt.readCharacteristic(wCharacterstic);
                waitIdle(GATT_WRITE_TIMEOUT);
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
