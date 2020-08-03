package de.embl.cba.crosshair.ui.swing;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.*;
import de.embl.cba.crosshair.PlaneManager;
import de.embl.cba.crosshair.microtome.MicrotomeManager;
import ij.IJ;

import javax.swing.*;
import java.awt.*;

// similar to mobie source panel - https://github.com/mobie/mobie-viewer-fiji/blob/master/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java

    public class PlanePanel extends JPanel {

        private final PlaneManager planeManager;
        private final MicrotomeManager microtomeManager;

        public PlanePanel(PlaneManager planeManager, MicrotomeManager microtomeManager) {
            this.planeManager = planeManager;
            this.microtomeManager = microtomeManager;

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Planes"),
                    BorderFactory.createEmptyBorder(5,5,5,5)));

            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            addPlaneToPanel("target");
            addPlaneToPanel("block");
        }


        private void addColorButton(JPanel panel, int[] buttonDimensions, String planeName) {
            JButton colorButton;
            colorButton = new JButton("C");

            colorButton.setPreferredSize(
                    new Dimension(buttonDimensions[0], buttonDimensions[1]));

            colorButton.addActionListener(e -> {
                if (planeManager.getTrackPlane() == 0) {
                    Color colour = JColorChooser.showDialog(null, "", null);

                    if (colour == null) return;

                    if (planeName == "target") {
                        if (planeManager.checkNamedPlaneExists("target")) {
                            planeManager.setTargetPlaneColour(colour);
                        } else {
                            IJ.log("Target plane not initialised");
                        }
                    } else if (planeName == "block") {
                        if (planeManager.checkNamedPlaneExists("block")) {
                            planeManager.setBlockPlaneColour(colour);
                        } else {
                            IJ.log("Block plane not initialised");
                        }
                    }
                } else {
                    IJ.log("Can only change colour, when not tracking a plane");
                }

            });

            panel.add(colorButton);
        }

        private void addGOTOButton(JPanel panel, int[] buttonDimensions, String planeName) {
            JButton goToButton;
            goToButton = new JButton("GO TO");

            goToButton.setPreferredSize(
                    new Dimension(2*buttonDimensions[0], buttonDimensions[1]));

            goToButton.addActionListener(e -> {
                if (planeManager.getTrackPlane() == 0) {
                    if (planeManager.checkNamedPlaneExists(planeName)) {
                        planeManager.moveViewToNamedPlane(planeName);
                    } else {
                        IJ.log("Plane not initialised");
                    }
                } else {
                    IJ.log("Can only go to plane, when not tracking a plane");
                }
            });

            panel.add(goToButton);
        }

        private void addTrackingButton(JPanel panel, int[] buttonDimensions, String planeName) {
            JButton trackButton;
            trackButton = new JButton("TRACK");

            trackButton.setPreferredSize(
                    new Dimension(2*buttonDimensions[0], buttonDimensions[1]));

            trackButton.addActionListener(e -> {
                        // TODO - move checks to just making buttons inactive
                    if (planeName == "block") {
                        toggleBlockTracking(trackButton);
                    } else if (planeName == "target") {
                        toggleTargetTracking(trackButton);
                    }
            });

            panel.add(trackButton);
        }

        private void toggleBlockTracking (JButton trackButton) {
            if (planeManager.getTrackPlane() == 0 & !microtomeManager.isMicrotomeModeActive()) {
                // check if there are already vertex points
                if (planeManager.getBlockVertices().size() > 0) {
                    int result = JOptionPane.showConfirmDialog(null, "If you track the block plane, you will lose all current vertex points", "Are you sure?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (result == JOptionPane.YES_OPTION) {
                        planeManager.removeAllBlockVertices();
                        planeManager.setTrackPlane(2);
                        planeManager.updatePlaneCurrentView("block");
                        trackButton.setBackground(new Color (255, 0,0));
                    }
                } else {
                    planeManager.setTrackPlane(2);
                    planeManager.updatePlaneCurrentView("block");
                    trackButton.setBackground(new Color (255, 0,0));
                }
            } else if (planeManager.getTrackPlane() == 2) {
                planeManager.setTrackPlane(0);
                trackButton.setBackground(null);
            }
        }

        private void toggleTargetTracking (JButton trackButton) {
            if (planeManager.getTrackPlane() == 0 & planeManager.getVisiblityNamedPlane("target") & !microtomeManager.isMicrotomeModeActive()) {
                planeManager.setTrackPlane(1);
                planeManager.updatePlaneCurrentView("target");
                trackButton.setBackground(new Color (255, 0,0));
            } else if (planeManager.getTrackPlane() == 1) {
                planeManager.setTrackPlane(0);
                trackButton.setBackground(null);
            }
        }

        private void addVisibilityButton (JPanel panel, int[] buttonDimensions, String planeName) {
            JButton visbilityButton;
            visbilityButton = new JButton("V");

            visbilityButton.setPreferredSize(
                    new Dimension(buttonDimensions[0], buttonDimensions[1]));

            visbilityButton.addActionListener(e -> {
                if (planeManager.getTrackPlane() == 0) {
                    if (planeName == "target") {
                        if (planeManager.checkNamedPlaneExists("target")) {
                            planeManager.toggleTargetVisbility();
                        } else {
                            IJ.log("Target plane not initialised");
                        }
                    } else if (planeName == "block") {
                        if (planeManager.checkNamedPlaneExists("block")) {
                            planeManager.toggleBlockVisbility();
                        } else {
                            IJ.log("Block plane not initialised");
                        }
                    }
                } else {
                    IJ.log("Can only toggle visiblity, when not tracking a plane");
                }
            });

            panel.add(visbilityButton);
        }

        private void addPlaneToPanel(String planeName) {

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            panel.add(Box.createHorizontalGlue());

            JLabel sourceNameLabel = new JLabel(planeName);
            sourceNameLabel.setHorizontalAlignment(SwingUtilities.CENTER);

            int[] buttonDimensions = new int[]{50, 30};

            panel.add(sourceNameLabel);

            addColorButton(panel, buttonDimensions, planeName);
            addTransparencyButton(panel, buttonDimensions, planeName);
            addVisibilityButton(panel, buttonDimensions, planeName);
            addTrackingButton(panel, buttonDimensions, planeName);
            addGOTOButton(panel, buttonDimensions, planeName);

            add(panel);
            refreshGui();
        }

        private void refreshGui() {
            this.revalidate();
            this.repaint();
        }

        public void addTransparencyButton(JPanel panel, int[] buttonDimensions,
                                                String planeName) {
            JButton button = new JButton("T");
            button.setPreferredSize(new Dimension(
                    buttonDimensions[0],
                    buttonDimensions[1]));

            button.addActionListener(e ->
            {
                if (planeManager.getTrackPlane() == 0) {
                    JFrame frame = new JFrame("Transparency");
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                    float currentTransparency = 0.7f;
                    if (planeName.equals("target")) {
                        if (planeManager.checkNamedPlaneExists("target")) {
                            currentTransparency = planeManager.getTargetTransparency();
                        } else {
                            IJ.log("Target plane not initialised");
                        }
                    } else if (planeName.equals("block")) {
                        if (planeManager.checkNamedPlaneExists("block")) {
                            currentTransparency = planeManager.getBlockTransparency();
                        } else {
                            IJ.log("Block plane not initialised");
                        }
                    }

//                as here https://github.com/tischi/imagej-utils/blob/b7bdece786c1593969ec469916adf9737a7768bb/src/main/java/de/embl/cba/bdv/utils/BdvDialogs.java
                    final BoundedValueDouble transparencyValue =
                            new BoundedValueDouble(
                                    0,
                                    1,
                                    currentTransparency);

                    double spinnerStepSize = 0.1;

                    JPanel transparencyPanel = new JPanel();
                    transparencyPanel.setLayout(new BoxLayout(transparencyPanel, BoxLayout.PAGE_AXIS));
                    final SliderPanelDouble transparencySlider = new SliderPanelDouble("Transparency", transparencyValue, spinnerStepSize);
                    transparencySlider.setNumColummns(7);
                    transparencySlider.setDecimalFormat("####E0");
                    transparencyValue.setUpdateListener(new TransparencyUpdateListener(transparencyValue,
                            transparencySlider, planeName, planeManager));

                    transparencyPanel.add(transparencySlider);
                    frame.setContentPane(transparencyPanel);

                    //Display the window.
                    frame.setBounds(MouseInfo.getPointerInfo().getLocation().x,
                            MouseInfo.getPointerInfo().getLocation().y,
                            120, 10);
                    frame.setResizable(false);
                    frame.pack();
                    frame.setVisible(true);
                } else {
                    IJ.log("Can only change transparency of plane, when not tracking a plane");
                }
            });

            panel.add(button);
        }

        public class TransparencyUpdateListener implements BoundedValueDouble.UpdateListener {
            final private BoundedValueDouble transparencyValue;
            private final SliderPanelDouble transparencySlider;
            private final String planeName;
            PlaneManager planeManager;

            public TransparencyUpdateListener(BoundedValueDouble transparencyValue,
                                              SliderPanelDouble transparencySlider,
                                              String planeName,
                                              PlaneManager planeManager) {
                this.transparencyValue = transparencyValue;
                this.transparencySlider = transparencySlider;
                this.planeName = planeName;
                this.planeManager = planeManager;
            }

            @Override
            public void update() {
                transparencySlider.update();
                if (planeName.equals("target")) {
                    planeManager.setTargetTransparency((float) transparencyValue.getCurrentValue());
                } else if (planeName.equals("block")) {
                    planeManager.setBlockTransparency((float) transparencyValue.getCurrentValue());
                }
            }
        }

    }
