package de.embl.schwab.crosshair.ui.command;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import de.embl.schwab.crosshair.Crosshair;
import ij.IJ;
import ij.ImagePlus;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Open>Target Current Image" )
public class OpenCrosshairFromCurrentImageCommand implements Command
{
    @Parameter
    public ImagePlus imagePlus;

    @Override
    public void run()
    {
        Image3DUniverse universe = new Image3DUniverse();
        Content imageContent = universe.addContent(imagePlus, Content.VOLUME);
        imageContent.setTransparency(0.7F);
        universe.show();

        final double pw = imagePlus.getCalibration().pixelWidth;
        final double ph = imagePlus.getCalibration().pixelHeight;
        final double pd = imagePlus.getCalibration().pixelDepth;
        final String unit = imagePlus.getCalibration().getUnit();

        final Img wrap = ImageJFunctions.wrap(imagePlus);
        BdvStackSource bdvStackSource = BdvFunctions.show(wrap, "raw", Bdv.options()
                .sourceTransform(pw, ph, pd));
        // TODO - make generic? Not just 8 bit
        bdvStackSource.setDisplayRange(0, 255);

        new Crosshair(bdvStackSource, universe, imageContent, unit);

    }

    public static void main( String[] args )
    {
        // final String INPUT_IMAGE = "C:\\Users\\meechan\\Documents\\test_3d_larger_anisotropic\\test_3d_larger_anisotropic.tif";
        final String INPUT_IMAGE = "C:\\Users\\meechan\\Documents\\test_images\\Flipped_images_before.tif";
        ImagePlus imagePlus = IJ.openImage(INPUT_IMAGE);
        Image3DUniverse universe = new Image3DUniverse();
        Content imageContent = universe.addContent(imagePlus, Content.VOLUME);
        imageContent.setTransparency(0.7F);
        universe.show();

        final double pw = imagePlus.getCalibration().pixelWidth;
        final double ph = imagePlus.getCalibration().pixelHeight;
        final double pd = imagePlus.getCalibration().pixelDepth;
        final String unit = imagePlus.getCalibration().getUnit();

        final Img wrap = ImageJFunctions.wrap(imagePlus);
        BdvStackSource bdvStackSource = BdvFunctions.show(wrap, "raw", Bdv.options()
                .sourceTransform(pw, ph, pd));
        // TODO - make generic? Not just 8 bit
        bdvStackSource.setDisplayRange(0, 255);

        new Crosshair(bdvStackSource, universe, imageContent, unit);

    }
}
