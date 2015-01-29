package com.marklipson.astrologyclock;
//
// ChartWheel.java
//
// The ChartWheel class is a 'Component'-derived class that displays a view
// of the solar system at a given time and scale.
//
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Display an interactive chart.
 */
public class ChartWheel extends Canvas implements ImageObserver, Runnable
{
    private DateFormat timeFormat = new SimpleDateFormat( "yyyy-MMM-dd HH:mm" );

    private static final String signNames[] =
      {
          "Aries", "Taurus", "Gemini", "Cancer",
          "Leo", "Virgo", "Libra", "Scorpio",
          "Saggitarius", "Capricorn", "Aquarius", "Pisces"
      };

    private Color paperColor = new Color( 0xe0, 0xe0, 0xe0 );
    private Color insideColor = new Color( 0xff, 0xff, 0xf0 );
    private Color lineColor = new Color( 0, 0, 0 );
    private Color degreeLineColor = new Color( 160, 160, 160 );
    private Color degree5LineColor = new Color( 128, 128, 128 );
    private Color degreeNumberColor = new Color( 128, 128, 128 );
    private Color horizonColor = new Color( 0x60, 0xb0, 0x60 );
    private Color zenithColor = new Color( 0x60, 0x60, 0xb0 );
    private Color houseCuspColor = new Color( 0xd0, 0xe0, 0xe0 );
    
    private Color fireColor = new Color(255,232,232);
    private Color earthColor = new Color(248,248,224);
    private Color airColor = new Color(232,255,232);
    private Color waterColor = new Color(224,224,255);
    private Color signBgColors[] = 
    {
        fireColor, earthColor, airColor, waterColor,
        fireColor, earthColor, airColor, waterColor,
        fireColor, earthColor, airColor, waterColor
    };
    
    // show phase of the moon in the background
    private boolean moonPhaseBackground = false;

    // PROPERTIES
	// current time (JD)
	private double time;					// currently selected time (JD)
	private double t_disp = 0;				// time currently calculated for
	//private double timezone = 0;			// current timezone for display of dates
	// how accurate the time is
	private double time_accuracy = 0;       // maximum error in time, in days
	// current aspects
    Aspects aspectList;
	// current place
	private double curLng = 123.0856 * d2r;
	private double curLat = 44.0522 * d2r;
	// other switches
	boolean enableGraphics = true;			// used to disable graphics display while initializing or changing opions
	boolean displayNode = false;            // display moon's node
	double chartRotation = 0;				// rotate entire chart by this angle (radians)
	private boolean enableChartRotation = true;		// whether to rotate chart
  private boolean enableHouseCusps = true;        // whether to show house cusps
  private boolean heliocentric = false;   // heliocentric vs geocentric display
  private boolean square = false;         // square chart
  private boolean showStars = true;       // whether stars are displayed
  private int zodiac = TROPICAL;          // which zodiac to use
  private boolean enableInteraction = true; // user can drag planets, etc.
  private Map<String,Boolean> otherBodies = new HashMap<String,Boolean>();
    
	// display time threshold for planets (not for angles)
	static final double ROUGH_TIME_THRESHOLD = 0.04;
	// multiply by this to convert degrees to radians
	public static final double d2r = Math.PI / 180;
	// two pi
	public static final double twopi = Math.PI * 2;
	
  // ZODIAC CONSTANTS
  static final int TROPICAL = ChartData.TROPICAL;
  static final int RAMAN = ChartData.RAMAN;
  static final int LAHIRI = ChartData.LAHIRI;
  static final int FAGAN_BRADLEY = ChartData.FAGAN_BRADLEY;
    
	// PLANET CONSTANTS
	static final int SUN = 0;
	static final int MERCURY = 1;
	static final int VENUS = 2;
	static final int EARTH = 3;
	static final int MOON = 4;
	static final int MARS = 5;
	static final int JUPITER = 6;
	static final int SATURN = 7;
	static final int URANUS = 8;
	static final int NEPTUNE = 9;
	static final int PLUTO = 10;
  static final int NORTHNODE = 11;
  static final int SOUTHNODE = 12;
  static final int CHIRON = 13;
  static final int CERES = 14;
  static final int SEDNA = 15;
	static final int NPLANETS = 16;

	// TOOLS
	// the most recent position for the Earth is stored here:
	double lastT;		// buffered time
	double[] lastEarth;	// buffered earth position
	// these are used for 'spinning' (see 'run()')
	protected double spinPrevious;	// when the last update request took place
	protected double spinElapsed;	// how much time has elapsed since the last successful update
	protected Thread spinner;		// thread that keeps the solar system spinning
	protected boolean spinnerPaused;// pause flag
	protected double lastRefresh = 0;// time of last screen refresh (when the control was re-painted)
	protected Image[] signs;

	// each ring of planets...
	public class PlanetRing
	{
	    double t;           // time for this ring (may be different from rest of chart)
	    ChartPlanet[] planets;
	    int rPlanetDots;    // where to draw dots for planets
	    int rPlanetDots2;   // where to draw secondary dots for planets
	    int rPlanetSyms;    // where to draw planetary symbols/graphics
	    float symbolScale = 1; // different size for symbols
	    
	    PlanetRing()
	    {
	        ChartWheel wheel = ChartWheel.this;
	        planets = new ChartPlanet[ NPLANETS ];
	        planets[SUN] = new ChartPlanet( wheel, this, SUN, "Sun", new Color(177,128,13), 109.123/4 );
	        // NOTE: Sun's radius has been reduced to keep it from overlapping with Mercury
	        planets[MERCURY] = new ChartPlanet( wheel, this, MERCURY, "Mercury", new Color(204,153,51), 0.3824 );
	        planets[VENUS] = new ChartPlanet( wheel, this, VENUS, "Venus", new Color(4,118,118), 0.9489 );
	        planets[EARTH] = new ChartPlanet( wheel, this, EARTH, "Earth", new Color(0,80,128), 1.0 );
	        planets[MOON] = new ChartPlanet( wheel, this, MOON, "Moon", new Color(36,36,36), 0.27249 );
	        planets[MARS] = new ChartPlanet( wheel, this, MARS, "Mars", new Color(250,75,67), 0.5320 );
	        planets[JUPITER] = new ChartPlanet( wheel, this, JUPITER, "Jupiter", new Color(255,102,0), 11.1942 );
	        planets[SATURN] = new ChartPlanet( wheel, this, SATURN, "Saturn", new Color(107,57,57), 9.4071 );
	        planets[URANUS] = new ChartPlanet( wheel, this, URANUS, "Uranus", new Color(5,115,5), 3.98245 );
	        planets[NEPTUNE] = new ChartPlanet( wheel, this, NEPTUNE, "Neptune", new Color(2,2,255), 3.8099 );
	        planets[PLUTO] = new ChartPlanet( wheel, this, PLUTO, "Pluto", new Color(79,79,79), 0.2352 );
            planets[NORTHNODE] = new ChartPlanet( wheel, this, NORTHNODE, "NorthNode", new Color(129,139,129), 0.27249 );
            planets[SOUTHNODE] = new ChartPlanet( wheel, this, SOUTHNODE, "SouthNode", new Color(129,139,129), 0.27249 );
            planets[CHIRON] = new ChartPlanet( wheel, this, CHIRON, "Chiron", new Color(159,159,109), 0.01406 );
            planets[CERES] = new ChartPlanet( wheel, this, CERES, "Ceres", new Color(100,170,150), 0.00000 );
            planets[SEDNA] = new ChartPlanet( wheel, this, SEDNA, "Sedna", new Color(160,180,160), 0.00000 );
	        // initialize planet display (load graphics, etc.)
	        for (int n=0; n < planets.length; n++)
	            planets[n].paint( null );
	    }
	    public ChartWheel getChartWheel()
	    {
	      return ChartWheel.this;
	    }
	    void refresh( double t )
	    {
            this.t = t;
	        for (int n=0; n < planets.length; n++)
	            planets[n].refresh( t );
	    }
	    void paint( Graphics gr )
	    {
          for (int n=0; n < planets.length; n++)
            if (isPlanetVisible( planets[n].name ))
                planets[n].paint( gr );
	    }
	    ChartPlanet planetAtPoint( int x, int y )
	    {
          for (int n=0; n < planets.length; n++)
            if (isPlanetVisible(planets[n].name)  &&  planets[n].inside( x, y ))
                return planets[n];
          return null;
	    }
	}

    // each planet is controlled by a 'Planet' object
	protected PlanetRing mainRing;
	protected PlanetRing outerRing;
	
	// graph
	protected MotionChart motionChart;
	protected boolean showMotionChart;
	
	// GRAPHICS-RELATED
	// Applet that contains this window (if the parent is an Applet, otherwise null)
	JavaChart parent = null;
	// URL where images are located
	URL imageLocation = null;
	// media tracker used to load images
	MediaTracker imgTracker = null;
	// the current graphical content is stored here...
	protected Image displayBuffer = null;
	// current error/status/caption message
	private String caption = "";
	private double captionExpires = 0;
	
	// LAYOUT-RELATED
  int rBounds;     // outer radius of drawing area
	int rOuter;			// outer radius of chart
  int rInner;         // inner radius of zodiac band
  int rStars;         // innermost stars
	int rSignSyms;		// where to draw sign symbols
	int rPlanetDots;	// where to draw dots for planets
  int rPlanetDots2;   // where to draw secondary dots for planets
	int rPlanetSyms;	// where to draw planetary symbols/graphics
	int cx, cy;			// center of chart wheel
	int widthHelp;		// width of help button
	int widthCaption;	// width of current caption/copyright
	Font fontDate = null;	// font for current date
	Font fontCprt = null;	// font for copyright notice
	boolean needCompose = false;	// frame needs recomposing
	
	// MOUSE-RELATED
	private ChartPlanet overPlanet = null;	// index of planet under mouse, or -1
	private ChartPlanet clickPlanet = null; // index of clicked-on planet, or -1
	private Point clickOffset;				// offset of initial click, relative to center of clicked planet
	private int mouseX = -1;				// current mouse position (-1 if not inside control)
	private int mouseY = -1;
  private Stars.Star overStar = null;     // star under mouse

  private ArrayList<ChartListener> chartListeners = new ArrayList<ChartListener>();
    
    public static interface ChartListener
    {
        public void mouseOverPlanet( ChartPlanet planet );
        public void mouseOverObject( String name );
        public void timeChanged( double t );
    }
    public void addChartListener( ChartListener l )
    {
        chartListeners.add( l );
    }
    public void removeChartListener( ChartListener l )
    {
        chartListeners.remove( l );
    }
    private void setOverPlanet(ChartPlanet overPlanet, boolean alwaysUpdate)
    {
        if (overPlanet != this.overPlanet  ||  alwaysUpdate)
        {
            this.overPlanet = overPlanet;
            for (int n=0; n < chartListeners.size(); n++)
                chartListeners.get( n ).mouseOverPlanet( overPlanet );
        }
    }
    /*
    private int getOverPlanet()
    {
        return overPlanet;
    }
    */
    private String oldObject = null;
    private void fireOverObject( String descr )
    {
        if (descr == null  &&  oldObject == null)
            return;
        if (descr != null  &&  oldObject != null)
            if (descr.equals( oldObject ))
                return;
        for (int n=0; n < chartListeners.size(); n++)
            chartListeners.get( n ).mouseOverObject( descr );
        oldObject = descr;
        // not over an object anymore, but still over a planet
        if (descr == null  &&  overPlanet != null)
            setOverPlanet( overPlanet, true );
    }
    private void fireTimeChanged( double t )
    {
      for (ChartListener l : chartListeners)
        l.timeChanged( t );
    }

	//
	// Construct.
	//
	public ChartWheel( JavaChart parent, URL imageLocation )
	{
		// store applet (optional) and code base (URL where images are to be found)
		this.parent = parent;
		this.imageLocation = imageLocation;

		// set initial time
		time = currentJD();

		// reset 'earth buffer'
		lastT = 0;
		
        otherBodies.put( "Chiron", false );
        otherBodies.put( "Ceres", false );
        otherBodies.put( "Sedna", false );
        otherBodies.put( "NorthNode", false );
        otherBodies.put( "SouthNode", false );
		
		setPlaceForZone( TimeZone.getDefault() );
		
        // media tracker for images
		imgTracker = new MediaTracker( this );
        
        // mouse handling
        Mouser mouser = new Mouser();
        addMouseListener( mouser );
        addMouseMotionListener( mouser );

		// SET UP PLANET CALCULATORS
        mainRing = new PlanetRing();
		
		// load sign graphics
		signs = new Image[12];
		for (int n=0; n < 12; n++)
		{
			URL urlSign;
			try
			{
				urlSign = new URL( imageLocation, "sign" + (n+1) + "_lt.gif" );
			}
			catch( MalformedURLException x )
			{
				continue;
			}
            if (parent != null)
                signs[n] = parent.getImage( urlSign );
            else
                signs[n] = Toolkit.getDefaultToolkit().getImage( urlSign );
			imgTracker.addImage( signs[n], 100 + n );
		}
		// allow graphics to load
		try
		{
			imgTracker.waitForAll();
		}
		catch( InterruptedException x )
		{
		}
		// calculate planetary positions, but don't redraw the display yet
		refresh( true, false );

		// initialize the 'spin' thread
		spinElapsed = 0;
		spinPrevious = currentJD();
		spinner = new Thread( this );
		spinner.setPriority( Thread.MIN_PRIORITY );
        
        // track size
        addComponentListener( new ComponentAdapter()
        {
            public void componentResized(ComponentEvent e)
            {
                layout1( e.getComponent().getSize() );
            }
        } );
	}


	//////////////////////////////////////////////////////////////////////////
	//
	// PROPERTIES (time, scale, spin rate, 'now' flag & 'log scale' flag)
	//

	//
	// Get current time, in julian days.
	//
	public double getJD()
	{
		return time;
	}
	//
	// Set current time
	//
	public void setJD( double t )
	{
		// set new time
		_setJD( t );
    fireTimeChanged( t );
	}
	/**
	 * Timezone.
	 */
	public TimeZone getTimeZone()
	{
	  if (parent == null)
	    return TimeZone.getDefault();
	  else
      return parent.time.getTimezone();
	}
	/**
	 * How accurate the time is (maximum error).
	 */
	public double getTimeAccuracy()
    {
      return time_accuracy;
    }
	public void setTimeAccuracy(double timeAccuracy)
    {
      time_accuracy = timeAccuracy;
      refresh();
    }
	/**
	 * Format a time in the timezone and format for this wheel.
	 */
	public String formatJD( double jd, boolean shortFormat )
	{
	  DateFormat fmt = timeFormat;
	  if (shortFormat)
	  {
	    fmt = new SimpleDateFormat( "dd-MMM-yyyy" );
	    fmt.setTimeZone( timeFormat.getTimeZone() );
	  }
	  return fmt.format( new Date( jd_to_systime(jd) ) );
	}
	//
	// Set current time - this version does NOT reset the 'spinner' Thread.
	//
	private void _setJD( double t )
	{
	  /*
	    // partial class load seems to cause a problem where time gets randomly set to 0AD-ish
	    if (t < 2000000)
	      throw new InternalError( "HERE!" );
	  */
    // clip to range: 1AD to 3000AD
		if (t < 1721411)
			t = 1721411;
		if (t > 2816795)
			t = 2816795;
		// set new time
		time = t;
		// update display - update more often if chart is rotating with horizon
		double tThreshold = enableChartRotation ? 0.0002 : ROUGH_TIME_THRESHOLD;
		if (Math.abs( t - t_disp ) > tThreshold)
		{
			t_disp = t;
      updateAC();
			refresh();
		}
    // planets moving over mouse should generate the same results as the mouse moving over a planet
    check_mouseOverPlanet( mouseX, mouseY );
    
    // this belongs here but causes problems with the time entry field that need to be solved first
    //fireTimeChanged( t );
	}
	//
	// Get/set time, in terms of a 'Date' object.
	//
	public Date getDate()
	{
		Date dt = new Date();
		dt.setTime( jd_to_systime( time ) );
		return dt;
	}
  private long jd_to_systime( double time )
  {
    return (long)((time - 2440587.5) * 86400000.0);
  }
	public void setDate( Date dt )
	{
		setJD( dt.getTime() / 86400000.0 + 2440587.5 );
	}
	// Get/set place.
  public double[] getPlace()
  {
      return new double[] { curLat, curLng };
  }
	public void setPlace( double lat, double lng )
	{
    if (lat == curLat  &&  lng == curLng)
        return;
		curLat = lat;
		curLng = lng;
		updateAC();
        refresh();
	}
	// Specify timezone (currently used only for displaying dates)
	public void setTimeZone( TimeZone zone )
	{
	    timeFormat.setTimeZone( zone );
	}
	
	public void setPlaceForZone( TimeZone zone )
	{
        MiniAtlas.Entry tzLoc = TimeZoneTools.getTimeZoneLocation( zone );
        if (tzLoc != null)
        {
            curLng = tzLoc.lng * d2r;
            curLat = tzLoc.lat * d2r;
        }
	}
    /**
     * Get/set display options all at once, as a string.
     */
    public String getDisplayInfo()
    {
        String info = 
            "ac=" + (isEnableChartRotation()?1:0)
          + ",h=" + (isHeliocentric()?1:0)
          + ",sq=" + (isSquare()?1:0)
          + ",z=" + getZodiac();
        info += ",incl=";
        boolean first = true;
        for (ChartPlanet planet : mainRing.planets)
          if (isPlanetVisible( planet.name ))
          {
            if (! first)
              info += " ";
            info += planet.name;
            first = false;
          }
        return info;
    }
    public void setDisplayInfo( String info )
    {
        String parts[] = info.split( "\\," );
        for (String part : parts)
        {
            if (part.startsWith( "ac=" ))
                setEnableChartRotation( part.endsWith( "1" ) );
            else if (part.startsWith( "nn=" ))
                setDisplayBody("NorthNode", part.endsWith( "1" ) );
            else if (part.startsWith( "h=" ))
                setHeliocentric( part.endsWith( "1" ) );
            else if (part.startsWith( "sq=" ))
                setSquare( part.endsWith( "1" ) );
            else if (part.startsWith( "z=" ))
                setZodiac( Integer.parseInt(part.substring(2)) );
            else
            {
              // 'part' 
              String name = part.split( "=" )[0];
              if (ChartData.isPlanetName( name )  ||  name.equals("NorthNode")  ||  name.equals("SouthNode"))
                setDisplayBody( name, part.endsWith( "1" ) );
            }
        }
    }
	
	
	private void updateAC()
	{
		if (enableChartRotation)
		{
			// calculate ascendant for chart orientation
			double ac = calcAscendant( getJD(), curLng, curLat );
			chartRotation = -ac;
			// recompose soon
			needCompose = true;
		}
	}
	/**
	 * Whether to display other bodies.
	 */
	public void setDisplayBody( String name, boolean show )
	{
	  otherBodies.put( name, show );
	  refresh();
	}
	/**
	 * Enable or disable *all* bodies at once.
	 */
  public void setDisplayAllBodies( boolean show )
  {
    for (ChartPlanet p : mainRing.planets)
      otherBodies.put( p.name, show );
    refresh();
  }
	/**
   * Set the caption shown in the corner.
	 */
	public void setCaption( String str, int expiresIn )
	{
		if (str == caption)
			return;
		caption = str;
		if (caption == "")
			// don't blank the caption
			captionExpires = 0;
		else
			// blank the caption after indicated period of time
			captionExpires = currentJD() + expiresIn/86400.0;
		// refresh display unless initialization is not complete
		if (spinner != null)
			refresh( false, true );
	}
	public void setCaption( String str )
	{
		setCaption( str, 0 );
	}
	/**
	 * Turn the outer ring on or off and set its time.
	 */
	public void setOuterRing( Double time )
	{
	    if (time != null)
	    {
  	        outerRing = new PlanetRing();
  	        // re-layout with ring enabled to get correct value for rOuter
  	        layout1( null );
  	        // spread out planet symbols to avoid overlaps
            outerRing.refresh( time );
	    }
	    else
	        outerRing = null;
        // lay out to adjust for added ring
        layout1( null );
        // make sure new ring appears
        refresh();
	}
	public Double getOuterRingTime()
	{
	    if (outerRing != null)
	        return outerRing.t;
	    else
	        return null;
	}
	//
	// Enable/disable graphics while changing options.
	//
	public void setEnableGraphics( boolean state )
	{
		enableGraphics = state;
	}

	//////////////////////////////////////////////////////////////////////////
	//
	// GET INFORMATION ABOUT THE PLANETS
	//

	//
	// Get the name of a planet.
	//
	/*
	public String planetName( int planet )
	{
		if (planet < 0  ||  planet >= planets.length)
			return "";
		else
			return planets[planet].name;
	}
	*/

    private double calcAscendant( double t, double lng, double lat )
	{
      double ac = ChartData.calcAscendant( t, lng, lat );
      return makeZodiacAdjustment( ac );
	}
	private double calcMidheaven( double t, double lng )
	{
      double mc = ChartData.calcMidheaven( t, lng );
      return makeZodiacAdjustment( mc );
	}
	double calcAscendant( double t )
	{
	  return calcAscendant( t, curLng, curLat );
	}
	double calcMidheaven( double t )
	{
      return calcMidheaven( t, curLng );
	}
    double geoLongitude( ChartPlanet planet, double t )
    {
        double lng;
        if (planet.name.equals( "NorthNode" ))
            lng = ChartData.calculateMeanLunarNode( t );
        else if (planet.name.equals( "SouthNode" ))
            lng = mod2pi( ChartData.calculateMeanLunarNode( t ) + Math.PI );
        else
            lng = ChartData.geoLongitude( planet.name, t );
        return makeZodiacAdjustment( lng );
    }
    public double helioLongitude( ChartPlanet planet, double t )
    {
      double lng = ChartData.helioLongitude( planet.name, t );
      return makeZodiacAdjustment( lng );
    }
	

	//////////////////////////////////////////////////////////////////////////
	//
	// USER INTERACTION (mouse-related)
	//
	//  The user can drag planets, and as the mouse passes over planets, the
	//  parent is notified.
	//

	/**
	 * Find a planet at a given point.
	 */
	public ChartPlanet planetAtPoint( int x, int y )
	{
	  if (isGraphEnabled())
	    return null;
    ChartPlanet pl = mainRing.planetAtPoint( x, y );
    if (pl != null)
      return pl;
    if (outerRing != null)
      pl = outerRing.planetAtPoint( x, y );
    return pl;
	}
    /**
     * Find a star at a given point.
     */
    public Stars.Star starAtPoint( int x, int y )
    {
      if (! showStars)
          return null;
      if (isGraphEnabled())
        return null;
      // make sure point is within star radius band
      double r = Math.sqrt( (x-cx)*(x-cx) + (y-cy)*(y-cy) );
      if (r < rStars-6  ||  r > rStars+10)
          return null;
      double acOffs = isEnableChartRotation() ? chartRotation : 0;
      double a = mod2pi( Math.atan2( cy - y, x - cx ) - acOffs + Math.PI );
      a -= makeZodiacAdjustment( 0 );
      double t = getJD();
      double aMin = Math.atan( 4f / rStars );
      Stars.Star stars[] = Stars.getInstance().getStars();
      Stars.Star closest = null;
      double dClosest = aMin;
      for (int n=0; n < stars.length; n++)
      {
          double d = mod2pi( a - stars[n].getLongitude( t ) );
          if (d > Math.PI)
              d -= Math.PI * 2;
          d = Math.abs( d );
          if (d < dClosest)
          {
              closest = stars[n];
              dClosest = d;
          }
      }
      return closest;
    }

  //
	// Get index of planet under the mouse (or -1 if none).
	//
	public ChartPlanet getPlanetUnderMouse()
	{
		if (clickPlanet != null)
			return clickPlanet;
		return overPlanet;
	}

    private class Mouser implements MouseListener, MouseMotionListener
    {
        private int prevX = -999;
        public void mouseClicked(MouseEvent e)
        {
        }
        public void mouseDragged(MouseEvent e)
        {
            if (! enableInteraction)
              return;
            if (isGraphEnabled())
            {
              if (prevX == -999)
                prevX = e.getPoint().x;
              int curX = e.getX();
              int slide = curX - prevX;
              if (slide != 0)
              {
                double dt = slide * motionChart.getTimeScale();
                double newTime = getJD() - dt;
                setJD( newTime );
                refresh( true, true );
                prevX = curX;
                // inform listeners that we changed our own time
                fireTimeChanged( newTime );
              }
              return;
            }
            int x = e.getX();
            int y = e.getY();
//          // no single-pixel moves
//          if (Math.abs( x - mouseX ) <= 1  ||  Math.abs( y - mouseY ) <= 1)
//              return true;
//          // only allow solar system updates every so often - NETSCAPE queues up drag events
//          double tNow = currentJD();
//          if (tDragPrev == 0)
//              tDragPrev = tNow;
//          else if (tNow - tDragPrev < 0.2 / 86400)
//              return true;
            // drag the clicked planet
            if (clickPlanet != null)
            {
                // determine 'ideal' point for center for planet (based on mouse positions)
                Point ideal = new Point( x - clickOffset.x, y - clickOffset.y );
                // pick a starting increment
                double dt;
                double t0 = clickPlanet.ring.t;
                double AC0 = calcAscendant( t0, curLng, curLat );
                ChartData.OrbElems elems = ChartData.getOrbitalElements( clickPlanet.name, t0 );
                double dt_resolveFactor = -0.5;
                if (elems != null  &&  elems.a > 4) // past Mars
                    dt = 0.01 * 36525;  // 1 year
                else if (clickPlanet.name.equals( "Sun" ))
                    dt = 2;
                else if (clickPlanet.name.equals( "Moon" ))
                    dt = 2;
                else if (clickPlanet.name.equals( "Mercury" ))
                    dt = 30;
                else if (clickPlanet.name.equals( "Venus" ))
                    dt = 30;
                else if (elems == null)
                    dt = 8;
                else
                    dt = (0.2 / elems.dLng) * 36525;  // about 10 degrees
                double dtOrig = dt;
                // iterate to find minima (shortest distance to ideal point)
                double err0, err1 = 0;
                double t = t0;
                for (int n=0; n < 40; n++)
                {
                    err0 = err1;
                    // calculate error (distance of planet from ideal location)
                    err1 = findDragError(ideal, t);
                    // if distance increases, reverse direction of search (and reduce time step)
                    if (n != 0)
                        if (err1 > err0)
                            dt *= dt_resolveFactor;
                    // advance by current time step
                    t += dt;
                    // lock AC in place, if it is being used to rotate the chart
                    if (isEnableChartRotation()  &&  clickPlanet.ring == mainRing)
                        t = closestACTime( t, AC0 );
                    // stop when time step gets sufficiently small
                    if (Math.abs(dt) < ROUGH_TIME_THRESHOLD/4)
                        break;
                }

                // do a linear sweep in each direction to deal with retrogrades
                double tBetter = Double.NaN;
                for (int n=1; n < 20; n++)
                {
                  double tTry = t - n*dtOrig/10;
                  double err2 = findDragError( ideal, tTry );
                  if (err2 < err1)
                  {
                    tBetter = tTry;
                    err1 = err2;
                  }
                  tTry = t + n*dtOrig/10;
                  err2 = findDragError( ideal, t + n*dtOrig/10 );
                  if (err2 < err1)
                  {
                    tBetter = tTry;
                    err1 = err2;
                  }
                }
                if (! Double.isNaN( tBetter ))
                {
                  if (isEnableChartRotation()  &&  clickPlanet.ring == mainRing)
                    t = closestACTime( tBetter, AC0 );
                  else
                    t = tBetter;
                }
                // update solar system to final time - clear 'now' flag
                if (clickPlanet.ring == outerRing)
                  outerRing.refresh( t );
                else
                  setJD( t );
                // TODO figure out why
                // do a full refresh after the drag ends - the moon's position was observed to not be updated
                refresh();
                setOverPlanet(clickPlanet, true);
                // inform listeners that we changed our own time
                fireTimeChanged( t );
            }
        }
        private double findDragError(Point ideal, double t)
        {
          double err1;
          clickPlanet.refresh( new Double(t) );
          Point p = clickPlanet.posScreenDot2;
          err1 = Math.sqrt( (long)(p.x - ideal.x) * (p.x - ideal.x)
              + (long)(p.y - ideal.y) * (p.y - ideal.y) );
//          System.out.printf( "%3d,%3d  %3d,%3d  %6.1f  %10.4f\n", ideal.x, ideal.y, p.x, p.y, err1, t );
          return err1;
        }
        public void mouseEntered(MouseEvent e)
        {
            mouseMoved( e );
        }
        public void mouseExited(MouseEvent e)
        {
            mouseX = -1;
            mouseY = -1;
            setOverPlanet(null,false);
        }
        public void mouseMoved(MouseEvent e)
        {
            if (isGraphEnabled())
              return;
            int x = e.getX();
            int y = e.getY();
            mouseX = x;
            mouseY = y;
            if (clickPlanet == null  &&  square)
            {
              ChartPlanet np = check_mouseOverPlanet( x, y );
              boolean overSomething = false;
              if (np != null)
                overSomething = true;
              else
                for (int n=0; n < 12; n++)
                  if (getSquareSignBounds( n ).contains( x, y ))
                  {
                    String descr = signNames[n];
                    if (isEnableChartRotation())
                    {
                      int acSign = (int)((calcAscendant( mainRing.t, curLng, curLat )/d2r)/30);
                      int house = (n - acSign + 12) % 12 + 1;
                      String ord = (house == 1) ? "st" : (house % 10 == 2) ? "nd" : (house == 3) ? "rd" : "th";
                      descr += ", " + house + ord + " house";
                    }
                    fireOverObject( descr );
                    overSomething = true;
                    break;
                  }
              if (! overSomething)
                fireOverObject( null );
            }
            if (clickPlanet == null  &&  ! square)
            {
                boolean isOverPlanet = false;
                int r = (int)Math.sqrt( (x - cx)*(x - cx) + (y - cy)*(y - cy) );
                // show information about...
                // - planets
                if (r > rPlanetDots - 9)
                {
                    ChartPlanet np = check_mouseOverPlanet( x, y );
                    isOverPlanet = (np != null);
                    if (! isOverPlanet  &&  r >= rPlanetDots  &&  r <= rOuter)
                    {
                        Stars.Star newOverStar = starAtPoint( x, y );
                        if (newOverStar != overStar)
                        {
                            overStar = newOverStar;
                            refresh();
                        }
                        if (overStar != null)
                        {
                            double lng = makeZodiacAdjustment( overStar.getLongitude( getJD() ) );
                            fireOverObject( overStar.getName() + ", " + formatDegrees(lng) + ", v=" + overStar.getMagnitude() + ", d=" + overStar.getDistance() );
                        }
                        else
                        {
                            double acOffs = isEnableChartRotation() ? chartRotation : 0;
                            double a = mod2pi( Math.atan2( cy - y, x - cx ) - acOffs + Math.PI );
                            double ac = -chartRotation;
                            double mc = calcMidheaven( getJD(), curLng );
                            if (isEnableChartRotation()  &&  Math.abs( angleDelta( a, ac ) ) < 2 * d2r)
                                fireOverObject( "Ascendant, " + formatDegrees(ac) );
                            else if (isEnableChartRotation()  &&  Math.abs( angleDelta( a, ac+Math.PI ) ) < 2 * d2r)
                                fireOverObject( "Descendant, " + formatDegrees(ac+Math.PI) );
                            else if (isEnableChartRotation()  &&  Math.abs( angleDelta( a, mc ) ) < 2 * d2r)
                                fireOverObject( "Midheaven, " + formatDegrees(mc) );
                            else if (isEnableChartRotation()  &&  Math.abs( angleDelta( a, mc+Math.PI ) ) < 2 * d2r)
                                fireOverObject( "Nadir, " + formatDegrees(mc+Math.PI) );
                            else
                                fireOverObject( signNames[ (int)(a / (Math.PI / 6)) % 12 ] );
                        }
                    }
                    else if (! isOverPlanet)
                        fireOverObject( null );
                }
                // - aspects
                if (! isOverPlanet  &&  aspectList != null  &&  r < rInner)
                {
                    String descr = aspectList.findNearbyAspect( x, y );
                    if (descr == null  &&  isEnableChartRotation())
                    {
                        // houses
                        double a = mod2pi( Math.atan2( cy - y, x - cx ) - chartRotation + Math.PI );
                        double ac = -chartRotation;
                        double mc = calcMidheaven( getJD(), curLng );
                        double cusps[] = ChartData.calcCusps( ac, mc );
                        int house = ChartData.findHouse( a, ac, mc, cusps );
                        descr = house + ((house==1)?"st":(house==2)?"nd":(house==3)?"rd":"th") + " house";
                    }
                    fireOverObject( descr );
                }
            }
        }
        public void mousePressed(MouseEvent e)
        {
            int x = e.getX();
            int y = e.getY();
            // detect click over a planet
            clickPlanet = planetAtPoint( x, y );
            if (clickPlanet != null)
            {
                // store offset of click from center of planet
                clickOffset = new Point( x - clickPlanet.posScreenDot2.x, y - clickPlanet.posScreenDot2.y );
                setOverPlanet(clickPlanet, true);
            }
            else
                clickPlanet = null;
            prevX = e.getX();
        }
        public void mouseReleased(MouseEvent e)
        {
            clickPlanet = null;
            prevX = -999;
        }
    }

    //
	// Mouse click & drag.
	//
	double tDragPrev = 0;

    // find the time closest to the given time where the AC is at the given location
    double closestACTime( double t0, double AC )
    {
        for (int n=0; n < 9; n++)
        {
            double error = calcAscendant( t0, curLng, curLat ) - AC;
            if (error > Math.PI)
                error -= 2*Math.PI;
            if (error < -Math.PI)
                error += 2*Math.PI;
            double adjust = error * 0.75 / (Math.PI*2);
            t0 -= adjust;
        }
        return t0;
    }

    // TODO reconcile with mouse-over-object code - confusing!
    private ChartPlanet check_mouseOverPlanet(int x, int y)
    {
        // find planet
        ChartPlanet over = (clickPlanet != null) ? clickPlanet : planetAtPoint( x, y );
        // inform parent (so that it can update the mouse or other information)
        if (over != overPlanet)
        {
            setOverPlanet(over, false);
            if (over == null)
                setCursor( Cursor.getDefaultCursor() );
            else
                setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
        }
        return over;
    }
    
	//////////////////////////////////////////////////////////////////////////
	//
	// DISPLAY - internal functions related to graphical representation.
	//

	//
	// Repaint...
	//
	public void update( Graphics g )
	{
		if (g == null)
			return;
		// don't redraw background
		paint( g );
	}
	synchronized public void paint( Graphics g )
	{
		if (g == null)
			return;
		// make sure there's something to draw
		if (displayBuffer == null)
			refresh( false, true );
		// draw from current buffer
		g.drawImage( displayBuffer, 0, 0, this );
	}

    /**
     * Get the current drawing size.
     */
    private Dimension getDisplaySize()
    {
        if (displayBuffer != null)
            return new Dimension( displayBuffer.getWidth(null), displayBuffer.getHeight(null) );
        else
            return getSize();
    }

    //
	// When the canvas is reshaped, create a new buffer.
	//
	public void layout1( Dimension newSize )
	{
		Dimension sz = (newSize == null) ? getDisplaySize() : newSize;
		// create fonts
		fontDate = new Font( "System", Font.PLAIN, 10 );
		// set radii and center - leave room for copyright
		int rMax = sz.width;
		cx = rMax / 2;
		int hWheel = sz.height;
		cy = hWheel / 2;
		if (hWheel < rMax)
			rMax = hWheel;
		// find radii
		rMax /= 2;
        if (outerRing != null)
            rMax -= 25;
        if (square)
        {
          // make room for copyright
          rMax -= 6;
          cy -= 6;
        }
    rBounds = rMax;
		rOuter = rMax - 6 /* margin for AC/MC arrows */;
        rInner = rMax - ((rOuter<150)?44:54);
        rStars = rInner + 10;
        rSignSyms = (rInner+rOuter)/2 + 6;
        rPlanetDots2 = rInner;
        rPlanetDots = rInner - 40;
        rPlanetSyms = rInner - 20;
        /* was:
        rPlanetDots = rMax - 54;
        rPlanetSyms = rMax - 32;
        */
        if (outerRing != null)
        {
          outerRing.rPlanetDots = 0;
          outerRing.rPlanetDots2 = rOuter + 2;
          outerRing.rPlanetSyms = rOuter + 20;
          outerRing.symbolScale = 0.8f;
        }
        mainRing.rPlanetDots = rPlanetDots;
        mainRing.rPlanetDots2 = rPlanetDots2;
        mainRing.rPlanetSyms = rPlanetSyms;
        // use smaller fonts for tiny charts
        if (rOuter < 180)
        {
          mainRing.symbolScale = 0.85f;
          if (outerRing != null)
            outerRing.symbolScale = 0.7f;
        }
        else
        {
          mainRing.symbolScale = 1;
        }
        // build new display buffer
		if (displayBuffer == null  ||  sz.width != displayBuffer.getWidth(this)  ||  sz.height != displayBuffer.getHeight(this))
		{
            if (sz.width > 0  &&  sz.height > 0)
            {
                // create new screen buffer
                displayBuffer = createImage( sz.width, sz.height );
                // update display
                refresh();
            }
		}
	}

	//
	// Convert the location of a point in the solar
	// system (in AU) to a location in this 'Component'.
	//
	Point polarPosition( double longitude, int radius )
	{
		// calculate position
		double a = longitude;
		if (enableChartRotation)
			a += chartRotation;
		Point pt = new Point( cx - (int)(Math.cos(a)*radius), cy + (int)(Math.sin(a)*radius) );
		return pt;
	}
    // convert polar coordinates to x-y coordinates (returned in referenced arrays)
	void polarXY( double longitude, int radius, int px[], int py[], int index )
	{
		Point p = polarPosition( longitude, radius );
		px[index] = p.x;
		py[index] = p.y;
	}
	// draw a line, given polar coordinates (angles are in ecliptic longitude)
	void polarLine( Graphics gr, double a1, int r1, double a2, int r2 )
	{
		Point p1 = polarPosition( a1, r1 );
		Point p2 = polarPosition( a2, r2 );
		gr.drawLine( p1.x, p1.y, p2.x, p2.y );
	}
	// draw a triangle pointing toward or away from the center of the wheel
	void polarTriangle( Graphics gr, double a, double da, int rBase, int rTip )
	{
		int px[] = new int[3];
		int py[] = new int[3];
		polarXY( a, rTip, px, py, 0 );
		polarXY( a + da, rBase, px, py, 1 );
		polarXY( a - da, rBase, px, py, 2 );
		gr.fillPolygon( px, py, 3 );
	}


	//
	// Refresh the display - all updates go through this function.
	// - planetary positions are calculated from orbital elements if '_calculate' is set.
	//   calculated (from time and orbital elements).
	// - planetary positions and radii are always re-scaled
	// - the screen buffer is re-built if '_compose' is set.
	//
	protected void refresh()
	{
		refresh( true, true );
	}
    /**
     * Paint to the given image.
     */
    public void paintBuffer( Image toImage )
    {
        Image saveBuffer = displayBuffer;
        // draw to targeted image
        displayBuffer = toImage;
        // lay out the canvas
        layout1( null );
        // draw everything
        refresh( true, true );
        displayBuffer = saveBuffer;
    }
    protected void refresh( boolean _calculate, boolean _compose )
    {
      refresh( _calculate, _compose, true );
    }
	synchronized protected void refresh( boolean _calculate, boolean _compose, boolean update )
	{
		// recalculate planetary positions (if requested) and find current positions
    if (_calculate)
    {
        if (outerRing != null)
 	        outerRing.refresh( outerRing.t );
        mainRing.refresh( time );
    }
		// spread out planet symbols to avoid overlaps
		spreadOutPlanets( mainRing.planets );
		/*  FOR SOME REASON THIS BREAKS LAYOUT BADLY
		 *  TODO fix this
		if (outerRing != null)
		    spreadOutPlanets( outerRing.planets );
		*/
		// only refresh graphics if enabled
		if (! enableGraphics)
			return;
		// rebuild screen buffer if requested
		if (_compose)
		{
      Dimension sz = getDisplaySize();
			
			// save time of screen refresh
			lastRefresh = currentJD();
			// make sure buffer exists
			if (displayBuffer == null)
			{
				if (sz.width == 0  ||  sz.height == 0)
					return;
				displayBuffer = createImage( sz.width, sz.height );
			}
			// get drawing context
			Graphics gr = displayBuffer.getGraphics();
	    {
	      RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
	      renderHints.put( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY );
	      ((Graphics2D)gr).setRenderingHints(renderHints);
	    }
			
      // draw background
      drawBackground(gr);
			// draw chart
      if (motionChart != null  &&  showMotionChart)
      {
        Rectangle b = new Rectangle( 0, 0, getBounds().width, getBounds().height );
        motionChart.setBounds( b );
        motionChart.draw( gr );
      }
      else if (square)
        drawSquareChart(gr);
      else
        drawCircularChart(gr);
      
      drawCopyright( gr );

      // repaint immediately
      if (update)
        update( getGraphics() );
		}
	}
	/**
	 * Draw the copyright message in the corner.
	 */
  protected void drawCopyright(Graphics gr)
  {
    int w = getWidth();
    int h = getHeight();
    if (w <= 0)
      w = 2*rBounds;
    if (h <= 0)
      h = 2*rBounds;
    Color fadeColor = new Color( paperColor.getRed(), paperColor.getGreen(), 7*paperColor.getBlue()/8, 176 );
    gr.setFont( new Font( gr.getFont().getName(), Font.PLAIN, 9 ) );
    FontMetrics fm = gr.getFontMetrics();
    String copyright0 = "astrology-clock.com";
    int sw0 = fm.stringWidth( copyright0 );
    gr.setColor( fadeColor );
    gr.fillRect( w - sw0 - 3, 0, sw0 + 4, fm.getHeight() );
    gr.setColor( new Color(96,192,192) );
    gr.drawString( copyright0, w - 1 - sw0, fm.getAscent() );

    gr.setFont( new Font( gr.getFont().getName(), Font.PLAIN, 8 ) );
    fm = gr.getFontMetrics();
    String copyright1 = "(c) 1998-2011";
    String copyright2 = "by Helianthus, Inc.";
    gr.setColor( fadeColor );
    int sw2 = fm.stringWidth( copyright2 );
    gr.fillRect( w - sw2 - 3, h - fm.getHeight()*2, sw2 + 4, fm.getHeight()*2 );
    gr.setColor( new Color(128,160,128) );
    gr.drawString( copyright1, w - 1 - fm.stringWidth( copyright1 ), h - fm.getHeight()*2 + fm.getAscent() + 1 );
    gr.drawString( copyright2, w - 1 - sw2, h - fm.getHeight() + fm.getAscent() );
  }
	
    /**
     * Jyotish style.
     */
    private void drawSquareChart(Graphics gr)
    {
      gr.setColor( insideColor );
      gr.fillRect( cx-rOuter, cy-rOuter, rOuter*2, rOuter*2 );
      gr.setColor( lineColor );
      gr.drawRect( cx-rOuter, cy-rOuter, rOuter*2, rOuter*2 );
      // get sorted list of planet
      ChartPlanet planetsInOrder[] = new ChartPlanet[mainRing.planets.length];
      for (int nPl=0; nPl < planetsInOrder.length; nPl++)
        planetsInOrder[nPl] = mainRing.planets[nPl];
      Arrays.sort( planetsInOrder );
      // ascendant
      int acSign = -1;
      if (isEnableChartRotation())
      {
        double AC = calcAscendant( mainRing.t, curLng, curLat );
        acSign = (int)((AC/d2r)/30);
        double acDgrs = (AC/d2r)%30;
        Rectangle ACbounds = getSquareSignBounds( acSign );
        gr.setColor( degreeNumberColor );
        gr.setFont( new Font( gr.getFont().getName(), Font.PLAIN, 10 ) );
        FontMetrics fm = gr.getFontMetrics();
        gr.drawString( String.format( "%.1f", acDgrs ), ACbounds.x + fm.getHeight() + 2, ACbounds.y + ACbounds.height - 2 - fm.getDescent() );
        gr.setColor( horizonColor );
        gr.drawLine( ACbounds.x, ACbounds.y + ACbounds.height, ACbounds.x + ACbounds.width, ACbounds.y );
      }
      // draw signs and their contents
      for (int sign=0; sign < 12; sign++)
      {
        // box around each sign
        Rectangle rSign = getSquareSignBounds(sign);
        gr.setColor( lineColor );
        gr.drawRect(rSign.x, rSign.y, rSign.width, rSign.height);
        // sign/zodiac symbols
        Point pUR = new Point( rSign.x + rSign.width - 2, rSign.y + 2 );
        int imgW = signs[sign].getWidth(this);
        int imgH = signs[sign].getHeight(this);
        //if (rOuter < 150)
        {
          imgW = rOuter * imgW / 300;
          imgH = rOuter * imgH / 300;
        }
        gr.drawImage(signs[sign], pUR.x - imgW, pUR.y, imgW, imgH, this);
        // house
        if (acSign != -1)
        {
          int house = (sign - acSign + 12) % 12;
          gr.setColor( houseCuspColor );
          gr.setFont( new Font( gr.getFont().getName(), Font.PLAIN, 14 ) );
          FontMetrics fm = gr.getFontMetrics();
          String txt = String.valueOf(house+1);
          gr.drawString( txt, pUR.x - fm.stringWidth( txt ), rSign.y + rSign.height - 2 - fm.getDescent() );
        }
        // each planet in this sign
        int numInSign = 0;
        int px = rSign.x + rSign.width/5;
        int py = rSign.y + 2;
        gr.setFont( new Font( gr.getFont().getName(), Font.PLAIN, 10 ) );
        FontMetrics fm = gr.getFontMetrics();
        int signDir = (sign >= 7) ? -1 : 1;
        for (int nPl=(signDir==-1)?planetsInOrder.length-1 : 0; (signDir==-1)?nPl >= 0 :nPl < planetsInOrder.length; nPl+=signDir)
        {
          ChartPlanet planet = planetsInOrder[nPl];
          double dgrs = planet.getGeoLongitude()/d2r;
          if ((int)(dgrs / 30) == sign  &&  isPlanetVisible(planet.name))
          {
            // draw planet
            gr.drawImage( planet.image, px, py, null );
            planet.posScreenSym = new Point(px + planet.image.getWidth(null)/2,py + planet.image.getHeight(null)/2);
            planet.posScreenDot2 = planet.posScreenDot = planet.posScreenSym;
            int planetImgH = planet.image.getHeight(null);
            gr.setColor( degreeNumberColor );
            gr.drawString( String.format( "%.1f", dgrs % 30 ), px + 25, py + planetImgH/2 + fm.getAscent()/2 );
            py += planetImgH + 2;
            numInSign ++;
          }
        }
      }
    }
    Rectangle getSquareSignBounds( int sign )
    {
      int x0 = cx - rOuter;
      int y0 = cy - rOuter;
      int W = rOuter*2;
      int H = rOuter*2;
      int x1 = x0 + W/4;
      int x2 = x0 + W/2;
      int x3 = x0 + 3*W/4;
      int x4 = x0 + W;
      int y1 = y0 + H/4;
      int y2 = y0 + H/2;
      int y3 = y0 + 3*H/4;
      int y4 = y0 + H;
      switch( sign % 12 )
      {
      case 11: return new Rectangle(x0,y0,x1-x0,y1-y0);
      case 0:  return new Rectangle(x1,y0,x2-x1,y1-y0);
      case 1:  return new Rectangle(x2,y0,x3-x2,y1-y0);
      case 2:  return new Rectangle(x3,y0,x4-x3,y1-y0);
      case 3:  return new Rectangle(x3,y1,x4-x3,y2-y1);
      case 4:  return new Rectangle(x3,y2,x4-x3,y3-y2);
      case 5:  return new Rectangle(x3,y3,x4-x3,y4-y3);
      case 6:  return new Rectangle(x2,y3,x3-x2,y4-y3);
      case 7:  return new Rectangle(x1,y3,x2-x1,y4-y3);
      case 8:  return new Rectangle(x0,y3,x1-x0,y4-y3);
      case 9:  return new Rectangle(x0,y2,x1-x0,y3-y2);
      case 10: return new Rectangle(x0,y1,x1-x0,y2-y1);
      default: return null;
      }
    }
	/**
	 * All the drawing takes place here.
	 */
    private void drawCircularChart(Graphics gr)
    {
      if (!moonPhaseBackground)
      {
        gr.setColor(insideColor);
        gr.fillOval(cx - rOuter, cy - rOuter, 2 * rOuter, 2 * rOuter);
      }
      // text
      // - outer ring info
      gr.setFont(new Font(gr.getFont().getName(), Font.PLAIN, 10));
      FontMetrics fm = gr.getFontMetrics();
      gr.setColor(Color.DARK_GRAY);
      // draw caption - show outer ring data if not specified
      String fullCaption = caption; 
      // TODO show the outer ring data in a different way -- we don't know the timezone here
      if (fullCaption.length() == 0  &&  outerRing != null)
      {
        Date dt = new Date( JD_to_systemTime( outerRing.t ) );
        fullCaption = "outer ring: " + timeFormat.format( dt );
      }
      if (fullCaption.length() > 0)
        gr.drawString( fullCaption, 4, 4 + fm.getAscent() );
      // zodiac backgrounds
      final int wDgrs = 7;
      {
        int xpt[] = new int[32];
        int ypt[] = new int[32];
        for (int sgn = 0; sgn < 12; sgn++)
        {
          for (int npt = 0; npt <= 15; npt++)
          {
            polarXY((sgn * 30 + npt * 2) * d2r, rInner + wDgrs, xpt, ypt, npt);
            polarXY((sgn * 30 + 30 - npt * 2) * d2r, rOuter, xpt, ypt, npt + 16);
          }
          gr.setColor(signBgColors[sgn]);
          gr.fillPolygon(xpt, ypt, 32);
        }
      }
      // draw circles
      gr.setColor(lineColor);
      gr.drawOval(cx - rOuter, cy - rOuter, 2 * rOuter, 2 * rOuter);
      gr.setColor(lineColor);
      gr.drawOval(cx - rInner, cy - rInner, 2 * rInner, 2 * rInner);
      // zodiac lines
      gr.setColor(lineColor);
      gr.drawOval(cx - rInner - wDgrs, cy - rInner - wDgrs, 2 * rInner + 2
          * wDgrs, 2 * rInner + 2 * wDgrs);
      for (int dgr = 0; dgr < 360; dgr++)
      {
        double a = dgr * d2r;
        int r1 = rInner;
        int r2 = (dgr % 30 == 0) ? rOuter : r1 + wDgrs;
        gr.setColor((dgr % 5 == 0) ? degree5LineColor : degreeLineColor);
        polarLine(gr, a, r1, a, r2);
        if (dgr % 30 == 15)
        {
          // zodiac symbols
          Point p = polarPosition(a, rSignSyms);
          int imgW = signs[dgr / 30].getWidth(this);
          int imgH = signs[dgr / 30].getHeight(this);
          if (rOuter < 150)
          {
            imgW = 8 * imgW / 10;
            imgH = 8 * imgH / 10;
          }
          p.x -= imgW / 2;
          p.y -= imgH / 2;
          gr.drawImage(signs[dgr / 30], p.x, p.y, imgW, imgH, this);
        }
      }
      // stars
      if (showStars)
      {
        Stars.Star[] stars = Stars.getInstance().getStars();
        for (int n = 0; n < stars.length; n++)
        {
          double a = makeZodiacAdjustment(stars[n].getLongitude(getJD()));
          Point p = polarPosition(a, rStars);
          int magC = 140 + (int) (stars[n].getMagnitude() * 40);
          gr.setColor((stars[n] == overStar) ? Color.BLACK : new Color(magC,
              magC, magC, 108));
          gr.fillOval(p.x - 2, p.y - 2, 4, 4);
        }
      }
      if (enableChartRotation)
      {
        // ascendant / horizon
        int rAngles = rOuter + 6;
        gr.setColor(horizonColor);
        double a = -chartRotation;
        polarLine(gr, a, rInner + wDgrs, a, rAngles);
        polarLine(gr, a + Math.PI, rInner + wDgrs, a + Math.PI, rAngles);
        polarTriangle(gr, a, 2 * d2r, rOuter, rAngles);
        polarTriangle(gr, a + Math.PI, 2 * d2r, rOuter, rAngles);
        // midheaven
        double mc = calcMidheaven(getJD(), curLng);
        gr.setColor(zenithColor);
        polarLine(gr, mc, rInner + wDgrs, mc, rAngles);
        polarLine(gr, mc + Math.PI, rInner + wDgrs, mc + Math.PI, rAngles);
        polarTriangle(gr, mc, 2 * d2r, rOuter, rAngles);
        polarTriangle(gr, mc + Math.PI, 2 * d2r, rOuter, rAngles);
        // house cusps
        if (enableHouseCusps)
        {
          double cusps[] = ChartData.calcCusps(a, mc);
          gr.setColor(houseCuspColor);
          for (int nC = 0; nC < cusps.length; nC++)
            polarLine(gr, cusps[nC], rInner + wDgrs, cusps[nC], -rInner - wDgrs);
          gr.setColor(horizonColor);
          polarLine(gr, a, rInner + wDgrs, a, -rInner - wDgrs);
          gr.setColor(zenithColor);
          polarLine(gr, mc, rInner + wDgrs, mc, -rInner - wDgrs);
          // draw house numbers
          double h[] = new double[6];
          h[0] = a;
          h[1] = cusps[2];
          h[2] = cusps[3];
          h[3] = mc + Math.PI;
          h[4] = cusps[0] + Math.PI;
          h[5] = cusps[1] + Math.PI;
          for (int nC = 0; nC < 12; nC++)
          {
            double aC = (nC < 6) ? h[nC] : mod2pi(h[nC - 6] + Math.PI);
            int nC2 = (nC + 1) % 12;
            double aC2 = (nC2 < 6) ? h[nC2] : mod2pi(h[nC2 - 6] + Math.PI);
            double aMid = aC + mod2pi(aC2 - aC) / 2;
            Point pt = polarPosition(aMid, rInner / 3);
            gr.setFont(new Font(gr.getFont().getName(), Font.PLAIN, 15));
            gr.setColor(new Color(224, 224, 224));
            String text = "" + (nC + 1);
            gr.drawString(text, pt.x - gr.getFontMetrics().stringWidth(text) / 2,
                pt.y + gr.getFontMetrics().getAscent() / 2);
          }
        }
      }
      // draw aspect lines
      drawAspects(gr);
      // update each planet
      mainRing.paint(gr);
      if (outerRing != null)
        outerRing.paint(gr);
  
      // draw sun in the middle for heliocentric display, earth if no AC
      if (!isEnableChartRotation())
      {
        Image img = isHeliocentric() ? mainRing.planets[SUN].image
            : mainRing.planets[EARTH].image;
        int r = img.getWidth(null) / 2;
        int rClear = r + 4;
        gr.setColor(Color.WHITE);
        gr.fillOval(cx - rClear, cy - rClear, rClear * 2, rClear * 2);
        gr.drawImage(img, cx - r, cy - r, null);
        gr.setColor(Color.LIGHT_GRAY);
        gr.drawOval(cx - rClear, cy - rClear, rClear * 2, rClear * 2);
      }
    }

    /**
     * Some planets are disabled depending on the perspective, i.e. Earth is not visible from Earth, Sun from Sun.
     */
	public boolean isPlanetVisible( String name )
    {
        if (heliocentric)
        {
            if (name.equals( "Sun" )  ||  name.equals("Moon")  ||  name.endsWith("Node"))
                return false;
        }
        else
        {
            if (name.equals("Earth"))
                return false;
            /*
            if (name.equals("NorthNode")  &&  ! displayNode)
                return false;
            if (name.equals("SouthNode")  &&  (! displayNode  ||  ! square))
              return false;
            */
        }
        Boolean show = otherBodies.get( name );
        if (show != null)
          return show;
        return true;
    }
	//
	// Spread out planets so as to avoid overlapping symbols.
	//
	void spreadOutPlanets( ChartPlanet planets[] )
	{
		int n;
		// determine which planets are visible
		int order[] = new int[ planets.length ];
		int nUse = 0;
		for (n=0; n < planets.length; n++)
			if (planets[n].getSize().width > 0  &&  isPlanetVisible( planets[n].name ))
				order[nUse++] = n;
		// sort planets by longitude
		int swapped = 1;
		while (swapped != 0)
		{
			swapped = 0;
			for (n=0; n < nUse-1; n++)
				if (planets[order[n]].obsLongitude > planets[order[n+1]].obsLongitude)
				{
					swapped = 1;
					int t = order[n];
					order[n] = order[n+1];
					order[n+1] = t;
				}
		}
		// set initial positions
		for (n=0; n < planets.length; n++)
		{
			planets[n].tmpPos = planets[n].obsLongitude;
			planets[n].posScreenSym = polarPosition( planets[n].tmpPos, rPlanetSyms );
		}
		// push planet symbols apart so they don't overlap
		double push[] = new double[ nUse ];
		double push2[] = new double[ nUse ];
		double scale = 0.5;
		for (int mx=0; mx < 15; mx++)
		{
			// preserve a fraction of the previous velocity
			for (n=0; n < nUse; n++)
				push2[n] = push[n] * 0.1;
			// find 'push' amount from overlapping symbols on the right
			double all = 0;
			for (n=0; n < nUse; n++)
			{
				int np1 = order[ n % nUse ];
				int np2 = order[ (n+1) % nUse ];
				push[n] = planets[np1].overlap( planets[np2] );
				all += push[n];
			}
			// few overlaps?
			if (all < 0.01)
				break;
			// spread 'pushes' to the right
			for (n=0; n < nUse; n++)
			{
				if (push[n] == 0)
					continue;
				double amt = push[n] * 0.5;
				for (int n2=1; n2 < nUse; n2++)
				{
					int npl = (n + n2) % nUse;
					push2[npl] += amt;
					if (push[npl] == 0)
						break;
					amt *= 0.8;
				}
			}
			// spread 'pushes' to the left
			for (n=0; n < nUse; n++)
			{
				if (push[n] == 0)
					continue;
				double amt = push[n] * -0.5;
				for (int n2=1; n2 < nUse; n2++)
				{
					int npl = (n + nUse - n2) % nUse;
					int npl2 = (n + nUse - n2 + 1) % nUse;
					push2[npl2] += amt;
					if (push[npl] == 0)
						break;
					amt *= 0.8;
				}
			}
			// put 'pushes' back in push[] (and scale them back a bit)
			for (n=0; n < nUse; n++)
				push[n] = push2[n] * scale;
			// add in push to remain near 'ideal' position
			for (n=0; n < nUse; n++)
			{
				if (push[n] != 0)
					continue;
				int np1 = order[ n % nUse ];
				double d = mod2pi( planets[np1].obsLongitude - planets[np1].tmpPos );
				if (d > Math.PI)
					d -= Math.PI*2;
				push[n] += d * -0.05;
			}
			// adjust positions
			for (n=0; n < nUse; n++)
				if (push[n] != 0)
				{
					int np1 = order[ n % nUse ];
					planets[np1].tmpPos = mod2pi( planets[np1].tmpPos + push[n] );
					planets[np1].posScreenSym = polarPosition( planets[np1].tmpPos, rPlanetSyms );
                    planets[np1].posScreenDot = polarPosition( planets[np1].tmpPos, rPlanetDots );
				}
			// reduce scale
			scale *= 0.9;
		}
	}

    private static double angleDelta( double start, double end )
    {
        double d = mod2pi( end - start );
        if (d > Math.PI)
            d -= Math.PI*2;
        return d;
    }

    //
	// Draw aspect lines between planets.
	//
	private void drawAspects( Graphics g )
	{
        aspectList = new Aspects( this, mainRing );
        aspectList.draw( g );
	}
	
	//
	// Draw the background of the solar system.
	//
	private void drawBackground( Graphics g )
	{
		g.setColor( paperColor );
		g.fillRect( 0, 0, getWidth(), getHeight() );
        if (moonPhaseBackground)
        {
            int phase = (int)(mod2pi( mainRing.planets[MOON].obsLongitude - mainRing.planets[SUN].obsLongitude ) / d2r);
            String strPhase = String.valueOf( phase+1000 ).substring( 1 );
            // TODO - needs correct URL
            Image img = Toolkit.getDefaultToolkit().getImage( "/Users/Mark/Documents/workspace/Google Moon Widget/phases/standard/LunarLogic_standard_" + strPhase + ".png" );
            Composite oldComposite = ((Graphics2D)g).getComposite();
            ((Graphics2D)g).setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.15f ));
            int r = (int)(rInner * 1.27);
            g.drawImage( img, cx - r, cy - r, r*2, r*2, null );
            ((Graphics2D)g).setComposite( oldComposite );
        }
	}


	//////////////////////////////////////////////////////////////////////////
	//
	// SPINNING...  (these routines control the periodic update of the solar system)
	//

	//
	// Start & stop 'spinnner' thread.
	//
	public void start()
	{
	    spinnerPaused = false;
	}
	public void stop()
	{
        spinnerPaused = true;
	}

	//
	// This function runs in a separate thread...
	//
	//  It is responsible for periodically updating the solar system so that
	//  it 'spins'.  (see 'spinner' member).
	//
	public void run()
	{
		// allow graphics to load
		try
		{
			imgTracker.waitForAll();
		}
		catch( InterruptedException x )
		{
		}
		for (;;)
		{
			// wait a while
			try
			{
				Thread.sleep( 50 );
			}
			catch( InterruptedException e )
			{
				break;
			}
			// continue to sleep while paused
			if (spinnerPaused)
			  continue;
			// get current time
			double t_now = currentJD();
			// update caption
			if (captionExpires != 0  &&  t_now > captionExpires)
				setCaption( "" );
			// if a planet is being dragged, delay the update
			if (clickPlanet == null)
			{
				// update elapsed time
				spinElapsed += t_now - spinPrevious;
				spinPrevious = t_now;
				/*
				// make sure the display is updated at least every 2 seconds
				// WHY:  when this control is first displayed, the planets may not be visible,
				//    and if there is no 'spin', they will not be refreshed.
				if (t_now - lastRefresh > 2.5 / 86400)
					refresh( false, true );
				*/
				
				//
				// Recompose the frame if requested.
				//
				if (t_now - lastRefresh > 0.25 / 86400  &&  needCompose)
				{
					refresh( false, true );
					needCompose = false;
				}
			}
			// reset elapsed time
			spinElapsed = 0;
			spinPrevious = currentJD();
		}
	}

	//
	// ImageObserver hook - monitors progress of loading images (not currently used).
	//
    public boolean imageUpdate( Image img, int infoflags, int x, int y, int width, int height )
	{
		if ((infoflags & (ImageObserver.WIDTH | ImageObserver.HEIGHT)) != 0)
			needCompose = true;
		if ((infoflags & ImageObserver.ALLBITS) != 0)
			return true;
		return false;
	}


	//////////////////////////////////////////////////////////////////////////
	//
	// STATIC
	//

	//
	// Determine the current time (JD).
	//
	public static double currentJD()
	{
		Date now = new Date();
		return now.getTime() / 86400000.0 + 2440587.5;
	}
	public static long JD_to_systemTime( double jd )
	{
	    return (long)((jd - 2440587.5) * 86400000);
	}
  public static double systemTime_to_JD( long sysTime )
  {
      return sysTime / 86400000.0 + 2440587.5;
  }

    /**
     * Calculate adjustment for selected zodiac.
     */
    double makeZodiacAdjustment( double x )
    {
      return ChartData.makeZodiacAdjustment( getJD(), x, zodiac );
    }

	static double mod2pi( double x )
	{
		if (x < 0)
		  x += twopi * (1 + Math.floor( -x / twopi ));
		if (x > twopi)
			x -= twopi * Math.floor( x / twopi );
		return x;
	}
    
    void setEnableChartRotation(boolean enableChartRotation)
    {
        this.enableChartRotation = enableChartRotation;
        // recalculate to update angles
        refresh( true, true );
    }

    boolean isEnableChartRotation()
    {
        return enableChartRotation;
    }
    
    void setGraphEnabled( boolean show )
    {
      showMotionChart = show;
      if (show  &&  motionChart == null)
        motionChart = new MotionChart( this );
      refresh( false, true );
    }
    boolean isGraphEnabled()
    {
      return showMotionChart;
    }
    public MotionChart getGraph()
    {
      return motionChart;
    }

    void setHeliocentric( boolean helio )
    {
        this.heliocentric = helio;
        // recalculate to update angles
        refresh( true, true );
    }
    boolean isHeliocentric()
    {
        return heliocentric;
    }
    public void setSquare(boolean square)
    {
      this.square = square;
      // redraw
      refresh( true, true );
    }
    public boolean isSquare()
    {
      return square;
    }
    /**
     * Zodiac selection.
     */
    public void setZodiac(int zodiac)
    {
        if (zodiac == this.zodiac)
            return;
        this.zodiac = zodiac;
        updateAC();
        refresh( true, true );
        // positions are affected, so we notify listeners
        fireTimeChanged( getJD() );
    }
    public int getZodiac()
    {
        return zodiac;
    }
    /**
     * Allow/disallow dragging of planets.
     */
    public void setEnableInteraction(boolean enableInteraction)
    {
      this.enableInteraction = enableInteraction;
    }
    
    public void setShowStars(boolean showStars)
    {
        this.showStars = showStars;
    }
    public boolean isShowStars()
    {
        return showStars;
    }
    public static String formatDegrees( double v )
    {
      if (Double.isNaN( v ))
        return "----";
      v /= d2r;
      v += 0.5 / 60;
      int dgr = (int)Math.floor( v );
      int min = (int)Math.floor( (v - Math.floor(v)) * 60 );
      int sgn = dgr / 30;
      dgr %= 30;
      String sgnNames[] = { "ar","ta","ge","ca","le","vi","li","sc","sa","cp","aq","pi" };
      if (sgn < 0)
          sgn += 24;
      return dgr + sgnNames[sgn%12] + min/10 + "" + min%10;
    }
}
