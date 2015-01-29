package com.marklipson.astrologyclock.unused;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * save/restore multiple values
 */
public class Memorizer extends JPanel
{
    private Color mColor0 = new Color(0xe0,0xb0,0x80);
    private Color mColor1 = new Color(0xc0,0x40,0x40);
    private Color bFilledColor = new Color(0xc0,0xe0,0xc0);
    private JButton memorize = new JButton( "M" );
    private JButton memories[];
    private Object values[];
    private Target target;
    
    public static interface Target
    {
        public void setValue( Object v );
        public Object getValue();
    }
    public Memorizer( int nMemories, Target target )
    {
        this.target = target;
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        add( memorize );
        memorize.setBackground( mColor0 );
        memorize.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                // toggle
                memorize.setSelected( memorize.isSelected() );
                memorize.setBackground( memorize.isSelected() ? mColor1 : mColor0 );
            }
        } );
        @SuppressWarnings( "unused" )
        ActionListener memListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JButton btn = (JButton)e.getSource();
                int btnNo = Integer.parseInt( btn.getText() ) - 1;
                if (memorize.isSelected())
                {
                    // memorize a setting
                    save( btnNo );
                    // indicate memory has been filled
                    if (values[btnNo] != null)
                        btn.setBackground( bFilledColor );
                }
                else
                {
                    // restore a setting from memory
                    restore( btnNo );
                }
                memorize.setSelected( false );
                memorize.setBackground( mColor0 );
            }
        };
        // create buttons for each memory, and places to store their values
        memories = new JButton[ nMemories ];
        values = new Object[ nMemories ];
        for (int n=0; n < memories.length; n++)
        {
            memories[n] = new JButton( String.valueOf( n + 1 ) );
            add( memories[n] );
        }
    }

    /**
     * Save value in indicated memory.
     */
    public void save( int memory )
    {
        if (target != null)
            values[ memory ] = target.getValue();
    }
    /**
     * Restore value from indicated memory.
     */
    public void restore( int memory )
    {
        if (target != null  &&  values[memory] != null)
            target.setValue( values[ memory ] );
    }
}
