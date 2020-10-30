package com.spectrochips.spectrumsdk.FRAMEWORK;

import android.util.Log;

import com.spectrochips.spectrumsdk.DeviceConnectionModule.DataPoint;
import com.spectrochips.spectrumsdk.DeviceConnectionModule.PolynomialRegression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by ADMIN on 29-05-2019.
 */

public class SGAlgorithm {

    public static double getSGPloynominalYValue(ArrayList<Float> xValues, ArrayList<Float> yValues, int polynominalDegree, int noOfPoints, int index) {

        ArrayList<Float> originalXValues = xValues;
        ArrayList<Float> originalYValues = yValues;
        double centerValue = xValues.get(index);

        ArrayList<Float> considerXValues = sortXValuesArray(originalXValues, centerValue);
        int finalnoOfPoints = noOfPoints;
        if (finalnoOfPoints > considerXValues.size()) {
            finalnoOfPoints = considerXValues.size();
        }

        considerXValues = gettingCopyOfItems(considerXValues, finalnoOfPoints);

        Collections.sort(considerXValues, new Comparator<Float>() {
            @Override
            public int compare(Float s1, Float s2) {
                return Double.valueOf(s1).compareTo(Double.valueOf(s2));
            }
        });
        Log.e("considerXValues", "call" + considerXValues.toString());

        DataPoint dataPoints[] = new DataPoint[considerXValues.size()];
        for (int x = 0; x < considerXValues.size(); x++) {
            int indexOfOriginalXArray = originalXValues.indexOf(x);
            DataPoint objDataPoint = new DataPoint(x, originalYValues.get(indexOfOriginalXArray));
            dataPoints[x] = objDataPoint;
        }
        PolynomialRegression poly = new PolynomialRegression(dataPoints, polynominalDegree);
        poly.fillMatrix();
        double[] terms = poly.getTerms();
        double newValue = poly.predictY(terms, xValues.get(index));
        Log.e("newValue", "call" + newValue);
        return newValue;
    }
    public static ArrayList<Float> processSGAlgorithm(ArrayList<Float> xValues, ArrayList<Float> yValues, int polynominalDegree, int noOfPoints, float centerValue) {

        ArrayList<Float> originalXValues = xValues;
        ArrayList<Float> originalYValues = yValues;

        ArrayList<Float> considerXValues = sortXValuesArray(originalXValues, centerValue);

        int finalnoOfPoints = noOfPoints;
        if (finalnoOfPoints > considerXValues.size()) {
            finalnoOfPoints = considerXValues.size();
        }
        considerXValues = gettingCopyOfItems(considerXValues, finalnoOfPoints);
        Collections.sort(considerXValues, new Comparator<Float>() {
            @Override
            public int compare(Float s1, Float s2) {
                return Double.valueOf(s1).compareTo(Double.valueOf(s2));
            }
        });
        Log.e("considerXValues", "call" + considerXValues.toString());
        for (int x = 0; x < considerXValues.size(); x++) {
            int indexOfOriginalXArray = originalXValues.indexOf(x);
            double newYValue=getSGPloynominalYValue(xValues,yValues,polynominalDegree,noOfPoints,indexOfOriginalXArray);
            originalYValues.set(indexOfOriginalXArray,(float) newYValue);
        }
        return originalYValues;
    }

    public ArrayList<Float> processSGAlgorithmForAll(ArrayList<Float> xValues, ArrayList<Float> yValues, int polynominalDegree, int noOfPoints)
    {
        ArrayList<Float> originalYValues = yValues;
        for (int index = 0; index < xValues.size(); index++) {
            double newYValue=getSGPloynominalYValue(xValues,yValues,polynominalDegree,noOfPoints,index);
            originalYValues.set(index,(float) newYValue);
        }
        return originalYValues;
    }
    public static ArrayList<Float> sortXValuesArray(ArrayList<Float> xValues, final double centerValue) {
        Log.e("beforesort", "call" + xValues.toString());
        Log.e("beforecritical", "call" + centerValue);
        Collections.sort(xValues, new Comparator<Float>() {
            @Override
            public int compare(Float s1, Float s2) {
                return Double.valueOf(Math.abs(centerValue - s1)).compareTo(Double.valueOf(Math.abs(centerValue - s2)));
            }
        });
        Log.e("criticalWavelength", "call" + centerValue);
        Log.e("sortXValuesArray", "call" + xValues.toString());
        return xValues;
    }

    public static ArrayList<Float> gettingCopyOfItems(ArrayList<Float> considerXValues, final double finalnoOfPoints) {
        Log.e("gettingCopyOfItems", "call" + considerXValues.toString());
        Log.e("finalnoOfPoints", "call" + finalnoOfPoints);
        ArrayList<Float> considerValues = new ArrayList<>();
        for (int x = 0; x <= finalnoOfPoints; x++) {
            considerValues.add(considerXValues.get(x));
        }
        Log.e("gettingCopyOfItems", "call" + considerValues.toString());
        return considerValues;
    }
}
