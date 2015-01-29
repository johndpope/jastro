package com.marklipson.astrologyclock;

/**
 * Run an action after a certain amount of time.  New requests clear old requests,
 * deferring any action until there is idle time.  This is typically used in a user
 * interface when a field changes or a key is pressed - validation or some other update
 * is delayed until no key has been pressed for some amount of time, typically about
 * half a second.
 */
public class DelayedAction
{
    private Thread waitThread;
    private static final int WAIT_PARTS = 10;
    private int waitTimeMS;
    private int countdown;
    private Runnable action;

    /**
     * Just specify the delay in the constructor.
     */
    public DelayedAction( int waitTimeMS )
    {
        this.waitTimeMS = waitTimeMS;
    }
    /**
     * Trigger request for an action and clear any pending action.
     */
    public synchronized void trigger( Runnable theAction )
    {
        // update the action to perform
        action = theAction;
        // reset the countdown
        countdown = WAIT_PARTS;
        // create a new thread if needed
        if (waitThread == null)
        {
          // start a new thread
          waitThread = new Thread("delayedActionThread")
          {
              public void run()
              {
                  try
                  {
                      while (countdown > 0)
                      {
                        countdown --;
                        Thread.sleep( waitTimeMS / WAIT_PARTS );
                      }
                      // counter reached zero - run the action
                      action.run();
                  }
                  catch( InterruptedException x )
                  {
                  }
                  // indicate thread has exited
                  waitThread = null;
              }
          };
          waitThread.start();
        }
    }
}
