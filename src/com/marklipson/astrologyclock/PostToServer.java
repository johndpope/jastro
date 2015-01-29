package com.marklipson.astrologyclock;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import com.marklipson.astrologyclock.thirdparty.Base64;

public class PostToServer
{
  /**
   * Generate a graphic for a chart.
   */
  public byte[] captureChart( String strTime, String strZone, double latitude, double longitude, String options, Double outerTime ) throws Exception
  {
      // get time and zone
      DateFormat timeFormat = TimeAdjuster.getTimeFormat();
      if (strZone != null)
          timeFormat.setTimeZone( TimeZone.getTimeZone( strZone ) );
      double jd = ChartWheel.systemTime_to_JD( timeFormat.parse( strTime ).getTime() );
      // set up to draw chart
      int radius = 600;
      URL imageLocation = PostToServer.class.getResource("resources/sun.gif");
      ChartWheel ss = new ChartWheel( null, imageLocation );
      ss.setJD( jd );
      ss.setDisplayInfo( options );
      ss.setPlace( latitude * ChartWheel.d2r, longitude * ChartWheel.d2r );
      ss.setShowStars( false );
      if (outerTime != null)
        ss.setOuterRing( outerTime );
      BufferedImage image = new BufferedImage( radius, radius, BufferedImage.TYPE_4BYTE_ABGR );
      ss.paintBuffer( image );
      ByteArrayOutputStream outBuf = new ByteArrayOutputStream( 30000 );
      if (! ImageIO.write( image, "PNG", outBuf ))
        throw new IOException( "No image writer found for PNG" );
      return outBuf.toByteArray();
  }

  /**
   * Post binary data to a server.
   */
  public String post( URL url, byte[] data ) throws IOException
  {
    URLConnection conn = url.openConnection();
    conn.setDoOutput( true );
    String base64data = Base64.encodeBytes( data );
    conn.connect();
    OutputStream postStream = conn.getOutputStream();
    postStream.write( "data=".getBytes() );
    postStream.write( URLEncoder.encode( new String( base64data.getBytes() ), "UTF8" ).getBytes() );
    postStream.close();
    Object response = conn.getContent();
    if (response instanceof InputStream)
    {
      InputStream in = (InputStream)response;
      byte[] buf = new byte[1000];
      int pos = 0;
      for (;;)
      {
        int nr = in.read( buf, pos, buf.length - pos );
        if (nr <= 0)
          break;
        pos += nr;
      }
      return new String( buf, 0, pos );
    }
    else
      return response.toString();
  }

}
