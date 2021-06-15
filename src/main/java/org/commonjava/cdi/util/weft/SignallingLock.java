/**
 * Copyright (C) 2013-2021 Red Hat, Inc. (jdcasey@commonjava.org)
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

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SignallingLock
{
    private ReentrantLock lock = new ReentrantLock();

    private final Condition changed = lock.newCondition();

    private String locker;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public boolean lock()
            throws InterruptedException
    {
        logger.trace( "Locking: {} for: {}", this, Thread.currentThread().getName() );

        lock.lockInterruptibly();
        locker = Thread.currentThread().getName();

        logger.trace( "Lock established." );
        return true;
    }

    public boolean isStale()
    {
        synchronized ( lock )
        {
            return !lock.isLocked() && !lock.hasQueuedThreads();
        }
    }

    public void unlock()
    {
        synchronized ( lock )
        {
            if ( lock.isHeldByCurrentThread() )
            {
                logger.trace( "Locking: {}", this );

                changed.signal();
                lock.unlock();
                locker = null;

                logger.trace( "Locked released" );
            }
        }
    }

    public void await( long timeoutMs )
            throws InterruptedException
    {
        if ( lock.isLocked() )
        {
            logger.trace( "Waiting for unlock of: {}", this );
            changed.await( timeoutMs, TimeUnit.MILLISECONDS );
        }
    }

    public void signal()
    {
        if ( lock.isHeldByCurrentThread() )
        {
            logger.trace( "Signal from: {} in lock of: {}", Thread.currentThread().getName(), this );
            changed.signal();
        }
    }

    public void await()
            throws InterruptedException
    {
        if ( lock.isLocked() )
        {
            logger.trace( "Waiting for unlock of: {}", this );
            changed.await();
        }
    }

    public boolean await( final long l, final TimeUnit timeUnit )
            throws InterruptedException
    {
        if ( lock.isLocked() )
        {
            logger.trace( "Waiting for unlock of: {}", this );

            return changed.await( l, timeUnit );
        }

        return true;
    }

    public void signalAll()
    {
        if ( lock.isHeldByCurrentThread() )
        {
            logger.trace( "Signal from: {} in lock of: {}", Thread.currentThread().getName(), this );
            changed.signalAll();
        }
    }

    public void lockInterruptibly()
            throws InterruptedException
    {
        logger.trace( "Locking: {} for: {}", this, Thread.currentThread().getName() );

        lock.lockInterruptibly();
        locker = Thread.currentThread().getName();

        logger.trace( "Lock established." );
    }

    public boolean tryLock()
    {
        boolean locked = lock.tryLock();
        if ( locked )
        {
            locker = Thread.currentThread().getName();
        }

        return locked;
    }

    public boolean tryLock( final long timeout, final TimeUnit unit )
            throws InterruptedException
    {
        boolean locked = lock.tryLock( timeout, unit );
        if ( locked )
        {
            locker = Thread.currentThread().getName();
        }

        return locked;
    }

    public int getHoldCount()
    {
        return lock.getHoldCount();
    }

    public boolean isHeldByCurrentThread()
    {
        return lock.isHeldByCurrentThread();
    }

    public boolean isLocked()
    {
        return lock.isLocked();
    }

    public boolean isFair()
    {
        return lock.isFair();
    }

    public boolean hasQueuedThreads()
    {
        return lock.hasQueuedThreads();
    }

    public boolean hasQueuedThread( final Thread thread )
    {
        return lock.hasQueuedThread( thread );
    }

    public int getQueueLength()
    {
        return lock.getQueueLength();
    }

    public boolean hasWaiters( final Condition condition )
    {
        return lock.hasWaiters( condition );
    }

    public int getWaitQueueLength( final Condition condition )
    {
        return lock.getWaitQueueLength( condition );
    }

    public String getLocker()
    {
        return locker;
    }
}
