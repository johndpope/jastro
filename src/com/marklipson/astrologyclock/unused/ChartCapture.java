package com.marklipson.astrologyclock.unused;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import com.marklipson.astrologyclock.ChartWheel;
import com.marklipson.astrologyclock.TimeAdjuster;

/**
 * Captures chart content and saves it to a file.
 * 
 * Use this switch when running on Linux:
 *   -Djava.awt.headless=true
 */
public class ChartCapture
{
    public static void captureChart( String strTime, String strZone, double latitude, double longitude, String options, String outputFile ) throws Exception
    {
        // get time and zone
        DateFormat timeFormat = TimeAdjuster.getTimeFormat();
        if (strZone != null)
            timeFormat.setTimeZone( TimeZone.getTimeZone( strZone ) );
        double jd = ChartWheel.systemTime_to_JD( timeFormat.parse( strTime ).getTime() );
        // set up to draw chart
        int radius = 600;
        URL imageLocation = ChartCapture.class.getResource("resources/sun.gif");
        ChartWheel ss = new ChartWheel( null, imageLocation );
        ss.setJD( jd );
        ss.setDisplayInfo( options );
        ss.setPlace( latitude * ChartWheel.d2r, longitude * ChartWheel.d2r );
        BufferedImage image = new BufferedImage( radius, radius, BufferedImage.TYPE_4BYTE_ABGR );
        ss.paintBuffer( image );
        if (outputFile == null)
        {
          if (! ImageIO.write( image, "PNG", System.out ))
            throw new IOException( "No image writer found for PNG" );
        }
        else
        {
          if (! ImageIO.write( image, "PNG", new File( outputFile ) ))
              throw new IOException( "No image writer found for PNG" );
        }
    }
    
    public static void main( String args[] )
    {
        int n = 0;
        String time = args[n++];
        String zone = args[n++];
        double lat = Double.parseDouble( args[n++] );
        double lng = Double.parseDouble( args[n++] );
        String opts = args[n++];
        // grab chart
        try
        {
            captureChart( time, zone, lat, lng, opts, null );
        }
        catch( Exception x )
        {
            x.printStackTrace( System.err );
        }
    }
}
