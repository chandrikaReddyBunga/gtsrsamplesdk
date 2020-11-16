package com.spectrochips.spectrumsdk.FRAMEWORK;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.icu.text.DecimalFormat;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands;
import com.spectrochips.spectrumsdk.DeviceConnectionModule.DataPoint;
import com.spectrochips.spectrumsdk.DeviceConnectionModule.PolynomialRegression;
import com.spectrochips.spectrumsdk.MODELS.ConcentrationControl;
import com.spectrochips.spectrumsdk.MODELS.ImageSensorStruct;
import com.spectrochips.spectrumsdk.MODELS.IntensityChart;
import com.spectrochips.spectrumsdk.MODELS.LimetLineRanges;
import com.spectrochips.spectrumsdk.MODELS.RCTableData;
import com.spectrochips.spectrumsdk.MODELS.ReflectanceChart;
import com.spectrochips.spectrumsdk.MODELS.SpectorDeviceDataStruct;
import com.spectrochips.spectrumsdk.MODELS.SpectroDeviceDataController;
import com.spectrochips.spectrumsdk.MODELS.Steps;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.INTESITY_VALUES_TAG;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.LED_TURN_OFF;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.LED_TURN_ON;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.MOVE_STRIP_CLOCKWISE_TAG;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.MOVE_STRIP_COUNTER_CLOCKWISE_TAG;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.UV_TURN_OFF;
import static com.spectrochips.spectrumsdk.DeviceConnectionModule.Commands.UV_TURN_ON;

public class SCTestAnalysis extends Activity {
    private static SCTestAnalysis ourInstance;
    //public Context context;
    public ArrayList<Steps> motorSteps;
    private String darkSpectrumTitle = "Dark Spectrum";
    private String standardWhiteTitle = "Standard White (Reference)";
    public SpectorDeviceDataStruct spectroDeviceObject;
    private ArrayList<IntensityChart> intensityChartsArray = new ArrayList<>();
    private ArrayList<Float> pixelXAxis = new ArrayList<>();
    private ArrayList<Float> wavelengthXAxis = new ArrayList<>();
    private ArrayList<ReflectanceChart> reflectenceChartsArray = new ArrayList<>();
    private ArrayList<ConcentrationControl> concentrationArray = new ArrayList<>();
    private ArrayList<Float> darkSpectrumIntensityArray = new ArrayList<>();
    private ArrayList<Float> standardWhiteIntensityArray = new ArrayList<>();
    private int stripNumber = 0;
    private boolean isForDarkSpectrum = false;
    private boolean isForSync = false;
    private int commandNumber = 0;
    public String requestCommand = "";

    private ByteArrayOutputStream outputStream;
    private ArrayList<Float> intensityArray;
    private ArrayList<String> hexaDecimalArray;// For Hexadecimal values , Does't need in real time. It's for testing purpose only.
    private byte[] socketresponseData;
    private String receivedDataString = "";
    public UartService mService = null;
    private int cal_c = 0;
    private ArrayList<TestFactors> testItems = new ArrayList<>();
    private TeststaResultInterface testAnalysisListener;
    private SyncingInterface syncingInterface;
    public JsonFileInterface jsonFileInterface;
    private AbortInterface abortInterface;
    private boolean isEjectType = false;
    private boolean isInterrupted = false;
    public   ArrayList<TestFactors> testResults = new ArrayList<>();

    // public String  receivedDataStringString = "";

    public static SCTestAnalysis getInstance() {
        if (ourInstance == null) {
            ourInstance = new SCTestAnalysis();
            ourInstance.outputStream = new ByteArrayOutputStream();
            ourInstance.hexaDecimalArray = new ArrayList<>();
        }
        return ourInstance;
    }

    public void fillContext(Context context1) {
        // context = context1;
        ourInstance.intensityArray = new ArrayList<>();
        spectroDeviceObject = new SpectorDeviceDataStruct();
        loadDefaultSpectrodeviceObject("VEDA_UrineTest.json");
        loadInterface();
    }
    public void loadInterface(){
        syncDeviceData(new TestDataInterface() {
            @Override
            public void gettingData(byte[] data) {
                final byte[] txValue = data;
                String text = decodeUTF8(txValue);
                socketDidReceiveMessage(text, requestCommand);
            }
            @Override
            public void testComplete(ArrayList<TestFactors> results, String msg) {
                testResults=results;
                Log.e("testCompleteReceived", "call" + testResults.size());
                if(testAnalysisListener !=null){
                    testAnalysisListener.onSuccessForTestComplete(testResults,"Test Complete");
                }
            }
        });
    }
    public TestDataInterface testDataInterface;

    public void syncDeviceData(TestDataInterface testDataInterface1) {
        this.testDataInterface = testDataInterface1;
    }

    public interface TestDataInterface {
        void gettingData(byte[] data);
        void testComplete(ArrayList<TestFactors> results, String msg);
    }

    public boolean canDo() {
        if (SpectroCareSDK.getInstance().isSDKAccess) {
            return SCConnectionHelper.getInstance().isConnected;
        } else {
            Log.e("sdkaccess", "call" + SpectroCareSDK.getInstance().sdkAccessMessage);
            return false;
        }
    }

    private void loadDefaultSpectrodeviceObject(String fileName) {
        SpectroDeviceDataController.getInstance().loadJsonFromUrl(fileName);
        if (SpectroDeviceDataController.getInstance().spectroDeviceObject != null) {

            spectroDeviceObject = SpectroDeviceDataController.getInstance().spectroDeviceObject;
            motorSteps = spectroDeviceObject.getStripControl().getSteps();
            for (int i = 0; i < motorSteps.size(); i++) {
                Log.e("motorStepstestname", "call" + motorSteps.get(i).getTestName() + motorSteps.get(i).getStripIndex());
            }

        }
    }

    public void activatenotifications(JsonFileInterface jsonFileInterface1) {
        this.jsonFileInterface = jsonFileInterface1;
    }

    public void getDeviceSettings(String testName, String category, String date, JsonFileInterface jsonFileInterface1) {
        this.jsonFileInterface = jsonFileInterface1;
        SpectroDeviceDataController.getInstance().loadJsonFromUrl(testName);
        if (SpectroDeviceDataController.getInstance().spectroDeviceObject != null) {
            spectroDeviceObject = SpectroDeviceDataController.getInstance().spectroDeviceObject;
            motorSteps = spectroDeviceObject.getStripControl().getSteps();

        }
    }

    public void startTestAnalysis(TeststaResultInterface teststaResultInterface1) {
        this.testAnalysisListener = teststaResultInterface1;
        // startTesting();
       /* stripNumber = 0;
        SCConnectionHelper.getInstance().prepareCommandForMoveToPosition();*/
    }
    public void startTesting(){
        stripNumber = 0;
        SCConnectionHelper.getInstance().prepareCommandForMoveToPosition();
    }

    private void getDarkSpectrum() {
        clearPreviousTestResulsArray();
        loadPixelArray();
        reprocessWavelength();
        prepareChartsDataForIntensity();
        isForDarkSpectrum = true;
        getIntensity();
    }

    int count = 0;

    private void getIntensity() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                requestCommand = Commands.INTESITY_VALUES_TAG;
                sendString(requestCommand);
            }
        }, 1000);
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                requestCommand = Commands.INTESITY_VALUES_TAG;
                sendString(requestCommand);
            }
        }, 1000 * 1);*/
    }

    private void loadPixelArray() {
        pixelXAxis = new ArrayList<>();
        pixelXAxis.clear();
        int roiArray[] = spectroDeviceObject.getImageSensor().getROI();
        int pixelCount = roiArray[1];

        for (int i = 1; i <= pixelCount; i++) {
            pixelXAxis.add(Float.valueOf(i));
        }

    }

    private void reprocessWavelength() {
        //reprocess wavelength calculation
        for (int i = 0; i < pixelXAxis.size(); i++) {
            wavelengthXAxis.add(pixelXAxis.get(i));
        }
        wavelengthXAxis.clear();
        if (spectroDeviceObject.getWavelengthCalibration() != null) {
            double[] resultArray = spectroDeviceObject.getWavelengthCalibration().getCoefficients();
            DataPoint theData[] = new DataPoint[0];
            PolynomialRegression poly = new PolynomialRegression(theData, spectroDeviceObject.getWavelengthCalibration().getNoOfCoefficients());
            poly.fillMatrix();
            DecimalFormat df = new DecimalFormat("#.##");
            for (int index = 0; index < pixelXAxis.size(); index++) {
                wavelengthXAxis.add(Float.valueOf(df.format(poly.predictY(resultArray, pixelXAxis.get(index)) * 100 / 100)));
            }
        }
    }

    private void prepareChartsDataForIntensity() {
        intensityArray.clear();
        intensityChartsArray.clear();
        if (spectroDeviceObject.getRCTable() != null) {
            for (RCTableData objRc : spectroDeviceObject.getRCTable()) {
                IntensityChart objIntensity = new IntensityChart();
                objIntensity.setTestName(objRc.getTestItem());
                objIntensity.setPixelMode(true);
                objIntensity.setOriginalMode(true);
                objIntensity.setAutoMode(true);
                objIntensity.setxAxisArray(pixelXAxis);
                objIntensity.setyAxisArray(null);
                objIntensity.setSubstratedArray(null);
                objIntensity.setWavelengthArray(wavelengthXAxis);
                objIntensity.setCriticalWavelength(objRc.getCriticalwavelength());
                intensityChartsArray.add(objIntensity);

            }
        }
        // If needed to show dark spectrum and Standard White spectrum Then use below methods

        IntensityChart objSWIntensity = new IntensityChart();
        objSWIntensity.setTestName(standardWhiteTitle);
        objSWIntensity.setPixelMode(true);
        objSWIntensity.setOriginalMode(true);
        objSWIntensity.setAutoMode(true);
        objSWIntensity.setxAxisArray(pixelXAxis);
        objSWIntensity.setyAxisArray(null);
        objSWIntensity.setSubstratedArray(null);
        objSWIntensity.setWavelengthArray(wavelengthXAxis);
        objSWIntensity.setCriticalWavelength(0.0);
        intensityChartsArray.add(objSWIntensity);

        IntensityChart objDarkIntensity = new IntensityChart();
        objDarkIntensity.setTestName(darkSpectrumTitle);
        objDarkIntensity.setPixelMode(true);
        objDarkIntensity.setOriginalMode(true);
        objDarkIntensity.setAutoMode(true);
        objDarkIntensity.setxAxisArray(pixelXAxis);
        objDarkIntensity.setyAxisArray(null);
        objDarkIntensity.setSubstratedArray(null);
        objDarkIntensity.setWavelengthArray(wavelengthXAxis);
        objDarkIntensity.setCriticalWavelength(0.0);
        intensityChartsArray.add(objDarkIntensity);

    }

    public void clearPreviousTestResulsArray() {
        intensityChartsArray.clear();
        reflectenceChartsArray.clear();
        concentrationArray.clear();
        stripNumber = 0;
        isForDarkSpectrum = false;
        isForSync = false;
    }

    public void initializeService() {
        Log.d("initializeService", "Connect request result=");
        Intent gattServiceIntent = new Intent(SpectroCareSDK.getInstance().context, UartService.class);
        SpectroCareSDK.getInstance().context.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(SpectroCareSDK.getInstance().context).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (this.mService != null) {
        }
    }

    public void removereceiver() {
        LocalBroadcastManager.getInstance(SpectroCareSDK.getInstance().context).unregisterReceiver(mGattUpdateReceiver);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mService = ((UartService.LocalBinder) service).getService();
            if (!mService.initialize()) {
                Log.e("not", "Unable to initialize Bluetooth");
                //finish();
            }

            //  mService.connect();
        }

        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                // showMessage("Device Connected.");
            }

            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                //showMessage("Device Disconnected.");
            }
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {

            }
            if (action.equals(UartService.ACTION_DATA_AVAILABLE_DATA)) {

            }
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                SCConnectionHelper.getInstance().disconnectWithPeripheral();
            }
        }
    };

    private Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private String decodeUTF8(byte[] bytes) {
        return new String(bytes, UTF8_CHARSET);
    }

    private byte[] encodeUTF8(String string) {
        return string.getBytes(UTF8_CHARSET);
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    public void socketDidReceiveMessage(String response, String request) {
        Log.e("datacount", "call" + response + "request" + request);
        if(testAnalysisListener !=null){
            testAnalysisListener.getRequestAndResponse(response);
        }
        receivedDataString = receivedDataString + response;

        if (receivedDataString.toUpperCase().startsWith("^ERR#") || receivedDataString.toUpperCase().startsWith("$ERR#")) {
                       Log.e("^ERR#DataRecieved", "call");
            dataRecieved(receivedDataString, request);
            receivedDataString = "";
        }

        if (receivedDataString.toUpperCase().startsWith("$OK#") || receivedDataString.toUpperCase().startsWith("$OK!") || receivedDataString.toUpperCase().startsWith("^OK#")) {
            Log.e("$OK# Data Recieved", "call");
            dataRecieved(receivedDataString, request);
            receivedDataString = "";

        }
        // 2423 - $# - invalid data
        if (receivedDataString.toUpperCase().startsWith("$#")) {
            receivedDataString = "";
        }

        // Position Sensor success response  ^POS# or $POS#
        if (receivedDataString.toUpperCase().startsWith("^POS#") || receivedDataString.toUpperCase().startsWith("$POS#")) {
            dataRecieved(receivedDataString, request);
            receivedDataString = "";
        }

        // Motor move stop Sensor. ^STP#
        if (receivedDataString.toUpperCase().startsWith("^STP#") || receivedDataString.toUpperCase().startsWith("$STP#")) {
            dataRecieved(receivedDataString, request);
            receivedDataString = "";
        }

        //notify graph processing only when we got complete data 5e3235363023
        if (receivedDataString.toUpperCase().startsWith("^288$") && receivedDataString.toUpperCase().endsWith("^EOF#")) {
            Log.e("receivedDataStringcall", "call" + receivedDataString);
            intensityDataRecieved(receivedDataString, request);
            receivedDataString = "";
        }
    }

    //intenisityDataRecieved
    private void intensityDataRecieved(String responseData, String request) {
        if (processIntensityValues(responseData)) {
            if (!isForDarkSpectrum) {
                performMotorStepsFunction();
            } else {
                isForDarkSpectrum = false;
                syncingInterface.isSyncingCompleted(true);
            }
        } else {
            requestCommand = "";
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    getIntensity();
                }
            }, 1000);

           /* new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getIntensity();
                }
            }, 1000 * 1);*/
        }
    }

    private void dataRecieved(String responseData, String request) {
        processResponseData(request, responseData);
    }

    private void processResponseData(String command, String response) {
        //String decodeStr = decodeUTF8(byteArray);
        if (response.contains("OK")) {
            // Log.e("abort2","call");
            if (isForSync) {
                // Log.e("abort3","call");
                if (isInterrupted) {
                    Log.e("abort4", "call");
                    isInterrupted = false;
                    if (abortInterface != null) {
                        abortInterface.onAbortForTesting(true);
                        abortInterface = null;
                        clearCache();
                    }
                    syncDone();
                    return;
                }
                switch (commandNumber) {
                    case 1:
                        SCConnectionHelper.getInstance().sendExposureTime();
                        break;
                    case 2:
                        SCConnectionHelper.getInstance().sendAnanlogGain();
                        break;
                    case 3:
                        SCConnectionHelper.getInstance().sendDigitalGain();
                        break;
                    case 4:
                        SCConnectionHelper.getInstance().sendSpectrumAVG();
                        break;
                    case 5:
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                syncingInterface.isSyncingCompleted(true);
                                getDarkSpectrum();
                                syncDone();
                            }
                        }, 1000);
                       /* new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                syncingInterface.isSyncingCompleted(true);
                                getDarkSpectrum();
                                syncDone();
                            }
                        }, 1000 * 1);*/
                        break;
                    default:
                        syncingInterface.isSyncingCompleted(false);
                        syncDone();
                        break;
                }
                commandNumber = commandNumber + 1;
            } else {
                Log.e("abort5", "call");
                if(isInterrupted){ //for abort from my own logic.
                   // Log.e("abort234","call");
                    if (abortInterface != null) {
                        abortInterface.onAbortForTesting(true);
                        abortInterface = null;
                        clearCache();
                    }
                }
                if (command.equals(UV_TURN_ON)) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (!isInterrupted) {
                                stripNumber = 1;//1;
                                getIntensity();
                            }
                        }
                    }, 1000);
                   /* new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isInterrupted) {
                                stripNumber = 0;//1;
                                getIntensity();
                            }
                        }
                    }, 1000 * 1);*/
                }
            }
        } else if (response.contains("POS")) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    ledControl(true);
                }
            }, 1000);
            /*new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ledControl(true);
                }
            }, 1000 * 1);*/
        } else if (response.contains("STP")) {
            Log.e("abort1", "call");
            if (isInterrupted) {
                if (isEjectType) {// Call when eject completed. .
                    isEjectType = false;
                    isInterrupted = false;
                    if (abortInterface != null) {
                        abortInterface.onAbortForTesting(true);
                    }
                }
            } else {
                Log.e("abort7", "call");
                if (stripNumber != motorSteps.size() - 1) {
                    int dwellTime = motorSteps.get(stripNumber).getDwellTimeInSec();
                    Log.e("Waited DwellTime:", "" + dwellTime);
                    Log.e("Strip Number:", "" + stripNumber);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            requestCommand = "";
                            stripNumber += 1;
                            getIntensity();
                        }
                    }, 1000);
                    /*Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            requestCommand = "";
                            stripNumber += 1;
                            getIntensity();
                        }
                    }, dwellTime * 1000);*/
                } else {
                    Log.e("abort9", "call");
                    stripNumber = 0;
                    ledControl(false);
                }
            }
        } else if (response.contains("ERR")) {
            if (isInterrupted) {
                Log.e("abort12", "call");
                if (isEjectType) { // Call when eject used.
                    isEjectType = false;
                    isInterrupted = false;
                    abortInterface.onAbortForTesting(true);
                }
            } else {
                //  Log.e("abort13","call");
                if (isForSync) {
                    isForSync = false;
                    clearCache();
                    syncingInterface.isSyncingCompleted(false);
                } else {
                    Log.e("strinpnotdetected", "call");
                    if (testAnalysisListener != null) {
                        testAnalysisListener.onFailureForTesting("Strip is not detected");
                        clearPreviousTestResulsArray();
                    }
                }
            }
        }
    }

    public void unRegisterReceiver() {
        isForSync = false;
        if (isInterrupted && isEjectType) {
            mService.disconnect();
        }
        isInterrupted = false;
        isEjectType = false;
        clearCache();
        testAnalysisListener = null;
        clearPreviousTestResulsArray();
        LocalBroadcastManager.getInstance(SpectroCareSDK.getInstance().context).unregisterReceiver(mGattUpdateReceiver);
    }


    private String rawBuffer2Hex(byte[] buf) {
        String str = "";

        for (int i = 0; i < buf.length; i++) {
            Log.e("obj", "call" + buf[i]);
            String immedidateData = String.format("%02x", buf[i] & 0xff);

            if (immedidateData.length() == 1) {
                immedidateData = "0" + immedidateData;
            }
            str = str + immedidateData;
        }
        return str;
    }

    public void clearCache() {
        receivedDataString = "";
        socketresponseData = null;
    }

    private void syncDone() {
        Log.e("syncDone", "call");
        commandNumber = 0;
        isForSync = false;
        // requestCommand = "";
        clearCache();
    }

    public void sendString(final String message) {
        Log.e("SendString", "Sent: " + message);

        clearCache();
        requestCommand = message;
        if(testAnalysisListener !=null){
            testAnalysisListener.getRequestAndResponse("----------Send Command--------------");
        }
        if(testAnalysisListener !=null){
            testAnalysisListener.getRequestAndResponse(message);
        }
        byte[] value;
        try {
            //send data to service
            value = message.getBytes("UTF-8");
            mService.writeRXCharacteristic(message);//(packet);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private boolean processIntensityValues(String response/*byte[] data*/) {
        hexaDecimalArray = new ArrayList<>();
        intensityArray = new ArrayList<>();
        ArrayList<String> intensityArray1 = new ArrayList<>();

        String[] intensity = null;
        intensity = response.split(",");
        ArrayList<String> stringList = new ArrayList<String>(Arrays.asList(intensity)); //new ArrayList is only needed if you absolutely need an ArrayList
        stringList.remove(0);
        stringList.remove(stringList.size() - 1);
        intensityArray1 = stringList;

        if (intensityArray1.size() != pixelXAxis.size()) {
            Log.e("intensityDatamismatched", "call" + pixelXAxis.size());
            if(testAnalysisListener !=null){
                testAnalysisListener.getRequestAndResponse("intensity Data Mismatched");
            }
            return false;
        }
        intensityArray.clear();

        intensityArray = new ArrayList<>(intensityArray1.size());
        for (int i = 0; i < intensityArray1.size(); i++) {
            Float number = Float.valueOf(intensityArray1.get(i));
            intensityArray.add(number);
        }
        if (isForDarkSpectrum) {
            darkSpectrumIntensityArray = intensityArray;
            if (getPositionForTilte(darkSpectrumTitle) != -1) {
                int position = getPositionForTilte(darkSpectrumTitle);
                IntensityChart object = intensityChartsArray.get(position);
                object.setyAxisArray(darkSpectrumIntensityArray);
                intensityChartsArray.set(position, object);
                Log.e("intensityArrayfordark", "call" + object.getyAxisArray().toString());
                if (syncingInterface != null) {
                    syncingInterface.gettingDarkSpectrum(true);
                    syncDone();
                }
            }
        } else {
            Log.e("otherThandarkArray", "call" + intensityArray.toString());
            setIntensityArrayForTestItem();
            if (stripNumber == motorSteps.size() - 1) {  // Before Eject command , Process the Testing completed command.
                testCompleted();
            }
        }

        return true;
    }

    private void setIntensityArrayForTestItem() {
        Steps currentObject = motorSteps.get(stripNumber-1);
        /*if(testAnalysisListener !=null){
            testAnalysisListener.getRequestAndResponse("Intensity for "+currentObject.getTestName());
        }*/
        if (currentObject.getStandardWhiteIndex() == 0) {
            for (int i = 0; i < intensityChartsArray.size(); i++) {
                IntensityChart object = intensityChartsArray.get(i);
                if (object.getTestName().equals(currentObject.getTestName())) {
                    object.setyAxisArray(intensityArray);
                    object.setSubstratedArray(getSubstratedArray(intensityArray, darkSpectrumIntensityArray));
                    intensityChartsArray.set(i, object);
                }
            }
        } else {
            standardWhiteIntensityArray = intensityArray;
            int position = getPositionForTilte(standardWhiteTitle);
            IntensityChart object = intensityChartsArray.get(position);
            object.setyAxisArray(standardWhiteIntensityArray);
            object.setSubstratedArray(getSubstratedArray(standardWhiteIntensityArray, darkSpectrumIntensityArray));
            intensityChartsArray.set(position, object);
        }

        /*if(testAnalysisListener !=null){
            testAnalysisListener.getRequestAndResponse(intensityArray.toString());
        }*/
        if (stripNumber == motorSteps.size() - 1) {
            Log.e("testingended", "call");
            // Testing ended.
        }

    }

    private ArrayList<Float> getSubstratedArray(ArrayList<Float> spectrumIntensityArray, ArrayList<Float> darkSpectrumIntensityArray) {
        ArrayList<Float> substratedArray = new ArrayList<>();
        for (int i = 0; i < spectrumIntensityArray.size(); i++) {
            substratedArray.add(spectrumIntensityArray.get(i) - darkSpectrumIntensityArray.get(i));
        }
        Log.e("substratedArray", "call" + substratedArray.toString());
        return substratedArray;
    }

    private int getPositionForTilte(String title) {
        for (int i = 0; i < intensityChartsArray.size(); i++) {
            IntensityChart object = intensityChartsArray.get(i);
            if (object.getTestName().equals(title)) {
                return i;
            }
        }
        return -1;
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

    private float hex2decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16 * val + d;
        }
        return (float) val;
    }

    public void showMessage(String msg) {
        Toast.makeText(SpectroCareSDK.getInstance().context, msg, Toast.LENGTH_SHORT).show();
    }

    private void processRCConversion() {
        reflectenceChartsArray.clear();
        if (getStandardwhiteSubstrateArray() != null) {
            ArrayList<Float> swSubstratedArray = getStandardwhiteSubstrateArray();
            for (IntensityChart objIntensitychartObject : intensityChartsArray) {
                if (!objIntensitychartObject.getTestName().equals(standardWhiteTitle) && !objIntensitychartObject.getTestName().equals(darkSpectrumTitle)) {
                    ArrayList<Float> originalArray = getOriginalDivReference(objIntensitychartObject.getSubstratedArray(), swSubstratedArray);
                    double interpolationValue = getClosestValue(objIntensitychartObject.getWavelengthArray(), originalArray, objIntensitychartObject.getCriticalWavelength());
                    ReflectanceChart objReflectanceChart = new ReflectanceChart();
                    objReflectanceChart.setTestName(objIntensitychartObject.getTestName());
                    objReflectanceChart.setxAxisArray(wavelengthXAxis);
                    objReflectanceChart.setyAxisArray(originalArray);
                    objReflectanceChart.setCriticalWavelength(objIntensitychartObject.getCriticalWavelength());
                    objReflectanceChart.setAutoMode(true);
                    objReflectanceChart.setInterpolationValue(interpolationValue);
                    reflectenceChartsArray.add(objReflectanceChart);
                }
            }
        } else {
            Log.e("No SW available", "call");
        }
        processFinalTestResults();
    }

    private ArrayList<Float> getOriginalDivReference(ArrayList<Float> originalArray, ArrayList<Float> referenceArray) {
        ArrayList<Float> divisionArray = new ArrayList<>();
        for (int i = 0; i < originalArray.size(); i++) {
            if (referenceArray.get(i) != 0) {
                divisionArray.add(originalArray.get(i) / referenceArray.get(i));
            } else {
                divisionArray.add(0.0f);
            }
        }
        return divisionArray;
    }

    private ArrayList<Float> getStandardwhiteSubstrateArray() {

        for (IntensityChart objIntensitychartObject : intensityChartsArray) {
            if (objIntensitychartObject.getTestName().equals(standardWhiteTitle)) {
                return objIntensitychartObject.getSubstratedArray();
            }
        }
        return null;
    }

    private void processFinalTestResults() {
        concentrationArray.clear();
        int index = 1;
        for (ReflectanceChart objReflectance : reflectenceChartsArray) {
            if (getRCObjectFortestName(objReflectance.getTestName()) != null) {
                RCTableData rcTableObject = getRCObjectFortestName(objReflectance.getTestName());
                Log.e("tcunitvalue", "call" + rcTableObject.getUnit());
                ArrayList<Float> rArray = new ArrayList<>();
                ArrayList<Float> cArray = new ArrayList<>();

                for (double d : rcTableObject.getR()) {
                    rArray.add((float) d);
                }

                for (double d : rcTableObject.getC()) {
                    cArray.add((float) d);
                }
                double finalC = getClosestValue(rArray, cArray, objReflectance.getInterpolationValue());
                ConcentrationControl objConcetration = new ConcentrationControl();
                objConcetration.setSNo(String.valueOf(index));
                objConcetration.setConcentration(String.valueOf(finalC));
                objConcetration.setUnits(rcTableObject.getUnit());
                objConcetration.setTestItem(rcTableObject.getTestItem());
                objConcetration.setReferenceRange(rcTableObject.getReferenceRange());
                concentrationArray.add(objConcetration);
                index += 1;
            }
        }
        Log.e("concentrationArray", "call" + concentrationArray.size());
    }

    private ArrayList<Float> sortXValuesArray(ArrayList<Float> xValues, final double criticalWavelength) {
        Collections.sort(xValues, new Comparator<Float>() {
            @Override
            public int compare(Float s1, Float s2) {
                return Double.valueOf(Math.abs(criticalWavelength - s1)).compareTo(Double.valueOf(Math.abs(criticalWavelength - s2)));
            }
        });
        return xValues;
    }

    private double getClosestValue(final ArrayList<Float> xValues, ArrayList<Float> yValues, final double criticalWavelength) {

        // Sorting array based on Difference
        ArrayList<Float> beforeXvalues = new ArrayList<>(xValues);

        ArrayList<Float> sortedArrayBasedOnDifference = sortXValuesArray(xValues, criticalWavelength);
        float firstXValue = sortedArrayBasedOnDifference.get(0);
        float secondXValue = sortedArrayBasedOnDifference.get(1);

        float firstYValue = yValues.get(beforeXvalues.indexOf(firstXValue));
        float secondYValue = yValues.get(beforeXvalues.indexOf(secondXValue));


        float x1 = firstXValue;
        float x2 = secondXValue;
        float y1 = firstYValue;
        float y2 = secondYValue;
        Log.e("finalY", "call" + x1 + x2 + y1 + y2);

        if (x1 > x2) {
            x1 = secondXValue;
            x2 = firstXValue;
            y1 = secondYValue;
            y2 = firstYValue;
        }

        double finalY = y1 + ((criticalWavelength - x1) * (y2 - y1)) / (x2 - x1);
        Log.e("finalY", "call" + finalY);
        return finalY;
    }

    private RCTableData getRCObjectFortestName(String testName) {

        if (spectroDeviceObject.getRCTable() != null) {
            ArrayList<RCTableData> rcTable = spectroDeviceObject.getRCTable();
            for (RCTableData objRCTable : rcTable) {
                if (objRCTable.getTestItem().equals(testName)) {
                    return objRCTable;
                }
            }
        }
        return null;
    }

    public void syncSettingsWithDevice(SyncingInterface syncingInterface1) {
        this.syncingInterface = syncingInterface1;
        isForSync = false;
        isForDarkSpectrum = true;
        Log.e("getstatus", "call" + SCConnectionHelper.getInstance().isConnected);
        if (SCConnectionHelper.getInstance().isConnected) {
            getDarkSpectrum();
            /*if (!isForSync) {
                isForSync = true;
                commandNumber = 1;
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendCommandForROIParams();
                    }
                }, 3000 * 1);
            }*/
        }

    }

    private void sendCommandForROIParams() {
        if (spectroDeviceObject.getImageSensor() != null) {
            ImageSensorStruct objSensor = spectroDeviceObject.getImageSensor();
            int[] ROIvaluesArray = objSensor.getROI();
            SCConnectionHelper.getInstance().prepareCommandForROI(ROIvaluesArray[0], ROIvaluesArray[1], ROIvaluesArray[2], ROIvaluesArray[3]);
        }
    }

    private void performMotorStepsFunction() {
        Log.e("stripnumberandsize", "call" + stripNumber + "cal" + (intensityChartsArray.size()));
        if (stripNumber == intensityChartsArray.size() - 1) {  // Last Step is Eject . if match then turn of LED and execute last step with 1 sec delay.
            ledControl(false);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    motorStepsControl(motorSteps.get(stripNumber));
                }
            }, 1000);
           /* Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    motorStepsControl(motorSteps.get(stripNumber));
                }
            }, 1000 * 1);*/
        } else {
            motorStepsControl(motorSteps.get(stripNumber));
        }
    }

    private void motorStepsControl(Steps motorObject) {
        String direction = MOVE_STRIP_COUNTER_CLOCKWISE_TAG;

        if (motorObject.getDirection().equals("CW")) {
            direction = MOVE_STRIP_CLOCKWISE_TAG;
        }
        SCConnectionHelper.getInstance().prepareCommandForMotorMove(motorObject.getNoOfSteps(), direction);
    }

    private void ledControl(boolean isOn) {
        SCConnectionHelper.getInstance().prepareCommandForUV(isOn);
        /*String ledCommandString;
        ledCommandString = LED_TURN_OFF;
        if (isOn) {
            ledCommandString = LED_TURN_ON;
        }
        requestCommand = ledCommandString;
        Log.e("ledControl", "call" + requestCommand);

        if (SCConnectionHelper.getInstance().isConnected) {
            sendString(requestCommand);
        }*/
    }

    private void testCompleted() {
        clearCache();
        processRCConversion();
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");

        testItems.clear();
        int sno = 0;

        for (int index = 0; index < concentrationArray.size(); index++) {
            ConcentrationControl object = concentrationArray.get(index);
            String testName = object.getTestItem();
            String unit = " " + object.getUnits();
            String objValue = object.getConcentration();
            String objHealthReferenceRange = object.getReferenceRange();
            boolean flag = false;
            String resultText = " ";
            double finalValue = Double.parseDouble(objValue);
            flag = getFlagForTestItemWithValue(testName, finalValue);
            resultText = " " + getResultTextForTestItemwithValue(testName, finalValue);
            String testValue = getNumberFormatStringforTestNameWithValue(testName, finalValue);

            sno = index + 1;

            TestFactors objTest = new TestFactors();
            objTest.setFlag(flag);
            objTest.setUnits(unit);
            objTest.setReferenceRange(objHealthReferenceRange);
            objTest.setTestname(testName);
            objTest.setSNo(String.valueOf(sno));
            objTest.setResult(resultText);
            objTest.setValue(testValue);

            testItems.add(objTest);

        }
        if(testDataInterface !=null){
            testDataInterface.testComplete(testItems,"test");
        }
    }


    private String getNumberFormatStringforTestNameWithValue(String testName, double value) {
        String formattedString = String.valueOf(value);

        if (spectroDeviceObject.getRCTable() != null) {
            for (RCTableData objRCTable : spectroDeviceObject.getRCTable()) {
                if (objRCTable.getTestItem().equals(testName)) {
                    if (objRCTable.getNumberFormat().equals("X")) {
                        formattedString = String.format("%.0f", value);
                    } else if (objRCTable.getNumberFormat().equals("X.X")) {
                        formattedString = String.format("%.1f", value);
                    } else if (objRCTable.getNumberFormat().equals("X.XX")) {
                        formattedString = String.format("%.2f", value);
                    } else if (objRCTable.getNumberFormat().equals("X.XXX")) {
                        formattedString = String.format("%.3f", value);
                    } else if (objRCTable.getNumberFormat().equals("X.XXXX")) {
                        formattedString = String.format("%.4f", value);
                    }
                }
            }
        }
        return formattedString;
    }

    public boolean getFlagForTestItemWithValue(String testName, double value) {
        boolean isOk = false;
        if (spectroDeviceObject.getRCTable() != null) {
            for (RCTableData objRc : spectroDeviceObject.getRCTable()) {
                if (objRc.getTestItem().equals(testName)) {
                    if (objRc.getLimetLineRanges().get(0) != null) {
                        LimetLineRanges safeRange = objRc.getLimetLineRanges().get(0);
                        if (value > safeRange.getCMinValue() && value <= safeRange.getCMaxValue()) {
                            isOk = true;
                            return isOk;
                        }
                    }
                }
            }
        }
        return isOk;
    }

    public String getResultTextForTestItemwithValue(String testName, double value) {
        if (spectroDeviceObject.getRCTable() != null) {
            for (RCTableData objRc : spectroDeviceObject.getRCTable()) {
                if (objRc.getTestItem().equals(testName)) {
                    for (LimetLineRanges objLimitRange : objRc.getLimetLineRanges()) {
                        if (value > objLimitRange.getCMinValue() && value <= objLimitRange.getCMaxValue()) {
                            return objLimitRange.getLineSymbol();
                        }
                    }
                }
            }
        }
        return "";
    }

    /*public void startTestProcess(SyncingInterface syncingInterface1) {
        this.syncingInterface = syncingInterface1;
        removereceiver();
        initializeService();
        //syncSettingsWithDevice();
    }*/
    public void startTestProcess() {
        removereceiver();
        initializeService();
        //syncSettingsWithDevice();
    }

    public void abortTesting(AbortInterface abortInterface1) {
        this.abortInterface = abortInterface1;
        testAnalysisListener = null;
        //clearPreviousTestResulsArray();
        // String ejectCommand = "$MRS5000#";
        if (SCConnectionHelper.getInstance().isConnected) {
            isInterrupted = true;
            if (!isForSync) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        isEjectType = true;
                        ledControl(false);
                    }
                }, 1000);
                /*new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isEjectType = true;
                        ledControl(false);
                    }
                }, 5000);*/
            }
        }
    }

    public void performTryAgainFunction() {
        startTestAnalysis(testAnalysisListener);
    }

    public void performTestCancelFunction() {
        clearCache();
        clearPreviousTestResulsArray();
        SCConnectionHelper.getInstance().disconnect();// it will goes to SpectroDeviceScanViewController
        unRegisterReceiver();
    }

    public interface JsonFileInterface {
        void onSuccessForConfigureJson();

        void onFailureForConfigureJson(String bitmapList);
    }

    public interface SyncingInterface {
        void isSyncingCompleted(boolean error);

        void gettingDarkSpectrum(boolean isgetting);

    }

    public interface TeststaResultInterface {
        void onSuccessForTestComplete(ArrayList<TestFactors> results, String msg);
        void getRequestAndResponse(String data);
        void onFailureForTesting(String error);
    }

    public interface AbortInterface {
        void onAbortForTesting(boolean bool);
    }

}
