package com.marklipson.astrologyclock.unused;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.StringTokenizer;

/**
 * Simple interface to geocoder.us web service.
 */
public class GeoCoder
{
    private static URL docBase = null;
//    private final static String DOMAIN = "http://geocoder.us/";

    public static void setDocumentBase( URL _docBase )
    {
        docBase = _docBase;
    }
    public static double[] lookupCityState( String place )
    {
        try
        {
            place = place.trim();
            String city;
            String state;
            int p = place.indexOf( ',' );
            if (p == -1)
                p = place.lastIndexOf( ' ' );
            if (p != -1)
            {
                city = place.substring( 0, p ).trim();
                state = place.substring( p + 1 ).trim();
            }
            else
                return null;
            URL domain = docBase;
            URL url = new URL( domain, "geocode.php?city=" + URLEncoder.encode( city, "UTF-8" ) + "&state=" + URLEncoder.encode( state, "UTF-8" ) );
            InputStream in = (InputStream)url.getContent();
            InputStreamReader r = new InputStreamReader( in );
            char buf[] = new char[ 2000 ];
            int nr = r.read( buf );
            String content = new String( buf, 0, nr );
            StringTokenizer tok = new StringTokenizer( content, "," );
            if (tok.countTokens() < 2)
                return null;
            String strLat = tok.nextToken();
            String strLng = tok.nextToken();
            return new double[] { Double.parseDouble( strLat ), - Double.parseDouble( strLng ) };
        }
        catch( NumberFormatException x )
        {
            return null;
        }
        catch( MalformedURLException x )
        {
            return null;
        }
        catch( IOException x )
        {
            return null;
        }
    }
    
    public static double[] lookupAddress( String place )
    {
        try
        {
            URL domain = docBase;
            URL url = new URL( domain, "geocode.php?address=" + URLEncoder.encode( place, "UTF-8" ) );
            InputStream in = (InputStream)url.getContent();
            InputStreamReader r = new InputStreamReader( in );
            char buf[] = new char[ 2000 ];
            int nr = r.read( buf );
            String content = new String( buf, 0, nr );
            String strLng = findTag( content, "<geo:long>", "</geo:long>" );
            String strLat = findTag( content, "<geo:lat>", "</geo:lat>" );
            if (strLat == null  ||  strLng == null)
                return null;
            return new double[] { Double.parseDouble( strLat ), - Double.parseDouble( strLng ) };
        }
        catch( NumberFormatException x )
        {
            return null;
        }
        catch( MalformedURLException x )
        {
            return null;
        }
        catch( IOException x )
        {
            return null;
        }
    }
    private static String findTag( String str, String start, String end )
    {
        int p1 = str.indexOf( start );
        if (p1 == -1)
            return null;
        p1 += start.length();
        int p2 = str.indexOf( end );
        if (p2 == -1)
            return null;
        return str.substring( p1, p2 ).trim();
    }
}
