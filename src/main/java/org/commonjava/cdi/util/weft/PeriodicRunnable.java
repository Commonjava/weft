package org.commonjava.cdi.util.weft;

import java.util.concurrent.Executor;

public abstract class PeriodicRunnable
    extends StoppableRunnable
{

    private final long period;

    private long lastSkew = 0;

    private boolean wait = false;

    private final Executor executor;

    public PeriodicRunnable( final long period, final boolean initialWait, final Executor executor )
    {
        this.period = period;
        this.wait = initialWait;
        this.executor = executor;
    }

    @Override
    protected final void doExecute()
    {
        boolean abort = false;
        if ( wait )
        {
            try
            {
                logger.debug( "sleeping for wait period of %d ms minus skew of: %d", period, lastSkew );
                Thread.sleep( period - lastSkew );
            }
            catch ( final InterruptedException e )
            {
                abort = true;
            }
        }

        final long start = System.currentTimeMillis();

        if ( abort )
        {
            logger.debug( "aborting due to interruption." );
            return;
        }

        onPeriodExpire();

        wait = true;
        lastSkew = System.currentTimeMillis() - start;
    }

    protected abstract void onPeriodExpire();

    @Override
    protected final void doPostExecute()
    {
        executor.execute( this );
    }

}
