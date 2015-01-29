package com.marklipson.astrologyclock;
/**
 * JavaChart.java
 *
 * This applet displays the solar system in a window, along with a few controls
 * to control its time and spin rate.
 */
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AudioClip;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Applet to display dynamic / animated astrology chart.
 */
public class JavaChart extends Applet
{
    private static final long serialVersionUID = 7894643742958349296L;

    // other
	final static double d2r = 3.1415926 / 180;
	
	// solar system display component
	protected ChartWheel ss;
  // time widget
  protected TimeAdjuster time;
  protected ChartOptions options;

	//
	// Construct.
	//
	public JavaChart()
	{
	}

	//
	// Initialize.
	//
	public void init()
	{
	    // this flag causes only a non-modifiable chart to be displayed
	    boolean chartOnly = getParameter( "chartOnly" ) != null;

	    // initial values
		Date initialTime = new Date();
		double calcLat = 0, calcLng = 0;
		// make the default font bold
		setFont( new Font( getFont().getName(), Font.BOLD, getFont().getSize() ) );
        setLayout( new BorderLayout() );
        // create time adjuster
        time = new TimeAdjuster();
        add( time, BorderLayout.NORTH );
        if (chartOnly)
          time.setVisible( false );
		    // create chart wheel control - disable graphics initially
        ss = new ChartWheel( this, getClass().getResource( "resources/sun.gif" ) );
        if (chartOnly)
          ss.setEnableInteraction( false );
        options = new ChartOptions( this, time, ss );
        if (chartOnly)
          options.setVisible( false );
		// insert alternate status bar for chart-only mode
		if (chartOnly)
		{
		    final JLabel statusBar = new JLabel( ".", SwingConstants.CENTER );
	        ss.addChartListener( new ChartWheel.ChartListener()
	        {
	            public void mouseOverPlanet(ChartPlanet planet)
	            {
	                if (planet == null)
	                    statusBar.setText( "" );
	                else
	                {
	                    double pos = ss.isHeliocentric() ? planet.getHelioLongitude() : planet.getGeoLongitude();
	                    statusBar.setText( planet.name + ": " + ChartWheel.formatDegrees( pos ) );
	                }
	            }
	            public void mouseOverObject(String name)
	            {
	                statusBar.setText( (name == null) ? "" : name );
	            }
	            public void timeChanged(double t)
	            {
	            }
	        } );
	        JPanel statPanel = new JPanel( new BorderLayout() );
	        statusBar.setPreferredSize( new Dimension( 400, 20 ) );
	        statPanel.add( statusBar, BorderLayout.CENTER );
	        statusBar.setForeground( new Color(64,96,72) );
            statPanel.setBackground( new Color(144,192,160) );
	        add( statPanel, BorderLayout.SOUTH );
		}
		else
	        add( options, BorderLayout.SOUTH );
        add( ss, BorderLayout.CENTER );
        ss.setEnableGraphics( false );
        // check for display options parameter
        String pD = getParameter( "display" );
        if (pD != null)
            ss.setDisplayInfo( pD );
        else
            ss.setEnableChartRotation(true);
		// set place
        if (calcLat != 0  &&  calcLng != 0)
            ss.setPlace( calcLat * d2r, calcLng * d2r );
		// set initial time
		if (initialTime != null)
			ss.setDate( initialTime );
		// enable display again
		ss.setEnableGraphics( true );
		ss.refresh();
        // connect time adjuster to solar system
        time.setTarget( new TimeAdjuster.TimeAdjusterTarget()
        {
            public Date getTime()
            {
                return ss.getDate();
            }
            public void setTime(Date time)
            {
                ss.setDate( time );
            }
            public void setTimeZone(TimeZone tz)
            {
                ss.setTimeZone( tz );
            }
            public double getLatitude()
            {
                return ss.getPlace()[0];
            }
            public double getLongitude()
            {
                return ss.getPlace()[1];
            }
            public void setLatitude(double v)
            {
                ss.setPlace( v, getLongitude() );
            }
            public void setLongitude(double v)
            {
                ss.setPlace( getLatitude(), v );
            }
        } );
        ss.addChartListener( new ChartWheel.ChartListener()
        {
          public void timeChanged(double t)
          {
            time.timeChangedExternally( t );
          }
          public void mouseOverPlanet(ChartPlanet planet)
          {
          }
          public void mouseOverObject(String name)
          {
          }
        } );
        // get parameters
        String p;
        // fill in place information - guess the zone if one is not supplied
        boolean zoneGiven = (getParameter( "zone" ) != null);
        p = getParameter( "place" );
        if (p != null)
            setPlaceInfo( p, ! zoneGiven );
        p = getParameter( "zone" );
        if (p != null)
            setZoneInfo( p );
        p = getParameter( "time" );
        if (p != null)
            setTime( p );
        p = getParameter( "outerTime" );
        if (p != null)
            setOuterTime( p );
	}

	//
	// Start...
	//
	public void start()
	{
		// start it spinning
		if (ss != null)
			ss.start();
        // initial draw
        ss.refresh();
	}

	//
	// Stop....
	//
	public void stop()
	{
		// stop animation
		if (ss != null)
			ss.stop();
	}
    
    /**
     * Get/set current time for main ring.
     */
    public String getTime()
    {
        return time.getTimeStr();
    }
    public long getTime_ms()
    {
      return time.getTime().getTime();
    }
    public boolean setTime( String strTime )
    {
        try
        {
            time.setTime( strTime );
            return true;
        }
        catch( ParseException x )
        {
            return false;
        }
    }
    /**
     * Get/set current time for outer ring.
     */
    public String getOuterTime()
    {
        if (ss.outerRing == null)
            return "";
        else
            return time.formatTime( new Date( ChartWheel.JD_to_systemTime( ss.outerRing.t ) ) ); 
    }
    public long getOuterTime_ms()
    {
      if (ss.outerRing == null)
        return 0;
      else
        return ChartWheel.JD_to_systemTime( ss.outerRing.t );
    }
    public void setOuterTime( String strTime )
    {
        try
        {
            long t = time.parseTime( strTime ).getTime();
            double jd = ChartWheel.systemTime_to_JD( t );
            ss.setOuterRing( jd );
        }
        catch (Exception e)
        {
        }
    }
    /**
     * Post chart data to the given URL.
     */
    public String save( String toURL )
    {
      try
      {
        PostToServer poster = new PostToServer();
        Double outerTime = null;
        if (ss.outerRing != null)
          outerTime = ss.outerRing.t;
        byte[] chartData = poster.captureChart( getTime(), getZoneInfo(), time.getPlaceLatLng()[0], time.getPlaceLatLng()[1], getDisplayInfo(), outerTime ); 
        URL url = new URL( getDocumentBase(), toURL );
        String response = poster.post( url, chartData );
        return response;
      }
      catch( Exception x )
      {
        x.printStackTrace( System.err );
        return "ERROR: " + x;
      }
    }
    /**
     * Expose display options.
     */
    public String getDisplayInfo()
    {
      return ss.getDisplayInfo();
    }
    public void setDisplayInfo( String info )
    {
      ss.setDisplayInfo( info );
      // remove old options control
      options.setVisible( false );
      remove( options );
      // create new one
      options = new ChartOptions( this, time, ss );
      boolean chartOnly = getParameter( "chartOnly" ) != null;
      if (chartOnly)
        options.setVisible( false );
      else
      {
        add( options, BorderLayout.SOUTH );
        options.setVisible( true );
      }
      // force components to be redisplayed
      // - I can't find any other way to make them show up
      Dimension sz = getSize();
      resize( sz.width, sz.height - 1 );
      resize( sz );
    }
    /**
     * Get/set timezone.
     */
    public String getZoneInfo()
    {
        return time.getZoneName();
    }
    public void setZoneInfo( String zoneInfo )
    {
        time.setTimezone( TimeZone.getTimeZone( zoneInfo ) );
    }
    /**
     * Get/set current place name, latitude and longitude.
     */
    public String getPlaceInfo()
    {
        return time.getPlaceName() + "|" 
          + String.format( "%.2f",time.getPlaceLatLng()[0] ) + "|"
          + String.format( "%.2f",time.getPlaceLatLng()[1] );
    }
    public void setPlaceInfo( String placeInfo, boolean guessTZ )
    {
        String parts[] = placeInfo.split( "\\|" );
        if (parts.length == 3)
            time.setPlace( parts[0], Double.parseDouble( parts[1] ), Double.parseDouble( parts[2] ), guessTZ );
    }

	//
	// Define parameters used by this Applet.
	//
    //  { "Name", "Type", "Description" },
    //--------------------------------------------------------------------------
	public String[][] getParameterInfo()
	{
		String[][] info =
		{
			{ "time", "String", "Initial time, YYYY-MMM-DD HH:MM(:SS)" },
            { "outerTime", "String", "Initial time for outer ring, YYYY-MMM-DD HH:MM(:SS)" },
			{ "place", "String", "Placename|latitude|longitude" },
            { "zone", "String", "Named timezone rule (see TimeZone.getID())" },
            { "display", "String", "Display options (see code)" },
            { "chartOnly", "boolean", "Only display chart, no controls" },
		};
		return info;
	}

	//
	// Get applet authoring info.
	//
	public String getAppletInfo()
	{
		return "Name: JavaChart\r\n" +
		       "Author: Mark Lipson\r\n" +
		       "(c) 1998-2009 by Helianthus Inc\r\n" +
			   "Use Requires License\r\n";
	}
	
	
	/**
	 * Entry point when run as application.
	 */
	public static void main( String args[] )
	{
	  JFrame window = new JFrame( "AstrologyClock" );
	  Rectangle max = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
	  window.setBounds( max.width/6, max.height/6, 2*max.width/3, 2*max.height/3 );
	  window.setLayout( new BorderLayout() );
	  final JavaChart chart = new JavaChart()
	  {
	    private JavaChart self = this;
	    
	    @Override
	    public String getParameter(String name)
	    {
	      return null;
	    }
	    @Override
	    public AppletContext getAppletContext()
	    {
	      return new AppletContext()
          {
            public void showStatus(String status)
            {
            }
            public void showDocument(URL url, String target)
            {
            }
            public void showDocument(URL url)
            {
            }
            public void setStream(String key, InputStream stream) throws IOException
            {
            }
            public Iterator<String> getStreamKeys()
            {
              return null;
            }
            public InputStream getStream(String key)
            {
              return null;
            }
            public Image getImage(URL url)
            {
              try
              {
                return ImageIO.read( url );
              }
              catch( IOException x )
              {
                return null;
              }
            }
            public AudioClip getAudioClip(URL url)
            {
              return null;
            }
            public Enumeration<Applet> getApplets()
            {
              return null;
            }
            public Applet getApplet(String name)
            {
              return self;
            }
          };
	    }
	  };
	  window.add( chart, BorderLayout.CENTER );
	  window.setVisible( true );
      chart.init();
      chart.start();
	  WindowListener wL = new WindowAdapter()
        {
    	    @Override
    	    public void windowClosing(WindowEvent e)
    	    {
    	      System.exit( 0 );
    	    }
        };
      window.addComponentListener( new ComponentAdapter()
        {
          @Override
          public void componentResized(ComponentEvent e)
          {
          }
        });
      window.addWindowListener( wL );
	}
}
// north & south nodes

// needs ability to save state: time and place, and a list of memorized charts - how about querying this from JS
// wacky idea: make JSON available to JS for all chart features, let people comment/vote on every individual feature, use AJAX to show interpretations and discussions
// underline copyright

// some kind of overlay for natal data
// initially blank sometimes - draw once after a couple seconds, or every 10 seconds
// guess timezones
// get bigger international atlas
