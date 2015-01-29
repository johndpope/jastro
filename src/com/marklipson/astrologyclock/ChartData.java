package com.marklipson.astrologyclock;

import java.util.HashMap;
import java.util.Map;

public class ChartData
{
  // multiply by this to convert degrees to radians
  public static final double d2r = Math.PI / 180;

  static private Map<String,PlanetInfo> planetMap = new HashMap<String,PlanetInfo>();
  static private PlanetInfo EARTH, MOON, SUN;

  // ZODIAC CONSTANTS
  public static final int TROPICAL = 0;
  public static final int RAMAN = 1;
  public static final int LAHIRI = 2;
  public static final int FAGAN_BRADLEY = 3;

  /**
   * Orbital elements.
   */
  public static class OrbElems
  {
    // time that elements apply to, in JD
    public double refFrame = 2415020;
    // the orbit...
    public double a, e;
    public double peri, dPeri, node, dNode, incl, dIncl;
    public double lng, dLng;
    // radius (in Earth radii)
    public double radius;

    public OrbElems()
    {
    }
    public OrbElems( double r, double _a, double _e,
        double p, double dp, double n, double dn, double i, double di, double l, double dl )
    {
        // physical characteristics
        radius = r;
        a = _a;     e = _e;
        peri = p;   dPeri = dp;
        node = n;   dNode = dn;
        incl = i;   dIncl = di;
        lng = l;    dLng = dl;
    }
    /**
     * Calculate heliocentric rectangular coordinates for a given time.
     */
    public double[] position( double tJD )
    {
        // convert time to julian centuries (the orbital elements use this time frame)
        double t = (tJD - refFrame) / 36525;
        
        // get elements
        double p = (peri + dPeri * t) % 6.2831852;
        double n = (node + dNode * t) % 6.2831852;
        double i = incl + dIncl * t;
        double M = (lng + dLng * t) % 6.2831852;
        // adjust elements to the algorithm
        p -= n;
        M -= p + n;
        // Find true anomaly (v)
        double v;
        {
            double E = M;
            for (int iter=0; iter < 6; iter++)
                E = M + e * Math.sin(E);
            if (Math.cos(E/2.0) != 0.0)
                v = 2.0 * Math.atan( Math.sqrt( (1.0 + e) / (1.0 - e) ) * Math.tan( E / 2.0 ) );
            else
                v = E;
        }
        // find true distance (r)
        double r = a * ((1.0 - e*e) / (1.0 + e * Math.cos(v)));
        // find and return rectangular position
        double pos[] = new double[3];
        {
            // find offset from ascending node
            double w = p + v;
            // intermediate values
            double sinW = Math.sin( w );
            double cosW = Math.cos( w );
            double sinNode = Math.sin( n ); // was: node (not sure why dNode was being ignored)
            double cosNode = Math.cos( n );
            double cosIncl = Math.cos( i ); // was: incl (not sure why dIncl was being ignored)
            // calculate
            pos[0] = r * (cosW * cosNode  -  sinW * sinNode * cosIncl);
            pos[1] = r * (cosW * sinNode  +  sinW * cosNode * cosIncl);
            pos[2] = r * (sinW * Math.sin( i ));
        }
        return pos;
    }
  }

  static private class PlanetInfo
  {
      public String name;
      public OrbElems orbElems;
      PlanetInfo( String name, OrbElems oe )
      {
        this.name = name;
        this.orbElems = oe;
        planetMap.put( name.toLowerCase(), this );
      }
  }
  static
  {
    SUN = new PlanetInfo( "Sun", new OrbElems( 108.97, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ) );
    new PlanetInfo( "Mercury", new OrbElems( 0.3824, 0.3870984, 0.205614929, 1.32465, 0.027113, 0.82283, 0.020678, 0.12223, 3.03396e-5, 3.10982, 2608.81471 ) );
    new PlanetInfo( "Venus", new OrbElems( 0.9489, 0.72333015, 0.006816361, 2.27138, 0.023951, 1.32275, 0.015953, 0.059230123, 2.18554e-5, 5.98242, 1021.352936 ) );
    EARTH = new PlanetInfo( "Earth", new OrbElems( 1.0, 1.00000129, 0.016749801, 1.76660, 0.029922, 0, 0, 0, 0, 1.74004, 628.331955 ) );
    new PlanetInfo( "Mars", new OrbElems(  0.5320, 1.523678, 0.093309, 5.83321, 0.032121, 0.851488, 0.013560, 0.03229, -0.113277, 5.12674, 334.085624 ) );
    new PlanetInfo( "Jupiter", new OrbElems( 11.1942, 5.202561, 0.048335, 0.22202, 0.028099, 1.73561, 0.017637, 0.02284, -0.000099, 4.15474, 52.993466 ) );
    new PlanetInfo( "Saturn", new OrbElems( 9.4071, 9.554747, 0.05589, 1.58996, 0.034181, 1.96856, 0.015240, 0.043503, -0.000068, 4.65243, 21.354276 ) );
    new PlanetInfo( "Uranus", new OrbElems( 3.98245, 19.21814, 0.046344, 2.994088, 0.025908, 1.282417, 0.008703, 0.013482, 0.000011, 4.262050, 7.502534 ) );
    new PlanetInfo( "Neptune", new OrbElems( 3.8099, 30.10957, 0.008997, 0.815546, 0.024864, 2.280820, 0.019180, 0.031054, -0.000167, 1.474070, 3.837733 ) );
    new PlanetInfo( "Pluto", new OrbElems( 0.2352, 39.51774,  0.24864, 3.882936, 0.24365, 1.901614, 0.243650, 0.299268, 0, 1.613087, 2.77286 ) );
    new PlanetInfo( "Chiron", null );
    new PlanetInfo( "Ceres", null );
    new PlanetInfo( "Sedna", null );
    // NOTE: I'm not sure about the elements for the moon
    MOON = new PlanetInfo( "Moon", new OrbElems( 0.27249, 0.002569, 0.0549, 5.83515, 71.01804, 4.5236, -33.75714, 0.089804, 0, 4.72, 8399.709 ) );
  }

  /**
   * Get orbital elements.
   */
  public static OrbElems getOrbitalElements( String planetKey, double tJD )
  {
    OrbElems elems = Ephemeris.getInstance().lookupOrbElems( planetKey, tJD );
    if (elems != null)
      return elems;
    PlanetInfo planet = planetMap.get( planetKey.toLowerCase() );
    if (planet == null)
      return null;
    // TODO the returned object is MODIFIABLE!!!
    return planet.orbElems;
  }

  /**
   * Check for a valid planet name.
   */
  static public boolean isPlanetName( String planet )
  {
    return planetMap.containsKey( planet.toLowerCase() );
  }
  /**
   * Check reasonable time range for calculations.
   */
  public static boolean isTimeWithinMaximumBounds( double t )
  {
    if (t < 1721411)
      return false;
    if (t > 2816795)
      return false;
    return true;
  }
  /**
   * Calculate geocentric longitude.
   */
  public static double geoLongitude( String planetKey, double t )
  {
      PlanetInfo planet = planetMap.get( planetKey.toLowerCase() );
      if (planet == null)
          return Double.NaN;
      if (planet == EARTH)
          return Double.NaN;
      // check for an ephemeris entry
      Double lng = Ephemeris.getInstance().lookupLongitude( planet.name, t );
      if (lng != null)
          return lng;
      // use moon calculation method as backup
      if (planet == MOON)
          return calculateMoon( t );
      // check for orbital elements in ephemeris
      OrbElems elems = Ephemeris.getInstance().lookupOrbElems( planet.name, t );
      // check for orbital elements stored in code
      if (elems == null)
          elems = planet.orbElems;
      // orbital elements required for calculation after this point
      if (elems == null)
          return Double.NaN;

      // calculate position of earth
      double[] vEarth = EARTH.orbElems.position( t );
      // TODO cache this
      // calculate position of planet
      double[] vP = elems.position( t );
      // calculate the longitude (from earth)
      double x = mod2pi( Math.atan2( vP[1] - vEarth[1], vP[0] - vEarth[0] ) );
      return x;
  }
  //
  // Calculate a heliocentric longitude.
  //
  static public double helioLongitude( String planetKey, double t )
  {
      PlanetInfo planet = planetMap.get( planetKey.toLowerCase() );
      if (planet == null)
          return Double.NaN;
      // TODO should be NaN - check references before changing
      if (planet == SUN)
          return 0;
      double vP[];
      if (planet == MOON)
          vP = helioPos( planetKey, t );
      else
      {
          OrbElems elems = Ephemeris.getInstance().lookupOrbElems( planetKey.toString(), t );
          if (elems == null)
            elems = planet.orbElems;
          if (elems == null)
            return Double.NaN;
          vP = elems.position( t );
      }
      return mod2pi( Math.atan2( vP[1], vP[0] ) );
  }
  /**
   * Calculate rectangular heliocentric coordinates.
   */
  public static double[] helioPos( String planetKey, double t )
  {
      PlanetInfo planet = planetMap.get( planetKey.toLowerCase() );
      if (planet == null)
          return null;
      if (planet == MOON)
      {
          double lng = calculateMoon( t );
          double a = MOON.orbElems.a;
          double[] pos = EARTH.orbElems.position( t );
          pos[0] += Math.cos(lng)*a;
          pos[1] += Math.sin(lng)*a;
          return pos;
      }
      else if (planet == SUN)
      {
          double[] pos = new double[3];
          pos[0] = pos[1] = pos[2] = 0;
          return pos;
      }
      else
          return planet.orbElems.position( t );
  }

  //
  // Calculate ascendant, midheaven, etc...
  //
  private static double calcGMST( double t_jd )
  {
      /*
      double T = (t - 2451545.0) / 36525.0;
      // GMST
      double gmst = 280.46061837 + 360.98564736629 * (t - 2451545) + T*T*( 0.000387933 - T/38710000 );
      gmst /= 360;
      gmst = (gmst - Math.floor(gmst)) * Math.PI*2;
      return gmst;
      */
      double t = t_jd - 2415020;          // make relative to 1900
      double T = (t/36525.0) - 1.0;       // J2000 time
      double T2 = T * T;                  // time squared
      double gmst;                        // calculated value
      // calculate
      gmst = 24110.54841;
      gmst += 8640184.812866 * T + 0.093104 * T2 - 0.0000062 * T2*T;
      gmst /= 86400;
      gmst += t - Math.floor(t) + 0.5;
      // convert to radians
      gmst -= Math.floor(gmst);
      return gmst * 2*Math.PI;
  }
  private static double calcOE( double t )
  {
      // Make time relative to J2000, centuries.
      double tC = (t - 2451545.0) / 36525.0;
      // Calculate OE
      double oe = (23.439291 * d2r) -
          (0.0130042 * d2r
              - (0.00000016 * d2r
                  - (0.000000504 * d2r) * tC) * tC) * tC;
      return oe;
  }
  static public double calcAscendant( double t, double lng, double lat )
  {
      // Calculate OE
      double oe = calcOE( t );
      // RAMC
      double ramc = calcGMST( t ) - lng;
      double cl = Math.cos( lat );
      if (cl == 0)
          return -1;
      double v1 = -Math.cos( ramc );
      double v2 = Math.sin(oe)*Math.tan(lat) + Math.cos(oe)*Math.sin(ramc);
      double ac = Math.atan2( -v1, -v2 );
      //double v1 = Math.cos(ramc);
      //double tl = Math.sin( lat ) / cl;
      //double v2 = - Math.sin(ramc) * Math.cos(oe) - tl * Math.sin(oe);
      //double ac = Math.atan2( v1, v2 );
      if (ac < 0)
          ac += Math.PI*2;
      return ac;
  }
  static public double calcMidheaven( double t, double lng )
  {
      double oe = calcOE( t );
      double ramc = calcGMST( t ) - lng;
      double v1 = Math.sin(ramc);
      double v2 = Math.cos(ramc) * Math.cos(oe);
      double mc = Math.atan2( v1, v2 );
      if (mc < 0)
          mc += Math.PI*2;
      return mc;
  }

  /**
   * Approximate longitude of moon - accurate to a quarter of a degree or so.\
   */
  public static double calculateMoon( double tJD )
  {
      // convert time to julian centuries (the orbital elements use this time frame)
      double t = (tJD - 2415020) / 36525;
      // related values
      double mean_lunar_node = (4.52360 - 33.757145 * t) % (Math.PI*2);
      double elongation = (6.12152 + 7771.37719 * t) % (Math.PI*2);
      double solar_anomaly = (6.25658 + 628.301946 * t) % (Math.PI*2);
      double perigee = (5.83515 + 71.018041 * t) % (Math.PI*2);
      // start with mean moon
      double moon = (4.719967 + 8399.709128 * t) % (Math.PI*2);
      // perturbation values
      double a = solar_anomaly;
      double b = moon - perigee;
      double c2 = (moon - mean_lunar_node)*2;
      double d2 = elongation*2;
      // make adjustments to moon
      moon +=  0.109760 * Math.sin( b  ) - 0.022236 * Math.sin( b-d2 );
      moon +=  0.011490 * Math.sin( d2 ) + 0.003728 * Math.sin( b+b  );
      moon += -0.003239 * Math.sin( a  ) - 0.001993 * Math.sin( c2   );
      return mod2pi( moon );
  }

  /**
   * Calculate the mean lunar node.
   */
  public static double calculateMeanLunarNode( double tJD )
  {
    double t = (tJD - 2415020) / 36525;
    double mean_lunar_node = (4.52360 - 33.757145 * t) % (Math.PI*2);
    return mod2pi( mean_lunar_node );
  }
  
  /**
   * Calculate house cusps.  Returns an array with cusps for the 10th, 11th, 2nd and 3rd houses,
   * respectively.  Currently uses the Porphyry method.
   */
  public static double[] calcCusps( double asc, double mc )
  {
      double cusps[] = new double[ 4 ];
      double dM2A = mod2pi( asc - mc );
      cusps[0] = mod2pi( mc + dM2A/3 );
      cusps[1] = mod2pi( mc + 2*dM2A/3 );
      double dA2N = mod2pi( Math.PI - dM2A );
      cusps[2] = mod2pi( asc + dA2N/3 );
      cusps[3] = mod2pi( asc + 2*dA2N/3 );
      return cusps;
  }
  public static int findHouse( double a, double ac, double mc, double cusps[] )
  {
      double[] all = new double[12];
      all[0] = ac;
      all[1] = cusps[2];
      all[2] = cusps[3];
      all[9] = mc;
      all[10] = cusps[0];
      all[11] = cusps[1];
      for (int n=3; n <= 8; n++)
          all[n] = mod2pi( all[(n+6)%12] + Math.PI );
      for (int n=0; n < 12; n++)
      {
          double d = angleDelta( all[n], a );
          if (d >= 0  &&  d < angleDelta( all[n], all[(n+1)%12] ))
              return n + 1;
      }
      return 0;
  }

  /**
   * Synetic vernal point.
   * 
   * Calculates the Fagan-Bradley anayamsa.  Other anayamsa's can be calculated
   * from this.
   */
  static public double calcSVP( double _t )
  {
      /*
      **  Calculate variation of SVP from tropical zodiac after (t==0)
      **  (i.e. 1-0-1900).
      */
      double t = (_t - 2415020.0) / 36525.0;
      double MeanSun = mod2pi( (279.696667 * d2r) + (36000.76888 * d2r) * t);
      double MeanNode = mod2pi( (259.18330 * d2r) + ((-1934.141925 * d2r) + ((7.480791 * d2r/3600) + (0.00720 * d2r/3600) * t) * t) * t );
      double x;
      // calculate
      x = (17.23 * d2r/3600)*Math.sin(MeanNode);
      x += (1.27 * d2r/3600)*Math.sin(MeanSun*2.0) - (1.396011668 * d2r)*t - (1.11 * d2r/3600)*t*t;
      x -= (23.34396387 * d2r);
      return x;
  }

  private final static double TWOPI = Math.PI * 2;
  public static double mod2pi( double x )
  {
      if (x < 0)
      {
          x += TWOPI;
          if (x < 0)
            x -= Math.floor( x / TWOPI ) * TWOPI;
      }
      else if (x > TWOPI)
      {
          x -= TWOPI;
          if (x >= TWOPI)
            x -= Math.floor( x / TWOPI) * TWOPI;
      }
      return x;
  }
  /**
   * Calculate the relative difference from one angle to another.
   */
  public static double angleDelta( double start, double end )
  {
      double d = mod2pi( end - start );
      if (d >= TWOPI)
          d -= TWOPI;
      else if (d <= -TWOPI)
          d += TWOPI;
      return d;
  }

  /**
   * Calculate adjustment for selected zodiac.
   */
  static double makeZodiacAdjustment( double t_jd, double x, int zodiac )
  {
      if (zodiac == TROPICAL)
          return x;
      double adj = ChartData.calcSVP( t_jd );
      if (zodiac == RAMAN)
          adj += 2.333333 * d2r;
      else if (zodiac == LAHIRI)
          adj += (53/60.0) * d2r;
      else if (zodiac == FAGAN_BRADLEY)
          ;
      else
          // default to tropical
          return x;
      return mod2pi( x + adj );
  }
  
}
