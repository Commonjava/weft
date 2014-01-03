/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.cdi.util.weft;

import org.commonjava.util.logging.Logger;

public abstract class StoppableRunnable
    implements Runnable
{
    protected final Logger logger = new Logger( getClass() );

    private boolean stop = false;

    private Thread myThread;

    public final synchronized void stop()
    {
        logger.debug( "setting stop flag on %s", this );
        stop = true;
        if ( myThread != null )
        {
            logger.debug( "interrupting current thread: %s for task: %s", myThread, this );
            myThread.interrupt();
        }
    }

    @Override
    public final void run()
    {
        if ( stop )
        {
            logger.debug( "stopping task: %s", this );
            return;
        }

        synchronized ( this )
        {
            myThread = Thread.currentThread();
        }

        if ( myThread.isInterrupted() )
        {
            return;
        }

        doExecute();

        if ( myThread.isInterrupted() )
        {
            return;
        }

        synchronized ( this )
        {
            myThread = null;
        }
    }

    protected abstract void doExecute();

}
