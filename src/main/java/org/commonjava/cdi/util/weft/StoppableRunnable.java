/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
