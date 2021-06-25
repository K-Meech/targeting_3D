package de.embl.schwab.crosshair.targetingaccuracy;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.io.Settings;
import de.embl.schwab.crosshair.io.SettingsReader;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.plane.PlaneSettings;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.type.numeric.ARGBType;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;
import static de.embl.schwab.crosshair.utils.Utils.spaceOutWindows;

public class TargetingAccuracy {

    public static final String before = "before";
    public static final String after = "after";
    public static final String beforeBlock = "before block";
    public static final String afterBlock = "after block";
    public static final String beforeTarget = "before target";

    public TargetingAccuracy ( File beforeTargetingXml, File registeredAfterTargetingXml, File crosshairJson ) {

        final LazySpimSource beforeSource = new LazySpimSource("before", beforeTargetingXml.getAbsolutePath());
        final LazySpimSource afterSource = new LazySpimSource("after", registeredAfterTargetingXml.getAbsolutePath());

        String beforeUnit = beforeSource.getVoxelDimensions().unit();
        String afterUnit = afterSource.getVoxelDimensions().unit();

        if (!beforeUnit.equals(afterUnit)) {
            throw new UnsupportedOperationException("before and after images don't use the same units");
        } else {

            BdvStackSource beforeStackSource = BdvFunctions.show(beforeSource, 1);
            beforeStackSource.setDisplayRange(0, 255);
            BdvStackSource afterStackSource = BdvFunctions.show(afterSource, 1, BdvOptions.options().addTo(beforeStackSource));
            afterStackSource.setDisplayRange(0, 255);

            Image3DUniverse universe = new Image3DUniverse();
            universe.show();

            Map<String, Content> imageNameToContent = new HashMap<>();
            Source[] sources = new Source[]{beforeSource, afterSource};
            String[] sourceNames = new String[]{TargetingAccuracy.before, TargetingAccuracy.after};
            for (int i = 0; i < sources.length; i++) {
                // Set to arbitrary colour
                ARGBType colour = new ARGBType(ARGBType.rgba(0, 0, 0, 0));
                Content imageContent = addSourceToUniverse(universe, sources[i], 300 * 300 * 300, Content.VOLUME, colour, 0.7f, 0, 255);
                // Reset colour to default for 3D viewer
                imageContent.setColor(null);
                imageContent.setLocked(true);
                imageContent.showPointList(true);
                universe.getPointListDialog().setVisible(false);

                imageNameToContent.put( sourceNames[i], imageContent );
            }

            // we use the before image content to define the extent of the planes. The before x-ray should be the largest,
            // and so give an extent that covers both comfortably
            PlaneManager planeManager = new PlaneManager(beforeStackSource, universe, imageNameToContent.get( TargetingAccuracy.before ));
            new AccuracyBdvBehaviours( beforeStackSource.getBdvHandle(), planeManager );

            TargetingAccuracyFrame accuracyFrame = new TargetingAccuracyFrame( universe, imageNameToContent, planeManager,
                    beforeStackSource.getBdvHandle(), beforeUnit );

            SettingsReader reader = new SettingsReader();
            Settings settings = reader.readSettings( crosshairJson.getAbsolutePath() );
            // rename the image from crosshair to 'before', so it displays nicely in the panel
            settings.imageSettings.get(0).name = TargetingAccuracy.before;
            // rename planes to nicer names for display
            for ( PlaneSettings planeSettings : settings.planeSettings ) {
                if ( planeSettings.name.equals(Crosshair.target) ) {
                    planeSettings.name = TargetingAccuracy.beforeTarget;
                } else if ( planeSettings.name.equals( Crosshair.block) ) {
                    planeSettings.name = TargetingAccuracy.beforeBlock;
                }
            }

            reader.loadSettings( settings, planeManager,
                    accuracyFrame.getImagesPanel().getImageNameToContent(), accuracyFrame.getOtherPanel() );

            spaceOutWindows(beforeStackSource.getBdvHandle(), accuracyFrame, universe);

        }
    }
}
