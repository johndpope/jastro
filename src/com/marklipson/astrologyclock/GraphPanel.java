package com.marklipson.astrologyclock;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Controls for searching for planetary events.
 */
public class GraphPanel extends JPanel
{
  private static final long serialVersionUID = 1L;

  private MotionChart grapher;
  private JSpinner timeScale;
  private JSpinner harmonic;
  
  public GraphPanel( MotionChart graph, Font font )
  {
    this.grapher = graph;
    // colors
    setBorder( BorderFactory.createLineBorder( Color.LIGHT_GRAY ) );
    setBackground( new Color(224,224,224) );
    // create controls
    JLabel label = new JLabel( "Graph Options: " );
    label.setFont( font );
    JLabel lblTimeScale = new JLabel( " time scale:" );
    lblTimeScale.setFont( font );
    JLabel lblHarmonic = new JLabel( " harmonic:" );
    lblHarmonic.setFont( font );
    
    timeScale = new JSpinner( new SpinnerListModel( new Float[] { 0.001f, 0.003f, 0.01f, 0.03f, 0.1f, 0.3f, 1f, 3f, 10f } ) );
    timeScale.setValue( 0.1f );
    timeScale.setPreferredSize( new Dimension( 70, (int)timeScale.getPreferredSize().getHeight() ) );
    timeScale.addChangeListener( new ChangeListener()
    {
      public void stateChanged(ChangeEvent evt)
      {
        float v = (Float)timeScale.getValue();
        grapher.setTimeScale( v );
      }
    });

    harmonic = new JSpinner( new SpinnerListModel( new Integer[] { 1, 4, 12 } ) );
    harmonic.setValue( 12 );
    harmonic.setPreferredSize( new Dimension( 70, (int)harmonic.getPreferredSize().getHeight() ) );
    harmonic.addChangeListener( new ChangeListener()
    {
      public void stateChanged(ChangeEvent evt)
      {
        int h = (Integer)harmonic.getValue();
        grapher.setHarmonic( h );
      }
    });

    setLayout( new GridBagLayout() );
    GridBagConstraints c = new GridBagConstraints();
    int x = 0;
    c.gridx = x ++;
    add( label, c );

    c = new GridBagConstraints();
    c.gridx = x ++;
    add( lblTimeScale, c );
    c = new GridBagConstraints();
    c.gridx = x ++;
    add( timeScale, c );
    
    c = new GridBagConstraints();
    c.gridx = x ++;
    add( lblHarmonic, c );
    c = new GridBagConstraints();
    c.gridx = x ++;
    add( harmonic, c );
    
    c = new GridBagConstraints();
    c.gridx = x;
    c.weightx = 1;
    c.anchor = GridBagConstraints.WEST;
    JTextField message = new JTextField( "NOTE: time scale is in days per pixel", 30 );
    message.setFont( font );
    message.setForeground( Color.GRAY );
    message.setEditable( false );
    message.setBackground( getBackground() );
    add( message, c );
  }
}
