package com.marklipson.astrologyclock;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.marklipson.astrologyclock.Search.Event;
import com.marklipson.astrologyclock.Search.SearchParseException;
import com.marklipson.astrologyclock.Search.SearchResult;
import com.marklipson.astrologyclock.Search.SearchTimeout;

/**
 * Controls for searching for planetary events.
 */
public class SearchPanel extends JPanel
{
  private static final long serialVersionUID = 1L;

  private TimeAdjuster timeAdjuster;
  private ChartWheel.PlanetRing ring;
  private Search searcher;
  
  private JTextField searchField;
  private JTextField message;
  
  private ActionListener lastSearch;

  public SearchPanel( TimeAdjuster timeAdjuster, ChartWheel.PlanetRing forRing, Font font )
  {
    this.timeAdjuster = timeAdjuster;
    ring = forRing;
    searcher = new Search( ring );
    // colors
    setBorder( BorderFactory.createLineBorder( Color.LIGHT_GRAY ) );
    setBackground( new Color(224,224,224) );
    // create controls
    JLabel label = new JLabel( "Search:" );
    label.setFont( font );
    searchField = new JTextField( 13 );
    searchField.setMinimumSize( new Dimension( 80, searchField.getMinimumSize().height ) );
    searchField.setFont( font );
    final JButton next = new JButton( "next" );
    final JButton prev = new JButton( "previous" );
    next.setFont( font );
    prev.setFont( font );
    message = new JTextField( "examples: jupiter in cancer, mars square uranus, mercury rx", 30 );
    message.setFont( font );
    message.setEditable( false );
    message.setBackground( getBackground() );
    setLayout( new GridBagLayout() );
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    add( label, c );
    c = new GridBagConstraints();
    c.gridx = 1;
    c.weightx = 0.2;
    c.fill = GridBagConstraints.HORIZONTAL;
    add( searchField, c );
    c = new GridBagConstraints();
    c.gridx = 2;
    add( next, c );
    c = new GridBagConstraints();
    c.gridx = 3;
    add( prev, c );
    c = new GridBagConstraints();
    c.gridx = 4;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.WEST;
    c.weightx = 1;
    add( message, c );
    // ENTER performs search
    Action goSearch = new AbstractAction()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (lastSearch != null)
          lastSearch.actionPerformed( null );
        else
          next.getActionListeners()[0].actionPerformed( null );
      }
    };
    searchField.getKeymap().addActionForKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ), goSearch );
    // set up search actions
    next.addActionListener( new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        Event event = parseSearchExpression();
        if (event != null)
          search( event, true );
        lastSearch = next.getActionListeners()[0];
      }
    });
    prev.addActionListener( new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        Event event = parseSearchExpression();
        if (event != null)
          search( event, false );
        lastSearch = prev.getActionListeners()[0];
      }
    });
  }
  private Event parseSearchExpression()
  {
    try
    {
      Event event = searcher.parseSearch( searchField.getText() );
      return event;
    }
    catch( SearchParseException x )
    {
      setMessage( x.getMessage() );
      final int offset = x.getInputOffset();
      final int length = x.getTokenLength();
      searchField.requestFocus();
      new Thread() {
        public void run() {
          try { Thread.sleep( 100 ); } catch(InterruptedException x ) { }
          searchField.setCaretPosition( offset );
          searchField.select( offset, offset + length );
        }
      }.start();
      return null;
    }
  }
  private void search( final Event event, final boolean forward )
  {
    setMessage( "Searching..." );
    new Thread() {
      public void run() {
        try
        {
          SearchResult result = searcher.search( ring.t, event, forward, 10000 );
          String msg =
            ring.getChartWheel().formatJD( result.tPeak, false ) + " (" +
            ring.getChartWheel().formatJD( result.tStart, true ) + " to " +
            ring.getChartWheel().formatJD( result.tEnd, true ) + ")";
          setMessage( msg );
          timeAdjuster.setTime( new Date( ChartWheel.JD_to_systemTime( result.tPeak ) ) );
        }
        catch( SearchTimeout x )
        {
          setMessage( "Search timed out or reached ephemeris limits." );
        }
      }
    }.start();
  }
  private void setMessage( String text )
  {
    message.setText( text );
    message.repaint();
  }
}
