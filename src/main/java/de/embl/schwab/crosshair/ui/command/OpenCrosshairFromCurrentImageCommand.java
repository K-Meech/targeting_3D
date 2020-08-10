package de.embl.schwab.crosshair.ui.command;

import de.embl.schwab.crosshair.Crosshair;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Open Current Image" )
public class OpenCrosshairFromCurrentImageCommand implements Command
{
    @Parameter
    public ImagePlus imagePlus;

    @Override
    public void run()
    {
        new Crosshair(imagePlus);
    }

    public static void main( String[] args )
    {

    }
}
