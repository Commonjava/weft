/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.cdi.util.weft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.commonjava.cdi.util.weft.config.WeftConfig;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class ExecutorProvider
{

    private final Map<String, ScheduledExecutorService> services = new HashMap<String, ScheduledExecutorService>();

    private final Logger logger = new Logger( getClass() );

    @Inject
    private WeftConfig config;

    @PreDestroy
    public void shutdown()
    {
        for ( final Map.Entry<String, ScheduledExecutorService> entry : services.entrySet() )
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
                        logger.warn( "%d tasks remain for executor: %s", running.size(), entry.getKey() );
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
    public ScheduledExecutorService getExecutorService( final InjectionPoint ip )
    {
        final ExecutorConfig ec = ip.getAnnotated()
                                    .getAnnotation( ExecutorConfig.class );

        Integer threadCount = null;
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

        threadCount = config.getThreads( name, threadCount );
        priority = config.getPriority( name, priority );

        return getService( name, priority, threadCount, daemon );
    }

    private synchronized ScheduledExecutorService getService( final String name, final int priority, final int threadCount, final boolean daemon )
    {
        ScheduledExecutorService service = services.get( name );
        if ( service == null )
        {
            service = Executors.newScheduledThreadPool( threadCount, new NamedThreadFactory( name, daemon, priority ) );

            services.put( name, service );
        }

        return service;
    }

    //    @Produces
    //    public Executor getExecutor( final InjectionPoint ip )
    //    {
    //        return getExecutorService( ip );
    //    }

}
