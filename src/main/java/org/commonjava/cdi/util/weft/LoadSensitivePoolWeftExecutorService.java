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

import org.commonjava.cdi.util.weft.exception.PoolOverloadException;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LoadSensitivePoolWeftExecutorService
        implements WeftExecutorService, ScheduledExecutorService
{
    private PoolWeftExecutorService delegate;

    public LoadSensitivePoolWeftExecutorService( PoolWeftExecutorService delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public String getName()
    {
        return delegate.getName();
    }

    @Override
    public boolean isHealthy()
    {
        return delegate.isHealthy();
    }

    @Override
    public double getLoadFactor()
    {
        return delegate.getLoadFactor();
    }

    @Override
    public int getCurrentLoad()
    {
        return delegate.getCurrentLoad();
    }

    @Override
    public Integer getThreadCount()
    {
        return delegate.getThreadCount();
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
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        return delegate.submit( callable );
    }

    @Override
    public <T> Future<T> submit( Runnable runnable, T t )
    {
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        return delegate.submit( runnable, t );
    }

    @Override
    public Future<?> submit( Runnable runnable )
    {
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        return delegate.submit( runnable );
    }

    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> collection )
            throws InterruptedException
    {
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        return delegate.invokeAll( collection );
    }

    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit )
            throws InterruptedException
    {
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        return delegate.invokeAll( collection, l, timeUnit );
    }

    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> collection )
            throws InterruptedException, ExecutionException
    {
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        return delegate.invokeAny( collection );
    }

    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit )
            throws InterruptedException, ExecutionException, TimeoutException
    {
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        return delegate.invokeAny( collection, l, timeUnit );
    }

    @Override
    public void execute( Runnable runnable )
    {
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        delegate.execute( runnable );
    }

    @Override
    public ScheduledFuture<?> schedule( Runnable runnable, long l, TimeUnit timeUnit )
    {
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        return delegate.schedule( runnable, l, timeUnit );
    }

    @Override
    public <V> ScheduledFuture<V> schedule( Callable<V> callable, long l, TimeUnit timeUnit )
    {
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        return delegate.schedule( callable, l, timeUnit );
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate( Runnable runnable, long l, long l1, TimeUnit timeUnit )
    {
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        return delegate.scheduleAtFixedRate( runnable, l, l1, timeUnit );
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay( Runnable runnable, long l, long l1, TimeUnit timeUnit )
    {
        if ( !isHealthy() )
        {
            throw new PoolOverloadException();
        }

        return delegate.scheduleWithFixedDelay( runnable, l, l1, timeUnit );
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

}
