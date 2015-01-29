package com.marklipson.astrologyclock;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Find, draw, and remember aspect lines between planets.
 */
public class Aspects
{
    private List<Aspect> aspects = new ArrayList<Aspect>();
    
    private class Aspect
    {
        @SuppressWarnings("unused")
        int planet1;
        @SuppressWarnings("unused")
        int planet2;
        String description;
        Color color;
        Point p1, p2;
    }

    public static double point_line_distance2( Point p, Point line_p1, Point line_p2 )
    {
        return Line2D.ptSegDistSq( p.x, p.y, line_p1.x, line_p1.y, line_p2.x, line_p2.y );
    }
    /**
     * Find aspects.
     */
    public Aspects( ChartWheel wheel, ChartWheel.PlanetRing ring )
    {
        aspects.clear();
        for (int n1=0; n1 < ring.planets.length-1; n1++)
        {
            double p1 = ring.planets[n1].obsLongitude;
            double p1d = ring.planets[n1].tmpPos;
            if (! wheel.isPlanetVisible( ring.planets[n1].name ))
                continue;
            if (p1 < 0)
                continue;
            if (ring.planets[n1].posScreenDot.x == 0)
                continue;
            Point pt1 = wheel.polarPosition( p1d, wheel.rPlanetDots );
            for (int n2=n1+1; n2 < ring.planets.length; n2++)
            {
                double p2 = ring.planets[n2].obsLongitude;
                double p2d = ring.planets[n2].tmpPos;
                if (! wheel.isPlanetVisible( ring.planets[n2].name ))
                    continue;
                if (p2 < 0)
                    continue;
                if (ring.planets[n2].posScreenDot.x == 0)
                    continue;
                double asp = Math.abs( p1 - p2 ) * 180 / Math.PI;
                if (asp > 180)
                    asp = 360 - asp;
                Aspect a = new Aspect();
                a.color = Color.WHITE;
                a.planet1 = n1;
                a.planet2 = n2;
                a.p1 = pt1;
                String type = null;;
                if (asp < 8)
                {
                    a.color = new Color( 192, 192, 0 );
                    type = "conjunction";
                }
                else if (asp > 56  &&  asp < 64)
                {
                    a.color = new Color( 128, 192, 0 );
                    type = "sextile";
                }
                else if (asp > 84  &&  asp < 96)
                {
                    a.color = new Color( 192, 0, 0 );
                    type = "square";
                }
                else if (asp > 114  && asp < 126)
                {
                    a.color = new Color( 0, 0, 192 );
                    type = "trine";
                }
                else if (asp > 173)
                {
                    a.color = new Color( 128, 0, 0 );
                    type = "opposition";
                }
                if (type != null)
                {
                    a.p2 = wheel.polarPosition( p2d, wheel.rPlanetDots );
                    a.description = ring.planets[n1].name + " " + type + " " + ring.planets[n2].name;
                    aspects.add( a );
                }
            }
        }
    }
    /**
     * Draw the aspects.
     */
    public void draw( Graphics g )
    {
        for (int n=0; n < aspects.size(); n++)
        {
            Aspect a = (Aspect)aspects.get( n );
            g.setColor( a.color );
            g.drawLine( a.p1.x, a.p1.y, a.p2.x, a.p2.y );
        }
    }
    /**
     * Test for proximity to an aspect line.
     */
    public String findNearbyAspect( int x, int y )
    {
        double dClosest = 10000;
        Aspect aClosest = null;
        for (int n=0; n < aspects.size(); n++)
        {
            Aspect a = (Aspect)aspects.get( n );
            double d = Line2D.ptSegDistSq( a.p1.x, a.p1.y, a.p2.x, a.p2.y, x, y );
            if (d < 25  &&  (d < dClosest  ||  aClosest == null))
            {
                dClosest = d;
                aClosest = a;
            }
        }
        if (aClosest != null)
            return aClosest.description;
        return null;
    }
}
