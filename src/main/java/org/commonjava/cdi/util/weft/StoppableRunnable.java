/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.cdi.util.weft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StoppableRunnable
    implements Runnable
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

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
