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

import org.commonjava.cdi.util.weft.config.WeftConfig;
import org.commonjava.o11yphant.metrics.api.Gauge;
import org.commonjava.o11yphant.metrics.api.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.commonjava.cdi.util.weft.config.DefaultWeftConfig.DEFAULT_MAX_LOAD_FACTOR;
import static org.commonjava.cdi.util.weft.config.DefaultWeftConfig.DEFAULT_PRIORITY;
import static org.commonjava.cdi.util.weft.config.DefaultWeftConfig.DEFAULT_THREADS;
import static org.commonjava.o11yphant.metrics.util.NameUtils.name;

@ApplicationScoped
public class WeftPoolBoy
{
    private final Map<String, WeftExecutorService> pools = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String DUMMY_NAME = "weft-anonymous";

    @Inject
    private WeftConfig config;

    @Inject
    private Instance<MetricRegistry> metricRegistryInstance;

    private MetricRegistry metricRegistry;

    @Inject
    private Instance<ThreadContextualizer> contextualizers;

    protected WeftPoolBoy(){}

    public WeftPoolBoy( WeftConfig config, MetricRegistry registry )
    {
        this.config = config;
        this.metricRegistry = registry;
    }

    @PostConstruct
    public void init()
    {
        if ( !metricRegistryInstance.isUnsatisfied() )
        {
            this.metricRegistry = metricRegistryInstance.get();
        }
    }

    public WeftExecutorService getPool( final String key )
    {
        return pools.get( key );
    }

    private WeftExecutorService addPool( final WeftExecutorService pool )
    {
        return pools.put( pool.getName(), pool );
    }

    @PreDestroy
    public void shutdown()
    {
        for ( final Map.Entry<String, WeftExecutorService> entry : pools.entrySet() )
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

    /**
     * Get pool programmatically. This is simplified version for {@link WeftPoolBoy#getPool(String, int, int, float, boolean, boolean, boolean)}
     */
    public synchronized WeftExecutorService getPool( final String name, int threadCount, final boolean scheduled )
    {
        if ( threadCount <= 0 )
        {
            throw new IllegalStateException( "Cannot create executor, invalid threadCount: " + threadCount );
        }
        return getPool( name, threadCount, DEFAULT_PRIORITY, DEFAULT_MAX_LOAD_FACTOR, false, true, scheduled );
    }

    /**
     * This method is used when using cdi @Inject.
     */
    public synchronized WeftExecutorService getPool( final ExecutorConfig ec, final boolean scheduled )
    {
        if ( ec != null )
        {
            int threadCount = ec.threads();
            String name = ec.named();
            if ( isBlank( name ) )
            {
                name = DUMMY_NAME;
            }

            int priority = ec.priority();
            float maxLoadFactor = ec.maxLoadFactor();
            boolean daemon = ec.daemon();

            ExecutorConfig.BooleanLiteral ls = ec.loadSensitive();
            boolean loadSensitive = false;
            if ( ls == ExecutorConfig.BooleanLiteral.TRUE )
            {
                loadSensitive = true;
            }
            return getPool( name, threadCount, priority, maxLoadFactor, loadSensitive, daemon, scheduled );
        }
        else
        {
            return getPool( DUMMY_NAME, DEFAULT_THREADS, DEFAULT_PRIORITY, DEFAULT_MAX_LOAD_FACTOR, false, true, scheduled );
        }
    }

    /**
     * Get pool programmatically. The parameters can be overridden via configuration file. If no config, this will create thread pool as is.
     */
    public synchronized WeftExecutorService getPool( String name, int threadCount, int priority, float maxLoadFactor,
                                        boolean loadSensitive, boolean daemon, final boolean scheduled )
    {

        final String key = name + ( scheduled ? ":scheduled" : "" );
        WeftExecutorService service = getPool( key );
        if ( service == null && ( !config.isEnabled() || !config.isEnabled( name ) || config.getThreads( name ) < 2 ) )
        {
            if ( !scheduled )
            {
                service = new SingleThreadedExecutorService( key );
                addPool( service );
            }
            else
            {
                throw new IllegalStateException( "Cannot create executor for disabled scheduled executor: " + name );
            }
        }

        if ( service == null )
        {
            threadCount = config.getThreads( name, threadCount );
            priority = config.getPriority( name, priority );
            maxLoadFactor = config.getMaxLoadFactor( name, maxLoadFactor );
            loadSensitive = config.isLoadSensitive( name, loadSensitive );

            ThreadPoolExecutor svc;

            ThreadGroup threadGroup = new ThreadGroup( name );
            final NamedThreadFactory fac = new NamedThreadFactory( name, threadGroup, daemon, priority );

            if ( scheduled )
            {
                if ( threadCount < 1 )
                {
                    logger.warn( "Must specify a non-zero number for threads" );
                    threadCount = config.getDefaultThreads();
                }
                svc = (ThreadPoolExecutor) Executors.newScheduledThreadPool( threadCount, fac );
            }
            else if ( threadCount > 0 )
            {
                svc = (ThreadPoolExecutor) Executors.newFixedThreadPool( threadCount, fac );
            }
            else
            {
                svc = (ThreadPoolExecutor) Executors.newCachedThreadPool( fac );
            }

            String metricPrefix = name( config.getNodePrefix(), "weft.ThreadPoolExecutor", name );

            service = new PoolWeftExecutorService( name, svc, threadCount, maxLoadFactor, loadSensitive, metricRegistry,
                                                   metricPrefix, contextualizers );

            // TODO: Wrapper ThreadPoolExecutor that wraps Runnables to store/copy MDC when it gets created/started.

            addPool( service );
            registerMetrics( metricPrefix, service );
        }

        return service;
    }


    private void registerMetrics( String prefix, WeftExecutorService pool )
    {
        if ( metricRegistry != null )
        {
            metricRegistry.register( name( prefix, "corePoolSize" ), (Gauge<Integer>) () -> pool.getCorePoolSize() );
            metricRegistry.register( name( prefix, "activeThreads" ), (Gauge<Integer>) () -> pool.getActiveCount() );
            metricRegistry.register( name( prefix, "loadFactor" ), (Gauge<Double>) () -> pool.getLoadFactor() );
            metricRegistry.register( name( prefix, "currentLoad" ), (Gauge<Long>) () -> pool.getCurrentLoad() );

            metricRegistry.registerHealthCheck( name( prefix, pool.getName() ), new WeftPoolHealthCheck( pool ) );
        }
    }

    public Map<String, WeftExecutorService> getPools()
    {
        Map<String, WeftExecutorService> result = new HashMap<>( pools );

        logger.debug( "Getting pools. {} already initialized.", pools.size() );

        config.getKnownPools().forEach( name->{
            if ( !result.containsKey( name ) )
            {
                logger.debug( "Adding known-but-uninitialized pool: {}", name );
                result.put( name, null );
            }
        } );

        logger.debug( "Returning total of {} pools with {} uninitialized (null).", result.size(),
                      ( result.size() - pools.size() ) );

        return Collections.unmodifiableMap( result );
    }
}
