package de.embl.schwab.crosshair.io;


import org.apache.commons.math3.util.Precision;

public class SolutionToSave {

    private double initialKnifeAngle;
    private double initialTiltAngle;
    private double knife;
    private double tilt;
    private double rotation;
    private String firstTouch;
    private double distanceToCut;
    private String anglesUnit;
    private String distanceUnit;
    private int saveDecimalPlaces;

    public SolutionToSave(double initialKnifeAngle, double initialTiltAngle, double knife, double tilt,
                          double rotation, String firstTouch, double distanceToCut, String unit) {
        saveDecimalPlaces = 4;
        this.initialKnifeAngle = Precision.round(initialKnifeAngle, saveDecimalPlaces);
        this.initialTiltAngle = Precision.round(initialTiltAngle, saveDecimalPlaces);
        this.knife = Precision.round(knife,saveDecimalPlaces);
        this.tilt = Precision.round(tilt, saveDecimalPlaces);
        this.rotation = Precision.round(rotation, saveDecimalPlaces);
        this.firstTouch = firstTouch;
        this.distanceToCut = Precision.round(distanceToCut, saveDecimalPlaces);
        this.anglesUnit = "degrees";
        this.distanceUnit = unit;
    }

    public double getInitialKnifeAngle() {
        return initialKnifeAngle;
    }

    public double getInitialTiltAngle() {
        return initialTiltAngle;
    }

    public double getKnife() {
        return knife;
    }

    public double getTilt() {
        return tilt;
    }

    public double getRotation() {
        return rotation;
    }

    public String getFirstTouch() {
        return firstTouch;
    }

    public double getDistanceToCut() {
        return distanceToCut;
    }

    public String getAnglesUnit() {
        return anglesUnit;
    }

    public String getDistanceUnit() {
        return distanceUnit;
    }
}
