package de.embl.schwab.crosshair.ui.swing;

import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.io.*;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.PlaneManager;
import ij3d.Content;

import javax.swing.*;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import static de.embl.schwab.crosshair.io.IoHelper.chooseOpenFilePath;
import static de.embl.schwab.crosshair.io.IoHelper.chooseSaveFilePath;

public class SavePanel extends CrosshairPanel {
    private PlaneManager planeManager;
    private MicrotomeManager microtomeManager;
    private Content imageContent;
    private MicrotomePanel microtomePanel;
    private OtherPanel otherPanel;
    private ImagesPanel imagesPanel;
    private JButton saveSolution;
    private JButton saveSettings;
    private JButton loadSettings;
    private CrosshairFrame crosshairFrame;


    public SavePanel() {}

    public void initialisePanel( CrosshairFrame crosshairFrame ) {
        this.crosshairFrame = crosshairFrame;
        this.planeManager = crosshairFrame.getPlaneManager();
        this.microtomeManager = crosshairFrame.getMicrotomeManager();
        this.imageContent = crosshairFrame.getImageContent();
        this.microtomePanel = crosshairFrame.getMicrotomePanel();
        this.otherPanel = crosshairFrame.getPointsPanel();
        this.imagesPanel = crosshairFrame.getImagesPanel();

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Save and Load"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        setLayout(new GridLayout(1, 3));

        ActionListener saveLoadListener = new saveLoadListener();

        saveSettings = new JButton("Save Settings");
        saveSettings.setActionCommand("save_settings");
        saveSettings.addActionListener(saveLoadListener);
        add(saveSettings);
        PlanePanel planePanel = crosshairFrame.getPlanePanel();
        planePanel.addButtonAffectedByTracking( Crosshair.target, saveSettings );
        planePanel.addButtonAffectedByTracking( Crosshair.block, saveSettings );

        loadSettings = new JButton("Load Settings");
        loadSettings.setActionCommand("load_settings");
        loadSettings.addActionListener(saveLoadListener);
        add(loadSettings);
        planePanel.addButtonAffectedByTracking( Crosshair.target, loadSettings );
        planePanel.addButtonAffectedByTracking( Crosshair.block, loadSettings );

        saveSolution = new JButton("Save Solution");
        saveSolution.setActionCommand("save_solution");
        saveSolution.addActionListener(saveLoadListener);
        saveSolution.setEnabled(false);
        add(saveSolution);
    }

    void enableSaveSolution() {
        saveSolution.setEnabled(true);
    }

    void disableSaveSolution() {
        saveSolution.setEnabled(false);
    }

    class saveLoadListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("save_settings")) {

                String filePath = chooseSaveFilePath();
                if ( filePath != null ) {
                    SettingsWriter writer = new SettingsWriter();
                    Settings settings = writer.createSettings( planeManager, imagesPanel.getImageNameToContent() );
                    writer.writeSettings( settings, filePath );
                }

            } else if (e.getActionCommand().equals("load_settings")) {

                String filePath = chooseOpenFilePath();
                if ( filePath != null ) {
                    SettingsReader reader = new SettingsReader();
                    Settings settings = reader.readSettings( filePath );
                    reader.loadSettings( settings, microtomeManager, microtomePanel, planeManager,
                            imagesPanel.getImageNameToContent(), otherPanel );
                }

            } else if (e.getActionCommand().equals("save_solution")) {
                if ( microtomeManager.isValidSolution() ) {
                    String filePath = chooseSaveFilePath();

                    if ( filePath != null ) {
                        // if you set a solution then move other sliders, it will no longer be a solution. Here we force it
                        // to react and set all sliders for that current solution.
                        double currentSolutionRot = microtomePanel.getRotationSolutionAngle().getCurrentValue();
                        microtomePanel.getRotationSolutionAngle().setCurrentValue(currentSolutionRot);

                        Solution solution = microtomeManager.getCurrentSolution();
                        new SolutionWriter( solution, filePath ).writeSolution();
                    }
                } else {
                    JOptionPane.showMessageDialog(null,
                            "No valid solutions for this rotation. Try another rotation, or revise your target plane.");
                }
            }
        }
    }
}
