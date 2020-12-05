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
import java.util.Collections;
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
    public enum ScanState { NONE, LESCAN, DISCOVERY, DISCOVERY_FINISHED }
    private ScanState scanState = ScanState.NONE;
    private static final long  LESCAN_PERIOD = 10000;
    private Handler leScanStopHandler = new Handler();

    public static SCConnectionHelper getInstance() {
        if (myObj == null) {
            myObj = new SCConnectionHelper();
        }
        return myObj;
    }

    public ScanState getScanState(){
        return  scanState;
    }

    public void setScanState(ScanState scanState) {
        this.scanState =  scanState ;
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
            mScanning = true;
            leScanStopHandler.postDelayed(this::stopScan, LESCAN_PERIOD);
            this.mBluetoothAdapter.startLeScan(this.mLeScanCallback);
        } else {
            stopScan();
        }
    }
    private void didDevicesNotFound() {
        scanDeviceInterface.onSuccessForScanning(deviceList,false);
    }
    public void stopScan() {
        if(scanState == ScanState.NONE)
            return;
        switch(scanState) {
            case LESCAN:
                //   leScanStopHandler.removeCallbacks(this::stopScan);
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
                mHandler.removeCallbacksAndMessages(null);
                break;
            case DISCOVERY:
                mScanning = false;
                mBluetoothAdapter.cancelDiscovery();
                mHandler.removeCallbacksAndMessages(null);
                break;
            default:
                // already canceled
        }
        scanState = ScanState.NONE;

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device.getAddress() != null && device.getName()!=null) {
                addDevice(device, rssi);
            }
        }
    };


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(scanDeviceInterface!=null) {
                    scanDeviceInterface.onBLEStatusChange(state);
                }
            }
        }
    };


    private void addDevice(BluetoothDevice device, int rssi) {
        Log.e("scandevice","call"+device.getAddress()+device.getName()+device);
        boolean deviceFound = false;

        if(deviceList.indexOf(device) < 0) {
            deviceList.add(device);
            Collections.sort(deviceList, SCConnectionHelper::compareTo);
            if (scanDeviceInterface != null) {
                scanDeviceInterface.onSuccessForScanning(deviceList, true);
            }
        }
    }

    static int compareTo(BluetoothDevice a, BluetoothDevice b) {
        boolean aValid = a.getName()!=null && !a.getName().isEmpty();
        boolean bValid = b.getName()!=null && !b.getName().isEmpty();
        if(aValid && bValid) {
            int ret = a.getName().compareTo(b.getName());
            if (ret != 0) return ret;
            return a.getAddress().compareTo(b.getAddress());
        }
        if(aValid) return -1;
        if(bValid) return +1;
        return a.getAddress().compareTo(b.getAddress());
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
        // initilizeSevice();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        SpectroCareSDK.getInstance().context.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
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

        void onBLEStatusChange(int state);

    }

}

