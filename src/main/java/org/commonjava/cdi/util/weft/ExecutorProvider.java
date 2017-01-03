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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.commonjava.cdi.util.weft.config.WeftConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ExecutorProvider
{

    private final Map<String, ExecutorService> services = new ConcurrentHashMap<String, ExecutorService>();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private WeftConfig config;

    private SingleThreadedExecutorService singleThreaded = new SingleThreadedExecutorService();

    @PreDestroy
    public void shutdown()
    {
        for ( final Map.Entry<String, ExecutorService> entry : services.entrySet() )
        {
            final ExecutorService service = entry.getValue();

            service.shutdown();
            try
            {
                service.awaitTermination( 1000, TimeUnit.MILLISECONDS );

                if ( !service.isTerminated() )
                {
                    final List<Runnable> running = service.shutdownNow();
                    if ( !running.isEmpty() )
                    {
                        logger.warn( "{} tasks remain for executor: {}", running.size(), entry.getKey() );
                    }
                }
            }
            catch ( final InterruptedException e )
            {
                Thread.currentThread()
                      .interrupt();
                return;
            }
        }
    }

    @Produces
    @WeftManaged
    public ExecutorService getExecutorService( final InjectionPoint ip )
    {
        return getExec( ip, false );
    }

    @Produces
    @WeftScheduledExecutor
    public ScheduledExecutorService getScheduledExecutorService( final InjectionPoint ip )
    {
        return (ScheduledExecutorService) getExec( ip, true );
    }

    private ExecutorService getExec( final InjectionPoint ip, final boolean scheduled )
    {
        final ExecutorConfig ec = ip.getAnnotated()
                                    .getAnnotation( ExecutorConfig.class );

        Integer threadCount = 0;
        Integer priority = null;

        boolean daemon = true;

        // TODO: This may cause counter-intuitive sharing of thread pools for un-annotated injections...
        String name = "weft-unannotated";

        if ( ec != null )
        {
            threadCount = ec.threads();
            name = ec.named();
            priority = ec.priority();
            daemon = ec.daemon();
        }

        if ( !config.isEnabled() || !config.isEnabled( name ) )
        {
            return singleThreaded;
        }

        threadCount = config.getThreads( name, threadCount );
        priority = config.getPriority( name, priority );

        final String key = name + ":" + ( scheduled ? "scheduled" : "" );
        ExecutorService service = services.get( key );
        if ( service == null )
        {
            final ThreadFactory fac = new NamedThreadFactory( name, daemon, priority );

            if ( scheduled )
            {
                if ( threadCount < 1 )
                {
                    throw new RuntimeException( ip + " must specify a non-zero number for threads parameter in @ExecutorConfig." );
                }

                service = Executors.newScheduledThreadPool( threadCount, fac );
            }
            else if ( threadCount > 0 )
            {
                service = Executors.newFixedThreadPool( threadCount, fac );
            }
            else
            {
                service = Executors.newCachedThreadPool( fac );
            }

            service = new ContextSensitiveExecutorService( service );

            // TODO: Wrapper ThreadPoolExecutor that wraps Runnables to store/copy MDC when it gets created/started.

            services.put( key, service );
        }

        return service;
    }

    //    @Produces
    //    public Executor getExecutor( final InjectionPoint ip )
    //    {
    //        return getExecutorService( ip );
    //    }

}
