package com.spectrochips.spectrumsdk.FRAMEWORK;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.spectrochips.spectrumsdk.MODELS.ImageSensorStruct;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.ANALOG_GAIN_TAG;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.AVG_FRAME_TAG;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.DIGITALGAIN_CONST_VALUE;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.DIGITAL_GAIN_TAG;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.END_TAG;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.EXPOUSURE_TAG;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.LED_TURN_OFF;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.LED_TURN_ON;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.MOVE_STRIP_COUNTER_CLOCKWISE_TAG;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.MOVE_STRIP_POSITION;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.ROI_TAG;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.START_TAG;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.UV_TURN_OFF;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.UV_TURN_ON;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.WIFI_INFO_CHANGE_TAG;

public class SCConnectionHelper {
    private static SCConnectionHelper myObj;
    public boolean isConnected = false;
    public BluetoothAdapter mBluetoothAdapter;
    //  public BluetoothLeScanner scanner;
    private boolean mScanning;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private int mState = UART_PROFILE_DISCONNECTED;
    private static final String TAG = "SCConnectionHelper";
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    private static final long SCAN_PERIOD = 10000; //10 seconds
    private Handler mHandler = new Handler();
    public ScanDeviceInterface scanDeviceInterface;
    private String COMPANY_BLE_IDENTIFIER ="0D00DBFB";
    //  public UUID serviceUUID = UUID.fromString("FFE0");
    /// UUID of the characteristic to look for.
    // public UUID characteristicUUID = UUID.fromString("FFE1");


    public static SCConnectionHelper getInstance() {
        if (myObj == null) {
            myObj = new SCConnectionHelper();
        }
        return myObj;
    }

    private void devicesNotDiscovered() {
        if (deviceList.size() == 0) {
            didDevicesNotFound();
        }
       
        startScan(true);
    }
    public void startScan(boolean enable) {
       deviceList.clear();
        if (enable) {
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    mScanning = false;
                    // mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, 10000L);
            this.mScanning = true;
            this.mBluetoothAdapter.startLeScan(this.mLeScanCallback);
        } else {
            this.mScanning = false;
            this.mBluetoothAdapter.stopLeScan(this.mLeScanCallback);
        }
    }
    /*private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            // deviceList.add(device);
            Log.e("scanCallback", "call" + device.getName() + device.getAddress() );
            addDevice(device, 0);
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            // Ignore for now
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.e("onScanFailed", "call" +errorCode);
            // Ignore for now
        }
    };*/
   /* public void startScan(final boolean enable) {
        Log.e("scanmethod","call"+enable);
       final UUID uuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    deviceList.clear();
                    Log.e("uuid","call"+uuid);
                    UUID[] serviceUUIDs = new UUID[]{uuid};
                    List<ScanFilter> filters = null;
                    if(serviceUUIDs != null) {
                        System.out.print(uuid.toString());
                        filters = new ArrayList<>();
                        for (UUID serviceUUID : serviceUUIDs) {
                            ScanFilter filter = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                filter = new ScanFilter.Builder()
                                        .setServiceUuid(new ParcelUuid(serviceUUID))
                                        .build();
                            }
                            filters.add(filter);
                        }
                    }
                    ScanSettings scanSettings=null;
                   // if(android.os.Build.VERSION.SDK_INT >=Build.VERSION_CODES.M){
                         scanSettings = new ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                                .setReportDelay(0L)
                                .build();
                   // }
                    if (scanner != null) {
                        scanner.startScan(filters, scanSettings, scanCallback);
                        Log.d(TAG, "scan started");

                    }  else {
                        Log.e(TAG, "could not get scanner object");
                        startScan(true);
                    }
                   // startScan(true);
                   // didDevicesNotFound();
                }
            }, SCAN_PERIOD);
            mScanning = true;
        //  mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            stopScan();
        }
    }
*/
    private void didDevicesNotFound() {
        scanDeviceInterface.onSuccessForScanning(deviceList,false);
    }

    public void stopScan() {
        // scanner.stopScan(scanCallback);
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mHandler.removeCallbacksAndMessages(null);
    }
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device.getAddress() != null && device.getName()!=null) {
                addDevice(device, rssi);
            }
        }
    };
   /* private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            Log.e("mLeScanCallback", "call" + device.getName() + device.getAddress() + scanRecord.length);
            if (device.getAddress() != null) {
                addDevice(device, rssi);
            }
           *//* if (bytesToHexString(scanRecord).contains(COMPANY_BLE_IDENTIFIER)) {
                if (device.getAddress() != null) {
                    addDevice(device, rssi);
                }
            }*//*
        }
    };*/

    private void addDevice(BluetoothDevice device, int rssi) {
        Log.e("scandevice","call"+device.getAddress()+device.getName()+device);
        boolean deviceFound = false;
        for (BluetoothDevice listDev : deviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }
        if (!deviceFound) {
            deviceList.add(device);
            if (scanDeviceInterface == null) {
                //Fire proper event. bitmapList or error message will be sent to
                //class which set scanDeviceInterface.
            } else {
                scanDeviceInterface.onSuccessForScanning(deviceList ,true);
            }
        }
    }

    private final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                mState = UART_PROFILE_CONNECTED;
                Log.e("broadcastconnected", "call");
                /*isConnected = true;
                if (scanDeviceInterface == null) {
                    //Fire proper event. bitmapList or error message will be sent to
                    //class which set scanDeviceInterface.
                } else {
                    scanDeviceInterface.onSuccessForConnection("Device Connected");
                }*/
            }
            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                Log.d(TAG, "UART_DISCONNECT_MSG");
                mState = UART_PROFILE_DISCONNECTED;
                isConnected = false;
                SCTestAnalysis.getInstance().mService.close();
                Log.e("broadcast", "call");
                if (scanDeviceInterface == null) {
                } else {
                    scanDeviceInterface.onFailureForConnection("Dis Connected");
                }
            }
            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                Log.e("servicedis","call");
                // SCTestAnalysis.getInstance().mService.enableTXNotification();
                isConnected = true;
                if (scanDeviceInterface == null) {
                    //Fire proper event. bitmapList or error message will be sent to
                    //class which set scanDeviceInterface.
                } else {
                    scanDeviceInterface.onSuccessForConnection("Device Connected");
                }
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                /*final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                Log.e("Received Bytes", "" + txValue.length);*/
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
                disconnectWithPeripheral();
                if (scanDeviceInterface == null) {
                    //Fire proper event. bitmapList or error message will be sent to
                    //class which set scanDeviceInterface.
                } else {
                    scanDeviceInterface.onFailureForConnection("DisConnected");
                }
            }
//BluetoothAdapter: startLeScan: cannot get BluetoothLeScanner

        }
    };


    public void initilizeSevice() {
        /*Intent bindIntent = new Intent(SpectroCareSDK.getInstance().context, UartService.class);
        SpectroCareSDK.getInstance().context.bindService(bindIntent, mServiceConnection, SpectroCareSDK.getInstance().context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(SpectroCareSDK.getInstance().context).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
   */ }
    private  IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        //intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            SCTestAnalysis.getInstance().mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + SCTestAnalysis.getInstance().mService);
            if (!SCTestAnalysis.getInstance().mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                if (scanDeviceInterface == null) {
                    //Fire proper event. bitmapList or error message will be sent to
                    //class which set scanDeviceInterface.
                } else {
                    scanDeviceInterface.uartServiceClose("Close service");
                }
                //finish();
            }
        }
        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnectWithPeripheral(mDevice);
            SCTestAnalysis.getInstance().mService = null;
        }
    };

    public void ejectStripCommand() {
        String ejectCommand  = "$MRS5000#";
        Log.e("motorPostionControl", "call" + ejectCommand);
        if (isConnected) {
            SCTestAnalysis.getInstance().sendString(ejectCommand);
        }
    }

    public void prepareCommandForChangeSSIDandPassword(String ssid, String password) {
        Log.e("ssidpsw", "call" + ssid + password);

        //"$APC@SSID@PWD@PASSWORD@!"
        String commandString = WIFI_INFO_CHANGE_TAG;
        commandString = commandString.replace("@SSID@", ssid);
        commandString = commandString.replace("@PASSWORD@", password);
        Log.e("motorPostionControl", "call" + commandString);
        if (isConnected) {
            SCTestAnalysis.getInstance().sendString(commandString);
        }
    }

    public void prepareCommandForAnalogGain(String analogValue) {

        String commandString = START_TAG;
        commandString = commandString + ANALOG_GAIN_TAG;
        commandString = commandString + analogValue;
        commandString = commandString + END_TAG;
        Log.e("CommandForAnalogGain", "call" + commandString);
        if (isConnected) {
            SCTestAnalysis.getInstance().sendString(commandString);
        }
    }

    public void prepareCommandForDigitalGain(double digitalGainValue) {

        String commandString = START_TAG;
        commandString = commandString + DIGITAL_GAIN_TAG;
        String digitalGainString = String.valueOf(digitalGainValue);

        if (digitalGainString.contains(".")) {
            String digitalGainArray[] = digitalGainString.split("\\.");

            int firstValue = Integer.parseInt(digitalGainArray[0]);
            double secondValue = digitalGainValue - (double) firstValue;
            int finalSecondValue = (int) Math.round(secondValue / DIGITALGAIN_CONST_VALUE);

            String firstString = Integer.toBinaryString(firstValue);
            firstString = pad(firstString, 3);
            Log.e("firstString", "call" + firstString);

            String secondString = Integer.toBinaryString(finalSecondValue);
            secondString = pad(secondString, 5);
            Log.e("secondString", "call" + secondString);

            String finalString = firstString + secondString;

            Integer number = Integer.parseInt(finalString, 2);//binary to int
            Log.e("cccccccccccc", "call" + number);

            String number1 = Integer.toBinaryString(number);

            int digitalVal = Integer.parseInt(number1, 2);

            if (number1 != null) {
                Log.e("numbervlaue", "call" + digitalVal);
                commandString = commandString + digitalVal;
                commandString = commandString + END_TAG;
                Log.e("commandString", "call" + commandString);
                if (isConnected) {
                    SCTestAnalysis.getInstance().sendString(commandString);
                }
            }

        }


    }

    private String pad(String string, int toSize) {
        String padded = string;
        if (string.length() < toSize) {
            for (int t = 0; t < (toSize - string.length()); t++) {
                padded = "0" + padded;
            }
        }
        return padded;
    }

    public void prepareCommandForExitStrip() {
        //$MLS4000#
        String commandString = START_TAG;
        commandString = commandString + MOVE_STRIP_COUNTER_CLOCKWISE_TAG;

        commandString = commandString + String.valueOf("400");//
        commandString = commandString + END_TAG;
        // requestCommand = commandString
        Log.e("CommandForExitStrip", "call" + commandString);

        prepareCommandForUV(false);
    }
    public void  prepareCommandForUV(boolean isOn) {
        if(isOn){
            Log.e("prepareCommandForUV", "call" + UV_TURN_ON);
            SCTestAnalysis.getInstance().sendString(UV_TURN_ON);
        }else{
            SCTestAnalysis.getInstance().sendString(UV_TURN_OFF);
        }
    }
    private void prepareCommandForLED(boolean isOn) {
        if (isOn) {
            if (isConnected) {
                SCTestAnalysis.getInstance().sendString(LED_TURN_ON);
            }
        } else {
            if (isConnected) {
                SCTestAnalysis.getInstance().sendString(LED_TURN_OFF);
            }
        }

    }

    public void prepareCommandForROI(int ho, int hc, int vo, int vc) {
        //$ROIho,hc,vo,lc,#
        String commandString = START_TAG;
        commandString = commandString + ROI_TAG;
        commandString = commandString + String.valueOf(ho) + "," + String.valueOf(hc) + "," + String.valueOf(vo) + "," + String.valueOf(vc);
        commandString = commandString + END_TAG;
        // requestCommand = commandString
        Log.e("prepareCommandForROI", "call" + commandString);
        if (isConnected) {
            SCTestAnalysis.getInstance().sendString(commandString);
        }
    }

    public void disconnectWithPeripheral() {
        if (isConnected) {
            isConnected = false;
            SCTestAnalysis.getInstance().mService.disconnect();
        }
    }

    public void disconnect() {
        if (isConnected) {
            isConnected = false;
            SCTestAnalysis.getInstance().mService.close();
        }
    }

    public void prepareCommandForNoOfAverage(int count) {

        //$AFC40#
        String commandString = START_TAG;
        commandString = commandString + AVG_FRAME_TAG;
        commandString = commandString + String.valueOf(count);
        commandString = commandString + END_TAG;
        // requestCommand = commandString

        Log.e("CommandForNoOfAverage", "call" + commandString);
        if (isConnected) {
            SCTestAnalysis.getInstance().sendString(commandString);
        }

    }

    public void prepareCommandForExpousureCount(int count) {

        //$ELCxxxxx#
        String commandString = START_TAG;
        commandString = commandString + EXPOUSURE_TAG;
        commandString = commandString + String.valueOf(count);
        commandString = commandString + END_TAG;
        // requestCommand = commandString
        Log.e("prepaForExpousureCount", "call" + commandString);
        if (isConnected) {
            SCTestAnalysis.getInstance().sendString(commandString);
        }

    }

    public void sendExposureTime() {
        if (SCConnectionHelper.getInstance().isConnected) {
            if (SCTestAnalysis.getInstance().spectroDeviceObject.getImageSensor() != null) {
                ImageSensorStruct objSensor = SCTestAnalysis.getInstance().spectroDeviceObject.getImageSensor();
                int exposureTme = objSensor.getExposureTime();
                prepareCommandForExpousureCount(exposureTme);
            }
        } else {
            Toast.makeText(SpectroCareSDK.getInstance().context, "Device not connected !!!", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendAnanlogGain() {
        if (SCConnectionHelper.getInstance().isConnected) {
            if (SCTestAnalysis.getInstance().spectroDeviceObject.getImageSensor() != null) {
                ImageSensorStruct objSensor = SCTestAnalysis.getInstance().spectroDeviceObject.getImageSensor();
                String analogval = String.valueOf(objSensor.getAnalogGain());
                prepareCommandForAnalogGain(analogval + "X");
            }
        } else {
            Toast.makeText(SpectroCareSDK.getInstance().context, "Device not connected !!!", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendDigitalGain() {
        if (SCConnectionHelper.getInstance().isConnected) {
            if (SCTestAnalysis.getInstance().spectroDeviceObject.getImageSensor() != null) {
                ImageSensorStruct objSensor = SCTestAnalysis.getInstance().spectroDeviceObject.getImageSensor();
                String digitalGainString = String.valueOf(objSensor.getDigitalGain());
                prepareCommandForDigitalGain(Double.parseDouble(digitalGainString));
            }
        } else {
            Toast.makeText(SpectroCareSDK.getInstance().context, "Device not connected !!!", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendSpectrumAVG() {
        if (SCConnectionHelper.getInstance().isConnected) {
            if (SCTestAnalysis.getInstance().spectroDeviceObject.getImageSensor() != null) {
                ImageSensorStruct objSensor = SCTestAnalysis.getInstance().spectroDeviceObject.getImageSensor();
                int darkSpectrum = objSensor.getNoOfAverageForDarkSpectrum();
                prepareCommandForNoOfAverage(darkSpectrum);
            } else {
                Toast.makeText(SpectroCareSDK.getInstance().context, "Device not connected !!!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void prepareCommandForMotorMove(int steps, String direction) {
        String stepsInString=String.valueOf(steps);
        String commandForMotorControl = START_TAG;
        commandForMotorControl = commandForMotorControl + direction;
        if(stepsInString.length()==2){
            stepsInString="0"+stepsInString;
        }else if(stepsInString.length()==1){
            stepsInString="00"+stepsInString ;
        }
        commandForMotorControl = commandForMotorControl + stepsInString;
        commandForMotorControl = commandForMotorControl + END_TAG;
        SCTestAnalysis.getInstance().sendString(commandForMotorControl);
    }

    public void prepareCommandForMoveToPosition() {
        String commandForMotorControl = START_TAG;
        commandForMotorControl = commandForMotorControl + MOVE_STRIP_POSITION;
        // commandForMotorControl = commandForMotorControl + "1";
        commandForMotorControl = commandForMotorControl + END_TAG;
        //  print(commandForMotorControl
        Log.e("prepareCommandF", "call" + commandForMotorControl);
        SCTestAnalysis.getInstance().sendString(commandForMotorControl);
    }
    private String rawBuffer2Hex(byte[] buf) {
        String str = "";
        for (int i = 0; i < buf.length; i++) {
            //Log.e("obj","call"+buf[i]);
            String immedidateData = String.format("%02x", buf[i] & 0xff);
            if (immedidateData.length() == 1) {
                immedidateData = "0" + immedidateData;
            }
            str = str + immedidateData;
        }
        return str;
    }
    private void showMessage(String msg) {
        Toast.makeText(SpectroCareSDK.getInstance().context, msg, Toast.LENGTH_SHORT).show();
    }
    public void initializeAdapterAndServcie() {
        initilizeSevice();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //  scanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    public void activateScanNotification(ScanDeviceInterface scanDeviceInterface1) {
       if (scanDeviceInterface != null) {
           scanDeviceInterface=null;
        }
        scanDeviceInterface=scanDeviceInterface1;
    }

    //In this interface, you can define messages, which will be send to owner.
    public interface ScanDeviceInterface {
        //In this case we have two messages,
        //the first that is sent when the process is successful.
        void onSuccessForConnection(String msg);

        void onSuccessForScanning(ArrayList<BluetoothDevice> deviceArray,boolean msg);

        //And The second message, when the process will fail.
        void onFailureForConnection(String error);

        void uartServiceClose(String error);
    }

}





