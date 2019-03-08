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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Variant of {{@link Locker}} that wraps both a {@link ReentrantLock} AND a {@link java.util.concurrent.locks.Condition}
 * via {@link SignallingLock} to allow multiple threads to coordinate work via a single lock. The point of this class
 * is to manage the lock for a given operation, given some locking key. The lock itself (and the key) is passed into
 * the operation, so it can call things like {@link SignallingLock#await()}.
 *
 * This locker also uses a {@link TimerTask} to sweep for unused locks and clear them.

 * @param <K> The key used to map the locks.
 */
public class SignallingLocker<K>
{
    private static final long DEFAULT_SWEEP_MS = 10 * 1000;

    private final Timer timer = new Timer();

    private final Map<K, SignallingLock> locks;

    public SignallingLocker()
    {
        this( new ContextSensitiveWeakHashMap(), DEFAULT_SWEEP_MS );
    }

    public SignallingLocker( long staleSweepMillis )
    {
        this( new ContextSensitiveWeakHashMap(), staleSweepMillis );
    }

    public SignallingLocker( Map<K, SignallingLock> locks, long staleSweepMillis )
    {
        this.locks = Collections.synchronizedMap( locks );

        this.timer.scheduleAtFixedRate( new SweepStaleTask(), staleSweepMillis, staleSweepMillis );
    }

    private final class SweepStaleTask
            extends TimerTask
    {
        @Override
        public void run()
        {
            new HashMap<>( locks ).forEach( ( key, lock )-> {
                if ( lock.isStale() )
                {
                    locks.remove( key );
                }
            } );
        }
    }

    /**
     * Remove lock proactively
     */
    public void removeLock( K key )
    {
        locks.computeIfPresent( key, ( k, lock ) -> locks.remove( k ) );
    }

    public <T> T ifUnlocked( K key, Function<K, T> function, BiFunction<K, SignallingLock, T> lockedFunction )
    {
        final SignallingLock lock = locks.computeIfAbsent( key, k -> new SignallingLock() );
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

    public <T> T lockAnd( K key, BiFunction<K, SignallingLock, T> function )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        final SignallingLock lock = locks.computeIfAbsent( key, k -> new SignallingLock() );
        Boolean locked = false;
        try
        {
            locked = lock.lock();

            if ( locked )
            {
                logger.debug( "Applying function locked with: {}", key );
                return function.apply( key, lock );
            }
            else
            {
                logger.debug( "Lock failed for key: {}", key );
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

        logger.debug( "No retries, return null for locked operation on key: {}", key );
        return null;
    }

    public <T> T lockAnd( K key, long timeoutSeconds, BiFunction<K, SignallingLock, T> function )
    {
        return lockAnd( key, timeoutSeconds, function, ( k, lock ) -> false );
    }

    public <T> T lockAnd( K key, long timeoutSeconds, BiFunction<K, SignallingLock, T> function,
                             BiFunction<K, SignallingLock, Boolean> lockFailedFunction )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        final SignallingLock lock = locks.computeIfAbsent( key, k -> new SignallingLock() );
        Boolean retry = false;
        do
        {
            Boolean locked = false;
            try
            {
                logger.debug( "Locking on: {} with timeout seconds: {}", key, timeoutSeconds );
                if ( timeoutSeconds > 0 )
                {
                    locked = lock.tryLock( timeoutSeconds, TimeUnit.SECONDS );
                }
                else
                {
                    locked = lock.lock();
                }

                if ( locked )
                {
                    logger.debug( "Applying function locked with: {}", key );
                    return function.apply( key, lock );
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

}
