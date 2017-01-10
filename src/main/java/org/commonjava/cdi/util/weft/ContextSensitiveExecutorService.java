package org.commonjava.cdi.util.weft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by jdcasey on 1/3/17.
 */
public class ContextSensitiveExecutorService implements ScheduledExecutorService
{
    private ExecutorService delegate;

    public ContextSensitiveExecutorService( ExecutorService delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public void shutdown()
    {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow()
    {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown()
    {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated()
    {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination( long l, TimeUnit timeUnit )
            throws InterruptedException
    {
        return delegate.awaitTermination( l, timeUnit );
    }

    @Override
    public <T> Future<T> submit( Callable<T> callable )
    {
        return delegate.submit( wrapCallable( callable ) );
    }

    @Override
    public <T> Future<T> submit( Runnable runnable, T t )
    {
        return delegate.submit( wrapRunnable( runnable ), t );
    }

    @Override
    public Future<?> submit( Runnable runnable )
    {
        return delegate.submit( wrapRunnable( runnable ) );
    }

    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> collection )
            throws InterruptedException
    {
        return delegate.invokeAll( wrapAll(collection) );
    }

    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit )
            throws InterruptedException
    {
        return delegate.invokeAll( wrapAll( collection ), l, timeUnit );
    }

    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> collection )
            throws InterruptedException, ExecutionException
    {
        return delegate.invokeAny( wrapAll( collection ) );
    }

    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit )
            throws InterruptedException, ExecutionException, TimeoutException
    {
        return delegate.invokeAny( wrapAll( collection ), l, timeUnit );
    }

    @Override
    public void execute( Runnable runnable )
    {
        delegate.execute( wrapRunnable( runnable ) );
    }

    @Override
    public ScheduledFuture<?> schedule( Runnable runnable, long l, TimeUnit timeUnit )
    {
        return asScheduled( ( d ) -> d.schedule( wrapRunnable( runnable ), l, timeUnit ) );
    }

    @Override
    public <V> ScheduledFuture<V> schedule( Callable<V> callable, long l, TimeUnit timeUnit )
    {
        return asScheduled( (d) -> d.schedule( wrapCallable( callable ), l, timeUnit ) );
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate( Runnable runnable, long l, long l1, TimeUnit timeUnit )
    {
        return asScheduled( ( d ) -> d.scheduleAtFixedRate( wrapRunnable( runnable ), l, l1, timeUnit ) );
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay( Runnable runnable, long l, long l1, TimeUnit timeUnit )
    {
        return asScheduled( ( d ) -> d.scheduleWithFixedDelay( wrapRunnable( runnable ), l, l1, timeUnit ) );
    }

    private <T> ScheduledFuture<T> asScheduled( Function<ScheduledExecutorService, ScheduledFuture<T>> consumer )
    {
        if ( delegate instanceof ScheduledExecutorService )
        {
            return consumer.apply( (ScheduledExecutorService) delegate );
        }
        else
        {
            throw new IllegalStateException(
                    "Cannot run scheduled executions; underlying ExecutorService is not instanceof ScheduledExecutorService. Try using @WeftScheduledExecutor annotation in CDI injection." );
        }
    }

    private <T> Collection<Callable<T>> wrapAll( Collection<? extends Callable<T>> collection )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        return collection.parallelStream().map( ( callable ) -> {
            ThreadContext old = ThreadContext.setContext( ctx );
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug( "Using ThreadContext: {} (saving: {}) in {}", ctx, old, Thread.currentThread().getName() );
            return (Callable<T>) () -> {
                try
                {
                    return callable.call();
                }
                finally
                {
                    logger.debug( "Restoring ThreadContext: {} in: {}", old, Thread.currentThread().getName() );
                    ThreadContext.setContext( old );
                }
            };
        } ).collect( Collectors.toList() );
    }

    private Runnable wrapRunnable( Runnable runnable )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        return ()->{
            ThreadContext old = ThreadContext.setContext( ctx );
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug( "Using ThreadContext: {} (saving: {}) in {}", ctx, old, Thread.currentThread().getName() );
            try
            {
                runnable.run();
            }
            finally
            {
                logger.debug( "Restoring ThreadContext: {} in: {}", old, Thread.currentThread().getName() );
                ThreadContext.setContext( old );
            }
        };
    }

    private <T> Callable<T> wrapCallable( Callable<T> callable )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        return (Callable<T>) ()->{
            ThreadContext old = ThreadContext.setContext( ctx );
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug( "Using ThreadContext: {} (saving: {}) in {}", ctx, old, Thread.currentThread().getName() );
            try
            {
                return callable.call();
            }
            finally
            {
                logger.debug( "Restoring ThreadContext: {} in: {}", old, Thread.currentThread().getName() );
                ThreadContext.setContext( old );
            }
        };
    }
}
