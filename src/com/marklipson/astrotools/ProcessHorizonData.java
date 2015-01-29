package com.marklipson.astrotools;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.marklipson.astrologyclock.ChartData;

/**
 * Process data from the JPL Horizons web site:
 *   http://ssd.jpl.nasa.gov/horizons.cgi#top
 */
public class ProcessHorizonData
{
    private String php16( double val )
    {
      return String.format( "%15.9e\n",  val );
    }
    private String php8( double val )
    {
      if (val < 0)
        val = 0;
      return String.format( "%7.3f\n",  val );
    }
    public void convert( Reader source, String objectName, String outputFile, boolean phpMode )
    {
        try
        {
            LineNumberReader in = new LineNumberReader( source );
            String line;
            // find start of data
            while ((line = in.readLine()) != null)
            {
                if (line.equals( "$$SOE" ))
                    break;
            }
            FileOutputStream outStream = new FileOutputStream( outputFile );
            DataOutputStream ephemOut = new DataOutputStream( outStream );
            int row = 0;
            double t0 = 0, t1 = 0;
            double dt = 0;
//          Date_________JDUT, , ,           delta,     deldot,    ObsEcLon,   ObsEcLat,
//          2415385.500000000, , ,.002476073141918,  0.0099997,  48.3161389,  1.1700907,
            while ((line = in.readLine()) != null)
            {
                if (line.equals( "$$EOE" ))
                    break;
                String[] parts = line.split( "," );
                if (parts.length != 7)
                {
                    System.err.println( "skipping line " + in.getLineNumber() );
                    continue;
                }
                double JD = Double.parseDouble( parts[0] );
                @SuppressWarnings( "unused" )
                double r = Double.parseDouble( parts[3] );
                double a = Double.parseDouble( parts[5] );
                @SuppressWarnings( "unused" )
                double b = Double.parseDouble( parts[6] );
                if (row == 0)
                {
                    // write header
                    if (phpMode)
                    {
                        ephemOut.writeBytes( php16( JD ) );
                        ephemOut.writeBytes( php16( 1 ) );
                        ephemOut.writeBytes( php16( JD + 1000 ) );
                        ephemOut.writeBytes( php16( 0 ) );
                    }
                    else
                    {
                        String paddedName = objectName.toUpperCase();
                        while (paddedName.length() < 16)
                            paddedName += " ";
                        ephemOut.writeBytes( paddedName );
                        ephemOut.writeDouble( JD );  // start time
                        ephemOut.writeDouble( 1.0 ); // placeholder for interval
                        ephemOut.writeDouble( JD + 1000 ); // placeholder for end time
                        ephemOut.writeBytes( "        " ); // extra space
                        ephemOut.writeBytes( "        " ); // extra space
                        ephemOut.writeBytes( "        " ); // extra space
                    }
                    t0 = JD;
                }
                if (row == 1)
                    dt = JD - t0;
                t1 = JD;
                // write row data
                if (phpMode)
                    ephemOut.writeBytes( php8( a ) );
                else
                    ephemOut.writeFloat( (float)a );
                //ephemOut.writeFloat( (float)b );
                //ephemOut.writeFloat( (float)r );
                row ++;
            }
            in.close();
            ephemOut.close();
            
            // update header - patch in dt and end time
            RandomAccessFile ephemHdr = new RandomAccessFile( outputFile, "rw" );
            if (phpMode)
            {
              ephemHdr.seek( 16 );
              ephemHdr.writeBytes( php16( dt ) );
              ephemHdr.writeBytes( php16( t1 ) );
            }
            else
            {
                ephemHdr.seek( 24 );
                ephemHdr.writeDouble( dt );
                ephemHdr.writeDouble( t1 );
            }
            ephemHdr.close();
        }
        catch( IOException x )
        {
            x.printStackTrace( System.err );
        }
    }

    public void convertElements( Reader source, String objectName, String outputFile )
      {
          Pattern jdPtn = Pattern.compile( "^([0-9\\.]+) =" );
          Pattern varsPtn = Pattern.compile( "\\s([A-Z]+)\\s?=\\s+([0-9\\.E\\-\\+]+)" );
          try
          {
              LineNumberReader in = new LineNumberReader( source );
              String line;
              // find start of data
              while ((line = in.readLine()) != null)
              {
                  if (line.equals( "$$SOE" ))
                      break;
              }
              DataOutputStream out = new DataOutputStream( new FileOutputStream( outputFile ) );
              // write samples of orbital elements
              for (;;)
              {
                // next line has JD up to '=' sign
                line = in.readLine();
                if (line.equals( "$$EOE" ))
                  break;
                Matcher jdMatcher = jdPtn.matcher( line );
                if (! jdMatcher.find())
                  continue;
                double jd = Double.parseDouble( jdMatcher.group( 1 ) );
                Map<String,Double> vars = new HashMap<String,Double>();
                // next lines have var = value, 3 per line, 4 lines
                for (int nLine=0; nLine < 4; nLine++)
                {
                  line = in.readLine();
                  Matcher matcher = varsPtn.matcher( line );
                  for (int nV=0; nV < 3; nV++)
                    if (matcher.find())
                    {
                      String varName = matcher.group( 1 );
                      double varValue = Double.parseDouble( matcher.group( 2 ) );
                      vars.put( varName, varValue );
                    }
                }
                // write record
                double W = vars.get("W");
                double OM = vars.get("OM");
                double L = vars.get("MA") + W + OM;
                out.writeDouble( jd );
                out.writeFloat( (float)(double)vars.get("A") );
                out.writeFloat( (float)(double)vars.get("EC") );
                out.writeFloat( (float)ChartData.mod2pi( (W + OM) * ChartData.d2r ) );
                out.writeFloat( (float)ChartData.mod2pi( OM * ChartData.d2r ) );
                out.writeFloat( (float)(vars.get("IN") * ChartData.d2r) );
                out.writeFloat( (float)ChartData.mod2pi( L * ChartData.d2r ) );
                out.writeFloat( (float)(double)(vars.get("N") * ChartData.d2r * 36525) );
                // AD?  tP = time of perihelion passage,  QR?  TA?  PR?
              }
              out.close();
          }
          catch( Exception x )
          {
            x.printStackTrace( System.err );
          }
    }

    /**
     * args: (-oe) (-php) inputFile objectName outputFile
     * 
     * input file is from JPL Horizons system with certain columns enabled (see sample line in code above)
     * object name is EARTH, MOON, etc.
     * 
     * output file is written in binary, 4 bytes per sample of longitude (or 12 bytes per sample of a/b/r)
     * 
     * Orbital element files have no header, just 36 byte records containing:
     *   double JD;  float A, E, peri, node, incl, mean longitude, mean daily motion;
     */
    public static void main(String[] args)
    {
        if (args.length < 3)
            return;
        int nArg = 0;
        boolean elements = false;
        boolean phpMode = false;
        if (args[nArg].equals( "-oe" ))
        {
          nArg ++;
          elements = true;
        }
        if (args[nArg].equals( "-php" ))
        {
          nArg ++;
          phpMode = true;
        }
        ProcessHorizonData processor = new ProcessHorizonData();
        try
        {
          if (elements)
            processor.convertElements( new FileReader( args[nArg] ), args[nArg+1], args[nArg+2] );
          else
            processor.convert( new FileReader( args[nArg] ), args[nArg+1], args[nArg+2], phpMode );
        }
        catch( IOException x )
        {
            x.printStackTrace( System.err );
        }
    }

}
