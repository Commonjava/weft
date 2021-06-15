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

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Locker<K>
{
    private static final long DEFAULT_SWEEP_MS = 10 * 1000;

    private final Timer timer = new Timer();

    private Map<K, ReentrantLock> locks;

    public Locker()
    {
        this( new ContextSensitiveWeakHashMap<>(), DEFAULT_SWEEP_MS );
    }

    public Locker( long sweepStaleLocks )
    {
        this( new ContextSensitiveWeakHashMap<>(), sweepStaleLocks );
    }

    public Locker( Map<K, ReentrantLock> locks, long staleSweepMillis )
    {
        this.locks = Collections.synchronizedMap( locks );
        this.timer.scheduleAtFixedRate( new SweepStaleTask(), staleSweepMillis, staleSweepMillis );
    }

    public <T> T ifUnlocked( K key, Function<K, T> function, BiFunction<K, ReentrantLock, T> lockedFunction )
    {
        final ReentrantLock lock = locks.computeIfAbsent( key, k -> new ReentrantLock() );
        Boolean locked = false;
        try
        {
            locked = lock.tryLock();
            if ( locked )
            {
                return function.apply( key );
            }
            else
            {
                return lockedFunction.apply( key, lock );
            }
        }
        finally
        {
            if ( lock.isHeldByCurrentThread() )
            {
                lock.unlock();
            }
        }
    }

    public boolean waitForLock( long timeoutSeconds, ReentrantLock lock )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        boolean waitingLocked = false;
        try
        {
            //TODO: will let non-working threads wait here for seconds for the result of the working thread processing. Need to evaluate how long should wait here in future.
            waitingLocked = lock.tryLock( timeoutSeconds, TimeUnit.SECONDS );
        }
        catch ( InterruptedException e )
        {
            logger.warn( "Thread interrupted by other threads for waiting processing result: {}", e.getMessage() );
        }

        return waitingLocked;
    }

    public <T> T lockAnd( K key, long timeoutSeconds, Function<K, T> function, BiFunction<K, ReentrantLock, Boolean> lockFailedFunction )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        final ReentrantLock lock = locks.computeIfAbsent( key, k -> new ReentrantLock() );
        Boolean retry = false;
        do
        {
            Boolean locked = false;
            try
            {
                logger.debug( "Locking on: {} with timeout seconds: {}", key, timeoutSeconds );
                locked = lock.tryLock( timeoutSeconds, TimeUnit.SECONDS );
                if ( locked )
                {
                    logger.debug( "Applying function locked with: {}", key );
                    return function.apply( key );
                }
                else
                {
                    logger.debug( "Lock failed for key: {}", key );
                    retry = lockFailedFunction.apply( key, lock );
                    logger.debug( "Retry lock on: {}? {}", key, retry );
                }
            }
            catch ( InterruptedException e )
            {
                logger.warn( "Interrupted waiting for lock on key: {}", key );
            }
            finally
            {
                logger.debug( "Done, checking whether unlock needed for: {}", key );
                if ( locked )
                {
                    logger.debug( "Unlocking key: {}", key );
                    lock.unlock();
                }
            }
        }
        while ( retry == Boolean.TRUE );

        logger.debug( "No retries, return null for locked operation on key: {}", key );
        return null;
    }

    private boolean isStale( ReentrantLock lock )
    {
        synchronized ( lock )
        {
            return !lock.isLocked() && !lock.hasQueuedThreads();
        }
    }

    private final class SweepStaleTask
            extends TimerTask
    {
        @Override
        public void run()
        {
            Map<K, ReentrantLock> toScan;
            try
            {
                toScan = new HashMap<>( locks );
            }
            catch ( ConcurrentModificationException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.warn( "Failed to create toScan. Abort the sweeping task for once", e.getMessage() );
                return;
            }

            toScan.forEach( ( key, lock)->{
                if ( isStale(lock) )
                {
                    locks.remove( key );
                }
            } );
        }
    }
}
