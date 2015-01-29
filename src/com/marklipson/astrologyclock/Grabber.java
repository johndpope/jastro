package com.marklipson.astrologyclock;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

import javax.imageio.ImageIO;

/**
 * Grabs chart content and posts to a file.
 * 
 * Use this switch when running on Linux:
 *   -Djava.awt.headless=true
 */
public class Grabber
{
    public static void grabChart( Date date, boolean showHouses, double longitude, double latitude, File outputFile, String placeName ) throws IOException
    {
        int radius = 500;
        URL imageLocation = Grabber.class.getResource("resources/sun.gif");
        ChartWheel ss = new ChartWheel( null, imageLocation );
        ss.setDate( date );
        ss.setPlace( latitude * ChartWheel.d2r, longitude * ChartWheel.d2r );
        ss.setEnableChartRotation( showHouses );
        BufferedImage image = new BufferedImage( radius, radius, BufferedImage.TYPE_4BYTE_ABGR );
        ss.paintBuffer( image );
        /*
        BufferedImage imageHalf = new BufferedImage( radius/2, radius/2, BufferedImage.TYPE_4BYTE_ABGR );
        imageHalf.getGraphics().drawImage( image, 0, 0, radius/2, radius/2, null );
        if (! ImageIO.write( imageHalf, "PNG", outputHalf ))
            throw new IOException( "No image writer found for PNG" );
        */
        if (outputFile.getName().equals( "STDOUT" ))
        {
            if (! ImageIO.write( image, "PNG", System.out ))
                throw new IOException( "No image writer found for PNG" );
            else if (! ImageIO.write( image, "PNG", outputFile ))
                throw new IOException( "No image writer found for PNG" );
        }
    }
    
    
    public static void main( String args[] )
    {
        if (args.length > 0)
        {
            // argument are filename, timeMillis, latitudeDgrs, longitudeDgrs, showHouses
            String filename = args[0];
            String pT = (args.length >= 2) ? args[1] : null;
            String pLat = (args.length >= 3) ? args[2] : "40";
            String pLng = (args.length >= 4) ? args[3] : "105";
            String pHouses = (args.length >= 5) ? args[4] : "0";
            
            long t;
            double lat = 40, lng = 120;
            boolean houses = ! "0".equals( pHouses );
            if (pT == null  ||  pT.equals( "0" ))
                t = System.currentTimeMillis() / 1000;
            else
                t = Long.parseLong( pT );
            if (pLat != null)
                lat = Double.parseDouble( pLat );
            if (pLng != null)
                lng = Double.parseDouble( pLng );
            // grab chart
            try
            {
                grabChart( new Date( t * 1000 ), houses, lng, lat, new File( filename ), "" );
            }
            catch( IOException x )
            {
                x.printStackTrace( System.err );
            }
        }
        try
        {
            grabChart( new Date(), false, 0, 0, new File( "chart.png" ), "" );
            grabChart( new Date(), true, 123.0, 45.0, new File( "chart-PDX.png" ), "Portland, OR" );
            grabChart( new Date(), true, 105.0, 40.0, new File( "chart-DIA.png" ), "Denver, CO" );
            grabChart( new Date(), true, 122.7, 37.6, new File( "chart-SFO.png" ), "San Francisco, CA" );
            grabChart( new Date(), true, 118.0, 33.9, new File( "chart-LAX.png" ), "Los Angeles, CA" );
            grabChart( new Date(), true,  73.8, 40.7, new File( "chart-NYC.png" ), "New York, NY" );
            grabChart( new Date(), true,  75.2, 39.9, new File( "chart-PHL.png" ), "Philadelphia, PA" );
            grabChart( new Date(), true,  87.6, 41.9, new File( "chart-ORD.png" ), "Chicago, IL" );
            grabChart( new Date(), true,  96.9, 32.5, new File( "chart-DFW.png" ), "Dallas, TX" );
            grabChart( new Date(), true,  80.3, 25.5, new File( "chart-MIA.png" ), "Miami, FL" );
            grabChart( new Date(), true, 149.9, 61.1, new File( "chart-ANC.png" ), "Anchorage, AK" );
            grabChart( new Date(), true, 123.0, 47.8, new File( "chart-SEA.png" ), "Seattle, WA" );
        }
        catch( Exception x )
        {
            x.printStackTrace( System.err );
        }
    }
}
