package com.marklipson.astrotools;

import com.marklipson.astrologyclock.ChartData;
import com.marklipson.astrologyclock.Ephemeris;
import com.marklipson.astrologyclock.ChartData.OrbElems;

public class EphemTest
{
  private static double t_to_jd( double t )
  {
    return t / 86400000.0 + 2440587.5;
  }

  /**
   * Calculate geocentric longitude.
   */
  public static void testPlanetAtTime( String planet, double t )
  {
    Double lngEphem = Ephemeris.getInstance().lookupLongitude( planet, t );
    if (lngEphem == null)
      lngEphem = 0d;
    else
      lngEphem /= ChartData.d2r;
    // check for orbital elements in ephemeris
    OrbElems elems = Ephemeris.getInstance().lookupOrbElems( planet, t );
    OrbElems elemsEarth = Ephemeris.getInstance().lookupOrbElems( "earth", t );
    double lngElems = 0;
    if (elems == null  &&  planet.equals( "moon" ))
    {
      lngElems = ChartData.calculateMoon( t ) / ChartData.d2r;
    }
    else if (planet.equals( "mars" ))
    {
      double[] vP = ChartData.helioPos( planet, t );
      double[] vEarth = ChartData.helioPos( "earth", t );
      lngElems = ChartData.mod2pi( Math.atan2( vP[1] - vEarth[1], vP[0] - vEarth[0] ) ) / ChartData.d2r;
    }
    else if (elems != null)
    {
      // calculate position of earth
      double[] vEarth = elemsEarth.position( t );
      // calculate position of planet
      double[] vP = elems.position( t );
      // calculate the longitude (from earth)
      lngElems = ChartData.mod2pi( Math.atan2( vP[1] - vEarth[1], vP[0] - vEarth[0] ) ) / ChartData.d2r;
    }
    System.out.printf( "%10s @ %10.0f = %7.3f : %7.3f :: %6.3f\n", planet, t, lngEphem, lngElems, Math.abs(lngElems-lngEphem) );
  }

  static public void main( String args[] )
  {
    double jd = t_to_jd( System.currentTimeMillis() );
    double[] dts = new double[] { -365000, -36500, -18000, -9000, 0, 9000, 18000, 36500 };
    for (double dt : dts)
    {
      testPlanetAtTime( "pluto", jd + dt );
      //testPlanetAtTime( "neptune", jd + dt );
      //testPlanetAtTime( "saturn", jd + dt );
      testPlanetAtTime( "mars", jd + dt );
      testPlanetAtTime( "moon", jd + dt );
    }
  }
}
