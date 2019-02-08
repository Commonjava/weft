package org.commonjava.cdi.util.weft;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.commonjava.cdi.util.weft.config.WeftConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.CDI;
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

import static com.codahale.metrics.MetricRegistry.name;

@ApplicationScoped
public class WeftPoolBoy
{
    private final Map<String, WeftExecutorService> pools = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private WeftConfig config;

    @Inject
    private MetricRegistry metricRegistry;

    @Inject
    private HealthCheckRegistry healthCheckRegistry;

    protected WeftPoolBoy(){}

    public WeftPoolBoy( WeftConfig config, MetricRegistry registry, HealthCheckRegistry healthCheckRegistry )
    {
        this.config = config;
        this.metricRegistry = registry;
        this.healthCheckRegistry = healthCheckRegistry;
    }

    @PostConstruct
    public void init()
    {
        try
        {
            this.metricRegistry = CDI.current().select( MetricRegistry.class).get();
        }
        catch ( UnsatisfiedResolutionException e )
        {
            logger.info( e.getMessage() );
        }
    }

    private WeftExecutorService singleThreaded =
            new WeftExecutorService( "singlethreaded", (ThreadPoolExecutor) Executors.newFixedThreadPool( 1 ), 1, 10f,
                                     null, null );

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

    public WeftExecutorService getPool( final ExecutorConfig ec, final boolean scheduled )
    {
        Integer threadCount = 0;
        Integer priority = null;
        Float maxLoadFactor = null;

        boolean daemon = true;

        // TODO: This may cause counter-intuitive sharing of thread pools for un-annotated injections...
        String name = "weft-unannotated";

        if ( ec != null )
        {
            threadCount = ec.threads();
            name = ec.named();
            priority = ec.priority();
            maxLoadFactor = ec.maxLoadFactor();
            daemon = ec.daemon();
        }

        if ( !config.isEnabled() || !config.isEnabled( name ) )
        {
            return singleThreaded;
        }

        threadCount = config.getThreads( name, threadCount );
        priority = config.getPriority( name, priority );
        maxLoadFactor = config.getMaxLoadFactor( name, maxLoadFactor );

        final String key = name + ":" + ( scheduled ? "scheduled" : "" );
        WeftExecutorService existing = getPool( key );
        WeftExecutorService service = existing;
        ThreadPoolExecutor svc = null;
        if ( service == null )
        {
            final NamedThreadFactory fac = new NamedThreadFactory( name, daemon, priority );

            if ( scheduled )
            {
                if ( threadCount < 1 )
                {
                    logger.warn( ec + " must specify a non-zero number for threads parameter in @ExecutorConfig." );
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

            service = new WeftExecutorService( name, svc, threadCount, maxLoadFactor, metricRegistry, metricPrefix );

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
            metricRegistry.register( name( prefix, "maxPoolSize" ), (Gauge<Integer>) () -> pool.getMaximumPoolSize() );
            metricRegistry.register( name( prefix, "queueSize" ), (Gauge<Long>) () -> pool.getTaskCount() );
        }

        if ( healthCheckRegistry != null )
        {
            healthCheckRegistry.register( pool.getName(), new WeftPoolHealthCheck( pool ) );
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
