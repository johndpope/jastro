package com.marklipson.astrologyclock;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.marklipson.astrologyclock.ChartWheel.ChartListener;

/**
 * Options for drawing chart, and chart information.
 */
public class ChartOptions extends JPanel
{
    private Applet applet;
    private ChartWheel target;
    private TimeAdjuster timeAdjuster;
    private JLabel statusBar;
    private JPanel moreOptions;
    private JPanel dataPanel;
    private SearchPanel searchPanel;
    private GraphPanel graphPanel;
    
    private GridBagConstraints g( int x, int y )
    {
        GridBagConstraints c = new GridBagConstraints();
        Insets ins = new Insets( 0,1,0,2 );
        c.insets = ins;
        c.gridx = x;
        c.gridy = y;
        return c;
    }

    public ChartOptions( Applet _applet, TimeAdjuster timeAdjuster, ChartWheel wheel )
    {
        applet = _applet;
        this.timeAdjuster = timeAdjuster;
        target = wheel;
        setBorder( BorderFactory.createLineBorder( Color.LIGHT_GRAY ) );
        setLayout( new GridBagLayout() );
        final Font smallerFont = new Font( getFont().getName(), Font.PLAIN, 10 );

        // accuracy
        Box accAndLbl = Box.createHorizontalBox();
        {
          final JTextField accuracy = new JTextField( 3 );
          accuracy.setPreferredSize( new Dimension( 30, accuracy.getPreferredSize().height ) );
          accuracy.setToolTipText( "Time accuracy range, in days" );
          accuracy.addKeyListener( new KeyListener()
          {
            public void keyReleased(KeyEvent e)
            {
              try {
                String strVal = accuracy.getText().trim();
                double value = (strVal.equals( "" )) ? 0 : Double.parseDouble( strVal );
                if (value < 0  ||  value > 100)
                  throw new NumberFormatException();
                target.setTimeAccuracy( value );
                accuracy.setBackground( getBackground() );
                accuracy.setToolTipText( "Time accuracy range, in days" );
              } catch( NumberFormatException x ) {
                accuracy.setBackground( new Color(255,128,128) );
                accuracy.setToolTipText( "Requires a number of days, between 0 and 100!" );
              }
            }
            public void keyPressed(KeyEvent e) {}
            public void keyTyped(KeyEvent e) {}
          } );
          JLabel lbl = new JLabel("range:");
          lbl.setFont( smallerFont );
          accAndLbl.add( lbl );
          accAndLbl.add( accuracy );
        }
        // zodiac
        final JComboBox zodiac = new JComboBox( new String[] { "Tropical", "Raman", "Lahiri", "Fagan-Br." } );
        zodiac.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                target.setZodiac( zodiac.getSelectedIndex() );
            }
        } );
        zodiac.setSelectedIndex( target.getZodiac() );

        // show ascendant
        final JCheckBox showAC = new JCheckBox( "ascendant", wheel.isEnableChartRotation() );
        showAC.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                target.setEnableChartRotation( ((JCheckBox)e.getSource()).isSelected() );
            }
        } );
        if (wheel.isHeliocentric())
            showAC.setSelected( false );
        showAC.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                target.setEnableChartRotation( ((JCheckBox)e.getSource()).isSelected() );
            }
        } );
        // more...
        final JCheckBox btnMoreOpts = new JCheckBox( "choose bodies" );
        btnMoreOpts.addActionListener( new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            if (moreOptions != null)
            {
              applet.remove( moreOptions );
              moreOptions = null;
              btnMoreOpts.setSelected( false );
            }
            else
            {
              btnMoreOpts.setSelected( true );
              moreOptions = new JPanel();
              moreOptions.setLayout( new BoxLayout(moreOptions,BoxLayout.Y_AXIS) );
              moreOptions.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createLineBorder( Color.LIGHT_GRAY ), BorderFactory.createEmptyBorder(4,4,4,4) ) );
              JLabel title;
              moreOptions.add( title = new JLabel( "Bodies" ) );
              title.setForeground( new Color( 160,160,160) );
              String[] extraBodies = new String[] { "Sun","Moon","Mercury","Venus","Mars","Jupiter","Saturn","Uranus","Neptune","Pluto","Chiron","Ceres","Sedna","NorthNode","SouthNode" };
              for (String body : extraBodies)
              {
                JCheckBox item = new JCheckBox( body, target.isPlanetVisible(body) );
                item.setFont( smallerFont );
                item.addActionListener( new ActionListener()
                {
                  public void actionPerformed(ActionEvent e)
                  {
                    JCheckBox itm = ((JCheckBox)e.getSource());
                    itm.setSelected( ! target.isPlanetVisible( itm.getText() ) );
                    target.setDisplayBody( itm.getText(), itm.isSelected() );
                  }
                });
                moreOptions.add( item );
              }
              applet.add( moreOptions, BorderLayout.WEST );
            }
            applet.invalidate();
            applet.validate();
            applet.repaint();
          }
        });
        // more...
        final JCheckBox btnDataPanel = new JCheckBox( "data" );
        btnDataPanel.addActionListener( new ActionListener()
        {
          ChartListener cl;
          JLabel labels[];
          public void actionPerformed(ActionEvent e)
          {
            if (dataPanel != null)
            {
              applet.remove( dataPanel );
              dataPanel = null;
              btnDataPanel.setSelected( false );
              target.removeChartListener( cl );
            }
            else
            {
              cl = new ChartListener()
              {
                public void mouseOverObject( String name )
                {
                }
                public void mouseOverPlanet( ChartPlanet planet )
                {
                }
                public void timeChanged(double t)
                {
                  for (int n=0; n < labels.length; n++)
                  {
                    String text;
                    if (n >= target.mainRing.planets.length)
                      text = ChartWheel.formatDegrees( target.calcAscendant( target.getJD() ) );
                    else
                    {
                      ChartPlanet body = target.mainRing.planets[ n ];
                      text = ChartWheel.formatDegrees( target.isHeliocentric() ? body.getHelioLongitude() : body.getGeoLongitude() );
                    }
                    labels[n].setText( text );
                  }
                  dataPanel.invalidate();
                  dataPanel.repaint();
                }
              };
              labels = new JLabel[ target.mainRing.planets.length + 1 ];
              btnDataPanel.setSelected( true );
              dataPanel = new JPanel();
              dataPanel.setLayout( new BoxLayout(dataPanel,BoxLayout.Y_AXIS) );
              dataPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createLineBorder( Color.LIGHT_GRAY ), BorderFactory.createEmptyBorder(4,4,4,4) ) );
              JLabel title;
              dataPanel.add( title = new JLabel( "Data" ) );
              title.setForeground( new Color( 160,160,160) );
              for (int plIndex=0; plIndex < target.mainRing.planets.length + 1; plIndex++)
              {
                JLabel lbl = new JLabel();
                JLabel item = new JLabel();
                labels[plIndex] = item;
                lbl.setFont( smallerFont );
                item.setFont( smallerFont );
                if (plIndex >= target.mainRing.planets.length)
                  lbl.setText( "Ascendant" );
                else
                  lbl.setText( target.mainRing.planets[plIndex].name );
                JPanel bar = new JPanel();
                bar.setLayout( new GridBagLayout() );
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.weightx = 0;
                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.insets = new Insets( 0, 0, 0, 6 );
                bar.add( lbl, gbc );
                gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.weightx = 1;
                gbc.anchor = GridBagConstraints.NORTHEAST;
                item.setHorizontalAlignment( JLabel.CENTER );
                item.setPreferredSize( new Dimension( 70, lbl.getPreferredSize().height ) );
                bar.add( item, gbc );
                dataPanel.add( bar );
              }
              target.addChartListener( cl );
              cl.timeChanged( target.getJD() );
              applet.add( dataPanel, BorderLayout.EAST );
            }
            applet.invalidate();
            applet.validate();
            applet.repaint();
          }
        });
        final JCheckBox btnSearch = new JCheckBox( "search" );
        btnSearch.addActionListener( new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            if (searchPanel != null)
            {
              ChartOptions.this.remove( searchPanel );
              searchPanel = null;
              btnSearch.setSelected( false );
            }
            else
            {
              btnSearch.setSelected( true );
              searchPanel = new SearchPanel( ChartOptions.this.timeAdjuster, target.mainRing, smallerFont );
              GridBagConstraints c = new GridBagConstraints();
              c.gridx = 0;
              c.gridy = 3;
              c.gridwidth = 6;
              c.anchor = GridBagConstraints.WEST;
              c.fill = GridBagConstraints.HORIZONTAL;
              ChartOptions.this.add( searchPanel, c );
            }
            ChartOptions.this.validate();
            applet.validate();
            applet.repaint();
          }
        });
        // motion chart
        final JCheckBox btnShowGraph = new JCheckBox( "graph", false );
        btnShowGraph.addActionListener( new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            target.setGraphEnabled( ((JCheckBox)e.getSource()).isSelected() );
            if (graphPanel != null)
            {
              ChartOptions.this.remove( graphPanel );
              graphPanel = null;
            }
            else
            {
              graphPanel = new GraphPanel( target.getGraph(), smallerFont );
              GridBagConstraints c = new GridBagConstraints();
              c.gridx = 0;
              c.gridy = 2;
              c.gridwidth = 6;
              c.anchor = GridBagConstraints.WEST;
              c.fill = GridBagConstraints.HORIZONTAL;
              ChartOptions.this.add( graphPanel, c );
            }
            ChartOptions.this.validate();
            applet.validate();
            applet.repaint();
          }
        } );
        
        // square chart
        final JCheckBox square = new JCheckBox( "square", wheel.isSquare() );
        square.setFont( smallerFont );
        square.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                target.setSquare( ((JCheckBox)e.getSource()).isSelected() );
            }
        } );
        // heliocentric vs geocentric
        JCheckBox helio = new JCheckBox( "heliocentric", wheel.isHeliocentric() );
        helio.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean sel = ((JCheckBox)e.getSource()).isSelected();
                target.setHeliocentric( sel );
                if (sel)
                {
                    showAC.setSelected( false );
                    target.setEnableChartRotation( false );
                }
                showAC.setEnabled( ! sel );
            }
        } );
        GridBagConstraints c;
        // status bar
        statusBar = new JLabel("...");
        statusBar.setMinimumSize( statusBar.getPreferredSize() );
        c = g( 0, 1 );
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        add( statusBar, c );
        c = g( 2, 1 );
        c.anchor = GridBagConstraints.WEST;
        final JCheckBox btnOuter = new JCheckBox( "outer ring" );
        btnOuter.setToolTipText( "Toggle outer ring (copies time from inner ring)" );
        add( btnOuter, c );
        btnOuter.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e)
          {
            Double tO = target.getOuterRingTime();
            if (tO == null)
              target.setOuterRing( target.getJD() );
            else
              target.setOuterRing( null );
            boolean onoff = (target.getOuterRingTime() != null);
            if (btnOuter.isSelected() != onoff)
              btnOuter.setSelected( onoff );
          }
        } );
        int gx1 = 2;
        // more...
        c = g( 0, 0 );
        c.anchor = GridBagConstraints.WEST;
        Box panelSwitches = new Box( BoxLayout.X_AXIS );
        panelSwitches.add( btnMoreOpts );
        panelSwitches.add( btnDataPanel );
        panelSwitches.add( btnSearch );
        panelSwitches.add( btnShowGraph );
        btnDataPanel.setFont( smallerFont );
        btnMoreOpts.setFont( smallerFont );
        btnSearch.setFont( smallerFont );
        btnMoreOpts.setForeground( new Color(0,128,0) );
        btnDataPanel.setForeground( new Color(0,128,0) );
        btnSearch.setForeground( new Color(0,128,0) );
        btnShowGraph.setForeground( new Color(0,128,0) );
        btnShowGraph.setFont( smallerFont );
        add( panelSwitches, c );
        // range
        c = g( 1, 0 );
        add( accAndLbl, c );
        // heliocentric
        c = g( gx1++, 0 );
        c.anchor = GridBagConstraints.WEST;
        add( helio, c );
        // square chart
        c = g( gx1++, 0 );
        c.anchor = GridBagConstraints.WEST;
        add( square, c );
        // ascendant
        c = g( gx1-1, 1 );
        c.anchor = GridBagConstraints.WEST;
        add( showAC, c );
        // zodiac
        c = g( gx1++, 0 );
        c.anchor = GridBagConstraints.WEST;
        add( zodiac, c );
        // copyright
        c = g( 4, 1 );
        c.anchor = GridBagConstraints.EAST;
        JLabel cprt = new JLabel( "www.helianthusinc.com" );
        cprt.setForeground( Color.BLUE );
        /*
        JTextPane cprt = new JTextPane();
        cprt.setContentType( "text/html" );
        cprt.setText( "<font face='arial'><u><small>(c) 1998-2009 Mark Lipson<br>www.marklipson.com</small></u></font>" );
        cprt.setForeground( Color.BLUE );
        cprt.setEditable( false );
        */
        cprt.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
        cprt.addMouseListener( new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    applet.getAppletContext().showDocument( new URL("http://helianthusinc.com/") );
                }
                catch( Exception x )
                {
                }
            }
        } );
        add( cprt, c );
        // set font
        setFont( smallerFont );
        for (int nC=0; nC < getComponentCount(); nC++)
          getComponent( nC ).setFont( getFont() );
        
        target.addChartListener( new ChartWheel.ChartListener()
        {
            public void mouseOverPlanet(ChartPlanet planet)
            {
                if (planet == null)
                    statusBar.setText( "" );
                else
                {
                    double pos = target.isHeliocentric() ? planet.getHelioLongitude() : planet.getGeoLongitude();
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
    }
}
