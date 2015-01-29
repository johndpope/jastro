package com.marklipson.astrologyclock;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Dasas
{
  private static final double DAYS_PER_YEAR = 365.2422;
  private int[] cycle = { 7, 20, 6, 10, 7, 18, 16, 19, 17 };
  private int cycleTotal = 120;
  private String[] cycleName = { "Ketu", "Venus", "Sun", "Moon", "Mars", "Rahu", "Jupiter", "Saturn", "Mercury" };
  static public class Period
  {
    public String name;
    public double tStart;
    public double tEnd;
    public Period sub[];
  }
  public Period periods[];
  
  public Dasas( double birth_jd, int zodiac )
  {
    double moon_radians = Ephemeris.getInstance().lookupLongitude( "Moon", birth_jd );
    moon_radians = ChartData.makeZodiacAdjustment( birth_jd, moon_radians, zodiac );
    double asterism = moon_radians * 27 / (Math.PI*2);
    double aFrac = asterism - Math.floor( asterism );
    int nCycle = (int)Math.floor( asterism ) % cycle.length;
    double tStart = birth_jd - aFrac * (cycle[ nCycle ] * DAYS_PER_YEAR);
    periods = findPeriods( nCycle, 3, tStart, cycleTotal );
    periods = trimToTimeRange( periods, birth_jd, birth_jd + 100 * DAYS_PER_YEAR );
  }
  Period[] findPeriods( int startCycle, int depth, double tStart, double scale )
  {
    Period[] out = new Period[ cycle.length ];
    double t = tStart;
    for (int n=0; n < out.length; n++)
    {
      int nCycle = (startCycle + n) % cycle.length;
      out[n] = new Period();
      out[n].name = cycleName[ nCycle ];
      out[n].tStart = t;
      t += cycle[ nCycle ] * DAYS_PER_YEAR * scale / cycleTotal;
      out[n].tEnd = t;
      if (depth > 1)
      {
        double subScale = cycle[ nCycle ] * (scale / cycleTotal);
        out[n].sub = findPeriods( nCycle, depth - 1, out[n].tStart, subScale );
      }
    }
    return out;
  }
  Period[] trimToTimeRange( Period[] in, double tLow, double tHigh )
  {
    if (in == null)
      return null;
    // find inclusive range to retain
    int nLow = 0;
    for (; nLow < in.length; nLow++)
      if (in[nLow].tEnd >= tLow)
        break;
    int nHigh = in.length - 1;
    for (; nHigh >= 0; nHigh--)
      if (in[nHigh].tStart < tHigh)
        break;
    if (nHigh < nLow)
      return null;
    Period[] out = Arrays.copyOfRange( in, nLow, nHigh + 1 );
    for (Period p : out)
    {
      if (p.tStart < tLow)
        p.tStart = tLow;
      p.sub = trimToTimeRange( p.sub, tLow, tHigh );
    }
    return out;
  }
  
  static public void dump( Period pp[], Writer out, int level ) throws IOException
  {
    for (Period p : pp)
    {
      if (level == 0)
        out.append( "\n" );
      for (int n=0; n < level; n++)
        out.append( (n == level-1) ? "+ " : "| " );
      out.append( String.format( "%-8s", p.name ) );
      out.append( ": " );
      out.append( new SimpleDateFormat("yyyy/MM/dd").format( new Date( ChartWheel.JD_to_systemTime( p.tStart ) ) ) );
      out.append( " - " );
      out.append( new SimpleDateFormat("yyyy/MM/dd").format( new Date( ChartWheel.JD_to_systemTime( p.tEnd ) ) ) );
      out.append( "\n" );
      if (p.sub != null)
        dump( p.sub, out, level + 1 );
    }
  }
  public static void main( String[] args )
  {
    try
    {
      long t = new SimpleDateFormat("yyyy/MM/dd HH:mm zzz").parse("1967/04/04 16:09 CST").getTime();
      double jd = ChartWheel.systemTime_to_JD( t );
      Dasas dasas = new Dasas( jd, ChartData.RAMAN );
      StringWriter sw = new StringWriter();
      dump( dasas.periods, sw, 0 );
      System.out.println( sw );
    }
    catch( Exception x )
    {
      x.printStackTrace();
    }
  }
}
