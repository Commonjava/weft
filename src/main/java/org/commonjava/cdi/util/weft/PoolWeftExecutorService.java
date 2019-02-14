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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.commonjava.cdi.util.weft.exception.PoolOverloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Created by jdcasey on 1/3/17.
 */
public class PoolWeftExecutorService
        implements WeftExecutorService, ScheduledExecutorService
{
    private static final String TIMER = "timer";
    private static final String METER = "meter";

    private String name;

    private ThreadPoolExecutor delegate;

    private Integer threadCount;

    private Float maxLoadFactor;

    private boolean loadSensitive;

    private MetricRegistry metricRegistry;

    private String metricPrefix;

    private final AtomicInteger load = new AtomicInteger( 0 );

    public PoolWeftExecutorService( final String name, ThreadPoolExecutor delegate, final Integer threadCount, final Float maxLoadFactor,
                                    boolean loadSensitive, final MetricRegistry metricRegistry, final String metricPrefix )
    {
        this.name = name;
        this.delegate = delegate;
        this.threadCount = threadCount;
        this.maxLoadFactor = maxLoadFactor;
        this.loadSensitive = loadSensitive;
        this.metricRegistry = metricRegistry;
        this.metricPrefix = metricPrefix;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isHealthy()
    {
        return !loadSensitive || getLoadFactor() < maxLoadFactor;
    }

    @Override
    public double getLoadFactor()
    {
        return getCurrentLoad() / getThreadCount();
    }

    @Override
    public int getCurrentLoad()
    {
        return load.get();
    }

    @Override
    public Integer getThreadCount()
    {
        return threadCount;
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

    private void verifyLoad()
    {
        if ( loadSensitive && !isHealthy() )
        {
            throw new PoolOverloadException( getName(), getLoadFactor(), getCurrentLoad(), maxLoadFactor, getThreadCount() );
        }
    }
    
    @Override
    public <T> Future<T> submit( Callable<T> callable )
    {
        verifyLoad();

        return delegate.submit( wrapCallable( callable ) );
    }

    @Override
    public <T> Future<T> submit( Runnable runnable, T t )
    {
        verifyLoad();

        return delegate.submit( wrapRunnable( runnable ), t );
    }

    @Override
    public Future<?> submit( Runnable runnable )
    {
        verifyLoad();

        return delegate.submit( wrapRunnable( runnable ) );
    }

    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> collection )
            throws InterruptedException
    {
        verifyLoad();

        return delegate.invokeAll( wrapAll(collection) );
    }

    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit )
            throws InterruptedException
    {
        verifyLoad();

        return delegate.invokeAll( wrapAll( collection ), l, timeUnit );
    }

    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> collection )
            throws InterruptedException, ExecutionException
    {
        verifyLoad();

        return delegate.invokeAny( wrapAll( collection ) );
    }

    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit )
            throws InterruptedException, ExecutionException, TimeoutException
    {
        verifyLoad();

        return delegate.invokeAny( wrapAll( collection ), l, timeUnit );
    }

    @Override
    public void execute( Runnable runnable )
    {
        verifyLoad();

        delegate.execute( wrapRunnable( runnable ) );
    }

    @Override
    public ScheduledFuture<?> schedule( Runnable runnable, long l, TimeUnit timeUnit )
    {
        verifyLoad();

        return asScheduled( ( d ) -> d.schedule( wrapRunnable( runnable ), l, timeUnit ) );
    }

    @Override
    public <V> ScheduledFuture<V> schedule( Callable<V> callable, long l, TimeUnit timeUnit )
    {
        verifyLoad();

        return asScheduled( (d) -> d.schedule( wrapCallable( callable ), l, timeUnit ) );
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate( Runnable runnable, long l, long l1, TimeUnit timeUnit )
    {
        verifyLoad();

        return asScheduled( ( d ) -> d.scheduleAtFixedRate( wrapRunnable( runnable ), l, l1, timeUnit ) );
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay( Runnable runnable, long l, long l1, TimeUnit timeUnit )
    {
        verifyLoad();

        return asScheduled( ( d ) -> d.scheduleWithFixedDelay( wrapRunnable( runnable ), l, l1, timeUnit ) );
    }

    @Override
    public int getCorePoolSize()
    {
        return delegate.getCorePoolSize();
    }

    @Override
    public int getMaximumPoolSize()
    {
        return delegate.getMaximumPoolSize();
    }

    @Override
    public int getActiveCount()
    {
        return delegate.getActiveCount();
    }

    @Override
    public long getTaskCount()
    {
        return delegate.getTaskCount();
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

    private <T> Callable<T> timeCallable( Callable<T> callable )
    {
        return (Callable<T>) ()->{
            if( metricRegistry != null )
            {
                metricRegistry.meter( name( metricPrefix, "call", METER ) ).mark();
                Timer.Context context = metricRegistry.timer( name( metricPrefix, "call", TIMER ) ).time();
                try
                {
                    return callable.call();
                }
                finally
                {
                    context.stop();
                }
            }
            else
            {
                return callable.call();
            }
        };
    }

    private Runnable timeRunnable( Runnable runnable )
    {
        return ()->{
            if( metricRegistry != null )
            {
                metricRegistry.meter( name( metricPrefix, "run", METER ) ).mark();
                Timer.Context context = metricRegistry.timer( name( metricPrefix, "run", TIMER ) ).time();
                try
                {
                    runnable.run();
                }
                finally
                {
                    context.stop();
                }
            }
            else
            {
                runnable.run();
            }
        };
    }

    private <T> Collection<Callable<T>> wrapAll( Collection<? extends Callable<T>> collection )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        return collection.parallelStream().map( ( callable ) -> {
            ThreadContext old = ThreadContext.setContext( ctx );
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug( "Using ThreadContext: {} (saving: {}) in {}", ctx, old, Thread.currentThread().getName() );
            return timeCallable((Callable<T>) () -> {
                load.incrementAndGet();
                try
                {
                    return callable.call();
                }
                finally
                {
                    logger.debug( "Restoring ThreadContext: {} in: {}", old, Thread.currentThread().getName() );
                    ThreadContext.setContext( old );
                    load.decrementAndGet();
                }
            });
        } ).collect( Collectors.toList() );
    }

    private Runnable wrapRunnable( Runnable runnable )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        return timeRunnable(()->{
            load.incrementAndGet();
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
                load.decrementAndGet();
            }
        });
    }

    private <T> Callable<T> wrapCallable( Callable<T> callable )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        return timeCallable((Callable<T>) ()->{
            load.incrementAndGet();
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
                load.decrementAndGet();
            }
        });
    }

}
