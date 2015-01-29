package com.marklipson.astrotools;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class ProcessAtlas
{
   // http://www.census.gov/geo/www/gazetteer/places2k.html
   /*
    * The place file contains data for all Incorporated and Census Designated places in the 50 states, the District of Columbia and Puerto Rico as of the January 1, 2000. The file is plain ASCII text, one line per record.
    *
    * Columns 1-2: United States Postal Service State Abbreviation
    * Columns 3-4: State Federal Information Processing Standard (FIPS) code
    * Columns 5-9: Place FIPS Code
    * Columns 10-73: Name
    * Columns 74-82: Total Population (2000)
    * Columns 83-91: Total Housing Units (2000)
    * Columns 92-105: Land Area (square meters) - Created for statistical purposes only.
    * Columns 106-119: Water Area(square meters) - Created for statistical purposes only.
    * Columns 120-131: Land Area (square miles) - Created for statistical purposes only.
    * Columns 132-143: Water Area (square miles) - Created for statistical purposes only.
    * Columns 144-153: Latitude (decimal degrees) First character is blank or "-" denoting North or South latitude respectively
    * Columns 154-164: Longitude (decimal degrees) First character is blank or "-" denoting East or West longitude respectively 
    */

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            InputStream in0 = ProcessAtlas.class.getResourceAsStream( "census-bureau-cities.txt" );
            BufferedInputStream in1 = new BufferedInputStream( in0, 16384 );
            LineNumberReader in = new LineNumberReader( new InputStreamReader( in1 ) );
            String line;
            while ((line = in.readLine()) != null)
            {
                if (line.length() < 163)
                {
                    System.err.println( "skipping line " + in.getLineNumber() );
                    continue;
                }
                String state = line.substring( 0, 2 );
                String city = line.substring( 9, 73 ).trim();
                String sPop = line.substring( 73, 82 ).trim();
                String sLongitude = line.substring( 143, 153 ).trim();
                String sLatitude = line.substring( 153, 164 ).trim();
                int pop = Integer.parseInt( sPop );
                double lng = Double.parseDouble( sLongitude );
                double lat = Double.parseDouble( sLatitude );
                city = unsuffix( city, " (balance)" );
                city = unsuffix( city, " city" );
                city = unsuffix( city, " village" );
                city = unsuffix( city, " borough" );
                city = unsuffix( city, " municipality" );
                city = unsuffix( city, " city and borough" );
                city = unsuffix( city, " town" );
                city = unsuffix( city, " CDP" );
                city = unsuffix( city, " zona urbana" );
                city = unsuffix( city, " comunidad" );
                boolean csvOutput = true;
                if (csvOutput)
                    // CSV
                    System.out.println( state + "," + city + "," + pop + "," + lat + "," + lng );
                else
                {
                    // SQL
                    System.out.println( "INSERT INTO atlas (state,city,population,latitude,longitude) VALUES" );
                    System.out.println( "('" + state + "','" + enquote(city) + "'," + pop + "," + lat + "," + lng + ");" );
                }
            }
            //System.out.println( ";" );
        }
        catch( IOException x )
        {
            x.printStackTrace( System.err );
        }
    }
    
    static private String unsuffix( String str, String suffix )
    {
        if (str.endsWith( suffix ))
            return str.substring( 0, str.length() - suffix.length() );
        else
            return str;
    }
    static public String enquote( String str )
    {
        int p = str.indexOf( '\'' );
        if (p == -1)
            return str;
        StringBuffer out = new StringBuffer( str.length() + 2 );
        for (int n=0; n < str.length(); n++)
        {
            char ch = str.charAt( n );
            if (ch == '\'')
                out.append( "\\'" );
            else
                out.append( ch );
        }
        return out.toString();
    }

}
