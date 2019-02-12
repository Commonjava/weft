package org.commonjava.cdi.util.weft;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DrainingExecutorCompletionService<T>
{
    private final ExecutorCompletionService<T> service;

    private final AtomicInteger count = new AtomicInteger( 0 );

    public DrainingExecutorCompletionService( ExecutorService service )
    {
        this.service = new ExecutorCompletionService<>( service );
    }

    public void drain( Consumer<T> consumer )
            throws InterruptedException, ExecutionException
    {
        while ( count.get() > 0 )
        {
            T item = take().get();
            consumer.accept( item );
        }
    }

    public int getCurrentCount()
    {
        return count.get();
    }

    public Future<T> submit( final Callable<T> task )
    {
        Future<T> item = service.submit( task );
        count.incrementAndGet();
        return item;
    }

    public Future<T> submit( final Runnable task, final T result )
    {
        Future<T> item = service.submit( task, result );
        count.incrementAndGet();

        return item;
    }

    public Future<T> take()
            throws InterruptedException
    {
        Future<T> item = service.take();
        count.decrementAndGet();
        return item;
    }

    public Future<T> poll()
    {
        Future<T> item = service.poll();
        if ( item != null )
        {
            count.decrementAndGet();
        }

        return item;
    }

    public Future<T> poll( final long timeout, final TimeUnit unit )
            throws InterruptedException
    {
        Future<T> item = service.poll( timeout, unit );
        if ( item != null )
        {
            count.decrementAndGet();
        }

        return item;
    }
}
