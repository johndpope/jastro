package com.marklipson.astrologyclock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MotionChart
{
  // time scale
  private double jdPerPixel = 0.1;
  // harmonic - 12 = 30 degrees displayed, 1 = 360, etc.
  private int harmonic = 12;
  // options are borrowed from here
  private ChartWheel refChart;
  // dimensions
  private Rectangle outerBounds;
  private Rectangle innerBounds;
  private int tickMargin = 21;
  
  public MotionChart( ChartWheel refChart )
  {
    this.refChart = refChart;
  }
  public void setBounds( Rectangle bounds )
  {
    outerBounds = bounds;
    innerBounds = new Rectangle( bounds.x + tickMargin, bounds.y + tickMargin, bounds.width - tickMargin*2, bounds.height - tickMargin*2 );
  }
  public void setTimeScale( double jdPerPixel )
  {
    this.jdPerPixel = jdPerPixel;
    refChart.refresh( false, true );
  }
  public double getTimeScale()
  {
    return jdPerPixel;
  }
  /**
   * Harmonic: longitudes are overlayed so that motion can be observed more finely, and aspects seen
   * more readily.  1 = 360, 4 = 90, 12 = 30, etc..
   */
  public void setHarmonic(int harmonic)
  {
    this.harmonic = harmonic;
    refChart.refresh( false, true );
  }
  /**
   * Calculate Y position from longitude, in radians.
   */
  protected int calcY( double longitude )
  {
    double pct = ChartWheel.mod2pi( longitude * harmonic ) / (Math.PI*2);
    return (int)(innerBounds.y + innerBounds.height - innerBounds.height * pct);
  }
  /**
   * Calculate T (time) from X position.
   */
  protected double calcT( int x )
  {
    double tC = refChart.getJD();
    int pX = innerBounds.x + innerBounds.width/2;
    return tC + (x - pX) * jdPerPixel;
  }
  /**
   * Calculate X from time.
   */
  protected int calcX( double t )
  {
    double xOffs = (t - refChart.getJD()) / jdPerPixel;
    return innerBounds.x + innerBounds.width/2 + (int)xOffs;
  }

  public void draw( Graphics g )
  {
    int tickW1 = 7;
    int tickW2 = 4;
    Font axisFont = new Font( g.getFont().getName(), Font.PLAIN, 8 );
    Color majorTickColor = Color.DARK_GRAY;
    Color minorTickColor = Color.GRAY;
    Color innerLineColor = new Color( 192, 192, 192 );
    Color innerLineColorMinor = new Color( 224, 224, 224 );
    Color axisTextColor = Color.GRAY;
    Color axisTextColorMinor = new Color( 144, 144, 144 );
    Graphics2D g2 = (Graphics2D)g;
    // inner box
    g.setColor( Color.WHITE );
    g.fillRect( innerBounds.x, innerBounds.y, innerBounds.width, innerBounds.height );
    // vertical axis (degrees)
    {
      int xL = tickMargin;
      int xR = innerBounds.x + innerBounds.width;
      g.setFont( axisFont );
      int major;
      int minor;
      if (harmonic <= 2)
      { major = 10; minor = 3; }
      else if (harmonic <= 4)
      { major = 5; minor = 6; }
      else
      { major = 1; minor = 5; }
      int nDgr = 0;
      for (double dgr=0; dgr <= 360.0/harmonic; dgr+=major, nDgr ++)
      {
        boolean isMinor = nDgr % minor != 0;
        int lY = calcY( dgr * ChartWheel.d2r );
        g.setColor( isMinor ? minorTickColor : majorTickColor );
        g.drawLine( xL, lY, xL - (isMinor?tickW2:tickW1), lY );
        g.drawLine( xR, lY, xR + (isMinor?tickW2:tickW1), lY );
        g.setColor( isMinor ? innerLineColorMinor : innerLineColor );
        g.drawLine( xL, lY, xR, lY );
        if (! isMinor)
        {
          g.setColor( axisTextColor );
          String sDgr = String.valueOf( (int)dgr );
          int wStr = g.getFontMetrics().stringWidth( sDgr );
          int hAsc = g.getFontMetrics().getAscent();
          g.drawString( sDgr, xL - (int)(0.7*tickMargin) - wStr/2, lY + hAsc/2 );
          g.drawString( sDgr, xR + (int)(0.7*tickMargin) - wStr/2, lY + hAsc/2 );
        }
      }
    }
    // horizontal axis (time)
    {
      int fields[] = { Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND };
      long tStart = ChartWheel.JD_to_systemTime( calcT( innerBounds.x ) );
      long tEnd = ChartWheel.JD_to_systemTime( calcT( innerBounds.x + innerBounds.width ) );
      int majorIncrType;
      int minorIncrType;
      int majorIncrAmount = 1;
      int minorIncrAmount = 1;
      SimpleDateFormat majorFmt;
      SimpleDateFormat minorFmt = null;
      if (jdPerPixel > 0.5)
      {
        majorIncrType = Calendar.YEAR;
        minorIncrType = Calendar.MONTH;
        majorFmt = new SimpleDateFormat( "yyyy" );
        minorIncrAmount = (jdPerPixel < 2) ? 1 : 3;
        if (jdPerPixel < 5)
          minorFmt = new SimpleDateFormat( "MMM" );
      }
      else if (jdPerPixel > 0.05)
      {
        majorIncrType = Calendar.MONTH;
        minorIncrType = Calendar.WEEK_OF_YEAR;
        majorFmt = new SimpleDateFormat( "MMM yyyy" );
        minorFmt = new SimpleDateFormat( "dd" );
      }
      else if (jdPerPixel > 0.02)
      {
        majorIncrType = Calendar.WEEK_OF_YEAR;
        minorIncrType = Calendar.DAY_OF_WEEK;
        majorFmt = new SimpleDateFormat( "MM/dd" );
        minorFmt = new SimpleDateFormat( "E dd" );
      }
      else if (jdPerPixel > 0.002)
      {
        majorIncrType = Calendar.DAY_OF_MONTH;
        minorIncrType = Calendar.HOUR_OF_DAY;
        majorFmt = new SimpleDateFormat( "EE MM/dd" );
        minorIncrAmount = (jdPerPixel < 0.01) ? 6 : 1;
        minorFmt = new SimpleDateFormat( "HH" );
      }
      else
      {
        majorIncrType = Calendar.HOUR_OF_DAY;
        minorIncrType = Calendar.MINUTE;
        minorIncrAmount = 15;
        majorFmt = new SimpleDateFormat( "ha" );
      }
      Calendar cal = Calendar.getInstance( refChart.getTimeZone() );
      cal.setTimeInMillis( tStart );
      boolean clear = false;
      for (int f : fields)
      {
        if (clear)
          cal.set( f, (f == Calendar.DAY_OF_MONTH  ||  f == Calendar.SECOND) ? 1 : 0 );
        else if (f == majorIncrType  ||  f == minorIncrType)
          clear = true;
      }
      if (majorIncrType == Calendar.WEEK_OF_YEAR)
        cal.set( Calendar.DAY_OF_WEEK, Calendar.SUNDAY );
      int yT = innerBounds.y;
      int yB = innerBounds.y + innerBounds.height;
      g.setFont( axisFont );
      while (cal.getTimeInMillis() < tEnd)
      {
        double t = ChartWheel.systemTime_to_JD( cal.getTimeInMillis() );
        Calendar calMinor = (Calendar)cal.clone();
        if (minorIncrType == Calendar.WEEK_OF_YEAR)
          calMinor.set( Calendar.DAY_OF_WEEK, Calendar.SUNDAY );
        cal.add( majorIncrType, majorIncrAmount );
        int x = calcX( t );
        if (x >= innerBounds.x  &&  x < innerBounds.x + innerBounds.width)
        {
          g.setColor( majorTickColor );
          g.drawLine( x, yT, x, yT - tickW1 );
          g.drawLine( x, yB, x, yB + tickW1 );
          g.setColor( innerLineColor );
          g.drawLine( x, innerBounds.y, x, innerBounds.y + innerBounds.height );
          String tMsg = majorFmt.format( new Date( ChartWheel.JD_to_systemTime( t ) ) );
          int wMsg = g.getFontMetrics().stringWidth( tMsg );
          int asc = g.getFontMetrics().getAscent();
          int dsc = g.getFontMetrics().getDescent();
          g.setColor( axisTextColor );
          g.drawString( tMsg, x - wMsg/2, outerBounds.y + outerBounds.height - dsc );
          g.drawString( tMsg, x - wMsg/2, outerBounds.y + asc );
        }
        long majorEnd = cal.getTimeInMillis();
        calMinor.add( minorIncrType, minorIncrAmount );
        while (calMinor.getTimeInMillis() < majorEnd)
        {
          double tM = ChartWheel.systemTime_to_JD( calMinor.getTimeInMillis() );
          x = calcX( tM );
          calMinor.add( minorIncrType, minorIncrAmount );
          if (x >= innerBounds.x  &&  x < innerBounds.x + innerBounds.width)
          {
            g.setColor( minorTickColor );
            g.drawLine( x, yT, x, yT - tickW2 );
            g.drawLine( x, yB, x, yB + tickW2 );
            g.setColor( innerLineColorMinor );
            g.drawLine( x, innerBounds.y, x, innerBounds.y + innerBounds.height );
            if (minorFmt != null)
            {
              String tMsg = minorFmt.format( new Date( ChartWheel.JD_to_systemTime( tM ) ) );
              int wMsg = g.getFontMetrics().stringWidth( tMsg );
              int asc = g.getFontMetrics().getAscent();
              int dsc = g.getFontMetrics().getDescent();
              g.setColor( axisTextColorMinor );
              g.drawString( tMsg, x - wMsg/2, outerBounds.y + outerBounds.height - dsc - 7 );
              g.drawString( tMsg, x - wMsg/2, outerBounds.y + asc + 7 );
            }
          }
        }
      }
    }
    Shape oldClip = g.getClip();
    g.setClip( innerBounds );
    // reference lines
    if (refChart.outerRing != null)
      for (ChartPlanet planet : refChart.outerRing.planets)
        if (refChart.isPlanetVisible( planet.name ))
        {
          double lng = refChart.isHeliocentric() ? refChart.helioLongitude( planet, refChart.outerRing.t ) : refChart.geoLongitude( planet, refChart.outerRing.t );
          int lY = calcY( lng );
          g.setColor( planet.color );
          g.drawLine( innerBounds.x, (int)lY, innerBounds.x + innerBounds.width, (int)lY );
        }
    // curves
    {
      Stroke oldStroke = ((Graphics2D)g).getStroke();
      g2.setStroke( new BasicStroke( 3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND ) );
      int planetNo = 0;
      for (ChartPlanet planet : refChart.mainRing.planets)
      {
        if (refChart.isPlanetVisible( planet.name ))
        {
          if (planet.name.equals( "Moon" )  &&  jdPerPixel > 0.04)
            continue;
          if ((planet.name.equals( "Mercury" ) || planet.name.equals( "Venus" ) || planet.name.equals( "Sun" ) || planet.name.equals( "Earth" ))  &&  jdPerPixel > 0.4)
            continue;
          if ((planet.name.equals( "Mars" ))  &&  jdPerPixel > 1.1)
            continue;
          int resolution = 5;
          if (jdPerPixel > 2)
            resolution = 2;
          double labelStep = jdPerPixel * 30; // days
          int xSteps = innerBounds.width / resolution;
          int px = 0, py = 0;
          g.setColor( new Color( planet.color.getRed(), planet.color.getGreen(), planet.color.getBlue(), 128 ) );
          double tStep = resolution * jdPerPixel;
          for (int xStep=0; xStep <= xSteps; xStep++)
          {
            int lX = (int)(innerBounds.x + innerBounds.width * ((double)xStep / xSteps));
            double tX = calcT( lX );
            double lng = refChart.isHeliocentric() ? refChart.helioLongitude( planet, tX ) : refChart.geoLongitude( planet, tX );
            int lY = calcY( lng );
            if (xStep > 0)
            {
              if (Math.abs( lY - py ) < 2*innerBounds.height/3)
                g.drawLine( px, py, lX, lY );
              else
              {
                int y0 = innerBounds.y;
                int y1 = innerBounds.y + innerBounds.height;
                int yMid = (y0 + y1)/2;
                int yA = (py < yMid) ? y0 : y1;
                int yB = (lY < yMid) ? y0 : y1;
                int d0 = Math.abs( py - yA );
                int d1 = Math.abs( yB - lY );
                int xM = px + (lX - px) * d0 / (d0 + d1);
                g.drawLine( px, py, xM, yA );
                g.drawLine( xM, yB, lX, lY );
              }
            }
            px = lX;
            py = lY;
            
            // planet line labels
            double labelX = tX / labelStep;
            double labelOffs = labelX % 1;
            if (Math.abs( labelOffs - 0.5 ) < (resolution*jdPerPixel)/(labelStep*2.0))
            {
              int labelPlanet = (int)labelX % refChart.mainRing.planets.length;
              if (labelPlanet == planetNo)
              {
                double eT = tX - (labelOffs - 0.5) * labelStep;
                int eX = calcX( eT );
                double eL = refChart.isHeliocentric() ? refChart.helioLongitude( planet, eT ) : refChart.geoLongitude( planet, eT );
                int eY = calcY( eL );
                int plSign = (int)( eL / (ChartWheel.twopi/12) );
                Image signImg = refChart.signs[ plSign ];
                if (planet.image != null)
                {
                  g.drawImage( planet.image, eX - planet.image.getWidth(null), eY - planet.image.getHeight(null)/2, null );
                  g.drawImage( signImg, eX, eY, (int)(signImg.getWidth(null)*0.6), (int)(signImg.getHeight(null)*0.6), null );
                }
              }
            }
          }
        }
        planetNo ++;
      }
      g2.setStroke( oldStroke );
    }
    g.setClip( oldClip );
    // "NOW" line
    {
      g.setColor( new Color( 0, 128, 0, 96 ) );
      Stroke oldStroke = ((Graphics2D)g).getStroke();
      g2.setStroke( new BasicStroke( 3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ) );
      int x = innerBounds.x + innerBounds.width/2;
      g.drawLine( x, outerBounds.y, x, innerBounds.y + outerBounds.height );
      g2.setStroke( oldStroke );
    }
    // box outline
    g.setColor( Color.BLACK );
    g.drawRect( innerBounds.x, innerBounds.y, innerBounds.width, innerBounds.height );
  }
}
