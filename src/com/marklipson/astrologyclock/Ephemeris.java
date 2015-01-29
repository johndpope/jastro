package com.marklipson.astrologyclock;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.marklipson.astrologyclock.ChartData.OrbElems;

/**
 * Accessor for ephemeris data.
 * 
 * Data comes from JPL's Horizon system, and is processed by ProcessHorizonData.
 * 
 * @see com.marklipson.astrotools.ProcessHorizonData
 */
public class Ephemeris
{
    // data is stored in memory
    private static class TableBase
    {
        String objectName;
        double t0, dt, t1;
        TableBase nextTable;
    }
    private static class PosTable extends TableBase
    {
        float[] data;
    }
    private static class ElementTable extends TableBase
    {
        List<OrbElems> elems = new ArrayList<OrbElems>();
    }
    private Map<String,TableBase> planets = new HashMap<String,TableBase>();
    private Map<String,TableBase> elements = new HashMap<String,TableBase>();

    // single instance
    private static Ephemeris instance;
    public static synchronized Ephemeris getInstance()
    {
        if (instance == null)
            instance = new Ephemeris();
        return instance;
    }
    private Ephemeris()
    {
        loadTables();
    }
    
    /**
     * Load one binary table into memory.
     */
    private TableBase loadTable( String planetName, int index, boolean elemTable )
    {
        String filename = planetName;
        if (index > 0)
            filename += index;
        filename += elemTable ? ".elem" : ".lng";
        try
        {
            Map<String, TableBase> tableSet = elemTable ? elements : planets;
            InputStream inRaw = Ephemeris.class.getResourceAsStream( "resources/ephemeris/" + filename );
            if (inRaw == null)
              throw new IOException( "resource not found" );
            DataInputStream in = new DataInputStream( inRaw );
            TableBase table;
            if (elemTable)
            {
              // orbital elements - no header, just read orbital element entries
              ElementTable eTable = new ElementTable();
              table = eTable;
              for (;;)
              {
                if (in.available() <= 0)
                  break;
                OrbElems oe = new OrbElems();
                oe.refFrame = in.readDouble();
                oe.a = in.readFloat();
                oe.e = in.readFloat();
                oe.peri = in.readFloat();
                oe.node = in.readFloat();
                oe.incl = in.readFloat();
                oe.lng = in.readFloat();
                oe.dLng = in.readFloat();
                eTable.elems.add( oe );
              }
              // capture time range information
              table.t0 = eTable.elems.get( 0 ).refFrame;
              table.t1 = eTable.elems.get( eTable.elems.size()-1 ).refFrame;
              if (eTable.elems.size() > 1)
                table.dt = (table.t1 - table.t0) / (eTable.elems.size()-1);
              table.objectName = planetName.toLowerCase();
            }
            else
            {
              // single value table
              PosTable posTable = new PosTable();
              table = posTable;
              // load header
              byte[] name = new byte[ 16 ];
              in.readFully( name );
              table.objectName = new String( name ).trim().toLowerCase();
              table.t0 = in.readDouble();
              table.dt = in.readDouble();
              table.t1 = in.readDouble();
              byte skip[] = new byte[ 24 ];
              in.readFully( skip );
              // load samples
              int nSamples = (int)((table.t1 - table.t0 - table.dt/2) / table.dt);
              posTable.data = new float[ nSamples ];
              for (int n=0; n < nSamples; n++)
                  posTable.data[n] = in.readFloat();
            }
            if (index <= 1)
                tableSet.put( table.objectName, table );
            else
            {
                // merge data with prior table
                TableBase prevTable = tableSet.get( table.objectName );
                while (prevTable.nextTable != null)
                    prevTable = prevTable.nextTable;
                if (! mergeTables( prevTable, table ))
                    prevTable.nextTable = table;
            }
            return table;
        }
        catch( IOException x )
        {
            System.err.println( filename + ": " + x.getMessage() );
            return null;
        }
    }
    /**
     * Merge 'b' into 'a'.
     * 
     * Merge is only implemented for single value tables, not orbital elements.
     */
    private boolean mergeTables( TableBase a, TableBase b )
    {
        if (a.dt != b.dt)
            return false;
        if (b.t0 > a.t1  ||  b.t0 < a.t0)
            return false;
        double sampleOffset = (b.t0 - a.t1) / a.dt;
        if (Math.abs( sampleOffset - Math.round( sampleOffset ) ) > a.dt/100000)
            return false;
        int bStart = (int)Math.round( sampleOffset );
        if (a instanceof PosTable  &&  b instanceof PosTable)
        {
          PosTable A = (PosTable)a;
          PosTable B = (PosTable)b;
          float[] newData = new float[ bStart + B.data.length ];
          System.arraycopy( newData, 0, A.data, 0, bStart );
          System.arraycopy( newData, bStart, B.data, 0, B.data.length );
          // Update the first table.
          A.data = newData;
        }
        a.t1 = b.t1;
        return true;
    }
    private void loadTables()
    {
        loadTable( "sun", 0, false );
        loadTable( "moon", 1, false );
        loadTable( "moon", 2, false );
        loadTable( "mercury", 0, false );
        loadTable( "venus", 0, false );
        loadTable( "mars", 0, false );
        loadTable( "jupiter", 0, false );
        loadTable( "saturn", 0, false );
        loadTable( "uranus", 0, false );
        loadTable( "neptune", 0, false );
        loadTable( "pluto", 0, false );
        loadTable( "chiron", 0, true );
        loadTable( "ceres", 0, true );
        loadTable( "sedna", 0, true );

        loadTable( "pluto", 0, true );
        loadTable( "neptune", 0, true );
        loadTable( "uranus", 0, true );
        loadTable( "saturn", 0, true );
        loadTable( "earth", 0, true );
    }
    
    /**
     * Find the table for a given named object.
     */
    private PosTable findTable( String objectName )
    {
        return (PosTable)planets.get( objectName.toLowerCase() );
    }
    /**
     * Find the orbital element table for a given named object.
     */
    private ElementTable findElementTable( String objectName )
    {
        return (ElementTable)elements.get( objectName.toLowerCase() );
    }

    /**
     * Do an orbital element lookup.
     */
    public OrbElems lookupOrbElems( String objectName, double JD )
    {
      // get table for planet and verify time range
      ElementTable table = findElementTable( objectName );
      if (table == null)
          return null;
      while (JD > table.t1  &&  table.nextTable != null)
          table = (ElementTable)table.nextTable;
      if (JD < table.t0  ||  JD >= table.t1 - table.dt)
          return null;
      // get the closest sample
      int sample0 = (int)Math.floor( (JD - table.t0) / table.dt );
      OrbElems oe = table.elems.get( sample0 );
      // TODO interpolate between elements
      return oe;
    }
    /**
     * Do a longitude lookup.
     */
    public Double lookupLongitude( String objectName, double JD )
    {
        // get table for planet and verify time range
        PosTable table = findTable( objectName );
        if (table == null)
            return null;
        while (JD > table.t1  &&  table.nextTable != null)
            table = (PosTable)table.nextTable;
        if (JD < table.t0  ||  JD >= table.t1 - table.dt)
            return null;
        // get the 3 closest samples
        int sample0 = (int)Math.floor( (JD - table.t0) / table.dt );
        if (sample0 + 2 >= table.data.length)
            return null;
        float a = table.data[sample0];
        float b = table.data[sample0+1];
        float c = table.data[sample0+2];
        // make them linear
        if (b < a - 180)
            b += 360;
        if (b > a + 180)
            b -= 360;
        if (c < b - 180)
            c += 360;
        if (c > b + 180)
            c -= 360;
        // find 2nd order polynomial curve
        float C = a;
        float A = (a-2*b+c)/2;
        float B = b - a - A;
        // interpolate
        double i = (JD - table.t0) / table.dt - sample0;
        double lng = A*i*i+B*i+C;
        if (lng < 0)
            lng += 360;
        if (lng > 360)
            lng -= 360;
        return new Double( lng * Math.PI/180 );
    }
}
