package org.commonjava.cdi.util.weft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Locker<K>
{
    private Map<K, ReentrantLock> locks;

    public Locker()
    {
        this.locks = Collections.synchronizedMap( new ContextSensitiveWeakHashMap<>() );
    }

    public Locker( Map<K, ReentrantLock> locks )
    {
        this.locks = Collections.synchronizedMap( locks );
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
                locked = lock.tryLock( timeoutSeconds, TimeUnit.SECONDS );
                if ( locked )
                {
                    return function.apply( key );
                }
                else
                {
                    retry = lockFailedFunction.apply( key, lock );
                }
            }
            catch ( InterruptedException e )
            {
                logger.warn( "Interrupted waiting for lock on key: {}", key );
            }
            finally
            {
                if ( locked )
                {
                    lock.unlock();
                }
            }
        }
        while ( retry == Boolean.TRUE );

        return null;
    }

}
