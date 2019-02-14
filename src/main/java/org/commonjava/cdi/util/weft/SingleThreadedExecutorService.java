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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dummy {@link ExecutorService} that forces everything to be read in the current thread. Used for when weft is disabled.
 * Created by jdcasey on 8/25/16.
 */
public class SingleThreadedExecutorService
    implements WeftExecutorService
{
    private boolean shutdown;

    private Random random = new Random();

    private String name;

    private AtomicBoolean isRunning = new AtomicBoolean( false );

    public SingleThreadedExecutorService( final String name )
    {
        this.name = name;
    }

    @Override
    public synchronized void shutdown()
    {
        this.shutdown = true;
        notifyAll();
    }

    @Override
    public synchronized List<Runnable> shutdownNow()
    {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown()
    {
        return shutdown;
    }

    @Override
    public boolean isTerminated()
    {
        return shutdown;
    }

    @Override
    public synchronized boolean awaitTermination( long l, TimeUnit timeUnit )
            throws InterruptedException
    {
        if ( !shutdown )
        {
            wait( TimeUnit.MILLISECONDS.convert( l, timeUnit ) );
        }

        return shutdown;
    }

    @Override
    public <T> Future<T> submit( Callable<T> callable )
    {
        return new FuturePast(callable);
    }

    @Override
    public <T> Future<T> submit( Runnable runnable, T t )
    {
        return new FuturePast(new RunAndReturn(t, runnable));
    }

    @Override
    public Future<?> submit( Runnable runnable )
    {
        return new FuturePast(new RunAndReturn(runnable, null));
    }

    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> collection )
            throws InterruptedException
    {
        ArrayList<Future<T>> futures = new ArrayList<>( collection.size() );
        for ( Callable callable : collection )
        {
            futures.add( new FuturePast( callable ) );
        }

        return futures;
    }

    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit )
            throws InterruptedException
    {
        return invokeAll( collection );
    }

    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> collection )
            throws InterruptedException, ExecutionException
    {
        ArrayList<Callable<T>> items = new ArrayList<>( collection );
        Callable<T> callable = null;
        while( callable == null )
        {
            callable = items.get( Math.abs( random.nextInt( items.size() ) ) );
        }
        try
        {
            return callable.call();
        }
        catch ( Exception e )
        {
            throw new ExecutionException( "Failed to call: " + callable, e );
        }
    }

    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit )
            throws InterruptedException, ExecutionException, TimeoutException
    {
        return invokeAny( collection );
    }

    @Override
    public void execute( Runnable runnable )
    {
        runnable.run();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isHealthy()
    {
        return true;
    }

    @Override
    public double getLoadFactor()
    {
        return isRunning.get() ? 1 : 0;
    }

    @Override
    public long getCurrentLoad()
    {
        return isRunning.get() ? 1L : 0L;
    }

    @Override
    public Integer getThreadCount()
    {
        return 1;
    }

    @Override
    public int getCorePoolSize()
    {
        return 1;
    }

    @Override
    public int getMaximumPoolSize()
    {
        return 1;
    }

    @Override
    public int getActiveCount()
    {
        return isRunning.get() ? 1 : 0;
    }

    @Override
    public long getTaskCount()
    {
        return isRunning.get() ? 1 : 0;
    }

    private final class RunAndReturn<T> implements Callable<T>
    {
        private final T result;

        private final Runnable runnable;

        public RunAndReturn( T result, Runnable runnable )
        {
            this.result = result;
            this.runnable = runnable;
        }

        @Override
        public T call()
                throws Exception
        {
            runnable.run();
            return result;
        }
    }

    private final class FuturePast<T>
            implements Future<T>
    {
        private ExecutionException error;

        private T result;

        public FuturePast( T result )
        {
            this.result = result;
        }

        public FuturePast( Callable<? extends T> callable )
        {
            try
            {
                result = callable.call();
            }
            catch ( Exception e )
            {
                error = new ExecutionException( "Failed to call: " + callable, e );
            }
        }

        @Override
        public boolean cancel( boolean b )
        {
            return false;
        }

        @Override
        public boolean isCancelled()
        {
            return false;
        }

        @Override
        public boolean isDone()
        {
            return true;
        }

        @Override
        public T get()
                throws InterruptedException, ExecutionException
        {
            if ( error != null )
            {
                throw error;
            }

            return result;
        }

        @Override
        public T get( long l, TimeUnit timeUnit )
                throws InterruptedException, ExecutionException, TimeoutException
        {
            return get();
        }
    }
}
