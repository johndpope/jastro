package com.marklipson.astrologyclock;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * Very simple in-memory atlas based on US census data.
 *
 * TODO needs international cities.
 */
public class MiniAtlas
{
    // there is only one atlas instance at a time
    private static MiniAtlas instance;
    // atlas content is stored in memory
    static public class Entry implements Cloneable, Comparable<Entry>
    {
        public String city;
        public String state;        // contains either state or country
        public float lat, lng;
        public int population;
        private float relevance;
        public String toString()
        {
            return city + ", " + state;
        }
        public Object clone()
        {
            try
            {
                return super.clone();
            }
            catch( CloneNotSupportedException x )
            {
                return null;
            }
        }
        // default sort is by relevance
        public int compareTo(Entry other)
        {
            if (relevance < other.relevance)
                return 1;
            if (relevance > other.relevance)
                return -1;
            return 0;
        }
        public String getState()
        {
          if (state == null)
            return null;
          if (state.length() == 2  &&  ! state.equals( "UK" ))
            return state;
          return null;
        }
        public String getCountry()
        {
          if (state == null)
            return null;
          return (state.length() == 2  &&  ! state.equals( "UK" )) ? null : state;
        }
    }
    private List<Entry> entries = new ArrayList<Entry>( 10000 );

    // this index helps narrow down entries based on the first few letters of
    private Map<String,List<Entry>> cityStartsWith = new HashMap<String,List<Entry>>();
    // this index covers states, provinces and countries
    private Map<String,List<Entry>> stateIndex = new HashMap<String,List<Entry>>();

    /**
     * An index is maintained of the first few letters of city names.  Supply this many characters
     * of city name to take advantage of this feature.
     */
    public final static int MIN_FIRST_CITY_LETTERS = 2;
    /**
     * The first-letter combinations are indexed up to this length.
     */
    private final static int MAX_FIRST_CITY_LETTERS = 4;

    /**
     * Get/create the singleton instance.
     */
    public static synchronized MiniAtlas getInstance()
    {
        if (instance == null)
            instance = new MiniAtlas();
        return instance;
    }
    
    /**
     * Construction.
     */
    private MiniAtlas()
    {
        if (! load_US( true ))
          load_US( false );
        load_intl();
    }

    /**
     * Look up a given city and/or state.  Matches are initially sorted by relevance.
     */
    public Entry[] lookup( String place )
    {
      return lookup( place, true, true, true, false );
    }
    public Entry[] lookup( String place, boolean allowPartial, boolean useSoundex, boolean sortResults, boolean useStateIndex )
    {
        place = place.trim().toLowerCase();
        String city = "", state = "", citySoundex;
        int pComma = place.indexOf( ',' );
        if (pComma == -1)
        {
            city = place;
        }
        else
        {
            city = place.substring( 0, pComma ).trim();
            state = place.substring( pComma + 1 ).trim();
        }
        citySoundex = soundex( city );
        if (city.length() < MIN_FIRST_CITY_LETTERS  ||  citySoundex.equals( "" ))
            return null;
        // scan for results
        // first, narrow down with an index
        List<Entry> candidates = null;
        if (useStateIndex)
          candidates = stateIndex.get( state.toLowerCase() );
        if (candidates == null)
        {
          String cityFirstLetters = city.substring( 0, Math.min(city.length(),MAX_FIRST_CITY_LETTERS) ).toLowerCase();
          candidates = cityStartsWith.get( cityFirstLetters );
        }
        if (candidates == null)
          return null;
        // now scan for results
        List<Entry> results = new ArrayList<Entry>();
        for (Entry entry : candidates)
        {
            // state must match if specified
            if (state.length() > 0  &&  ! entry.state.equalsIgnoreCase( state ))
                continue;
            // exact match on city?
            Entry match = null;
            if (city.equalsIgnoreCase( entry.city ))
            {
                match = (Entry)entry.clone();
                match.relevance = 1;
            }
            // partial match on city?
            else if (allowPartial  &&  entry.city.toLowerCase().startsWith( city ))
            {
                match = (Entry)entry.clone();
                match.relevance = 0.8f;
            }
            // soundex on city?
            else if (useSoundex  &&  citySoundex.equals( soundex( entry.city ) ))
            {
                match = (Entry)entry.clone();
                match.relevance = 0.6f;
            }
            if (match != null)
            {
                // factor in population as a value less than 0.1
                match.relevance += (float)Math.log(entry.population) / 200;
                results.add( match );
            }
        }
        if (results.size() == 0)
            return null;
        // sort and return as array
        if (sortResults)
          Collections.sort( results );
        return (Entry[])results.toArray( new Entry[ results.size() ] );
    }

    // overly simple soundex
    public static String soundex( String in )
    {
        in = in.toLowerCase();
        StringBuffer out = new StringBuffer();
        for (int n=0; n < in.length(); n++)
        {
            char ch = in.charAt( n );
            // remove all spaces, punctuation, etc.
            if (! Character.isLetter( ch ))
                continue;
            // treat all vowels the same
            if ("aeiouy".indexOf( ch ) != -1)
            {
                if (out.length() == 0  ||  out.charAt( out.length() - 1 ) != '-')
                    out.append( '-' );
                continue;
            }
            // skip doubled letters
            if (n > 0  &&  in.charAt( n-1 ) == ch)
                continue;
            // include consonants, etc.
            out.append( ch );
        }
        return out.toString();
    }
    
    // NOTE: US data came from here: http://www.census.gov/geo/www/gazetteer/places2k.html
    // it was processed through ../astrotools/ProcessAtlas

    /**
     * Load content from resource.
     */
    private boolean load_US( boolean large )
    {
        //long t0 = System.currentTimeMillis();
        try
        {
            InputStream in0 = MiniAtlas.class.getResourceAsStream( "resources/atlas/us-cities_" + (large?"lg":"sm") + ".txt.zip" );
            ZipInputStream in0a = new ZipInputStream( in0 );
            in0a.getNextEntry();
            BufferedInputStream in1 = new BufferedInputStream( in0a, 32768 );
            LineNumberReader in = new LineNumberReader( new InputStreamReader( in1 ) );
            String line;
            String parts[] = new String[6];
            while ((line = in.readLine()) != null)
            {
                // NOTE: quickSplit() brought load time from 540 down to 410ms
                //String[] parts = line.split( "," );
                int nParts = quickSplit( line, ',', parts );
                if (nParts == 6)
                {
                    parts[1] = parts[1] + ", " + parts[2];
                    parts[2] = parts[3];
                    parts[3] = parts[4];
                    parts[4] = parts[5];
                }
                if (parts.length != 5  &&  parts.length != 6)
                {
                    System.err.println( "skipping line " + in.getLineNumber() );
                    continue;
                }
                String city = parts[1];
                  // TODO re-run the atlas processing code - this processing is already done there
                  city = unsuffix( city, " municipality" );
                  city = unsuffix( city, " city and borough" );
                  city = unsuffix( city, " village" );
                  city = unsuffix( city, " borough" );
                Entry entry = new Entry();
                entry.city = city;
                entry.state = parts[0].intern();
                entry.population = Integer.parseInt( parts[2] );
                entry.lat = Float.parseFloat( parts[4] );
                entry.lng = -Float.parseFloat( parts[3] );
                entries.add( entry );
                // index it
                addToIndexes( entry );
            }
        }
        catch( IOException x )
        {
            System.err.println( "unable to load US atlas (" + (large?"large":"small") + "): " + x.getMessage() );
            return false;
        }
        return true;
        //System.out.println( "loaded US, took " + (System.currentTimeMillis() - t0) );
    }
    private int quickSplit( String str, char divider, String[] output )
    {
        int n = 0;
        int p0 = 0;
        while (n < output.length)
        {
            int p1 = str.indexOf( divider, p0 );
            if (p1 == -1)
            {
                output[n++] = str.substring( p0 );
                break;
            }
            else
                output[n++] = str.substring( p0, p1 );
            p0 = p1 + 1;
        }
        return n;
    }
    /**
     * Load international data from resource.
     */
    private boolean load_intl()
    {
        try
        {
            InputStream in0 = MiniAtlas.class.getResourceAsStream( "resources/atlas/intl-cities_sm.csv" );
            BufferedInputStream in1 = new BufferedInputStream( in0, 16384 );
            LineNumberReader in = new LineNumberReader( new InputStreamReader( in1 ) );
            String line;
            String parts[] = new String[5];
            while ((line = in.readLine()) != null)
            {
                int nParts = quickSplit( line, ',', parts );
                if (nParts != 4  &&  nParts != 5)
                {
                    System.err.println( "skipping line " + in.getLineNumber() );
                    continue;
                }
                Entry entry = new Entry();
                entry.city = parts[0];
                entry.state = parts[1];
                entry.lat = Float.parseFloat( parts[2] );
                entry.lng = Float.parseFloat( parts[3] );;
                entry.population = (nParts == 5) ? Integer.parseInt( parts[4] ) : 500000;
                entries.add( entry );
                // index it
                addToIndexes( entry );
            }
            return true;
        }
        catch( IOException x )
        {
            x.printStackTrace( System.err );
            return false;
        }
    }
    
    /**
     * Add an index entry to all appropriate indexes.
     */
    private void addToIndexes( Entry entry )
    {
      String cityLC = entry.city.toLowerCase();
      // city-starts-with index
      for (int letters=MIN_FIRST_CITY_LETTERS; letters <= MAX_FIRST_CITY_LETTERS; letters++)
      {
        // don't add short names to index
        if (letters > entry.city.length())
          break;
        // find or establish correct list
        String cityLetters = cityLC.substring( 0, letters );
        List<Entry> list = cityStartsWith.get( cityLetters );
        if (list == null)
          cityStartsWith.put( cityLetters, list = new ArrayList<Entry>( (letters <= 2) ? 50 : (letters==3)?20 : 5) );
        list.add( entry );
      }
      // state/country index
      if (entry.state != null)
      {
        String key = entry.state.toLowerCase();
        List<Entry> list = stateIndex.get( key );
        if (list == null)
          stateIndex.put( key, list = new ArrayList<Entry>( (entry.getState() != null) ? 300 : 10 ) );
        list.add( entry );
      }
    }

    static private String unsuffix( String str, String suffix )
    {
        if (str.endsWith( suffix ))
            return str.substring( 0, str.length() - suffix.length() );
        else
            return str;
    }
}
