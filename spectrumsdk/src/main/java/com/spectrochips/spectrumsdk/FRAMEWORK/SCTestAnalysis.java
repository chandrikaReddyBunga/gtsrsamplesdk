package com.spectrochips.spectrumsdk.FRAMEWORK;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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


public class SCTestAnalysis {
    private static SCTestAnalysis ourInstance;
    //public Context context;
    public ArrayList<Steps> motorSteps;
    private String darkSpectrumTitle = "Dark Spectrum";
    private String standardWhiteTitle = "Standard White (Reference)";
    public SpectorDeviceDataStruct spectroDeviceObject;
    public ArrayList<IntensityChart> intensityChartsArray = new ArrayList<>();
    private ArrayList<Float> pixelXAxis = new ArrayList<>();
    private ArrayList<Float> wavelengthXAxis = new ArrayList<>();
    private ArrayList<ReflectanceChart> reflectenceChartsArray = new ArrayList<>();
    private ArrayList<ConcentrationControl> concentrationArray = new ArrayList<>();
    private ArrayList<Float> darkSpectrumIntensityArray = new ArrayList<>();
    private ArrayList<Float> standardWhiteIntensityArray = new ArrayList<>();
   /* private ArrayList<String> cValArray = new ArrayList<>();
    private ArrayList<String> rArray = new ArrayList<>();*/

    private int stripNumber = 0;
    private boolean isForDarkSpectrum = false;
    //  private boolean isCalibration = false;
    public boolean isTestingCal = false;
    public boolean isTestStarted = false;

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
    private EjectInterface ejectInterface;
    public TestToastInterface testToastInterface;

    private boolean isEjectType = false;
    private boolean isInterrupted = false;
    private boolean isInsertStrip = false;
    private boolean isTestDone = false;

    public ArrayList<TestFactors> testResults = new ArrayList<>();

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

    public void loadInterface() {
        syncDeviceData(new TestDataInterface() {
            @Override
            public void gettingData(byte[] data) {
                final byte[] txValue = data;
                String text = decodeUTF8(txValue);
                if (isTestingCal) {
                    if (testAnalysisListener != null) {
                        testAnalysisListener.getRequestAndResponse(text);
                    }
                } else {
                    socketDidReceiveMessage(text, requestCommand);
                }
            }
            /*@Override
            public void testComplete(ArrayList<TestFactors> results, String msg, ArrayList<IntensityChart> intensityChartsArray) {
                testResults = results;
                Log.e("testCompleteReceived", "call" + testResults.size());
                if (testAnalysisListener != null) {
                    testAnalysisListener.onSuccessForTestComplete(testResults, "Test Complete", intensityChartsArray);
                }
            }*/
        });
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
        if (jsonFileInterface != null) {
            jsonFileInterface = null;
        }
        jsonFileInterface = jsonFileInterface1;
    }

    /*public void getDeviceSettings(String testName, String category, String date, JsonFileInterface jsonFileInterface1) {
        this.jsonFileInterface = jsonFileInterface1;
        SpectroDeviceDataController.getInstance().loadJsonFromUrl(testName);
        if (SpectroDeviceDataController.getInstance().spectroDeviceObject != null) {
            spectroDeviceObject = SpectroDeviceDataController.getInstance().spectroDeviceObject;
            motorSteps = spectroDeviceObject.getStripControl().getSteps();
            Log.e("loadDefaultSpect", "call" + spectroDeviceObject.getStripControl().getDistanceFromHolderEdgeTo1STStripInMM());
            Log.e("loadDefaultSpect", "call" + motorSteps.size());

        }
    }*/
    public void setDeviceSettings(JSONObject object) {
        SpectorDeviceDataStruct obj = SpectroDeviceDataController.getInstance().getObjectFromFile(object);
        if (obj != null) {
            Log.e("localspectroobject", "call" + obj.getStripControl().getDistancePerStepInMM());
            spectroDeviceObject = obj;
            motorSteps = spectroDeviceObject.getStripControl().getSteps();
        }
    }

    public void getDeviceSettings(String testName, String category, String date) {
        // this.jsonFileInterface = jsonFileInterface1;
        SpectroDeviceDataController.getInstance().loadJsonFromUrl(testName);
        if (SpectroDeviceDataController.getInstance().spectroDeviceObject != null) {
            spectroDeviceObject = SpectroDeviceDataController.getInstance().spectroDeviceObject;
            motorSteps = spectroDeviceObject.getStripControl().getSteps();
            Log.e("loadDefaultSpect", "call" + spectroDeviceObject.getStripControl().getDistanceFromHolderEdgeTo1STStripInMM());
            Log.e("loadDefaultSpect", "call" + motorSteps.size());

        }
    }

    public void startTestAnalysis(TeststaResultInterface teststaResultInterface1) {
        if (testAnalysisListener != null) {
            testAnalysisListener = null;
        }
        this.testAnalysisListener = teststaResultInterface1;
        // startTesting();
       /* stripNumber = 0;
        SCConnectionHelper.getInstance().prepareCommandForMoveToPosition();*/
    }

    public void startTesting() {
        setDefaultValues();
        stripNumber = 0;
        SCConnectionHelper.getInstance().prepareCommandForMoveToPosition();
    }
    private void setDefaultValues() {
        isInsertStrip = false;
        isInterrupted = false;
        isEjectType = false;
        isTestDone = false;
    }
    public void loadPixelAndWaveLengthArrays(ArrayList<Float> dark, ArrayList<Float> sw) {
        loadPixelArray();
        reprocessWavelength();
        prepareBeforeChartsDataForIntensity(dark, sw);
    }
    private void prepareBeforeChartsDataForIntensity(ArrayList<Float> dark, ArrayList<Float> sw) {
        intensityArray.clear();
        intensityChartsArray.clear();
        if (spectroDeviceObject.getRCTable() != null) {
            for (RCTableData objRc : spectroDeviceObject.getRCTable()) {
                IntensityChart objIntensity = new IntensityChart();
                objIntensity.setTestName(objRc.getTestItem());
                Log.e("ssss", "" + objIntensity.getTestName());
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
        objSWIntensity.setyAxisArray(sw);
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
        objDarkIntensity.setyAxisArray(dark);
        objDarkIntensity.setSubstratedArray(null);
        objDarkIntensity.setWavelengthArray(wavelengthXAxis);
        objDarkIntensity.setCriticalWavelength(0.0);
        intensityChartsArray.add(objDarkIntensity);

    }

    private void getDarkSpectrum() {
        clearPreviousTestResulsArray();
        loadPixelArray();
        reprocessWavelength();
        prepareChartsDataForIntensity();
        isForDarkSpectrum = true;
        getIntensity();
    }

    private void getIntensity() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                requestCommand = Commands.INTESITY_VALUES_TAG;
                sendString(requestCommand);
            }
        }, 1000);
    }

    private void loadPixelArray() {
        pixelXAxis = new ArrayList<>();
        pixelXAxis.clear();
        if (spectroDeviceObject.getImageSensor().getROI() != null) {
            int roiArray[] = spectroDeviceObject.getImageSensor().getROI();
            int pixelCount = roiArray[1];
            Log.e("pixelcount", "call" + pixelCount);

            for (int i = 1; i <= pixelCount; i++) {
                pixelXAxis.add(Float.valueOf(i));
            }
            Log.e("pixelcountarray", "call" + pixelXAxis.toString());
        }

    }

    private void reprocessWavelength() {
        //reprocess wavelength calculation
        for (int i = 0; i < pixelXAxis.size(); i++) {
            wavelengthXAxis.add(pixelXAxis.get(i));
        }
        Log.e("reprocessWavelength", "call" + wavelengthXAxis.toString());
        wavelengthXAxis.clear();
        if (spectroDeviceObject.getWavelengthCalibration() != null) {
            double[] resultArray = spectroDeviceObject.getWavelengthCalibration().getCoefficients();
            DataPoint theData[] = new DataPoint[0];
            PolynomialRegression poly = new PolynomialRegression(theData, spectroDeviceObject.getWavelengthCalibration().getNoOfCoefficients());
            poly.fillMatrix();
           /* java.text.DecimalFormat df = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                df = new java.text.DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
            }else{
                df = new DecimalFormat("#.##");
            }*/

            for (int index = 0; index < pixelXAxis.size(); index++) {
                Double d = poly.predictY(resultArray, pixelXAxis.get(index)) * 100 / 100;
                Log.e("zzzzzzzz", "call" + d.floatValue());
                wavelengthXAxis.add(d.floatValue());
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
                Log.e("ssss", "" + objIntensity.getTestName());
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
        // isCalibration = false;
        isForSync = false;
    }

    public void initializeService() {
        Log.d("initializeService", "Connect request result=");
        Intent gattServiceIntent = new Intent(SpectroCareSDK.getInstance().context, UartService.class);
        SpectroCareSDK.getInstance().context.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(SpectroCareSDK.getInstance().context).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (this.mService != null) {
            Log.e("sasassa", "Connect request result");
        }
    }

    public void removereceiver() {
        LocalBroadcastManager.getInstance(SpectroCareSDK.getInstance().context).unregisterReceiver(mGattUpdateReceiver);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            SCTestAnalysis.getInstance().mService = ((UartService.LocalBinder) service).getService();
            Log.d("ccccc", "onServiceConnected mService= " + SCTestAnalysis.getInstance().mService);
            if (!SCTestAnalysis.getInstance().mService.initialize()) {
                Log.e("sssssss", "Unable to initialize Bluetooth");
            }
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
                Log.e("showMessage", "call");
                //showMessage("Device Connected.");
            }

            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                //showMessage("Device Disconnected.");
            }
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                Log.e("zxzxzxzzx", "call");
                SCConnectionHelper.getInstance().isConnected = true;
               /* if (SCConnectionHelper.getInstance().scanDeviceInterface == null) {
                } else {
                    SCConnectionHelper.getInstance().scanDeviceInterface.onSuccessForConnection("Device Connected");
                }*/
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
        if (testAnalysisListener != null) {
            testAnalysisListener.getRequestAndResponse(response);
        }
        // Log.e("hexStringreceivedDataString", "call" + rawBuffer2Hex(data));
        receivedDataString = receivedDataString + response;

        if (receivedDataString.toUpperCase().startsWith("^ERR#") || receivedDataString.toUpperCase().startsWith("$ERR#")) {
            /*HashMap<String, String> dictionary = new HashMap<String, String>();
            dictionary.put("request", request);
            dictionary.put("response", receivedDataString);*/
            // NotificationCenter.default.post(name: NOTIFICATION_DATA_AVAILABLE.name, object: self, userInfo:dictionary)
            Log.e("^ERR#DataRecieved", "call");
            dataRecieved(receivedDataString, request);
            receivedDataString = "";
        }

        if (receivedDataString.toUpperCase().startsWith("$OK#") || receivedDataString.toUpperCase().startsWith("$OK!") || receivedDataString.toUpperCase().startsWith("^OK#")) {
            //   if (request.equals("$POR#")) {
            // receivedDataString = "$POS#";
                /*HashMap<String, String> dictionary = new HashMap<String, String>();
                dictionary.put("request", request);
                dictionary.put("response", receivedDataString);*/
            Log.e("$OK# Data Recieved", "call");
            dataRecieved(receivedDataString, request);
            receivedDataString = "";

        }
        // 2423 - $# - invalid data
        if (receivedDataString.toUpperCase().startsWith("$#")) {
            receivedDataString = "";
            //swal("Error", "Cofiguration error!", "error");
        }

        // Position Sensor success response  ^POS# or $POS#
        if (receivedDataString.toUpperCase().startsWith("^POS#") || receivedDataString.toUpperCase().startsWith("$POS#")) {
           /* HashMap<String, String> dictionary = new HashMap<String, String>();
            dictionary.put("request", request);
            dictionary.put("response", receivedDataString);*/
            dataRecieved(receivedDataString, request);
            receivedDataString = "";
        }

        // Motor move stop Sensor. ^STP#
        if (receivedDataString.toUpperCase().startsWith("^STP#") || receivedDataString.toUpperCase().startsWith("$STP#")) {
           /* HashMap<String, String> dictionary = new HashMap<String, String>();
            dictionary.put("request", request);
            dictionary.put("response", receivedDataString);*/
            dataRecieved(receivedDataString, request);
            receivedDataString = "";
        }

        //notify graph processing only when we got complete data 5e3235363023
        if (receivedDataString.toUpperCase().startsWith("^288$") && receivedDataString.toUpperCase().endsWith("^EOF#")) {
           /* HashMap<String, String> dictionary = new HashMap<String, String>();
            dictionary.put("request", request);
            dictionary.put("response", receivedDataString);*/
            Log.e("receivedDataStringcall", "call" + receivedDataString);
            intensityDataRecieved(receivedDataString, request);
            receivedDataString = "";
        }
    }

    //intenisityDataRecieved
    private void intensityDataRecieved(String responseData, String request) {
        Log.e("intensityDataRecieved", "call" + request + responseData.length());
        if (processIntensityValues(responseData)) {
            if (!isForDarkSpectrum /*&& !isCalibration*/) {
                performMotorStepsFunction();
            } else {
                isForDarkSpectrum = false;
                syncingInterface.isSyncingCompleted(true);
             /*   new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        ledControl(true);
                    }
                }, 1000);*/
            }
        } else {
            requestCommand = "";
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    getIntensity();
                }
            }, 1000);
        }
    }

    private void dataRecieved(String responseData, String request) {
        Log.e("dataRecieved", "call" + request + responseData);
        processResponseData(request, responseData);
    }

    private void processResponseData(String command, String response) {
        //String decodeStr = decodeUTF8(byteArray);
        Log.e("DeviceData", "CalledResponse" + command + ":" + response);
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
                                // syncingInterface.isSyncingCompleted(true);
                                Log.e("isSyncingCompleted", "call");
                                getDarkSpectrum();
                                syncDone();
                            }
                        }, 1000);
                        break;
                    default:
                        //  syncingInterface.isSyncingCompleted(false);
                        syncDone();
                        break;
                }
                commandNumber = commandNumber + 1;
            } else {
                Log.e("abort5", "call");
                if (isInterrupted && isEjectType) { //for test eject from my side
                    if (abortInterface != null) {
                        abortInterface.onAbortForTesting(true);
                    }
                } else if (isTestDone) {
                    Log.e("isTestDone", "call");
                    // isTestDone = false;
                    if (ejectInterface != null) {
                        ejectInterface.startTestForEjectTest(true);
                    }
                } else if (command.equals(UV_TURN_ON)) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (!isInterrupted) {
                                stripNumber = 1;//1;
                                getIntensity();
                            }
                        }
                    }, 1000);
                }
            }
        } else if (response.contains("POS")) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    ledControl(true);
                }
            }, 1000);
        } else if (response.contains("STP")) {
            if (isInterrupted) {
                if (isEjectType) { // Call when eject completed. .
                    Log.e("abortwithejecttype", "call");
                    isEjectType = false;
                    isInterrupted = false;
                    if (ejectInterface != null) {
                        ejectInterface.ejectStrip(true);
                    }
                }
            } else if (isEjectType) { // Call when eject completed. .
                Log.e("onlyejecttype", "call");
                isEjectType = false;
                if (ejectInterface != null) {
                    ejectInterface.ejectStrip(true);
                }
            } else if (isInsertStrip) {
                Log.e("insertstrip", "call");
                isInsertStrip = false;//called when isert strip
                if (ejectInterface != null) {
                    ejectInterface.insertStrip(true);
                }
            } else if (isTestDone) {
                Log.e("isTestDone", "call" + isTestDone);
                isTestDone = false;
                if (ejectInterface != null) {
                    ejectInterface.stoptestForEjectTest(true);
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
                            if (testToastInterface != null) {
                                testToastInterface.getResponse("StripNumber" + stripNumber);
                            }
                            stripNumber += 1;
                            getIntensity();
                        }
                    }, 1000);

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
        Log.e("unRegisterReceiver", "call");
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
    public void ejectStripCommand() {
        final String ejectCommand = "$MRS900#";
        Log.e("ejectStripCommand", "call" + ejectCommand);
        if (SCConnectionHelper.getInstance().isConnected) {
            isEjectType = true;
            // isInterrupted = true;
            ledControl(false);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    sendString(ejectCommand);
                }
            }, 1000);
        }
    }

    public void insertStripCommand() {
        final String insertCommand = "$MLS900#";
        Log.e("insertStripCommand", "call" + insertCommand);
        if (SCConnectionHelper.getInstance().isConnected) {
            isInsertStrip = true;
            ledControl(false);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    sendString(insertCommand);
                }
            }, 1000);
        }
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
        if (testAnalysisListener != null) {
            testAnalysisListener.getRequestAndResponse("----------Send Command--------------");
        }
        if (testAnalysisListener != null) {
            testAnalysisListener.getRequestAndResponse(message);
        }

        byte[] value;
        try {
            //send data to service
            value = message.getBytes("UTF-8");
            mService.writeRXCharacteristic(message);//(packet);
           /* int len = value.length;
            int index = 0;*/

           /* while (len > 0) {

                int packet_len;
                packet_len = (len >= 20) ? 20 : len;
                byte[] packet = new byte[packet_len];
                System.arraycopy(value, index, packet, 0, packet_len);
                mService.writeRXCharacteristic(packet);//(packet);
                len -= packet_len;
                index += packet_len;

            }*/
            //  mService.writeRXCharacteristic(value);
            //Update the log with time stamp
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void loadPreviousDarkAndSWArrays(ArrayList<Float> darkArray, ArrayList<Float> swArray) {
        darkSpectrumIntensityArray = darkArray;
        standardWhiteIntensityArray = swArray;
        loadPixelAndWaveLengthArrays(darkSpectrumIntensityArray, standardWhiteIntensityArray);
        Log.e("defalultdark", "call" + darkArray.toString());
        Log.e("defalultwhite", "call" + swArray.toString());

    }

    private boolean processIntensityValues(String response/*byte[] data*/) {
        Log.e("processIntensityValues", "calling" + response.length());
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
            if (testAnalysisListener != null) {
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
                // isCalibration = true;
                if (syncingInterface != null) {
                    syncingInterface.gettingDarkSpectrum(true/*, darkSpectrumIntensityArray*/);
                    syncDone();
                }
               /* new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        ledControl(true);
                    }
                }, 1000);*/
            }
        } /*else if (isCalibration) {
            standardWhiteIntensityArray = intensityArray;
            if (getPositionForTilte(standardWhiteTitle) != -1) {
                int position = getPositionForTilte(standardWhiteTitle);
                IntensityChart object = intensityChartsArray.get(position);
                object.setyAxisArray(standardWhiteIntensityArray);
                intensityChartsArray.set(position, object);
                Log.e("forwhitespectrum", "call" + object.getyAxisArray().toString());
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        ledControl(false);
                    }
                }, 1000);
                isCalibration = false;
                if (syncingInterface != null) {
                    syncingInterface.isSyncingCompleted(true, standardWhiteIntensityArray);
                    syncDone();
                }
            }
        } */ else {
            setIntensityArrayForTestItem();
            Log.e("lastpostion", "call" + motorSteps.get(stripNumber).getTestName());
            if (motorSteps.get(stripNumber).getTestName().contains("Eject")) {
                Log.e("mrs850", "call" );
                        isTestDone = true;
                if (ejectInterface != null) {
                    ejectInterface.startTestForEjectTest(true);
                }
            }
            if (stripNumber == motorSteps.size() - 1) {  // Before Eject command , Process the Testing completed command.
                if (testToastInterface != null) {
                    testToastInterface.getResponse("calling testCompleted methods");
                }
                testCompleted();
            }
        }

        return true;
    }

    private void setIntensityArrayForTestItem() {
        Steps currentObject = motorSteps.get(stripNumber - 1);
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
        if (stripNumber == motorSteps.size() - 1) {
            Log.e("testingended", "call");
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
                    Log.e("getyAxisArray", "call" + objIntensitychartObject.getyAxisArray().toString());
                    Log.e("getTestName", "call" + objIntensitychartObject.getTestName().toString());
                    ArrayList<Float> originalArray = getOriginalDivReference(objIntensitychartObject.getSubstratedArray(), swSubstratedArray);
                    double interpolationValue = getClosestValue(objIntensitychartObject.getWavelengthArray(), originalArray, objIntensitychartObject.getCriticalWavelength());
                    double correctValue = correctRValue(interpolationValue, objIntensitychartObject.getTestName());

                    ReflectanceChart objReflectanceChart = new ReflectanceChart();
                    objReflectanceChart.setTestName(objIntensitychartObject.getTestName());
                    objReflectanceChart.setxAxisArray(wavelengthXAxis);
                    objReflectanceChart.setyAxisArray(originalArray);
                    objReflectanceChart.setCriticalWavelength(objIntensitychartObject.getCriticalWavelength());
                    objReflectanceChart.setAutoMode(true);
                    objReflectanceChart.setInterpolationValue(correctValue);
                    reflectenceChartsArray.add(objReflectanceChart);
                }
            }
            if (testToastInterface != null) {
                testToastInterface.getResponse("filling reflectenceChartsArray completed");
            }
        } else {
            Log.e("No SW available", "call");
        }
        processFinalTestResults();
    }

    private double correctRValue(double rValue, String testName) {
        double returnRValue = rValue;
        RCTableData rcTableObject = getRCObjectFortestName(testName);
        if (rcTableObject != null) {
            double maxValue = getMax(rcTableObject.getR());
            double minValue = getMin(rcTableObject.getR());
            Log.e("minmaxvlaues", "call" + rcTableObject.getTestItem() + maxValue + "min" + minValue);
            if (returnRValue > maxValue) {
                returnRValue = maxValue;
            } else if (returnRValue < minValue) {
                returnRValue = minValue;
            }
        }
        return returnRValue;
    }

    // Method for getting the maximum value
    public double getMax(double[] inputArray) {
        double maxValue = inputArray[0];
        for (int i = 1; i < inputArray.length; i++) {
            if (inputArray[i] > maxValue) {
                maxValue = inputArray[i];
            }
        }
        return maxValue;
    }

    // Method for getting the minimum value
    public double getMin(double[] inputArray) {
        double minValue = inputArray[0];
        for (int i = 1; i < inputArray.length; i++) {
            if (inputArray[i] < minValue) {
                minValue = inputArray[i];
            }
        }
        return minValue;
    }

    private ArrayList<Float> getOriginalDivReference(ArrayList<Float> originalArray, ArrayList<Float> referenceArray) {
        //   Log.e("originalArray", "call" + originalArray.toString());
        Log.e("referenceArray", "call" + referenceArray.toString());
        ArrayList<Float> divisionArray = new ArrayList<>();
        for (int i = 0; i < originalArray.size(); i++) {
            if (referenceArray.get(i) == 0) {
                divisionArray.add(0.0f);
            } else {
                divisionArray.add(originalArray.get(i) / referenceArray.get(i));
                /* double value = originalArray.get(i) / referenceArray.get(i);
                 if (value >= 0){
                    divisionArray.add(originalArray.get(i) / referenceArray.get(i));
                }else{
                    divisionArray.add(0.0f);
                }*/
            }
        }
        Log.e("divisionArray", "call" + divisionArray.toString());
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
                Log.e("tcunitvalue", "call" + rcTableObject.getCriticalwavelength());
                ArrayList<Float> rArray = new ArrayList<>();
                ArrayList<Float> cArray = new ArrayList<>();

                for (double d : rcTableObject.getR()) {
                    rArray.add((float) d);
                }

                for (double d : rcTableObject.getC()) {
                    cArray.add((float) d);
                }
                double finalC = getClosestValue(rArray, cArray, objReflectance.getInterpolationValue());
                if (finalC < 0) {
                    finalC = 0.0;
                }
                ConcentrationControl objConcetration = new ConcentrationControl();
                objConcetration.setSNo(String.valueOf(index));
                objConcetration.setConcentration(String.valueOf(finalC));
                objConcetration.setUnits(rcTableObject.getUnit());
                objConcetration.setTestItem(rcTableObject.getTestItem());
                objConcetration.setReferenceRange(rcTableObject.getReferenceRange());
                objConcetration.setrValue(String.valueOf(objReflectance.getInterpolationValue()));
                objConcetration.setcValue(String.valueOf(finalC));
                objConcetration.setCriticalwavelength(String.valueOf(objReflectance.getCriticalWavelength()));
                Log.e("objConcetration", "call" + objConcetration.getCriticalwavelength());
                concentrationArray.add(objConcetration);
                index += 1;
            }
        }
        if (testToastInterface != null) {
            testToastInterface.getResponse("filling concentrationArray completed");
        }
    }

    private ArrayList<Float> sortXValuesArray(ArrayList<Float> xValues, final double criticalWavelength) {
        Log.e("beforesort", "call" + xValues.toString());
        Log.e("beforecritical", "call" + criticalWavelength);
        Collections.sort(xValues, new Comparator<Float>() {
            @Override
            public int compare(Float s1, Float s2) {
                return Double.valueOf(Math.abs(criticalWavelength - s1)).compareTo(Double.valueOf(Math.abs(criticalWavelength - s2)));
            }
        });
        Log.e("criticalWavelength", "call" + criticalWavelength);
        Log.e("sortXValuesArray", "call" + xValues.toString());
        return xValues;
    }

    private double getClosestValue(final ArrayList<Float> xValues, ArrayList<Float> yValues, final double criticalWavelength) {
        // Sorting array based on Difference
        ArrayList<Float> beforeXvalues = new ArrayList<>(xValues);
        if (xValues.indexOf(criticalWavelength) != -1) {
            return yValues.get(xValues.indexOf(criticalWavelength));
        } else {
            //https://stackoverflow.com/questions/13318733/get-closest-value-to-a-number-in-array
            double myNumber = criticalWavelength;
            double distance = Math.abs(xValues.get(0) - criticalWavelength);
            int idx = 0;
            for (int c = 1; c < xValues.size(); c++) {
                double cdistance = Math.abs(xValues.get(c) - myNumber);
                if (cdistance < distance) {
                    idx = c;
                    distance = cdistance;
                }
            }
            double nearestXValue = xValues.get(idx);
            Log.e("nearestXValue", "call" + nearestXValue);
            Log.e("Nearest X Value", String.valueOf(xValues.get(idx)));
            return yValues.get(idx);
        }
    }
  /*  private double getClosestValue(final ArrayList<Float> xValues, ArrayList<Float> yValues, final double criticalWavelength) {

        // Sorting array based on Difference
        ArrayList<Float> beforeXvalues = new ArrayList<>(xValues);

        ArrayList<Float> sortedArrayBasedOnDifference = sortXValuesArray(xValues, criticalWavelength);

        Log.e("sortedOnDifference", "call" + sortedArrayBasedOnDifference.toString());
        Log.e("yvluesarray", "call" + yValues.toString());
        Log.e("yvluesarraysize", "call" + yValues.size());
        Log.e("xvzlues", "call" + criticalWavelength);

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
    }*/

    private RCTableData getRCObjectFortestName(String testName) {

        if (spectroDeviceObject.getRCTable() != null) {
            ArrayList<RCTableData> rcTable = spectroDeviceObject.getRCTable();
            for (RCTableData objRCTable : rcTable) {
                if (objRCTable.getTestItem().equals(testName)) {
                    Log.e("rarray", "call" + objRCTable.getR().toString());
                    Log.e("carray", "call" + objRCTable.getC().toString());
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

            String finalTestValue = testValue.replace(",", ".");
            /* if(testValue.contains(",".replace(",","."))){
                Log.e("zzz","cll"+testValue);
            }
*/
            Log.e("testvalue", "" + finalTestValue);
            Log.e("testunits", "" + unit + resultText + object.getReferenceRange());
            Log.e("testresult", "" + resultText);

            sno = index + 1;

            TestFactors objTest = new TestFactors();
            objTest.setFlag(flag);
            objTest.setUnits(unit);
            objTest.setReferenceRange(objHealthReferenceRange);
            objTest.setTestname(testName);
            objTest.setSNo(String.valueOf(sno));
            objTest.setResult(resultText);
            objTest.setValue(finalTestValue);
            objTest.setrValue(object.getrValue());
            objTest.setcValue(object.getcValue());
            objTest.setCriticalWavelength(object.getCriticalwavelength());
            testItems.add(objTest);
        }
        if (testToastInterface != null) {
            testToastInterface.getResponse("testing Completed");
        }
        if (testAnalysisListener != null) {
            testAnalysisListener.onSuccessForTestComplete(testItems, "test", intensityChartsArray);
        }

       /* if (testDataInterface != null) {
            testDataInterface.testComplete(testItems, "test", intensityChartsArray);
        }*/

    }


    public String getNumberFormatStringforTestNameWithValue(String testName, double value) {
        Log.e("formantetestvalue", "" + value);
        String formattedString = String.valueOf(value);

        if (spectroDeviceObject.getRCTable() != null) {
            for (RCTableData objRCTable : spectroDeviceObject.getRCTable()) {
                if (objRCTable.getTestItem().equals(testName)) {
                    Log.e("numberformate", "call" + objRCTable.getNumberFormat());
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
                if (formattedString.contains(",")) {
                    formattedString.replace(",", ".");
                }
            }
        }
        return formattedString;
    }

    /*   public boolean getFlagForTestItemWithValue(String testName, double value) {
           boolean isOk = false;
           if (spectroDeviceObject.getRCTable() != null) {
               Log.e("getrctable", "calling" + spectroDeviceObject.getRCTable().size());
               for (RCTableData objRc : spectroDeviceObject.getRCTable()) {
                   if (objRc.getTestItem().equals(testName)) {
                       if (objRc.getLimetLineRanges().get(0) != null) {
                           LimetLineRanges safeRange = objRc.getLimetLineRanges().get(0);
                           if (value >= safeRange.getCMinValue() && value <= safeRange.getCMaxValue()) {
                               isOk = true;
                               return isOk;
                           }
                       }
                   }
               }
           }
           return isOk;
       }*/
    public boolean getFlagForTestItemWithValue(String testName, double value) {
        boolean isOk = false;
        String ascrobicTestName = "Ascorbic Acid";
        if (spectroDeviceObject.getRCTable() != null) {
            Log.e("getrctable", "calling" + spectroDeviceObject.getRCTable().size());
            for (RCTableData objRc : spectroDeviceObject.getRCTable()) {
                if (testName.toLowerCase().equals(ascrobicTestName.toLowerCase()) && objRc.getTestItem().toLowerCase().equals(ascrobicTestName.toLowerCase())) {
                    if (value >= 0 && value <= 40) { // for bypass the ascrobic smyle symbol
                        return true;
                    } else {
                        return false;
                    }
                }
                if (objRc.getTestItem().equals(testName)) {
                    if (objRc.getLimetLineRanges().get(0) != null) {
                        LimetLineRanges safeRange = objRc.getLimetLineRanges().get(0);
                        if (value >= safeRange.getCMinValue() && value <= safeRange.getCMaxValue()) {
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
                        if (value >= objLimitRange.getCMinValue() && value <= objLimitRange.getCMaxValue()) {
                            if (objLimitRange.getLineSymbol().trim().equals("-")) {
                                return "Negative";
                            } else {
                                return objLimitRange.getLineSymbol();
                            }
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

    public void ejectTesting(EjectInterface ejectInterface1) {
        if (ejectInterface != null) {
            ejectInterface = null;
        }
        ejectInterface = ejectInterface1;
    }
    public void abortTesting(AbortInterface abortInterface1) {
        if (abortInterface != null) {
            abortInterface = null;
        }
        abortInterface = abortInterface1;

        if (SCConnectionHelper.getInstance().isConnected) {
            isInterrupted = true;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    ejectStripCommand();
                    // isEjectType = true;
                    // ledControl(false);
                }
            }, 5000);
        }
    }
    /*public void abortTesting(AbortInterface abortInterface1) {
        if (abortInterface != null) {
            abortInterface = null;
        }
        abortInterface = abortInterface1;

        if (SCConnectionHelper.getInstance().isConnected) {
            isInterrupted = true;
            if (!isForSync) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        isEjectType = true;
                        ledControl(false);
                    }
                }, 5000);
            }
        }
    }*/

    public void performTryAgainFunction() {
        startTestAnalysis(testAnalysisListener);
    }

    public void performTestCancelFunction() {
        clearCache();
        clearPreviousTestResulsArray();
        SCConnectionHelper.getInstance().disconnect();// it will goes to SpectroDeviceScanViewController
        unRegisterReceiver();
    }

    public TestDataInterface testDataInterface;

    public void syncDeviceData(TestDataInterface testDataInterface1) {
        if (testDataInterface != null) {
            testDataInterface = null;
        }
        testDataInterface = testDataInterface1;
    }

    public interface TestDataInterface {
        void gettingData(byte[] data);

        // void testComplete(ArrayList<TestFactors> results, String msg, ArrayList<IntensityChart> intensityChartsArray);
        //void getCommandAndResponse(String msg);
    }

    public interface JsonFileInterface {
        void onSuccessForConfigureJson();

        void onFailureForConfigureJson(String bitmapList);
    }

    public interface SyncingInterface {
        void isSyncingCompleted(boolean error/*, ArrayList<Float> whiteArray*/);

        void gettingDarkSpectrum(boolean isgetting/*, ArrayList<Float> darkArray*/);

    }

    public interface TeststaResultInterface {
        void onSuccessForTestComplete(ArrayList<TestFactors> results, String msg, ArrayList<IntensityChart> intensityChartsArray);

        void getRequestAndResponse(String data);

        void onFailureForTesting(String error);
    }

    public void toastIntterfaceMethod(TestToastInterface testToastInterface1) {
        if (testToastInterface != null) {
            testToastInterface = null;
        }
        testToastInterface = testToastInterface1;
    }

    public interface TestToastInterface {
        void getResponse(String data);
    }

    public interface AbortInterface {
        void onAbortForTesting(boolean bool);
    }

    public interface EjectInterface {
        void ejectStrip(boolean bool);

        void startTestForEjectTest(boolean bool);

        void stoptestForEjectTest(boolean bool);

        void insertStrip(boolean bool);
    }
}




