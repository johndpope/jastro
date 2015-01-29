package com.marklipson.astrologyclock;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Generates chart graphics.
 * 
 * Use this switch when running on Linux:
 *   -Djava.awt.headless=true
 * 
 * ARGUMENTS:
 *   see webapp/index.jsp for full details
 */
public class Generator extends HttpServlet
{
  private static final long serialVersionUID = 1L;
  private static SimpleDateFormat http_date_fmt = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss Z" );

  static class Options
  {
    Date time;
    double lat = 40;
    double lng = 120;
    String caption;
    int diameter = 500;
    boolean showHouses = true;
    boolean square = false;
    boolean helio = false;
    boolean stars = false;
    int zodiac = ChartWheel.TROPICAL;
    boolean hideAll = false;
    List<String> hideBodies = new ArrayList<String>();
    List<String> showBodies = new ArrayList<String>();
    Date t2;
    
    String generateCacheKey()
    {
      String key = "t=" + time.getTime() / 60000 + ":";
      key += Math.round( lat * 2 ) + ":";
      key += Math.round( lng * 2 ) + ":";
      key += caption + ":";
      key += diameter + ":";
      key += showHouses?"houses:":"nohouses";
      key += square?"square:":"";
      key += helio?"helio:":"";
      key += stars?"stars:":"";
      key += zodiac + ":";
      for (String b : hideBodies)
        key += "H=" + b + ":";
      for (String b : showBodies)
        key += "S=" + b + ":";
      if (t2 != null)
        key += "t2=" + t2.getTime() / 60000 + ":";
      // md5
      String hash;
      try
      {
        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        digest.update( key.getBytes() );
        String md5 = String.format( "%032x", new BigInteger( 1, digest.digest() ) );
        hash = md5;
      }
      catch( NoSuchAlgorithmException x )
      {
        hash = "" + key.hashCode();
      }
      return hash;
    }
    /**
     * Determine if the request is based on the current time.
     * 
     * The implication of a "transient" set of options is that they would
     * not generate the same chart at a different time.
     */
    boolean isTransient()
    {
      if (Math.abs( System.currentTimeMillis() - time.getTime() ) < 1000)
        return true;
     if (t2 != null  &&  Math.abs( System.currentTimeMillis() - t2.getTime() ) < 1000)
       return true;
     return false;
    }
  }
    public static void generate( Options options, OutputStream out ) throws IOException
    {
        URL imageLocation = Generator.class.getResource("resources/sun.gif");
        ChartWheel ss = new ChartWheel( null, imageLocation );
        ss.setDate( options.time );
        ss.setPlace( options.lat * ChartWheel.d2r, options.lng * ChartWheel.d2r );
        ss.setEnableChartRotation( options.showHouses );
        ss.setHeliocentric( options.helio );
        ss.setSquare( options.square );
        if (options.hideAll)
          ss.setDisplayAllBodies( false );
        for (String hide : options.hideBodies)
          ss.setDisplayBody( hide, false );
        for (String show : options.showBodies)
          ss.setDisplayBody( show, true );
        ss.setZodiac( options.zodiac );
        ss.setShowStars( options.stars );
        if (options.caption != null)
          ss.setCaption( options.caption );
        if (options.t2 != null)
          ss.setOuterRing( options.t2.getTime() / 86400000.0 + 2440587.5 );
        BufferedImage image = new BufferedImage( options.diameter, options.diameter, BufferedImage.TYPE_4BYTE_ABGR );
        ss.paintBuffer( image );
        if (! ImageIO.write( image, "PNG", out ))
            throw new IOException( "No image writer found for PNG" );
    }
    
    /**
     * Parse time/date and zone parameters.  Returns millitime.
     */
    static private long parseDateAndZone( String pT, String pZ )
    {
      // null or 0 returns current time
      if (pT ==  null  ||  pT.equals( "" )  ||  pT.equals( "0" ))
        return System.currentTimeMillis();
      // specify unix time (seconds past 1970)
      if (pT.matches( "^\\-?\\d+$" ))
        return Long.parseLong( pT ) * 1000;
      // try to parse date/time components
      String dtParts[] = pT.split( "[ \\-:]" );
      // - fall back to current time if it isn't valid
      if (dtParts.length < 3)
        return System.currentTimeMillis();
      // specify time components
      Calendar cal = Calendar.getInstance();
      cal.clear();
      cal.set( Integer.parseInt( dtParts[0] ), Integer.parseInt( dtParts[1] ) - 1, Integer.parseInt( dtParts[2] ) );
      if (dtParts.length > 3)
        cal.set( Calendar.HOUR_OF_DAY, Integer.parseInt( dtParts[3] ) );
      else
        cal.set( Calendar.HOUR_OF_DAY, 12 );
      if (dtParts.length > 4)
        cal.set( Calendar.MINUTE, Integer.parseInt( dtParts[4] ) );
      if (dtParts.length > 5)
        cal.set( Calendar.SECOND, Integer.parseInt( dtParts[5] ) );
      // set time zone if specified
      if (pZ != null)
      {
        if (pZ.matches( "^\\-?\\d+(\\.\\d+)?$" ))
        {
          double tzh = Double.parseDouble( pZ );
          String sgn = (tzh < 0) ? "-" : "+";
          if (tzh < 0)
            tzh = -tzh;
          int hours = (int)tzh;
          int minutes = (int)Math.round( (tzh - hours) * 60 );
          pZ = "GMT" + sgn + hours + ":" + ((minutes<10)?"0":"") + minutes;
        }
        TimeZone tz = TimeZone.getTimeZone( pZ );
        if (tz != null)
          cal.setTimeZone( tz );
      }
      else
        cal.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
      //
      return cal.getTimeInMillis();
    }

    /**
     * Process image generation requests.
     */
    protected void doGet(HttpServletRequest rqst, HttpServletResponse resp)
            throws ServletException, IOException
    {
        // get arguments
        String pDiam = rqst.getParameter( "d" );
        String pT = rqst.getParameter( "t" );
        String pT2 = rqst.getParameter( "t2" );
        String pZ = rqst.getParameter( "z" );
        String pZ2 = rqst.getParameter( "z2" );
        String pLat = rqst.getParameter( "lat" );
        String pLng = rqst.getParameter( "lng" );
        String pHouses = rqst.getParameter( "h" );
        String pCenter = rqst.getParameter( "center" );
        String pHide = rqst.getParameter( "hide" );
        String pShow = rqst.getParameter( "show" );
        String pOnly = rqst.getParameter( "only" );
        String pSquare = rqst.getParameter( "square" );
        String pZodiac = rqst.getParameter( "zod" );
        String pStars = rqst.getParameter( "stars" );
        String pCaption = rqst.getParameter( "caption" );
        Options opts = new Options();
        opts.caption = pCaption;
        if (pDiam != null)
        {
          opts.diameter = Integer.parseInt( pDiam );
          if (opts.diameter < 250)
            opts.diameter = 250;
          if (opts.diameter > 800)
            opts.diameter = 800;
        }
        if (pZodiac != null)
        {
          if (pZodiac.equalsIgnoreCase( "fagan-bradley" ))
            opts.zodiac = ChartWheel.FAGAN_BRADLEY;
          else if (pZodiac.equalsIgnoreCase( "raman" ))
            opts.zodiac = ChartWheel.RAMAN;
          else if (pZodiac.equalsIgnoreCase( "lahiri" ))
            opts.zodiac = ChartWheel.LAHIRI;
          else if (pZodiac.matches( "\\d+" ))
            opts.zodiac = Integer.parseInt( pZodiac );
        }
        if (pT2 != null)
          opts.t2 = new Date( parseDateAndZone( pT2, pZ2 ) );
        opts.showHouses = ! "0".equals( pHouses );
        opts.stars = "1".equals( pStars );
        opts.square = "1".equals( pSquare );
        opts.helio = "sun".equalsIgnoreCase( pCenter );
        if (pOnly != null)
        {
          opts.hideAll = true;
          String[] toShow = pOnly.split( "[ ,]" );
          for (String b : toShow)
            opts.showBodies.add( b );
        }
        if (pHide != null)
        {
          String[] toHide = pHide.split( "[ ,]" );
          for (String b : toHide)
            opts.hideBodies.add( b );
        }
        if (pShow != null)
        {
          String[] toShow = pShow.split( "[ ,]" );
          for (String b : toShow)
            opts.showBodies.add( b );
        }
        opts.time = new Date( parseDateAndZone( pT, pZ ) );
        if (pLat != null)
          opts.lat = Double.parseDouble( pLat );
        if (pLng != null)
          opts.lng = Double.parseDouble( pLng );
        // detect transient chart (i.e. if it is based on current time)
        boolean isTransient = opts.isTransient();
        // get cache filename
        File appFolder;
        {
          String configDir = getServletConfig().getInitParameter( "appdir" );
          if (configDir == null)
          {
            appFolder = new File( new File( System.getProperty( "user.home" ) ), "chart-generator" );
            if (! appFolder.exists())
              appFolder = new File( "/home/marklipson/chart-generator/" );
          }
          else
            appFolder = new File( configDir );
        }
        File cacheFolder = new File( appFolder, "cache" );
        File transientFolder = new File( cacheFolder, "transient" );
        if (! transientFolder.exists())
          transientFolder.mkdirs();
        File cacheFile = new File( isTransient ? transientFolder : cacheFolder, opts.generateCacheKey() + ".png" );
        // purge old files from cache, occasionally
        File purgeMarker = new File( cacheFolder, "last-purge" );
        if (! purgeMarker.exists())
        {
          FileOutputStream out = new FileOutputStream( purgeMarker );
          out.write( "\n".getBytes() );
          out.close();
        }
        if (System.currentTimeMillis() - purgeMarker.lastModified() > 15*60*1000)
        {
          purgeMarker.setLastModified( System.currentTimeMillis() );
          purgeOldFiles( cacheFolder, 3*86400*1000 );
          purgeOldFiles( transientFolder, 3*60*1000 );
        }
        // set output headers
        // - image content
        resp.setHeader( "Content-type", "image/png" );
        // - expiration time for caching
        long expiresIn = 3*86400*1000;
        if (isTransient)
          expiresIn = 60*1000;
        resp.setHeader( "Date", http_date_fmt.format( new Date() ) );
        resp.setHeader( "Expires", http_date_fmt.format( new Date( System.currentTimeMillis() + expiresIn ) ) );
        // generate image or display from cache
        long cacheTime = isTransient ? 3*60*1000 : 3*86400*1000;
        if (cacheFile.exists()  &&  cacheFile.lastModified() > System.currentTimeMillis() - cacheTime)
        {
          copy( cacheFile, resp.getOutputStream() );
          cacheFile.setLastModified( System.currentTimeMillis() );
        }
        else
        {
          FileOutputStream saveToCache = new FileOutputStream( cacheFile );
          generate( opts, saveToCache );
          copy( cacheFile, resp.getOutputStream() );
        }
        /*
          // just generate it
          generate( opts, resp.getOutputStream() );
        */
    }
    /**
     * Delete files older than a certain age.
     */
    private static void purgeOldFiles( File folder, int maxAge )
    {
      long cutoff = System.currentTimeMillis() - maxAge;
      for (File f : folder.listFiles())
      {
        if (f.isDirectory())
          continue;
        if (f.lastModified() < cutoff)
          f.delete();
      }
    }
    
    public static void copy( File in, OutputStream out ) throws IOException
    {
      FileInputStream inStream = new FileInputStream( in );
      copy( inStream, out, true );
    }
    public static void copy( InputStream in, OutputStream out, boolean closeInput ) throws IOException
    {
      byte buf[] = new byte[ 20000 ];
      for (;;)
      {
        int nB = in.read( buf );
        if (nB <= 0)
          break;
        out.write( buf, 0, nB );
      }
      if (closeInput)
        in.close();
    }

    /**
     * Run as application - usage: Generate time lat lng
     * - Sends PNG image content to stdout.
     */
    public static void main( String args[] )
    {
      if (args.length == 0)
        args = new String[] { "1967 4 4", "41", "87" };
      
        if (args.length != 3)
        {
            System.err.println( "Usage: Generate time lat lng  - time=unixtime(s), lat/lng=degrees" );
            System.exit( 1 );
        }
        try
        {
            Options opts = new Options();
            opts.time = new Date( parseDateAndZone( args[0], "PST" ) );
            opts.lat = Double.parseDouble( args[1] );
            opts.lng = Double.parseDouble( args[2] );
            generate( opts, System.out );
        }
        catch( Exception x )
        {
            x.printStackTrace( System.err );
            System.exit( 2 );
        }
    }
}
