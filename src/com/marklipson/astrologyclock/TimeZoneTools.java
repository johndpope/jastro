package com.marklipson.astrologyclock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class TimeZoneTools
{
  /**
   * Attempts to determine time zones return this structure.
   */
  public static class TimeZoneGuess
  {
    public TimeZone timezone;
    public static final int QUALITY_GOOD = 3;
    public static final int QUALITY_FAIR = 2;
    public static final int QUALITY_GUESS = 1;
    public static final int QUALITY_ESTIMATE = 0;
    public int quality;
    TimeZoneGuess( TimeZone tz, int quality )
    {
      timezone = tz;
      this.quality = quality;
    }
    TimeZoneGuess( TimeZone tz )
    {
      timezone = tz;
      this.quality = QUALITY_GOOD;
    }
    TimeZoneGuess( String id, int quality )
    {
      timezone = TimeZone.getTimeZone( id );
      this.quality = quality;
    }
    TimeZoneGuess( String id )
    {
      timezone = TimeZone.getTimeZone( id );
      this.quality = QUALITY_GOOD;
    }
  }

  static private List<TimeZone> timezones = new ArrayList<TimeZone>();
  static private Map<TimeZone,MiniAtlas.Entry> tzLocs = new HashMap<TimeZone,MiniAtlas.Entry>();
  static
  {
    // pull out all the timezones we plan to use
    String[] ids = TimeZone.getAvailableIDs();
    for (String id : ids)
    {
      try
      {
        // skip some of the obscure or technical ones and just keep the geographic ones
        if (id.startsWith( "Etc/" ))
          continue;
        if (id.startsWith( "Antarctica/" ))
          continue;
        if (id.length() == 7  &&  Character.isDigit( id.charAt( 3 ) ))
          continue;
        if (id.length() == 3)
          continue;
        TimeZone tz = TimeZone.getTimeZone( id );
        timezones.add( tz );
      }
      catch( Exception x )
      {
      }
    }
  }
  /**
   * Find the atlas entry and physical location associated with a timezone.
   * For instance, America/Denver should map to Denver, CO
   */
  static public MiniAtlas.Entry getTimeZoneLocation( TimeZone tz )
  {
    MiniAtlas.Entry entry = tzLocs.get( tz );
    if (entry == null)
    {
      entry = new MiniAtlas.Entry();
      // -- we can get the city if the name is $CONTINENT/$CITY
      String parts[] = tz.getID().split( "/" );
      if (parts.length == 2)
      {
        if (parts[1].equalsIgnoreCase( "east" )  ||  parts[1].equalsIgnoreCase( "atlantic" )  ||
            parts[1].equalsIgnoreCase( "west" )  ||  parts[1].equalsIgnoreCase( "central" )  ||
            parts[1].equalsIgnoreCase( "mountain" )  ||  parts[1].equalsIgnoreCase( "pacific" ))
          ;
        else
        {
          String city = parts[1].replace( '_', ' ' );
          boolean couldBeUSorCanada = parts[0].equals( "US" )  ||  parts[0].equals( "America" )  ||  parts[0].equals( "Canada" );
          boolean isUSorCanada = parts[0].equals( "US" ) || parts[0].equals( "Canada" );
          MiniAtlas.Entry matches[] = MiniAtlas.getInstance().lookup( city, true, false, false, false );
          if (matches != null)
          {
            MiniAtlas.Entry best = null;
            double vBest = -2;
            for (MiniAtlas.Entry possible : matches)
            {
              if (possible.getState() != null  &&  ! couldBeUSorCanada)
                continue;
              if (possible.getState() == null  &&  isUSorCanada)
                continue;
              int value = 0;
              double tzHours = -tz.getOffset(System.currentTimeMillis())/3600000.0;
              double lngHours = (possible.lng / 15);
              double hourDifference = Math.abs( tzHours - lngHours );
              if (hourDifference > 3)
                continue;
              // ascertain a value for this match
              // best if city matches exactly, but allow partial matches through
              if (possible.city.equalsIgnoreCase( city ))
                value += 7;
              else if (possible.city.equalsIgnoreCase( city + " City" ))
                value += 6;
              else
                value -= 2;
              // lower the urgency of matching these - there is 
              //  less data to correspond them with so they are more error prone
              //  (i.e. Dallas, TX -> Yukon, OK -> Canada/Yukon)
              if (parts[0].equals( "America" ))
                value -= 5;
              else if (parts[0].equals( "Canada" ))
                value -= 2;
              // extreme north cities are less likely to be relevant
              if (Math.abs( possible.lat ) > 62)
                value -= 3;
              // population is a very big factor
              if (possible.population > 600000)
                value += 9;
              else if (possible.population > 300000)
                value += 7;
              else if (possible.population > 150000)
                value += 5;
              else if (possible.population > 50000)
                value += 2;
              else if (possible.population > 25000)
                value -= 4;
              else if (possible.population > 0  &&  possible.population < 25000)
                value -= 9;
              // factor in how closely the timezone matches the longitude
              value -= (int)(hourDifference*2);
              // capture best match
              //System.out.println( "  " + tz.getID() + " ==> " + possible.city + ", " + possible.state + " (" + possible.population + "): " + value );
              if (value > vBest)
              {
                vBest = value;
                best = possible;
              }
            }
            if (best != null)
              entry = best;
          }
        }
      }
      // store the matched city (or the blank entry)
      tzLocs.put( tz, entry );
      //if (entry.city != null)
      //  System.out.println( "tz " + tz.getID() + " (" + tz.getDisplayName() + ") =>" + entry.city + ", " + entry.state + ": " + entry.lat + ", " + entry.lng + "\n" );
    }
    return (entry.city == null) ? null : entry;
  }
  
  /**
   * Guess a timezone given a place name.
   * 
   * @return name of zone, or null if no zone could be guessed.
   */
  public static TimeZoneGuess guessTimezone( MiniAtlas.Entry entry )
  {
    // check for a few easy cases
    if ("CA".equals( entry.state ))
      return new TimeZoneGuess( "America/Los_Angeles" );
    if ("OR".equals( entry.state )  ||  "WA".equals( entry.state ))
      return new TimeZoneGuess( "America/Los_Angeles", TimeZoneGuess.QUALITY_FAIR );
    if ("CO".equals( entry.state ))
      return new TimeZoneGuess( "America/Denver" );
    if ("NY".equals( entry.state ))
      return new TimeZoneGuess( "America/New_York" );
    if ("MI".equals( entry.state ))
    {
      if (entry.city.equalsIgnoreCase( "Detroit" ))
        return new TimeZoneGuess( "America/Detroit" );
      else
        return new TimeZoneGuess( "US/Michigan" );
    }
    if ("HI".equals( entry.state ))
      return new TimeZoneGuess( "US/Hawaii" );
    if ("AZ".equals( entry.state ))
    {
      if (entry.city.equalsIgnoreCase( "Phoenix" ))
        return new TimeZoneGuess( "America/Phoenix" );
      else
        return new TimeZoneGuess( "US/Arizona" );
    }
    if ("UK".equals( entry.state ))
    {
      if (entry.city.equalsIgnoreCase( "London" ))
        return new TimeZoneGuess( "Europe/London" );
      else
        return new TimeZoneGuess( "Europe/London", TimeZoneGuess.QUALITY_FAIR );
    }
    if ("France".equals( entry.state ))
    {
      if (entry.city.equalsIgnoreCase( "Paris" ))
        return new TimeZoneGuess( "Europe/Paris" );
      else
        return new TimeZoneGuess( "Europe/Paris", TimeZoneGuess.QUALITY_FAIR );
    }
    // parts of florida & texas are too close to other countries to return US timezones
    if ("FL".equals( entry.state ))
      return new TimeZoneGuess( "America/New_York", TimeZoneGuess.QUALITY_GUESS );
    if ("TX".equals( entry.state ))
      return new TimeZoneGuess( "US/Mountain", TimeZoneGuess.QUALITY_GUESS );
    // use country and other information to find a timezone
    TimeZoneGuess tz = inferTimezoneFromLocation( entry );
    if (tz != null)
      return tz;
    // if all else fails...
    tz = estimateTimeZoneFromLongitude( entry );
    if (tz != null)
      return tz;
    return null;
  }
  
  /**
   * Get a timezone given its name.
   */
  static public TimeZone getTimeZone( String name )
  {
    // fall back to full list
    return TimeZone.getTimeZone( name );
  }

  /**
   * Get the name for the default timezone.
   */
  static public String getDefaultTimezoneName()
  {
    TimeZone tzDefault = TimeZone.getDefault();
    return tzDefault.getID();
  }
  /**
   * Generate a suggestion list for timezones that match a given input.
   * The results can be used with TimeZone.getTimeZone( String ).
   */
  static public String[] findSimilarTimezones( String input )
  {
    input = input.toLowerCase();
    String inputSoundex = MiniAtlas.soundex( input );
    Map<Integer,List<String>> matches = new HashMap<Integer,List<String>>();
    final int MIN_QUALITY = 1;
    String[] tzs = TimeZone.getAvailableIDs();
    for (String tz : tzs)
    {
      int quality = matchQuality( tz, input, inputSoundex );
      if (quality >= MIN_QUALITY)
      {
        List<String> list = matches.get( quality );
        if (list == null)
          matches.put( quality, list = new LinkedList<String>() );
        list.add( tz );
      }
    }
    List<String> results = new ArrayList<String>();
    int highestQ = 0;
    for (int q = 7; q >= MIN_QUALITY; q--)
    {
      List<String> list = matches.get( q );
      if (list != null)
      {
        if (highestQ != 0)
          highestQ = q;
        String listArr[] = list.toArray( new String[ list.size() ] );
        Arrays.sort( listArr );
        for (String r : listArr)
          results.add( r );
        // limit list to some size based on how good the best match is
        int limitList = (highestQ >= 6) ? 15 : 25;
        if (results.size() > limitList)
          break;
      }
    }
    // return final list
    String resultArr[] = results.toArray( new String[ results.size() ] );
    return resultArr;
  }
  
  // test a single timezone for matching quality against a given input
  static private int matchQuality( String tz, String input, String inputSoundex )
  {
    tz = tz.toLowerCase();
    // exact match
    if (tz.equalsIgnoreCase( input ))
      return 7;
    /*
    // soundex match
    else if (MiniAtlas.soundex( tz ).equals( inputSoundex ))
      return 6;
    */
    // first letters match
    else if (tz.startsWith( input ))
      return Math.min( input.length(), 5 );
    else
      return 0;
  }

  /**
   * Use the atlas to find a nearby time zone.
   */
  static protected TimeZoneGuess inferTimezoneFromLocation( MiniAtlas.Entry atlasEntry )
  {
    // do we have a country?
    String country = atlasEntry.getCountry();
    if (country != null)
    {
      // scan for a matching country-specific timezone
      for (TimeZone tz : timezones)
      {
        // check ID for $COUNTRY
        String tzId = tz.getID();
        tzId.replace( '_', ' ' );
        if (tzId.equalsIgnoreCase( country ))
          return new TimeZoneGuess( tz );
        // check ID for $CONTINENT/$COUNTRY
        int pS = tzId.indexOf( '/' );
        if (pS != -1  &&  tzId.substring( pS+1 ).equalsIgnoreCase( country ))
          return new TimeZoneGuess( tz );
        // check display name for "$COUNTRY Time"
        String dispName = tz.getDisplayName();
        if (dispName.endsWith( " Time"))
          dispName = dispName.substring( 0, dispName.length() - 5);
        if (dispName.equalsIgnoreCase( country ))
          return new TimeZoneGuess( tz );
      }
      // can't guess it by country name
    }
    // use lat/lng
    double x1 = Math.cos( atlasEntry.lng * ChartWheel.d2r ) * Math.cos( atlasEntry.lat * ChartWheel.d2r );
    double y1 = Math.sin( atlasEntry.lng * ChartWheel.d2r ) * Math.cos( atlasEntry.lat * ChartWheel.d2r );
    double z1 = Math.sin( atlasEntry.lat * ChartWheel.d2r );
    double bestD = 0;
    TimeZone bestTZ = null;
    for (TimeZone tz : timezones)
    {
      MiniAtlas.Entry tzEntry = getTimeZoneLocation( tz );
      if (tzEntry == null)
        continue;
      // check for an exact match on the timezone's location
      if (atlasEntry.city.equalsIgnoreCase( tzEntry.city )  &&  atlasEntry.state.equalsIgnoreCase( tzEntry.state ))
        return new TimeZoneGuess( tz );
      // continue with the distance search
      double x2 = Math.cos( tzEntry.lng * ChartWheel.d2r ) * Math.cos( tzEntry.lat * ChartWheel.d2r );
      double y2 = Math.sin( tzEntry.lng * ChartWheel.d2r ) * Math.cos( tzEntry.lat * ChartWheel.d2r );
      double z2 = Math.sin( tzEntry.lat * ChartWheel.d2r );
      // this is the arc-cosine of the geocentric angle
      double distance = x1*x2 + y1*y2 + z1*z2;
      // eliminate long distances
      if (distance < 0.97)
        continue;
      // find the closest location
      if (distance > bestD)
      {
        bestD = distance;
        bestTZ = tz;
      }
    }
    // return closest timezone
    if (bestTZ != null)
      return new TimeZoneGuess( bestTZ, TimeZoneGuess.QUALITY_GUESS );
    return null;
  }
  
  /**
   * Use a city's longitude to choose an approximate timezone.
   */
  static protected TimeZoneGuess estimateTimeZoneFromLongitude( MiniAtlas.Entry atlasEntry )
  {
    int hours = - Math.round( atlasEntry.lng / 15 );
    String customZoneId = "GMT";
    if (hours > 0)
      customZoneId += "+";
    customZoneId += hours;
    return new TimeZoneGuess( TimeZone.getTimeZone( customZoneId ), TimeZoneGuess.QUALITY_ESTIMATE );
  }
  
  /**
   * Get a custom time zone given an offset.
   */
  static public TimeZone getCustomTimeZone( double hours )
  {
    String customZoneId = "GMT";
    if (hours > 0)
      customZoneId += "+";
    else
    {
      customZoneId += "-";
      hours = -hours;
    }
    int m = (int)Math.round( hours * 60 );
    int h = m / 60;
    m %= 60;
    customZoneId += h;
    customZoneId += ":";
    customZoneId += m/10;
    customZoneId += m%10;
    return TimeZone.getTimeZone( customZoneId );
  }
  
  static public void main( String args[] )
  {
    
    MiniAtlas.Entry findFor = MiniAtlas.getInstance().lookup( "Boulder, CO" )[0];
    @SuppressWarnings( "unused" )
    TimeZoneGuess tz = inferTimezoneFromLocation( findFor );
    /*
    String[] tzs = TimeZone.getAvailableIDs();
    for (String tz : tzs)
    {
      try
      {
        System.out.println( tz + ": " + TimeZone.getTimeZone(tz).getDisplayName() );
      }
      catch( Exception x )
      {
        System.err.println( "oops" );
      }
    }
    */
  }

}
