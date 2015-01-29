package com.marklipson.astrologyclock;
import java.awt.*;
import java.net.URL;

/*
 *
 * ChartPlanet
 *
 * Represents:
 *	  A planet in the solar system.
 *
 * - Stores orbital information.
 * - Calculates position of a planet.
 * - Displays planet graphically.
 *
 */
class ChartPlanet implements Comparable<ChartPlanet>, Cloneable
{
	// parent
	ChartWheel parent;
	ChartWheel.PlanetRing ring;
	// name of planet
	String name;
	// radius (in Earth radii)
	public double radius;
	// color
	public Color color;

	// current location in solar system (current time comes from 'parent')
	//double[] posPhysical;
	// observed longitude (from selected center)
	double obsLongitude;
	// temporary position for visual spacing algorithm (and where the planet's symbol is actually drawn)
	double tmpPos;
	// screen position (coordinates within parent)
	Point posScreenDot;  // the dot showing the accurate longitude
    Point posScreenDot2; // the draggable inner dot showing alongside the symbol
	Point posScreenSym;  // the symbol
	// image for planet
	Image image = null;			// image
	private int imgDx=0, imgDy=0;		// current displayed size

	//
	// Construct.
	//
	ChartPlanet( ChartWheel parent, ChartWheel.PlanetRing ring, int planetIndex, String name, Color _c, double r )
	{
		// store link to parent (ChartWheel)
		this.parent = parent;
		this.ring = ring;
		// name of body
		this.name = name;
		// physical characteristics
		color = _c;
		radius = r;
		// start loading the image
		String imageSrc = "";
		try
		{
			if (! parent.enableGraphics)
				image = null;
			else if (parent.imageLocation != null)
			{
				// if the solar system has a defined location for images, use it
			  String extension = ".gif";
			  if (name.equals( "Sedna" ))
			    extension = ".png";
				URL imageURL = new URL( parent.imageLocation, name.toLowerCase() + extension );
				imageSrc = imageURL.toString();
				if (parent.parent != null)
					// we are running in an applet - use the applet's getImage()
					image = parent.parent.getImage( imageURL );
				else
					image = Toolkit.getDefaultToolkit().getImage( imageURL );
				if (image == null)
					System.err.println( "Unable to load image: " + name );
			}
			else
			{
				// look for the image in the current directory
				imageSrc = name + ".gif";
				image = Toolkit.getDefaultToolkit().getImage( imageSrc );
			}
			if (image != null)
			{
				parent.imgTracker.addImage( image, planetIndex );
			}
		}
		catch( Exception e )
		{
			String msg = imageSrc + ": " + e.getClass().getName();
			System.out.print( msg + "\n" );
			parent.setCaption( msg );
			image = null;
		}
	}
	
	/**
	 * Natural sort order is longitude.
	 */
	public int compareTo(ChartPlanet otherPlanet)
	{
	  return new Double(getGeoLongitude()).compareTo( otherPlanet.getGeoLongitude() );
	}
	/**
	 * Copy planets.
	 */
	public ChartPlanet clone()
	{
	  try { return (ChartPlanet)super.clone(); }
	  catch( CloneNotSupportedException x ) { return null; }
	}

	//
	// Re-calculate the position and radius of the planet, taking into
	// account the current scaling factors.
	//
	void refresh()
	{
		// call with no arguments - calculate for solar system's current time
		refresh( new Double( parent.getJD() ) );
	}
	void refresh( Double t )
	{
		// calculate position (if a time is given)
		if (t != null)
		{
			// calculate geo longitude
            if (parent.isHeliocentric())
                obsLongitude = parent.helioLongitude( this, t.doubleValue() );
            else
                obsLongitude = parent.geoLongitude( this, t.doubleValue() );
			// find displayed location
            if (parent.isSquare())
            {
              int sign = (int)((obsLongitude / ChartData.d2r) / 30);
              Rectangle signBox = parent.getSquareSignBounds( sign ).getBounds();
              int x = signBox.x + signBox.width/2;
              int y = signBox.y + signBox.height/2;
              posScreenDot = posScreenDot2 = posScreenSym = new Point(x,y);
            }
            else
            {
              posScreenDot = parent.polarPosition( obsLongitude, ring.rPlanetDots );
              posScreenDot2 = parent.polarPosition( obsLongitude, ring.rPlanetDots2 );
              posScreenSym = parent.polarPosition( obsLongitude, ring.rPlanetSyms );
            }
		}
	}
    
    /**
     * Get dimensions of planet's graphic.
     */
    public Dimension getSize()
    {
        if (image == null)
            return new Dimension( 0, 0 );
        if (imgDx == 0)
        {
            imgDx = image.getWidth( parent );
            imgDy = image.getHeight( parent );
        }
        return new Dimension( imgDx, imgDy );
    }

	//
	// Draw the planet.
	//
	void paint( Graphics g )
	{
		// draw planetary 'dot'
		if (g != null  &&  posScreenDot != null)
		{
		    if (ring.rPlanetDots > 0)
		        drawPlanetDot(g, color, posScreenDot);
            //drawPlanetDot(g, color, posScreenDot2);
			g.setColor( new Color( color.getRed(), color.getGreen(), color.getBlue(), 64 ) );
			if (ring.rPlanetDots2 > parent.rOuter)
	            parent.polarTriangle( g, obsLongitude, 0.75 * ChartData.d2r, ring.rPlanetDots2 + 5, ring.rPlanetDots2 );
			else
			    parent.polarTriangle( g, obsLongitude, 0.75 * ChartData.d2r, ring.rPlanetDots2 - 5, ring.rPlanetDots2 );
		}
		// draw range-motion indicator
		if (parent.getTimeAccuracy() > 0  &&  ! parent.isSquare()  &&  g != null)
		{
            double maxError = parent.getTimeAccuracy();
		    double t0 = parent.getJD() - maxError;
            int r0 = ring.rPlanetDots - 0;
            int nSteps = (int)(maxError*2+1);
            if (nSteps > 50)
              nSteps = 50;
            if (nSteps < 7)
              nSteps = 7;
            int px[] = new int[nSteps];
            int py[] = new int[nSteps];
            double dt = (maxError*2) / (nSteps-1);
            for (int n=0; n < nSteps; n++)
            {
              double t = t0 + n * dt;
              double lng;
              if (parent.isHeliocentric())
                lng = parent.helioLongitude( this, t );
              else
                lng = parent.geoLongitude( this, t );
              Point p = parent.polarPosition( lng, r0 );
              px[n] = p.x;
              py[n] = p.y;
            }
            g.setColor( new Color( color.getRed(), color.getGreen(), color.getBlue(), 50 ) );
            Stroke oldStroke = ((Graphics2D)g).getStroke();
            ((Graphics2D)g).setStroke( new BasicStroke(5,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND ) );
            g.drawPolyline( px, py, nSteps );
            ((Graphics2D)g).setStroke( oldStroke );
		}
		// now draw planetary symbol
		if (posScreenSym != null)
		{
			boolean showText = true;
			if (image != null  &&  parent.enableGraphics)
			{
				imgDx = (int)(image.getWidth( parent ) * ring.symbolScale);
				imgDy = (int)(image.getHeight( parent ) * ring.symbolScale);
				if (imgDx > 0  &&  imgDy > 0)
				{
					// draw image
					int px = posScreenSym.x - imgDx/2;
					int py = posScreenSym.y - imgDx/2;
					if (g != null)
						g.drawImage( image, px, py, imgDx, imgDy, parent );
					showText = false;
				}
			}
			if (showText)
			{
				// draw text for planet when graphics are unavailable
				int sz = (int)( 10 * Toolkit.getDefaultToolkit().getScreenResolution() / 36 );
				if (sz < 6)
					sz = 6;
				if (sz > 100)
					sz = 100;
				sz = (int)(sz * ring.symbolScale);
				Font font = new Font( "System"/*g.getFont().getName()*/, Font.BOLD, sz ); 
				FontMetrics metrics = parent.getFontMetrics( font );
				String abbr = name.substring( 0, 1 );
				imgDx = metrics.stringWidth( abbr );
				imgDy = metrics.getAscent();
				if (g != null)
				{
					int px = posScreenSym.x - imgDx / 2;
					int py = posScreenSym.y + imgDy / 2;
					g.setFont( font );
					g.setColor( color );
					g.drawString( abbr, px, py );
				}
			}
		}
	}

    /**
     * Draw a dot representing a planet.
     */
    private void drawPlanetDot(Graphics g, Color dotColor, Point pos )
    {
        Color mid = new Color( (int)(dotColor.getRed()*0.875) + 32, (int)(dotColor.getGreen()*0.875) + 32, (int)(dotColor.getBlue()*0.875) + 32 );
        Color light = new Color( (int)(dotColor.getRed()*0.75) + 64, (int)(dotColor.getGreen()*0.75) + 64, (int)(dotColor.getBlue()*0.75) + 64 );
        g.setColor( light );
        g.fillRect( pos.x - 2, pos.y - 2, 5, 5 );
        g.setColor( dotColor );
        g.fillRect( pos.x - 1, pos.y - 1, 3, 3 );
        {
        	int px1 = -2, py1 = -2;
        	int px2 = -2, py2 = 0;
        	for (int n=0; n < 4; n++)
        	{
        		g.setColor( Color.white );
        		g.fillRect( pos.x + px1, pos.y + py1, 1, 1 );
        		g.setColor( mid );
        		g.fillRect( pos.x + px2, pos.y + py2, 1, 1 );
        		int t = px1;
        		px1 = -py1;
        		py1 = t;
        		t = px2;
        		px2 = -py2;
        		py2 = t;
        	}
        }
    }

	//
	// Check whether a point (in 'parent') is 'inside' this planet.
	//
	boolean inside( int px, int py )
	{
        return insideDot( px, py )  ||  insideSym( px, py );
	}
    boolean insideDot( int px, int py )
    {
        if (px < posScreenDot.x - 3  ||  py < posScreenDot.y - 3)
            return false;
        if (px > posScreenDot.x + 3  ||  py > posScreenDot.y + 3)
            return false;
//      if ((long)(px - posScreen.x)*(px - posScreen.x) + (long)(py - posScreen.y)*(py - posScreen.y) > (long)rr*rr)
//          return false;
        return true;
    }
    boolean insideSym( int px, int py )
    {
        if (px < posScreenSym.x - imgDx/2  ||  py < posScreenSym.y - imgDy/2)
            return false;
        if (px > posScreenSym.x + imgDx/2  ||  py > posScreenSym.y + imgDy/2)
            return false;
        return true;
    }
	//
	// Calculate the required amount of angular separation to add
	// between planets, if they overlap, or 0 if they don't.
	//
	double overlap( ChartPlanet pChk )
	{
		if (imgDx <= 0  ||  imgDy <= 0)
			return 0;
		if (pChk.imgDx <= 0  ||  pChk.imgDy <= 0)
			return 0;
		// move the symbol out of the way of any other symbols
		int dx = posScreenSym.x - pChk.posScreenSym.x;
		int dy = posScreenSym.y - pChk.posScreenSym.y;
		if (dx < 0)
			dx = -dx;
		if (dy < 0)
			dy = -dy;
		if (dx*2 > imgDx + pChk.imgDx)
			return 0;
		else
			dx = (int)((imgDx + pChk.imgDx)/2.0) - dx;
		if (dy*2 > imgDy + pChk.imgDy)
			return 0;
		else
			dy = (int)((imgDy + pChk.imgDy)/2.0) - dy;
		double d;
		if (Math.abs( Math.cos( obsLongitude ) ) > 0.707)
			d = dy;
		else
			d = dx;
		// - find required distance to move along circumference, in pixels
		// - scale by radius
		// - convert to radians
		// - RESULT is required angular 'spacing' needed to keep symbols from overlapping
//		double d = Math.sqrt( dx*dx + dy*dy );
		double x = Math.atan( d / ring.rPlanetSyms );
		return x;
	}
    
    /**
     * Get geocentric longitude for current time in parent wheel.
     */
    public double getGeoLongitude()
    {
        return parent.geoLongitude( this, ring.t );
    }
    
    public double getHelioLongitude()
    {
        return parent.helioLongitude( this, ring.t );
    }
}
