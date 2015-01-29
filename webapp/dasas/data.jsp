<%@page import="com.marklipson.astrologyclock.ChartData"%>
<%@page import="com.marklipson.astrologyclock.ChartWheel"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.marklipson.astrologyclock.Dasas"%>
<%
  String strTime = request.getParameter( "time" );
  long t = new SimpleDateFormat("yyyy/MM/dd HH:mm zzz").parse( strTime ).getTime();
  double jd = ChartWheel.systemTime_to_JD( t );
  Dasas dasas = new Dasas( jd, ChartData.RAMAN );
%>
<pre>
<%
  Dasas.dump( dasas.periods, out, 0 );
%>
</pre>
