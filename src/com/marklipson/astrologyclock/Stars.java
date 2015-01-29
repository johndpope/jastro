package com.marklipson.astrologyclock;

import java.util.ArrayList;

/**
 * Access to star information.
 */
public class Stars
{
    // there is a single instance
    static private Stars instance;
    static public synchronized Stars getInstance()
    {
        if (instance == null)
            instance = new Stars();
        return instance;
    }
    
    // stars
    private Star[] stars;

    /**
     * Individual star information.
     */
    public static class Star
    {
        private String name;
        private float distance;
        private float magnitude;
        private float longitude;
        private float dLongitude;
        Star( String name, float distance, float magnitude, float longitude, float dLongitude )
        {
            this.name = name;
            this.distance = distance;
            this.magnitude = magnitude;
            this.longitude = longitude;
            this.dLongitude = dLongitude;
        }
        public String getName()
        {
            return name;
        }
        public float getDistance()
        {
            return distance;
        }
        public float getMagnitude()
        {
            return magnitude;
        }
        public double getLongitude( double t )
        {
            t -= 2447527;
            return longitude + t * dLongitude;
        }
    }
    
    private Stars()
    {
        ArrayList<Star> all = new ArrayList<Star>();
        float d2r = (float)(Math.PI / 180);
        float mpd = (float)(d2r / (60 * 3652.5)); 
        // magnitude 0
        all.add( new Star("Rigel",         772,0.92f,( 60+16+(32/60f))*d2r,8.3f*mpd) );
        all.add( new Star("Capella",     42.2f,0.21f,( 60+21+(39/60f))*d2r,8.3f*mpd) );
        all.add( new Star("Betelguese",    427,0.70f,( 60+28+(33/60f))*d2r,8.4f*mpd) );
        all.add( new Star("Sirius",       8.6f,0.06f,( 90+13+(54/60f))*d2r,8.2f*mpd) );
        all.add( new Star("Canopus",       312,0.86f,( 90+14+(51/60f))*d2r,8.0f*mpd) );
        all.add( new Star("Procyon",    11.41f,0.48f,( 90+25+(35/60f))*d2r,8.1f*mpd) );
        all.add( new Star("Spica",     262.18f,0.90f,(180+23+(36/60f))*d2r,7.1f*mpd) );
        all.add( new Star("Arcturus",   36.71f,0.24f,(180+23+(56/60f))*d2r,6.8f*mpd) );
        all.add( new Star("Agena/Hadar",   525,0.86f,(210+23+(34/60f))*d2r,6.4f*mpd) );
        all.add( new Star("Rigel Centaurus",4.39f,0.06f,(210+29+(17/60f))*d2r,6.1f*mpd) );
        all.add( new Star("Vega",       25.30f,0.14f,(270+15+( 9/60f))*d2r,8.5f*mpd) );
        all.add( new Star("Altair",     16.77f,0.60f,(300+ 1+(34/60f))*d2r,8.0f*mpd) );
        all.add( new Star("Achernar",      143,0.60f,(330+15+( 0/60f))*d2r,4.1f*mpd) );
        // magitude 1
        all.add( new Star("Aldebaran",  65.11f,1.06f,( 60+ 9+(36/60f))*d2r,8.2f*mpd) );
        all.add( new Star("Bellatrix",     240,1.60f,( 60+20+(45/60f))*d2r,8.3f*mpd) );
        all.add( new Star("El Nath",       130,1.78f,( 60+22+(30/60f))*d2r,8.3f*mpd) );
        all.add( new Star("Al Nilam",     1300,1.75f,( 60+23+(14/60f))*d2r,8.3f*mpd) );
        all.add( new Star("Al Hecka",      417,1.60f,( 60+24+(35/60f))*d2r,8.3f*mpd) );
        all.add( new Star("Menkalinan", 82.11f,1.80f,( 60+29+(44/60f))*d2r,8.4f*mpd) );
        all.add( new Star("Al Hena",       100,1.80f,( 90+ 8+(57/60f))*d2r,8.4f*mpd) );
        all.add( new Star("Castor",     51.55f,1.58f,( 90+20+( 4/60f))*d2r,8.2f*mpd) );
        all.add( new Star("Pollux",     33.71f,1.16f,( 90+23+( 2/60f))*d2r,8.0f*mpd) );
        all.add( new Star("Dubhe",         120,1.95f,(120+14+(52/60f))*d2r,5.5f*mpd) );
        all.add( new Star("Alphard",       180,1.98f,(120+27+( 0/60f))*d2r,7.4f*mpd) );
        all.add( new Star("Regulus",    77.49f,1.34f,(120+29+(38/60f))*d2r,7.4f*mpd) );
        all.add( new Star("Alioth",     80.93f,1.68f,(150+ 8+(39/60f))*d2r,4.7f*mpd) );
        all.add( new Star("Al Kaid",       100,1.91f,(150+26+(38/60f))*d2r,5.2f*mpd) );
      //all.add( new Star("Foramen",         0,1.99f,(180+22+( 3/60f))*d2r,4.1f*mpd) );  (variable - should it count as a mag=1?)
        all.add( new Star("Acrux",         320,1.05f,(210+11+(40/60f))*d2r,4.7f*mpd) );
        all.add( new Star("Antares",   603.99f,1.22f,(240+ 9+(40/60f))*d2r,8.1f*mpd) );
        all.add( new Star("Fomalhaut",  25.07f,1.29f,(330+ 3+(30/60f))*d2r,7.0f*mpd) );
        all.add( new Star("Deneb",        3200,1.26f,(330+ 4+(49/60f))*d2r,6.1f*mpd) );
        stars = (Star[])all.toArray( new Star[ all.size() ] );
    }

    public Star[] getStars()
    {
        return stars;
    }
}
