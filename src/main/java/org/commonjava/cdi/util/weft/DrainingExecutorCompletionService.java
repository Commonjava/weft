/**
 * Copyright (C) 2013-2022 Red Hat, Inc. (jdcasey@commonjava.org)
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
