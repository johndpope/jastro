package com.marklipson.astrologyclock;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.marklipson.astrologyclock.TimeZoneTools.TimeZoneGuess;

/**
 * Allows the adjustment of a time.
 */
public class TimeAdjuster extends JPanel
{
    // what time is being adjusted
    private TimeAdjusterTarget target;
    // slider for logarithmic time adjustment
    private SpeedSlider slider = new SpeedSlider();
    // text field for display/edit of time
    private JTextField timeView = new JTextField();
    // format for display/parse of time
    private static DateFormat timeFormat = new SimpleDateFormat( "yyyy-MMM-dd HH:mm" );
    private DecimalFormat latLngFormat = new DecimalFormat( "#0.000" );
    private DecimalFormat deltaFormat = new DecimalFormat( "#0.00" );
    private boolean changingTime = false;
    private boolean editingTimeField = false;
    private boolean changingTimezone = false;
    private boolean editingDeltaField = false;
    private boolean updatingToNow = false;
    private JCheckBox cbNow = new JCheckBox( "now", true );
    private JTextField latField = new JTextField( latLngFormat.format( 44.050486 ) );
    private JTextField lngField = new JTextField( latLngFormat.format( 123.092919 ) );
    private JTextField deltaField = new JTextField( deltaFormat.format( -8 ) );
    //private Vector tzSuggestions = new Vector();
    private JComboBox tzField = new JComboBox(/* tzSuggestions */);
    //private Vector placeSuggestions = new Vector();
    private JComboBox placeName2 = new JComboBox(/* placeSuggestions */);
    private DelayedAction placeUpdater = new DelayedAction( 650 );
    private DelayedAction timezoneUpdater = new DelayedAction( 650 );
    
    // STATE
    // currently editing the time editor
    private boolean editing = false;
    private Date snapshot = null;
    
    // thread to animate time changes
    private Thread animationThread = new Thread( "TimeAdjuster.animator" )
    {
        long t0 = System.currentTimeMillis();
        public void run()
        {
            for (;;)
            {
                try
                {
                    Thread.sleep( 50 );
                }
                catch( InterruptedException x )
                {
                    break;
                }
                long t1 = System.currentTimeMillis();
                tick( (t1 - t0) / 1000.0 );
                t0 = t1;
            }
        }
    };
    /**
     * The time is adjusted in an object that implements this interface.
     */
    static public interface TimeAdjusterTarget
    {
        public Date getTime();
        public void setTime( Date time );
        public void setTimeZone( TimeZone tz );
        public double getLatitude();
        public void setLatitude( double time );
        public double getLongitude();
        public void setLongitude( double time );
    }
    /**
     * This hook informs the time adjuster that the time it's adjusting has been changed.
     */
    public void timeChangedExternally( double t )
    {
        // TODO why can't we use t?
        // turn off the 'now' button
        if (! updatingToNow)
          if (Math.abs( t - ChartWheel.currentJD() ) > 2/1440.0)
            cbNow.setSelected( false );
        // update time display from chart
        setTimeInternal( target.getTime() );
    }
    
    private GridBagConstraints g( int x, int y )
    {
        GridBagConstraints c = new GridBagConstraints();
        Insets ins = new Insets( 0,1,0,1 );
        c.insets = ins;
        c.gridx = x;
        c.gridy = y;
        return c;
    }
    public TimeAdjuster()
    {
        setLayout( new GridBagLayout() );
        setBorder( BorderFactory.createLineBorder( Color.LIGHT_GRAY ) );
        GridBagConstraints c;
        // LINE 1
        // - time label
        c = g( 0, 0 );
        c.anchor = GridBagConstraints.WEST;
        add( new JLabel("time:"), c );
        // - time editor
        c = g( 1, 0 );
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.weightx = 1.4;
        add( timeView, c );
        // - 'now' button
        c = g( 3, 0 );
        add( cbNow, c );
        // - place name editor
        c = g( 4, 0 );
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridwidth = 9;
        placeName2.setEditable( true );
        add( placeName2, c );
        // LINE 2
        // - speed label
        c = g( 0, 1 );
        c.anchor = GridBagConstraints.WEST;
        add( new JLabel("speed:"), c );
        // - speed slider
        c = g( 1, 1 );
        c.gridwidth = 2;
        c.weightx = 0.8;
        c.fill = GridBagConstraints.BOTH;
        add( slider, c );
        slider.addChangeListener( new ChangeListener() {
            public void stateChanged(ChangeEvent e)
            {
              cbNow.setSelected( false );
            }
          } );
        // - stickiness of slider
        final JCheckBox sticky = new JCheckBox( "sticky" );
        sticky.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean state = ((JCheckBox)e.getSource()).isSelected();
                slider.setGravity( state ? 0 : 0.05 );
            }
        } );
        c = g( 3, 1 );
        c.fill = GridBagConstraints.BOTH;
        add( sticky, c );
        // - lat label
        c = g( 4, 1 );
        c.anchor = GridBagConstraints.WEST;
        add( new JLabel( "lat:"), c );
        // - lat editor
        c = g( 6, 1 );
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.3;
        add( latField, c );
        // - lng label
        c = g( 7, 1 );
        c.anchor = GridBagConstraints.WEST;
        add( new JLabel( "lng:"), c );
        // - lng editor
        c = g( 8, 1 );
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.3;
        add( lngField, c );
        // - delta label
        c = g( 9, 1 );
        c.anchor = GridBagConstraints.WEST;
        add( new JLabel( "tz:"), c );
        // - time diff editor
        c = g( 10, 1 );
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.3;
        add( deltaField, c );
        deltaField.setToolTipText( "You can manually override the timezone here if needed." );
        // - tz label
        c = g( 11, 1 );
        c.anchor = GridBagConstraints.WEST;
        add( new JLabel( "/"), c );
        // - tz editor
        c = g( 12, 1 );
        c.fill = GridBagConstraints.HORIZONTAL;
        add( tzField, c );
        tzField.setEditable( true );
        tzField.setPreferredSize( new Dimension( 100, tzField.getPreferredSize().height ) );
        tzField.setToolTipText( "Popular time zone rules for automatic timezone selection." );
        tzField.getEditor().getEditorComponent().addFocusListener( new FocusAdapter()
        {
          public void focusGained(FocusEvent arg0)
          {
            tzField.setPreferredSize( new Dimension( 230, tzField.getPreferredSize().height ) );
            TimeAdjuster.this.getLayout().layoutContainer( TimeAdjuster.this );
            tzField.getLayout().layoutContainer( tzField );
          }
          public void focusLost(FocusEvent arg0)
          {
            tzField.setPreferredSize( new Dimension( 100, tzField.getPreferredSize().height ) );
            TimeAdjuster.this.getLayout().layoutContainer( TimeAdjuster.this );
            tzField.getLayout().layoutContainer( tzField );
          }
        });
        // set font
        setFont( new Font( getFont().getName(), Font.PLAIN, 10 ) );
        for (int nC=0; nC < getComponentCount(); nC++)
          getComponent( nC ).setFont( getFont() );
        // select initial zone
        tzField.getEditor().setItem( TimeZoneTools.getDefaultTimezoneName() );

        // detect change to edited time field - update time as a new time is typed
        timeView.getDocument().addDocumentListener( new DocChangeAdapter()
        {
            public void changed( String newText )
            {
                try
                {
                    Date parsed = timeFormat.parse( newText );
                    if (! changingTime)
                      if (Math.abs( parsed.getTime() - System.currentTimeMillis() ) > 90000)
                        cbNow.setSelected( false );
                    editingTimeField = true;
                    setTimeExternal( parsed );
                    timeView.setBackground( TimeAdjuster.this.getBackground() );
                    timeView.repaint();
                    editingTimeField = false;
                    return;
                }
                catch( Exception x )
                {
                    timeView.setBackground( new Color( 0xff,0xc0,0xc0 ) );
                }
            }
        } );
        // detect change to latitude, longitude
        latField.getDocument().addDocumentListener( new DocChangeAdapter()
        {
            public void changed( String newText )
            {
                try
                {
                    Number parsed = latLngFormat.parse( newText );
                    if (target != null)
                        target.setLatitude( parsed.doubleValue() * ChartWheel.d2r );
                    latField.setBackground( TimeAdjuster.this.getBackground() );
                }
                catch( Exception x )
                {
                    latField.setBackground( new Color( 0xff,0xc0,0xc0 ) );
                }
            }
        } );
        lngField.getDocument().addDocumentListener( new DocChangeAdapter()
        {
            public void changed( String newText )
            {
                try
                {
                    Number parsed = latLngFormat.parse( newText );
                    if (target != null)
                        target.setLongitude( parsed.doubleValue() * ChartWheel.d2r );
                    lngField.setBackground( TimeAdjuster.this.getBackground() );
                }
                catch( Exception x )
                {
                    lngField.setBackground( new Color( 0xff,0xc0,0xc0 ) );
                }
            }
        } );
        deltaField.getDocument().addDocumentListener( new DocChangeAdapter()
        {
            public void changed( String newText )
            {
                try
                {
                    Number parsed = deltaFormat.parse( newText );
                    if (target != null)
                    {
                        // set custom timezone
                        if (! changingTimezone)
                            setCustomTimezone( parsed.doubleValue() );
                    }
                    deltaField.setBackground( TimeAdjuster.this.getBackground() );
                }
                catch( ParseException x )
                {
                    deltaField.setBackground( new Color( 0xff,0xc0,0xc0 ) );
                }
            }
        } );
        deltaField.addFocusListener( new FocusAdapter() {
          public void focusGained(FocusEvent e)
          {
              editingDeltaField = true;
          }
          public void focusLost(FocusEvent e)
          {
              editingDeltaField = false;
          }
        } );
        // don't advance time while editing it
        timeView.addFocusListener( new FocusAdapter()
        {
            public void focusGained(FocusEvent e)
            {
                editing = true;
            }
            public void focusLost(FocusEvent e)
            {
                editing = false;
            }
        } );
        // keep dropdown items synchronized
        ((JTextField)placeName2.getEditor().getEditorComponent()).getDocument().addDocumentListener( new DocChangeAdapter() {
            public void changed(String newText)
            {
                if (! inhibitLookup)
                    placeUpdater.trigger( new PlaceUpdateAction( newText ) );
            }
        } );
        // keep dropdown items synchronized
        ((JTextField)tzField.getEditor().getEditorComponent()).getDocument().addDocumentListener( new DocChangeAdapter() {
            public void changed(String newText)
            {
                if (! inhibitLookup)
                    timezoneUpdater.trigger( new TimezoneUpdateAction( newText ) );
            }
        } );
        // reset color of text when user selects a timeone themselves
        tzField.addActionListener( new ActionListener()
          {
            public void actionPerformed(ActionEvent arg0)
            {
              tzField.getEditor().getEditorComponent().setForeground( Color.BLACK );
            }
          } );
        // start new one
        animationThread.start();
        // TODO stop thread at some point
    }
    private boolean inhibitLookup = false;

    /**
     * This class is responsible for updating combo box entries based on what has been typed in the place name editor.
     */
    private class PlaceUpdateAction implements Runnable
    {
        private String placeName;
        PlaceUpdateAction( String placeName )
        {
            this.placeName = placeName;
        }
        public void run()
        {
            MiniAtlas.Entry results[] = MiniAtlas.getInstance().lookup( placeName );
            // exit now if thread has been interrupted
            if (Thread.interrupted())
                return;
            if (results == null  ||  results.length == 0)
            {
                // no results - flag control as errorful
                placeName2.setBackground( new Color( 0xff,0xc0,0xc0 ) );
            }
            else
            {
                // results - display them
                placeName2.setBackground( TimeAdjuster.this.getBackground() );
                ComboBoxModel model = new DefaultComboBoxModel( results );
                inhibitLookup = true;
                placeName2.setModel( model );
                inhibitLookup = false;
                JTextField field =  (JTextField)placeName2.getEditor().getEditorComponent();
                field.select( placeName.length(), field.getText().length() );
                // fill in lat/lng from first match
                MiniAtlas.Entry entry = results[0];
                if (target != null)
                {
                    target.setLatitude( entry.lat * ChartWheel.d2r );
                    target.setLongitude( entry.lng * ChartWheel.d2r );
                    setLatLngInternal( target.getLatitude(), target.getLongitude() );
                    // try to guess the timezone
                    TimeZoneTools.TimeZoneGuess tzGuess = TimeZoneTools.guessTimezone( entry );
                    if (tzGuess != null)
                    {
                      tzField.getEditor().setItem( tzGuess.timezone.getID() );
                      tzField.getEditor().getEditorComponent().setForeground( (tzGuess.quality >= TimeZoneGuess.QUALITY_FAIR) ? Color.BLACK : new Color(160,120,0) );
                    }
                }
            }
        }
    }
    /**
     * This class is responsible for updating combo box entries based on what has been typed in the place name editor.
     */
    private class TimezoneUpdateAction implements Runnable
    {
        private String zoneName;
        TimezoneUpdateAction( String zoneName )
        {
            this.zoneName = zoneName;
        }
        public void run()
        {
            String results[] = TimeZoneTools.findSimilarTimezones( zoneName );
            // exit now if thread has been interrupted
            if (Thread.interrupted())
                return;
            if (results == null  ||  results.length == 0)
            {
                // no results - flag control as errorful
                tzField.setBackground( new Color( 0xff,0xc0,0xc0 ) );
            }
            else
            {
                // results - display them
                tzField.setBackground( TimeAdjuster.this.getBackground() );
                ComboBoxModel model = new DefaultComboBoxModel( results );
                inhibitLookup = true;
                tzField.setModel( model );
                inhibitLookup = false;
                JTextField field =  (JTextField)tzField.getEditor().getEditorComponent();
                field.select( zoneName.length(), field.getText().length() );
                // select first matched timezone
                if (results.length > 0)
                {
                  changeTimezoneInternal( results[0] );
                }
            }
        }
    }
    /**
     * Look up a place name in the atlas.
     */
    /*
    private static MiniAtlas.Entry lookupCityState( String place )
    {
        MiniAtlas.Entry results[] = MiniAtlas.getInstance().lookup( place );
        if (results == null)
            return null;
        return results[0];
    }
    */
    
    abstract private static class DocChangeAdapter implements DocumentListener, Runnable
    {
      private String newText;
      private DelayedAction timer;
      DocChangeAdapter()
      {
        timer = new DelayedAction( 300 );
      }
      public void changedUpdate(DocumentEvent e)
      {
        Document doc = e.getDocument();
        try
        {
          newText = doc.getText( 0, doc.getLength() );
          trigger();
        }
        catch( BadLocationException x )
        {
        }
      }
      public void insertUpdate(DocumentEvent e)
      {
          Document doc = e.getDocument();
          try
          {
            newText = doc.getText( 0, doc.getLength() );
            trigger();
          }
          catch( BadLocationException x )
          {
          }
      }
      public void removeUpdate(DocumentEvent e)
      {
        Document doc = e.getDocument();
        try
        {
          newText = doc.getText( 0, doc.getLength() );
          trigger();
        }
        catch( BadLocationException x )
        {
        }
      }
      public void trigger()
      {
        timer.trigger( this );
      }
      public void run()
      {
        changed( newText );
      }
      abstract public void changed( String newText );
    }
    
    /**
     * Get the format used for time/date.
     */
    static public DateFormat getTimeFormat()
    {
      return (DateFormat) timeFormat.clone();
    }

    /**
     * Get current time represented by the adjuster.
     */
    public Date getTime()
    {
        TimeAdjusterTarget t = getTarget();
        if (t == null)
            return new Date();
        return t.getTime();
    }
    /**
     * Get current time as a string.
     */
    public String getTimeStr()
    {
        //cbNow.setSelected( false );
        return formatTime( getTime() );
    }
    /**
     * Set adjuster to a given time, specified by a string.
     */
    public void setTime( String time ) throws ParseException
    {
        setTime( parseTime( time ) );
    }
    /**
     * Interpret a date in the currently selected timezone.
     */
    public Date parseTime( String time ) throws ParseException
    {
      DateFormat fmt = (DateFormat)timeFormat.clone();
      fmt.setTimeZone( getTimezone() );
      return fmt.parse( time );
    }
    
    /**
     * Format a time in the currently selected timezone.
     */
    public String formatTime( Date time )
    {
      DateFormat fmt = (DateFormat)timeFormat.clone();
      fmt.setTimeZone( getTimezone() );
      return fmt.format( time );
    }
    /**
     * Get string representation of current timezone delta.
     */
    public String getZoneDeltaStr()
    {
        return String.format( "%.2d", getZoneDelta(null) );
    }
    /**
     * Get the currently selected timezone rule.
     */
    public String getZoneName()
    {
      return getTimezone().getID();
    }
    /**
     * Change the timezone.
     */
    private void changeTimezoneInternal( String tzName )
    {
      setTimeInternal( getTime() );
    }
    /**
     * Select a custom timezone.
     */
    public void setCustomTimezone( double hourDelta )
    {
      TimeZone tz = TimeZoneTools.getCustomTimeZone( hourDelta );
      tzField.setSelectedItem( tz.getDisplayName() );
      if (target != null)
        target.setTimeZone( tz );
    }
    /**
     * Select a named timezone rule.
     */
    public void setTimezone( TimeZone zone )
    {
      tzField.setSelectedItem( zone.getID() );
      if (target != null)
        target.setTimeZone( zone );
    }
    /**
     * Get current time zone from the timezone editing field.
     */
    public TimeZone getTimezone()
    {
      String name = tzField.getEditor().getItem().toString().trim();
      if (name.matches( "[+-]?\\d+(:\\d+)" ))
      {
        // parse hours & minutes
        // - find matching timezone and return it
        // fall back to system default
        return TimeZone.getDefault();
      }
      else
      {
        TimeZone tz = null;
        try
        {
          if (name.equals( "" ))
            tz = TimeZone.getDefault();
          else
            tz = TimeZoneTools.getTimeZone( name );
        }
        catch( Exception x )
        {
        }
        if (tz == null)
          return TimeZone.getDefault();
        return tz;
      }
    }
    
    /**
     * Set adjuster to a given time.
     */
    public void setTime( Date time )
    {
        TimeAdjusterTarget t = getTarget();
        if (t != null)
          t.setTime( time );
        setTimeInternal( time );
        // changing the time turns off 'tracking to present time'
        if (! updatingToNow)
          cbNow.setSelected( false );
    }
    /**
     * Set time within the controls without changing the target.
     */
    private void setTimeInternal( Date time )
    {
        if (editingTimeField)
          return;
        timeFormat.setTimeZone( getTimezone() );
        String newValue = timeFormat.format( time );
        changingTime = true;
        if (! newValue.equals( timeView.getText() ))
        {
          timeView.setText( newValue );
          timeView.repaint();
        }
        changingTime = false;
        double delta = getZoneDelta( time );
        if (! editingDeltaField)
        {
          changingTimezone = true;
          String newDelta = deltaFormat.format( delta );
          if (! newDelta.equals( deltaField.getText() ))
            deltaField.setText( newDelta );
          changingTimezone = false;
        }
    }
    /**
     * Get the timezone offset for a given date, or for the current date.
     */
    public double getZoneDelta(Date time)
    {
      if (time == null)
        time = getTime();
      return (double)getTimezone().getOffset( time.getTime() ) / (86400000/24);
    }
    /**
     * Specify a new geographic location.
     */
    public void setPlace( String placeName, double lat, double lng, boolean guessTZ )
    {
        lat *= ChartWheel.d2r;
        lng *= ChartWheel.d2r;
        TimeAdjusterTarget t = getTarget();
        if (t != null)
        {
            if (placeName != null)
            {
                inhibitLookup = true;
                //JTextField field =  (JTextField)placeName2.getEditor().getEditorComponent();
                //field.setText( placeName );
                placeName2.setSelectedItem( placeName );
                inhibitLookup = false;
            }
            t.setLatitude( lat );
            t.setLongitude( lng );
            setLatLngInternal( lat, lng );
            // try to guess the timezone
            if (guessTZ)
            {
              MiniAtlas.Entry results[] = MiniAtlas.getInstance().lookup( placeName );
              if (results != null  &&  results.length > 0)
              {
                TimeZoneTools.TimeZoneGuess tzGuess = TimeZoneTools.guessTimezone( results[0] );
                if (tzGuess != null)
                {
                  tzField.getEditor().setItem( tzGuess.timezone.getID() );
                  tzField.getEditor().getEditorComponent().setForeground( (tzGuess.quality >= TimeZoneGuess.QUALITY_FAIR) ? Color.BLACK : new Color(160,120,0) );
                }
              }
            }
        }
    }
    public String getPlaceName()
    {
        JTextField field =  (JTextField)placeName2.getEditor().getEditorComponent();
        return field.getText();
    }
    /**
     * Gets latitude and longitude as an array, in degrees.
     */
    public double[] getPlaceLatLng()
    {
        TimeAdjusterTarget t = getTarget();
        if (t != null)
            return new double[] { t.getLatitude() / ChartWheel.d2r, t.getLongitude() / ChartWheel.d2r };
        else
            return null;
    }
    private void setLatLngInternal( double lat, double lng )
    {
        latField.setText( latLngFormat.format( lat / ChartWheel.d2r ) );
        lngField.setText( latLngFormat.format( lng / ChartWheel.d2r ) );
    }
    /**
     * Set time within the target without changing the controls.
     */
    private void setTimeExternal( Date time )
    {
        TimeAdjusterTarget t = getTarget();
        if (t != null)
            t.setTime( time );
    }
    /**
     * Specify what time is being adjusted.
     */
    public void setTarget(TimeAdjusterTarget target)
    {
        this.target = target;
        // update to time from target
        setTimeInternal( getTime() );
        setLatLngInternal( target.getLatitude(), target.getLongitude() );
    }
    /**
     * Get current target of time adjustment.
     */
    public TimeAdjusterTarget getTarget()
    {
        return target;
    }
    /**
     * Is it representing the present moment?
     */
    public boolean isNow()
    {
        // TODO add NOW button and implement this
        return false;
    }
    
    /**
     * Handle the advancement of time.
     */
    private void tick( double dt )
    {
        slider.tick( dt );
        if (! editing)
        {
            Date current;
            if (cbNow.isSelected())
            {
              updatingToNow = true;
              // set current time
              current = new Date();
            }
            else if (slider.getValue() != 0)
            {
              // get rate of change
              double rate = slider.getValue() * 86400;
              double change = dt * rate;
              // apply changes
              current = getTime();
              current.setTime( (long)(current.getTime() + change * 1000) );
            }
            else
              return;
            // update time based on change
            if (getTarget() != null)
              getTarget().setTime( current );
            setTimeInternal( current );
            // check whether target time has changed, and update controls
            Date newSnapshot = getTime();
            if (snapshot != null  &&  Math.abs( snapshot.getTime() - newSnapshot.getTime()) > 5000)
              setTimeInternal( newSnapshot );
            snapshot = newSnapshot;
            updatingToNow = false;
        }
    }
}
