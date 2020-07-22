package de.embl.cba.targeting;

import bdv.util.Bdv;
import bdv.util.BdvStackSource;
import customnode.CustomMesh;
import edu.mines.jtk.sgl.Point3;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.targeting.GeometryUtils.*;
import static de.embl.cba.targeting.utils.printImageMinMax;
import static java.lang.Math.*;

//TODO - add all sliders up here?
//TODO - get sliders to handle doubles!! Also have a box to type value if don't want to use slider - more precise
//TODO - some variable for block has been initialised at least once before can control it with sliders
//TODO - save initial tilt / knife values when initialise clicked, so don't change with those sliders anymore

public class MicrotomeManager extends JPanel {

    private final Image3DUniverse universe;
    private final PlaneManager planeManager;
    private final BdvStackSource bdvStackSource;
    private Map<String, CustomMesh> microtomeSTLs;
    private final Content imageContent;

    private double knifeTilt;
    private double tilt;
    private double rotation;

    private Vector3d rotationAxis;
    private Vector3d tiltAxis;
    private Vector3d initialArcCentre;
    private Vector3d initialKnifeCentre;
    private Vector3d currentKnifeCentre;
    private Vector3d currentArcCentre;
    private Vector3d currentHolderFront;
    private Vector3d currentKnifeNormal;
    private Vector3d initialKnifeNormal;

    private Matrix4d initialBlockTransform;
    private Matrix4d arcComponentsInitialTransform;
    private Matrix4d knifeInitialTransform;

    private double initialKnifeAngle;
    private double initialTiltAngle;
    private double initialTargetOffset;
    private double initialTargetTilt;

    private MicrotomePanel microtomePanel;
    private VertexAssignmentPanel vertexAssignmentPanel;
    private boolean microtomeModeActive;

    private Vector3d firstTouchPointSolution;
    private Vector3d firstTouchPointCutting;

    public MicrotomeManager(PlaneManager planeManager, Image3DUniverse universe, Content imageContent, BdvStackSource bdvStackSource) {

        this.planeManager = planeManager;
        this.universe = universe;
        this.imageContent = imageContent;
        this.bdvStackSource = bdvStackSource;
        microtomeModeActive = false;

        rotationAxis = new Vector3d(0, 1, 0);
        tiltAxis = new Vector3d(1, 0, 0);
        initialArcCentre = new Vector3d(0,1,0);
        initialKnifeCentre = new Vector3d(0,-2,0);
        initialKnifeNormal = new Vector3d(0, -1, 0);
        currentKnifeNormal = new Vector3d(0, -1, 0);

        loadMicrotomeMeshes();

    }

    public double getKnifeTilt() {return knifeTilt;}
    public double getTilt() {return tilt;}
    public double getRotation() {return rotation;}

    public boolean checkMicrotomeMode () {return microtomeModeActive;}
    public boolean setMicrotomeMode(boolean active) {return microtomeModeActive = active;}

    public void setMicrotomePanel(MicrotomePanel microtomePanel) {
        this.microtomePanel = microtomePanel;
    }
    public void setVertexAssignmentPanel (VertexAssignmentPanel vertexAssignmentPanel) {
        this.vertexAssignmentPanel = vertexAssignmentPanel;
    }

    private void loadMicrotomeMeshes() {
        microtomeSTLs = new HashMap<>();
        String[] stlFiles = {"/arc.stl", "/holder_back.stl", "/holder_front.stl", "/knife.stl"};
        for (String file: stlFiles) {
            Map<String, CustomMesh> currentStl = STLResourceLoader.loadSTL(file);
            // in case there are multiple objects in single stl file
            for (String key : currentStl.keySet()) {
                microtomeSTLs.put(key, currentStl.get(key));
            }
        }


    }

    public void initialiseMicrotome (double initialKnifeAngle, double initialTiltAngle) {
        if (planeManager.checkAllPlanesPointsDefined() & planeManager.getTrackPlane() == 0) {
            microtomeModeActive = true;

            int microtomePiecesAdded = 0;
            //TODO - setup so doesn't reload scale if just change inital values
            for (String key : microtomeSTLs.keySet()) {
                System.out.println(key);
                // TODO - set as locked - should probably set my other custom meshes to be locked too?
                if (!universe.contains(key)) {
                    universe.addCustomMesh(microtomeSTLs.get(key), key);
                    universe.getContent(key).setLocked(true);
                    microtomePiecesAdded += 1;
                } else {
                    universe.getContent(key).setVisible(true);
                }
            }

            // resize microtome parts, if first time adding them
            if (microtomePiecesAdded > 0) {
                resizeMicrotomeParts();
            }

            initialBlockTransform = setupBlockOrientation(initialKnifeAngle);
            this.initialKnifeAngle = initialKnifeAngle;
            this.initialTiltAngle = initialTiltAngle;

            calculateTargetOffsetTilt();

            // activate sliders
            microtomePanel.enableSliders();
            microtomePanel.getKnifeAngle().setCurrentValue(initialKnifeAngle);
            microtomePanel.getTiltAngle().setCurrentValue(initialTiltAngle);
            microtomePanel.getRotationAngle().setCurrentValue(0);

            // inactivate buttons for vertex assignemnt
            vertexAssignmentPanel.disableButtons();
        } else {
            System.out.println("Some of: target plane, block plane, top left, top right, bottom left, bottom right aren't defined. Or you are currently tracking a plane");
        }
    }

    private void calculateTargetOffsetTilt() {
        Map<String, RealPoint> namedVertices = planeManager.getNamedVertices();
        Map<String, Vector3d> planeNormals = planeManager.getPlaneNormals();
        Vector3d blockNormal = planeNormals.get("block");
        Vector3d targetNormal = planeNormals.get("target");

        double[] topLeft = new double[3];
        double[] bottomLeft = new double[3];
        double[] bottomRight = new double[3];
        namedVertices.get("top_left").localize(topLeft);
        namedVertices.get("bottom_left").localize(bottomLeft);
        namedVertices.get("bottom_right").localize(bottomRight);

//        Vector along the bottom edge of block, left to right
        Vector3d bottomEdgeVector = new Vector3d();
        bottomEdgeVector.sub(new Vector3d(bottomRight), new Vector3d(bottomLeft));

//        Vector pointing 'up' along left edge of block. Bear in mind may not be exactly perpendicular to the edge_vector
//        due to not perfectly rectangular block shape. I correct for this later
        Vector3d upLeftSideVector = new Vector3d();
        upLeftSideVector.sub(new Vector3d(topLeft), new Vector3d(bottomLeft));

//        Calculate line perpendicular to x (edge vector), in plane of block face
        Vector3d vertical = new Vector3d();
        vertical.cross(blockNormal, bottomEdgeVector);

//        But depending on orientation of block normal, this could be 'up' or 'down' relative to user
//        to force this to be 'up' do:
        if (upLeftSideVector.dot(vertical) < 0) {
            vertical.negate();
        }

        initialTargetOffset = rotationPlaneToPlane(vertical, targetNormal, blockNormal);

//        Calculate initial target tilt
//        Calculate tilt (about the new axis of rotation, i.e. about line of intersection from previous calc)
//        new axis of rotation, unknown orientation
        Vector3d axisRotation = new Vector3d();
        axisRotation.cross(targetNormal, vertical);
//        I force this to be pointing in the general direction of the edge vector to give consistent clockwise vs anticlockwise
//        i.e. I look down the right side of the vector
        if (axisRotation.dot(bottomEdgeVector) < 0) {
            axisRotation.negate();
        }

        Vector3d intersectionNormal = new Vector3d();
        intersectionNormal.cross(axisRotation, vertical);
        initialTargetTilt = rotationPlaneToPlane(axisRotation, targetNormal, intersectionNormal);

    }

    private void resizeMicrotomeParts () {
        // scale meshes / position them
        Point3d minArc = new Point3d();
        Point3d maxArc = new Point3d();
        universe.getContent("/arc.stl").getMax(maxArc);
        universe.getContent("/arc.stl").getMin(minArc);
        double heightArc = abs(maxArc.getZ() - minArc.getZ());

        Point3d minImage = new Point3d();
        Point3d maxImage = new Point3d();
        imageContent.getMax(maxImage);
        imageContent.getMin(minImage);
        ArrayList<Double> dims = new ArrayList<>();
        dims.add(abs(maxImage.getX() - minImage.getX()));
        dims.add(abs(maxImage.getY() - minImage.getY()));
        dims.add(abs(maxImage.getZ() - minImage.getZ()));
        double endHeight = Collections.max(dims);

        double scaleFactor = endHeight / heightArc;

        Matrix4d scaleMatrix = new Matrix4d(scaleFactor, 0, 0, 0,
                0, scaleFactor, 0, 0,
                0, 0, scaleFactor, 0,
                0, 0, 0, 1);

        Transform3D trans = new Transform3D(scaleMatrix);

        Vector3d maxImageVector = new Vector3d();
        maxImageVector.sub(new Vector3d(maxImage.getX(), maxImage.getY(), maxImage.getZ()),
                new Vector3d(minImage.getX(), minImage.getY(), minImage.getZ()));
        double maxImageDistance = maxImageVector.length();

        // translate so front of holder one max image distance away from 0,0,0
        // location of min holder front is approximate from original blender file
        Point3d minHolderFront = new Point3d(0, 3, 0);

        // hodler front after scaling
        Point3d holderFrontAfter = new Point3d(minHolderFront.getX(), minHolderFront.getY(), minHolderFront.getZ());
        trans.transform(holderFrontAfter);
        double yHolderFront = holderFrontAfter.getY();

        double translateY = maxImageDistance - yHolderFront;

        Matrix4d translateArcComponents = new Matrix4d(1, 0, 0, 0,
                0, 1, 0, translateY,
                0, 0, 1, 0,
                0, 0, 0, 1);

        translateArcComponents.mul(scaleMatrix);
        Transform3D arcComponentsTransform = new Transform3D(translateArcComponents);
        String[] arcComponents = new String[]{"/arc.stl", "/holder_front.stl", "/holder_back.stl"};
        for (String key : arcComponents) {
            universe.getContent(key).setTransform(arcComponentsTransform);
        }
        arcComponentsInitialTransform = translateArcComponents;

        // arc centre after scaling
        Point3d arcC = new Point3d(initialArcCentre.getX(), initialArcCentre.getY(), initialArcCentre.getZ());
        arcComponentsTransform.transform(arcC);
        currentArcCentre = new Vector3d(arcC.getX(), arcC.getY(), arcC.getZ());

        // holder front after scaling - save for cutting range
        arcComponentsTransform.transform(minHolderFront);
        currentHolderFront = new Vector3d(minHolderFront.getX(), minHolderFront.getY(), minHolderFront.getZ());

        // Set knife to same distance
        // hodler front after scaling
        Point3d knifeCentreAfter = new Point3d(initialKnifeCentre.getX(), initialKnifeCentre.getY(), initialKnifeCentre.getZ());
        trans.transform(knifeCentreAfter);
        double yKnife = knifeCentreAfter.getY();

        double translateK = -maxImageDistance - yKnife;

        Matrix4d translateKnife = new Matrix4d(1, 0, 0, 0,
                0, 1, 0, translateK,
                0, 0, 1, 0,
                0, 0, 0, 1);

        translateKnife.mul(scaleMatrix);
        Transform3D knifeTransform = new Transform3D(translateKnife);
        universe.getContent("/knife.stl").setTransform(knifeTransform);
        knifeInitialTransform = translateKnife;

        // knife centre after scaling
        Point3d knifeC = new Point3d(initialKnifeCentre.getX(), initialKnifeCentre.getY(), initialKnifeCentre.getZ());
        knifeTransform.transform(knifeC);
        currentKnifeCentre = new Vector3d(knifeC.getX(), knifeC.getY(), knifeC.getZ());
    }

    public void exitMicrotomeMode (){
        microtomeModeActive = false;

        initialBlockTransform.setIdentity();
        microtomePanel.getKnifeAngle().setCurrentValue(0);
        microtomePanel.getTiltAngle().setCurrentValue(0);
        microtomePanel.getRotationAngle().setCurrentValue(0);

        imageContent.setTransform(new Transform3D());
        universe.centerSelected(imageContent);

        // make microtome models invisible
        for (String key : microtomeSTLs.keySet()) {
            if (universe.contains(key)) {
                universe.getContent(key).setVisible(false);
            }
        }

        // inactivate sliders
        microtomePanel.disableSliders();
        vertexAssignmentPanel.enableButtons();

    }


    private Matrix4d setupBlockOrientation(double initialKnifeAngle) {
        String[] planeNames = {"target", "block"};
        //reset translation / rotation in case it has been modified
        imageContent.setTransform(new Transform3D());
        for (String name : planeNames) {
            universe.getContent(name).setTransform(new Transform3D());
        }

        Map<String, RealPoint> namedVertices = planeManager.getNamedVertices();

        // check normal in right orientation, coming out of block surface
        double[] topLeft = new double[3];
        double[] bottomLeft = new double[3];
        double[] bottomRight = new double[3];
        namedVertices.get("top_left").localize(topLeft);
        namedVertices.get("bottom_left").localize(bottomLeft);
        namedVertices.get("bottom_right").localize(bottomRight);

        Vector3d bottomEdgeVector = new Vector3d();
        bottomEdgeVector.sub(new Vector3d(bottomRight), new Vector3d(bottomLeft));

        double lengthEdge = bottomEdgeVector.length();

        Vector3d upLeftSideVector = new Vector3d();
        upLeftSideVector.sub(new Vector3d(topLeft), new Vector3d(bottomLeft));

        // bottom edge cross up left side, gives a normal that points out of teh block surface
        Vector3d blockNormal = new Vector3d();
        blockNormal.cross(bottomEdgeVector, upLeftSideVector);
        blockNormal.normalize();

        Vector3d endBlockNormal = new Vector3d(0, -1, 0);
        Vector3d endEdgeVector = new Vector3d(1, 0, 0);
        AxisAngle4d initialKnifeOffset = new AxisAngle4d(new Vector3d(0, 0, 1), initialKnifeAngle * Math.PI / 180);
        Matrix4d matrixInitialKnifeOffset = new Matrix4d();
        matrixInitialKnifeOffset.set(initialKnifeOffset);
        Transform3D initialKnifeTransform = new Transform3D(matrixInitialKnifeOffset);

        initialKnifeTransform.transform(endBlockNormal);
        endBlockNormal.normalize();
        initialKnifeTransform.transform(endEdgeVector);
        endEdgeVector.normalize();

        // normalise just in case
        bottomEdgeVector.normalize();

        // what is transform to bring block normal to be end block normal & edge vector to be end edge vector?
        //TODO - maybe translate so centre of edge vector == centre of knife location
        Rotation endRotation = new Rotation(new Vector3D(blockNormal.getX(), blockNormal.getY(), blockNormal.getZ()),
                new Vector3D(bottomEdgeVector.getX(), bottomEdgeVector.getY(), bottomEdgeVector.getZ()),
                new Vector3D(endBlockNormal.getX(), endBlockNormal.getY(), endBlockNormal.getZ()),
                new Vector3D(endEdgeVector.getX(), endEdgeVector.getY(), endEdgeVector.getZ()));


        // convert back to scijava conventions
        double[][] endRotMatrix = endRotation.getMatrix();
        Matrix4d scijavaFormMatrix = new Matrix4d(endRotMatrix[0][0], endRotMatrix[0][1], endRotMatrix[0][2], 0,
                endRotMatrix[1][0],endRotMatrix[1][1],endRotMatrix[1][2], 0,
                endRotMatrix[2][0],endRotMatrix[2][1],endRotMatrix[2][2], 0,
                0,0,0,1);

        // initial position of bottom edge centre
        Vector3d bottomEdgeCentre = new Vector3d(bottomLeft);
        bottomEdgeCentre.add(new Vector3d(bottomEdgeVector.getX() * 0.5 * lengthEdge,
                bottomEdgeVector.getY() * 0.5 * lengthEdge,
                bottomEdgeVector.getZ() * 0.5 * lengthEdge));
        // vector from initial to final position of bottom edge centre
        Vector3d endBottomEdgeCentre = new Vector3d(0, 0, 0);
        endBottomEdgeCentre.sub(bottomEdgeCentre);

        //final transform
        Matrix4d finalSetupTransform = new Matrix4d();
        // rotate about the initial position of bottom edge centre, then translate bottom edge centre to (0,0,0)
        compose(scijavaFormMatrix, new Vector3d(bottomEdgeCentre.getX(), bottomEdgeCentre.getY(), bottomEdgeCentre.getZ()), new Vector3d(endBottomEdgeCentre.getX(), endBottomEdgeCentre.getY(), endBottomEdgeCentre.getZ()), finalSetupTransform);

        Transform3D finalTransform = new Transform3D(finalSetupTransform);
        imageContent.setTransform(finalTransform);
        for (String name : planeNames) {
            universe.getContent(name).setTransform(finalTransform);
        }

        // change so view rotates about (0,0,0)
        universe.centerAt(new Point3d());

        return finalSetupTransform;
    }

        //as here for recalculate global min max
        // https://github.com/fiji/3D_Viewer/blob/c1cba02d475a05c94aebe322c2d5d76790907d6b/src/main/java/ij3d/Image3DUniverse.java
    private double[] calculateCentre(Content imageContent) {
        final Point3d cmin = new Point3d();
        imageContent.getMin(cmin);
        final Point3d cmax = new Point3d();
        imageContent.getMax(cmax);

        double[] centre = new double[3];

        centre[0] = cmin.getX() + (cmax.getX() - cmin.getX()) / 2;
        centre[1] = cmin.getY() + (cmax.getY() - cmin.getY()) / 2;
        centre[2] = cmin.getZ() + (cmax.getZ() - cmin.getZ()) / 2;

        return centre;
    }



    //        The two methods below are adapted from the imagej 3d viewer
    //        Interactive transform setter in 3d viewer: https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/gui/InteractiveTransformDialog.java
    //        setting of transform: https://github.com/fiji/3D_Viewer/blob/ed05e4b2275ad6ad7c94b0e22f4789ebd3472f4d/src/main/java/ij3d/Executer.java
    private Matrix4d makeMatrix(double angleDegrees, Vector3d axis, Vector3d rotationCentre, Vector3d translation) {
        double angleRad = angleDegrees * Math.PI / 180;
        Matrix4d m = new Matrix4d();
        compose(new AxisAngle4d(axis, angleRad), rotationCentre, translation, m);
        return m;
    }

    public void compose(final AxisAngle4d rot, final Vector3d origin,
                               final Vector3d translation, final Matrix4d ret)
    {
        ret.set(rot);
        final Vector3d trans = new Vector3d(origin);
        trans.scale(-1);
        ret.transform(trans);
        trans.add(translation);
        trans.add(origin);

        ret.setTranslation(trans);
    }

    public void compose(final Matrix4d rot, final Vector3d origin,
                               final Vector3d translation, final Matrix4d ret)
    {
        ret.set(rot);
        final Vector3d trans = new Vector3d(origin);
        trans.scale(-1);
        ret.transform(trans);
        trans.add(translation);
        trans.add(origin);

        ret.setTranslation(trans);
    }

    private void updateTiltRotationBlock () {
        Vector3d translation = new Vector3d(new double[] {0,0,0});

        Matrix4d initialTransformForArc = new Matrix4d(arcComponentsInitialTransform);

        Matrix4d tiltTransform = makeMatrix(tilt, tiltAxis, currentArcCentre, translation);
        Matrix4d rotationTransform = makeMatrix(rotation, rotationAxis, currentArcCentre, translation);
        // account for scaling
        Matrix4d holderBackTransform = new Matrix4d(tiltTransform);
        holderBackTransform.mul(initialTransformForArc);
        universe.getContent("/holder_back.stl").setTransform(new Transform3D(holderBackTransform));
        tiltTransform.mul(rotationTransform);
        tiltTransform.mul(initialTransformForArc);
        universe.getContent("/holder_front.stl").setTransform(new Transform3D(tiltTransform));

        // transform for block > must account for initial tilt
        Matrix4d blockTiltTransform = makeMatrix(tilt - initialTiltAngle, tiltAxis, currentArcCentre, translation);
        blockTiltTransform.mul(rotationTransform);
        Matrix4d init_transform = new Matrix4d(initialBlockTransform);
        blockTiltTransform.mul(init_transform);

        Transform3D finalTransform = new Transform3D(blockTiltTransform);
        imageContent.setTransform(finalTransform);
        for (String planeName : new String[] {"target", "block"}) {
//            planeManager.updatePlanesInPlace();
            universe.getContent(planeName).setTransform(finalTransform);
        }

        //TODO -remove
        printImageMinMax(imageContent);
    }

    public void setRotation(double rotation) {
//        System.out.println(rotation);
        this.rotation = rotation;
        updateTiltRotationBlock();
        microtomePanel.setRotationLabel(rotation);
    }

    public void setTilt(double tilt) {
//        System.out.println(tilt);
        this.tilt = tilt;
        updateTiltRotationBlock();
        microtomePanel.setTiltLabel(tilt);
    }

    public void setKnifeAngle(double knifeTilt) {
//        System.out.println(knifeTilt);
        this.knifeTilt = knifeTilt;
        Vector3d axis = new Vector3d(new double[] {0, 0, 1});
        Vector3d translation = new Vector3d(new double[] {0,0,0});

        Matrix4d fullTransform = makeMatrix(knifeTilt, axis, currentKnifeCentre, translation);

        // Update normal
        Transform3D knifeTiltTransform = new Transform3D(fullTransform);
        knifeTiltTransform.transform(initialKnifeNormal, currentKnifeNormal);

        // Account for initial scaling of knife
        Matrix4d initialTransformForKnife = new Matrix4d(knifeInitialTransform);
        fullTransform.mul(initialTransformForKnife);
        universe.getContent("/knife.stl").setTransform(new Transform3D(fullTransform));

        microtomePanel.setKnifeLabel(knifeTilt);
    }

    public void setCuttingBounds () {
//        All image vertices in microtome coordinates
        Map<String, RealPoint> vertices = planeManager.getNamedVertices();
        ArrayList<Point3d> verticesMicrotomeCoords = new ArrayList<>();
        ArrayList<Vector3d> verticesMicrotomeCoordsV = new ArrayList<>();
        for (String key: vertices.keySet()) {
            double[] location = new double[3];
            vertices.get(key).localize(location);
            verticesMicrotomeCoords.add(new Point3d(location));
        }

        for (Point3d point : verticesMicrotomeCoords) {
            initialBlockTransform.transform(point);
            verticesMicrotomeCoordsV.add(new Vector3d(point.getX(), point.getY(), point.getZ()));
        }

//        Find one with minimum distance to Knife plane - this is the first point hit by this cutting orientation
        int indexMinDist = indexMinMaxPointsToPlane(currentKnifeCentre, currentKnifeNormal, verticesMicrotomeCoordsV,"min");
        firstTouchPointCutting = verticesMicrotomeCoordsV.get(indexMinDist);

//        Set cutting range so first touch point == 0
        microtomePanel.setCuttingRange(currentKnifeCentre.getY() - firstTouchPointCutting.getY(),
                currentHolderFront.getY() - firstTouchPointCutting.getY());
    }

    public void updateCut(double currentDepth) {
//        Convert to microtome space coordinates - not adjusted for intial point == 0
        Point3d knifePoint = new Point3d(0, currentDepth + firstTouchPointCutting.getY(), 0);

//        Convert knife plane to image coordinates
        Transform3D inverseInitialBlockTransform = new Transform3D();
        inverseInitialBlockTransform.invert(new Transform3D(initialBlockTransform));
        inverseInitialBlockTransform.transform(knifePoint);
        Vector3d knifeNormal = new Vector3d();
        inverseInitialBlockTransform.transform(currentKnifeNormal, knifeNormal);

        double[] knifePointDouble = {knifePoint.getX(), knifePoint.getY(), knifePoint.getZ()};
        double[] knifeNormalDouble = {knifeNormal.getX(), knifeNormal.getY(), knifeNormal.getZ()};
        moveToPosition(bdvStackSource, knifePointDouble, 0);
        levelCurrentView(bdvStackSource, knifeNormalDouble);

    }

    public void setSolutionFromRotation (double solutionRotation) {
        microtomePanel.getRotationAngle().setCurrentValue(solutionRotation);

        double rot = convertToRadians(solutionRotation);
        double iTilt = convertToRadians(initialTiltAngle);
        double iKnife = convertToRadians(initialKnifeAngle);
        double tOffset = convertToRadians(initialTargetOffset);
        double tRotation = convertToRadians(initialTargetTilt);

        double A = cos(iKnife + tOffset);
        double B =  sin(tRotation)*sin(iKnife + tOffset);
        double C = sin(iTilt)*sin(iKnife+tOffset);
        double D = cos(iTilt)*sin(iKnife+tOffset);
        double E = cos(tRotation)*sin(iKnife+tOffset);
        double F = sin(iTilt)*cos(tRotation);
        double G = sin(tRotation)*cos(iTilt);
        double H = sin(iTilt)*sin(tRotation);
        double I = cos(iTilt)*cos(tRotation);

//        solution tilt & rot
        double solTilt = atan(((-A*F + G)/(-A*I -H))*cos(rot) + ((E/(-A*I - H))*sin(rot)));

//    solution knife & rot
        double solKnife = atan((A*I + H)*(E*cos(rot) + (A*F - G)*sin(rot))/(sqrt(pow(A*I + H, 2) + pow(E*sin(rot) + (-A*F + G)*cos(rot), 2))*abs(A*I + H)));

        calculateDistance();

        microtomePanel.getTiltAngle().setCurrentValue(convertToDegrees(solTilt));
        microtomePanel.getKnifeAngle().setCurrentValue(convertToDegrees(solKnife));
    }

    private void calculateDistance () {
        //TODO - calculate once then only update for the knife angle interactively???
        Map<String, RealPoint> namedVertices = planeManager.getNamedVertices();
        Map<String, Vector3d> planeNormals = planeManager.getPlaneNormals();
        Map<String, Vector3d> planePoints = planeManager.getPlanePoints();
        Vector3d targetNormal = new Vector3d(planeNormals.get("target"));
        Vector3d targetPoint = new Vector3d(planePoints.get("target"));

        double[] topLeft = new double[3];
        double[] topRight = new double[3];
        double[] bottomLeft = new double[3];
        double[] bottomRight = new double[3];
        namedVertices.get("top_left").localize(topLeft);
        namedVertices.get("top_right").localize(topRight);
        namedVertices.get("bottom_left").localize(bottomLeft);
        namedVertices.get("bottom_right").localize(bottomRight);

//        Calculate first point touched on block face, done by calculating perpendicular distance
//        from target to each point, and returning that with the largest.

//        Calculate perpendicular distance to each point
        targetNormal.normalize();
        ArrayList<Vector3d> allVertices = new ArrayList<>();
        allVertices.add(new Vector3d(topLeft));
        allVertices.add(new Vector3d(topRight));
        allVertices.add(new Vector3d(bottomLeft));
        allVertices.add( new Vector3d(bottomRight));

        int maxDistanceIndex = indexMinMaxPointsToPlane(targetPoint, targetNormal, allVertices, "max");

        Vector3d firstTouch = new Vector3d();
//        Assign first touch to point with maximum distance
        if (maxDistanceIndex == 0) {
            firstTouch.set(topLeft);
            microtomePanel.setFirstTouch("Top Left");
        } else if (maxDistanceIndex == 1) {
            firstTouch.set(topRight);
            microtomePanel.setFirstTouch("Top Right");
        } else if (maxDistanceIndex == 2) {
            firstTouch.set(bottomLeft);
            microtomePanel.setFirstTouch("Bottom Left");
        } else if (maxDistanceIndex == 3) {
            firstTouch.set(bottomRight);
            microtomePanel.setFirstTouch("Bottom Right");
        }
        firstTouchPointSolution = new Vector3d(firstTouch);

//        Calculate perpendicular distance to target
        Vector3d firstTouchToTarget = new Vector3d();
        firstTouchToTarget.sub(targetPoint, firstTouch);
        double perpDist = abs(firstTouchToTarget.dot(targetNormal));
        System.out.println(perpDist);

//        Compensate for offset between perpendicular distance and true N-S of microtome
//        I believe this is just the angle of the knife in this scenario, as the knife was reset to true 0
        double NSDist = perpDist / cos(convertToRadians(knifeTilt));

        microtomePanel.setDistanceToCut(NSDist);
    }
}
