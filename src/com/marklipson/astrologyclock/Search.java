package com.marklipson.astrologyclock;

import java.util.Random;

import com.marklipson.astrologyclock.ChartWheel.PlanetRing;

/**
 * Search for events.
 */
public class Search
{
  // associated ChartWheel - needed for zodiac selection, etc.
  private ChartWheel wheel;
  private ChartWheel.PlanetRing ring;
  private static final int ASCENDANT = 100;
  private static final int MIDHEAVEN = 101;

  /**
   * Set up a searching instance.
   */
  public Search( ChartWheel.PlanetRing ring )
  {
    this.wheel = ring.getChartWheel();
    this.ring = ring;
  }

  /**
   * Get a planet's position.
   */
  private double getPlanetPos( int planet, double t )
  {
    if (planet == ASCENDANT)
      return wheel.calcAscendant( t );
    if (planet == MIDHEAVEN)
      return wheel.calcMidheaven( t );
    if (wheel.isHeliocentric())
      return wheel.helioLongitude( ring.planets[planet], t );
    else
      return wheel.geoLongitude( ring.planets[planet], t );
  }
  private double angleDiff( double a1, double a2 )
  {
    double d = Math.abs( a1 - a2 );
    if (d > Math.PI)
      d = 2*Math.PI - d;
    return d;
  }
  private int getPlanetNumber( String planet )
  {
    for (int n=0; n < ring.planets.length; n++)
      if (ring.planets[n].name.equalsIgnoreCase( planet ))
        return n;
    if (planet.equalsIgnoreCase( "AC" ))
      return ASCENDANT;
    if (planet.equalsIgnoreCase( "ASC" ))
      return ASCENDANT;
    if (planet.equalsIgnoreCase( "ASCENDANT" ))
      return ASCENDANT;
    if (planet.equalsIgnoreCase( "MC" ))
      return MIDHEAVEN;
    if (planet.equalsIgnoreCase( "MIDHEAVEN" ))
      return MIDHEAVEN;
    return -1;
  }
  
  /**
   * Searchable events derive from this class.
   */
  abstract public class Event
  {
    /**
     * Get the ideal initial time step to use when searching.
     */
    double getTimeStep()
    {
      return 10;
    }
    double getTimeStepForPlanet( int planet )
    {
      if (planet == ChartWheel.MOON)
        return 0.5;
      else if (planet == ASCENDANT  ||  planet == MIDHEAVEN)
        return 0.02;
      else if (planet >= ChartWheel.JUPITER)
        return 30;
      else
        return 5;
    }
    /**
     * Test whether this condition is met.  Returns NaN if it simply isn't, and
     * a smaller value as it approaches an exact match.
     */
    abstract double test( double t );
  }
  
  /**
   * Search for a planet at a given position.
   */
  private class PlanetAtPosition extends Event
  {
    private int planet;
    private double pos;
    private final double orb;
    PlanetAtPosition( int planet, double pos, double orb )
    {
      this.planet = planet;
      this.pos = pos;
      this.orb = orb;
    }
    public double getTimeStep()
    {
      return getTimeStepForPlanet( planet )/3;
    }
    double test(double t)
    {
      double p = getPlanetPos( planet, t );
      double err = angleDiff( p, pos );
      if (err > orb)
        return Double.NaN;
      return err;
    }
  }
  /**
   * Test for a planet in a sign.
   */
  private class PlanetInSign extends Event
  {
    private int planet;
    private int sign;
    public PlanetInSign( int planet, int sign )
    {
      this.planet = planet;
      this.sign = sign;
    }
    double getTimeStep()
    {
      return getTimeStepForPlanet( planet );
    }
    public double test(double t)
    {
      double pos = getPlanetPos( planet, t );
      pos /= (Math.PI/6);
      if (Math.floor( pos ) != sign)
        return Double.NaN;
      return (pos - sign)*(Math.PI/6);
    }
  }
  /**
   * Search for an aspect between two planets.
   */
  private class Aspect extends Event
  {
    private int planet1, planet2;
    private double aspect, orb;
    Aspect( int p1, int p2, double aspect, double orb )
    {
      this.planet1 = p1;
      this.planet2 = p2;
      this.aspect = aspect;
      this.orb = orb;
    }
    protected double getPos2( double t )
    {
      return getPlanetPos( planet2, t ); 
    }
    public double getTimeStep()
    {
      return Math.min( getTimeStepForPlanet( planet1 ), getTimeStepForPlanet( planet2 ) );
    }
    double test(double t)
    {
      double p1 = getPlanetPos( planet1, t );
      double p2 = getPos2( t );
      double delta = angleDiff( angleDiff( p1, p2 ), aspect );
      if (delta < orb)
        return delta;
      else
        return Double.NaN;
    }
  }
  /**
   * Aspect between a planet and a fixed point.
   */
  private class AspectPoint extends Aspect
  {
    private double pos2;
    AspectPoint( int p1, double pos2, double aspect, double orb )
    {
      super( p1, 0, aspect, orb );
      this.pos2 = pos2;
    }
    protected double getPos2(double t)
    {
      return pos2;
    }
  }
  /**
   * Search for a retrograde.
   */
  private class Rx extends Event
  {
    int planet;
    Rx( int planet )
    {
      this.planet = planet;
    }
    public double getTimeStep()
    {
      return getTimeStepForPlanet( planet );
    }
    double test(double t)
    {
      double p0 = getPlanetPos( planet, t );
      double p1 = getPlanetPos( planet, t + 0.0005 );
      if (p1 > p0)
        return Double.NaN;
      else
        return p0 - p1;
    }
  }
  /**
   * Test for two events at a time.
   */
  private class And extends Event
  {
    private Event a, b;
    And( Event a, Event b )
    {
      this.a = a;
      this.b = b;
    }
    public double getTimeStep()
    {
      return Math.min( a.getTimeStep(), b.getTimeStep() );
    }
    double test(double t)
    {
      double eA = a.test( t );
      double eB = b.test( t );
      if (Double.isNaN( eA )  ||  Double.isNaN( eB ))
        return Double.NaN;
      else
        return eA + eB;
    }
  }
  /**
   * Test for two alternate events.
   */
  private class Or extends Event
  {
    private Event a, b;
    Or( Event a, Event b )
    {
      this.a = a;
      this.b = b;
    }
    public double getTimeStep()
    {
      return Math.min( a.getTimeStep(), b.getTimeStep() );
    }
    double test(double t)
    {
      double eA = a.test( t );
      double eB = b.test( t );
      if (Double.isNaN( eA )  &&  Double.isNaN( eB ))
        return Double.NaN;
      else if (Double.isNaN( eA ))
        return eB;
      else if (Double.isNaN( eB ))
        return eA;
      else
        return eA + eB;
    }
  }
  /**
   * Logically negate an event.
   */
  private class Not extends Event
  {
    private Event a;
    Not( Event a )
    {
      this.a = a;
    }
    public double getTimeStep()
    {
      return a.getTimeStep();
    }
    double test(double t)
    {
      double eA = a.test( t );
      if (Double.isNaN( eA ))
        return 0;
      else
        return Double.NaN;
    }
  }
  /**
   * Errors during parsing.
   */
  static public class SearchParseException extends Exception
  {
    private static final long serialVersionUID = 1L;
    private int inputOffset;
    private int tokenLength;
    
    SearchParseException( String message )
    {
      super( message );
    }
    public int getInputOffset()
    {
      return inputOffset;
    }
    public int getTokenLength()
    {
      return tokenLength;
    }
  }
  /**
   * Translate a typed search expression into an Event object.
   */
  public Event parseSearch( String search ) throws SearchParseException
  {
    BasicTokenizer tokenizer = new BasicTokenizer( search.toLowerCase() );
    try
    {
      return parseExpr( tokenizer, 0 );
    }
    catch( SearchParseException x )
    {
      x.inputOffset = tokenizer.getLastTokenOffs();
      x.tokenLength = tokenizer.getLastTokenLen();
      throw x;
    }
  }
  /**
   * 
   */
  protected Event parseExpr( BasicTokenizer tokens, int parens ) throws SearchParseException
  {
    Event value = parseValue( tokens, parens );
    if (value == null)
      throw new SearchParseException( "Expected expression" );
    if ("and".equals( tokens.lookahead() ))
    {
      tokens.next();
      Event value2 = parseExpr( tokens, parens );
      return new And( value, value2 );
    }
    if ("or".equals( tokens.lookahead() ))
    {
      tokens.next();
      Event value2 = parseExpr( tokens, parens );
      return new Or( value, value2 );
    }
    if (parens > 0  &&  ")".equals( tokens.lookahead() ))
      ;
    else if (! tokens.isEOF())
      throw new SearchParseException( "Unexpected: '" + tokens.lookahead() + "'" );
    return value;
  }
  protected Event parseValue( BasicTokenizer tokens, int parens ) throws SearchParseException
  {
    Object token = tokens.lookahead();
    if (token == null)
      return null;
    if ("(".equals( token ))
    {
      tokens.next();
      Event event = parseExpr( tokens, parens+1 );
      // check for closing paren - it's OK for it to be missing at EOF
      if (! ")".equals( tokens.lookahead() )  &&  tokens.lookahead() != null)
        throw new SearchParseException( "Expected ')'" );
      tokens.next();
      return event;
    }
    if ("not".equals( token ))
    {
      tokens.next();
      return new Not( parseValue( tokens, parens ) );
    }
    if (token instanceof String  &&  ChartData.isPlanetName( token.toString() ))
    {
      String planet1 = token.toString();
      int planetNo1 = getPlanetNumber( planet1 );
      if (planetNo1 < 0)
        throw new SearchParseException( "Expected name of planet, not " + token );
      tokens.next();
      Object condition = tokens.next();
      if ("in".equals( condition ))
      {
        int sign = parseSign( tokens );
        if (sign < 0)
          throw new SearchParseException( "Expected sign after 'in'" );
        return new PlanetInSign( planetNo1, sign );
      }
      if ("at".equals( condition ))
      {
        // at position (ddSSmm)
        double d = parseEclLongitude(tokens);
        return new PlanetAtPosition( planetNo1, d * ChartData.d2r, 1 * ChartData.d2r );
      }
      if ("rx".equals( condition ))
      {
        return new Rx( planetNo1 );
      }
      // TODO put aspect orbs somewhere common
      double aspectAngle, aspectOrb;
      if ("conjunct".equals( condition )  ||  "cjn".equals( condition )  ||  "conjunction".equals( condition ))
      { aspectAngle=0; aspectOrb=8; }
      else if ("opposition".equals( condition )  ||  "opp".equals( condition )  ||  "opposite".equals( condition ))
      { aspectAngle=180; aspectOrb=7; }
      else if ("trine".equals( condition )  ||  "tri".equals( condition ))
      { aspectAngle=120; aspectOrb=6; }
      else if ("square".equals( condition )  ||  "sqr".equals( condition ))
      { aspectAngle=90; aspectOrb=6; }
      else if ("sextile".equals( condition )  ||  "sxt".equals( condition ))
      { aspectAngle=60; aspectOrb=4; }
      else if ("quintile".equals( condition )  ||  "qnt".equals( condition ))
      { aspectAngle=72; aspectOrb=3; }
      else if ("semisquare".equals( condition )  ||  "ssq".equals( condition ))
      { aspectAngle=45; aspectOrb=3; }
      else
        // invalid expression
        throw new SearchParseException( "Expected 'in', 'at', 'rx' or name of aspect after planet name, not " + condition );
      // parse target for aspect - another planet or a position
      if (tokens.lookahead() == null)
        throw new SearchParseException( "Expected planet name or zodiac position after aspect name" );
      if (tokens.lookahead() instanceof String)
        return new Aspect( planetNo1, parsePlanet(tokens), aspectAngle * ChartData.d2r, aspectOrb * ChartData.d2r );
      else
        return new AspectPoint( planetNo1, parseEclLongitude(tokens) * ChartData.d2r, aspectAngle * ChartData.d2r, aspectOrb * ChartData.d2r );
    }
    else
      throw new SearchParseException( "Expected name of planet" );
  }
  /**
   * Parse an ecliptic longitude, i.e. ddSSmm, i.e. 04leo27
   */
  protected double parseEclLongitude( BasicTokenizer tokens ) throws SearchParseException
  {
    Object dgrs = tokens.next();
    if (dgrs == null  ||  ! (dgrs instanceof Number))
      throw new SearchParseException( "Expected degrees after 'at'" );
    double d = ((Number)dgrs).intValue();
    int sign = parseSign( tokens );
    if (sign == -1)
      throw new SearchParseException( "Expected sign after degrees after 'at'" );
    d += sign*30;
    Object mins = tokens.lookahead();
    if (mins != null  &&  mins instanceof Number)
    {
      d += ((Number)mins).intValue()/60.0;
      tokens.next();
    }
    return d;
  }
  /**
   * Parse a mandatory planet name.
   */
  private int parsePlanet( BasicTokenizer tokens ) throws SearchParseException
  {
    Object token = tokens.next();
    if (token == null)
      throw new SearchParseException( "Expected name of planet" );
    int planetNumber = getPlanetNumber( token.toString() );
    if (planetNumber < 0)
      throw new SearchParseException( "Expected name of planet, not " + token );
    return planetNumber;
  }
  /**
   * Check for a named zodiac sign.
   */
  private int parseSign( BasicTokenizer tokens )
  {
    final String[] signsFull =
      {
        "aries","taurus","gemini","cancer","leo","virgo","libra","scorpio","saggittarius","capricorn","aquarius","pisces"
      };
    final String[] signsAbbr =
      {
        "ar","ta","ge","ca","le","vi","li","sc","sa","cp","aq","pi"
      };
    Object name = tokens.lookahead();
    for (int n=0; n < 12; n++)
      if (signsFull[n].equals( name )  ||  signsAbbr[n].equals( name ))
      {
        tokens.next();
        return n;
      }
    return -1;
  }
  
  /**
   * Search timeout.
   */
  public class SearchTimeout extends Exception
  {
    private static final long serialVersionUID = 1L;
  }

  /**
   * Results of a search operation.
   */
  public static class SearchResult
  {
    /**
     * Time event begins.
     */
    public double tStart;
    /**
     * Time event ends.
     */
    public double tEnd;
    /**
     * Peak time for event.
     */
    public double tPeak;
  }
  /**
   * Simple utility for checking for a timeout.
   */
  private class Timeout
  {
    private long timeoutAt;
    public Timeout( int ms )
    {
      timeoutAt = System.currentTimeMillis() + ms;
    }
    public void test() throws SearchTimeout
    {
      if (System.currentTimeMillis() > timeoutAt)
        throw new SearchTimeout();
    }
  }
  /**
   * Scan forward or backward for an event.
   */
  public SearchResult search( double tStart, Event event, boolean forward, int maxExecutionTime ) throws SearchTimeout
  {
    Timeout timeout = new Timeout( maxExecutionTime );
    double dt = event.getTimeStep();
    if (! forward)
      dt = -dt;
    double t = tStart;
    // if the event is currently in effect, scan until it is no longer in effect
    while (! Double.isNaN( event.test( t ) ))
    {
      timeout.test();
      t += dt;
    }
    // now scan until it *is* in effect and find the leading edge
    while (Double.isNaN( event.test( t ) ))
    {
      timeout.test();
      if (! ChartData.isTimeWithinMaximumBounds( t ))
        throw new SearchTimeout();
      t += dt;
    }
    double tLeadingEdge = findEdge( t, t - dt, event );
    // find the trailing edge (and keep track of lowest error value)
    double tBest = t;
    double errBest = event.test( t );
    for (;;)
    {
      double err = event.test( t );
      if (Double.isNaN( err ))
        break;
      if (err < errBest)
      {
        tBest = t;
        errBest = err;
      }
      t += dt;
    }
    double tTrailingEdge = findEdge( t - dt, t, event );
    // now find the peak (lowest error) within this range
    double tA = tBest - dt;
    double tB = tBest + dt;
    /*
    if (forward  &&  tA < tLeadingEdge)
      tA = tLeadingEdge;
    if (forward  &&  tB > tTrailingEdge)
      tB = tTrailingEdge;
    if (! forward  &&  tA > tLeadingEdge)
      tA = tLeadingEdge;
    if (! forward  &&  tB < tLeadingEdge)
      tB = tTrailingEdge;
    */
    for (int nIter=0; nIter < 10; nIter++)
    {
      double tM = (tA + tB) / 2;
      double tL = (tA*3 + tB) / 4;
      double errL = event.test( tL );
      if (! Double.isNaN( errL )  &&  errL < errBest)
      {
        tB = tM;
        errBest = errL;
      }
      else
      {
        double tR = (tA + tB*3)/4;
        double errR = event.test( tR );
        if (! Double.isNaN( errR )  &&  errR < errBest)
        {
          tA = tM;
          errBest = errR;
        }
        else
        {
          tA = tL;
          tB = tR;
        }
      }
    }
    tBest = (tA + tB)/2;
    SearchResult result = new SearchResult();
    result.tPeak = tBest;
    result.tStart = forward ? tLeadingEdge : tTrailingEdge;
    result.tEnd = forward ? tTrailingEdge : tLeadingEdge;
    return result;
  }
  
  /**
   * Find the point where an event transitions from in range to out of range.
   */
  private double findEdge( double tIn, double tOut, Event event )
  {
    double t = 0;
    for (int nIter=0; nIter < 10; nIter++)
    {
      t = (tIn + tOut)/2;
      if (Double.isNaN( event.test( t ) ))
        tOut = t;
      else
        tIn = t;
    }
    return t;
  }

  /**
   * Calculate how often an event occurs during a period of time, using random sampling.
   */
  public double calculateFrequency( Event event, double tLow, double tHigh, int nSamples, double maxOrb )
  {
    int hits = 0;
    double range = tHigh - tLow;
    Random rnd = new Random();
    for (int nSample=0; nSample < nSamples; nSample++)
    {
      double t = tLow + rnd.nextDouble() * range;
      double orb = event.test( t );
      if (orb < maxOrb)
        hits ++;
    }
    return (double)hits / nSamples;
  }
  
  public static void main( String[] args )
  {
    try
    {
      ChartWheel chart = new ChartWheel( null, null );
      Search s = new Search( chart.mainRing );
      Event event = s.parseSearch( "venus square moon and venus semisquare saturn and moon semisquare saturn" );
      long tNow = System.currentTimeMillis();
      long tRange = (long)(70*365.25*86400000);
      double maxOrb = 4 * ChartWheel.d2r;
      int nSamples = 200000;
      double freq = s.calculateFrequency( event, ChartWheel.systemTime_to_JD( tNow - tRange ), ChartWheel.systemTime_to_JD( tNow + tRange ), nSamples, maxOrb );
      System.out.println( freq*100 + " %  (1 in " + 1/freq + ")" );
    }
    catch( Exception x )
    {
      x.printStackTrace();
    }
  }
}
