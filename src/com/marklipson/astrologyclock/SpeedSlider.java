package com.marklipson.astrologyclock;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A slider bar with logarithmic impact and center-gravity.
 */
public class SpeedSlider extends JPanel
{
    private Color axisColor = new Color( 0x00, 0x00, 0x00 );
    private Color sliderFillColor = new Color( 0xa0, 0x00, 0x00 );
    private Color sliderOutlineColor = new Color( 0x50, 0x00, 0x00 );
    // offset between -1 and +1, representing location of middle of slider
    private double sliderOffset = 0;
    // radius of slider (assumes it is a circle right now)
    private int sliderRadius = 9;
    // strength of gravity (0=none, 1=absolute)
    private double gravity = 0.03;
    // where gravity is centered in terms of value
    private double gravityCenter = 0;
    // logarithmic extent
    private double logMin =    0.01;
    private double logMax = 1000.00;
    
    // STATE
    // slider is being dragged
    private boolean dragging = false;
    private int dragOffset = 0;
    
    private List<ChangeListener> changeListeners = null;
    
    // initialize
    public SpeedSlider()
    {
        // implement drag of slider
        addMouseListener( new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                if (! hasFocus())
                    requestFocus();
                dragging = true;
                if (! isPointOverSlider( e.getX(), e.getY() ))
                    setSliderCenter( e.getX() );
                dragOffset = getSliderCenter() - e.getX();
            }
            public void mouseReleased(MouseEvent e)
            {
                dragging = false;
                // high gravity means immediately drop to zero
                if (getGravity() >= 1)
                    setValue( gravityCenter );
            }
        } );
        addMouseMotionListener( new MouseMotionAdapter()
        {
            public void mouseDragged( MouseEvent e)
            {
                if (dragging)
                {
                    setSliderCenter( e.getX() + dragOffset );
                }
            }
        } );
    }
    public void addChangeListener( ChangeListener l )
    {
      if (changeListeners == null)
        changeListeners = new ArrayList<ChangeListener>();
      changeListeners.add( l );
    }
    private void fireChange()
    {
      if (changeListeners == null)
        return;
      ChangeEvent evt = new ChangeEvent( this );
      for (ChangeListener l : changeListeners)
        l.stateChanged( evt );
    }
    private boolean isPointOverSlider( int x, int y )
    {
        int sx = getSliderCenter();
        int sy = getHeight()/2;
        if (x < sx - sliderRadius  ||  x > sx + sliderRadius)
            return false;
        if (y < sy - sliderRadius  ||  y > sy + sliderRadius)
            return false;
        // TODO technically, there are corners to consider - later
        return true;
    }
    // draw innards of component
    protected void paintComponent( Graphics g )
    {
        int w = getWidth();
        int h = getHeight();
        int midX = w/2;
        int midY = h/2;
        // clear background
        g.setColor( getBackground() );
        g.fillRect( 0, 0, w, h );
        // line along the middle
        g.setColor( axisColor );
        g.drawLine( 0, midY, w, midY );
        g.drawLine( midX, 0, midX, h );
        // slider
        g.setColor( sliderFillColor );
        int sx = getSliderCenter();
        int diam = sliderRadius*2;
        g.fillOval( sx - sliderRadius, midY - sliderRadius, diam, diam );
        g.setColor( sliderOutlineColor );
        g.drawOval( sx - sliderRadius, midY - sliderRadius, diam, diam );
    }
    private int getSliderCenter()
    {
        int w1 = getWidth()/2 - sliderRadius;
        return (int)(sliderOffset * w1 + getWidth()/2);
    }
    private void setSliderCenter( int x )
    {
        if (x < sliderRadius)
            x = sliderRadius;
        if (x > getWidth() - sliderRadius)
            x = getWidth() - sliderRadius;
        int w1 = getWidth()/2 - sliderRadius;
        double v = ((double)(x - getWidth()/2)) / w1;
        sliderOffset = v;
        snapToZero();
        repaint( 30 );
        fireChange();
    }
    private void snapToZero()
    {
        // very close to X==0, use a value of exactly zero
        if (Math.abs( sliderOffset ) < 3.0/getWidth())
            sliderOffset = 0;
    }
    /**
     * Inform control of the advancing of time in order for gravity to work.
     */
    public void tick( double dt )
    {
        if (! dragging  &&  getGravity() > 0  &&  getGravity() < 1)
        {
            double gC = getSliderOffsetForValue( gravityCenter );
            double dV = sliderOffset - gC;
            sliderOffset = gC + dV * Math.pow( getGravity(), dt );
            snapToZero();
            repaint();
        }
    }
    public double getValue()
    {
        if (sliderOffset == 0)
            return 0;
        double l0 = Math.log( logMin );
        double l1 = Math.log( logMax );
        if (sliderOffset > 0)
            return Math.exp( l0 + sliderOffset * (l1-l0) );
        else
            return - Math.exp( l0 - sliderOffset * (l1-l0) );
    }
    private double getSliderOffsetForValue( double v )
    {
        double l0 = Math.log( logMin );
        double l1 = Math.log( logMax );
        double lV = Math.log( Math.abs( v ) );
        if (lV > l1)
            lV = l1;
        if (lV < l0)
            lV = l0;
        double absOffs = (lV-l0) / (l1-l0);
        return (v < 0) ? -absOffs : absOffs;
    }
    public void setValue( double v )
    {
        fireChange();
        sliderOffset = getSliderOffsetForValue( v );
        snapToZero();
        repaint();
    }
    public Dimension getMinimumSize()
    {
        return new Dimension( sliderRadius*8, sliderRadius*2 + 1 + 4 );
    }
    public Dimension getPreferredSize()
    {
        Dimension sz = super.getPreferredSize();
        Dimension min = getMinimumSize();
        if (sz.width < min.width)
            sz.width = min.width;
        if (sz.height < min.height)
            sz.height = min.height;
        return sz;
    }
    public void setGravity(double gravity)
    {
        this.gravity = gravity;
    }
    public double getGravity()
    {
        return gravity;
    }
};
