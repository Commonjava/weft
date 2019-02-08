package org.commonjava.cdi.util.weft;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private MetricRegistry metricRegistry;

    protected WeftPoolBoy(){}

    public WeftPoolBoy( WeftConfig config, MetricRegistry registry )
    {
        this.config = config;
        this.metricRegistry = registry;
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
            new WeftExecutorService( "singlethreaded", new SingleThreadedExecutorService(), 1, 10f, null, null );

    public WeftExecutorService getPool( final String key )
    {
        return pools.get( key );
    }

    public WeftExecutorService addPool( final String key, final WeftExecutorService pool )
    {
        return pools.put( key, pool );
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
        ExecutorService svc = null;
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

                svc = Executors.newScheduledThreadPool( threadCount, fac );
            }
            else if ( threadCount > 0 )
            {
                svc = Executors.newFixedThreadPool( threadCount, fac );
            }
            else
            {
                svc = Executors.newCachedThreadPool( fac );
            }

            String metricPrefix = name( config.getNodePrefix(), "weft.ThreadPoolExecutor", name );
            if ( metricRegistry != null && svc instanceof ThreadPoolExecutor )
            {
                logger.info( "Register thread pool metrics - {}", name );
                registerMetrics( metricRegistry, metricPrefix, (ThreadPoolExecutor) svc );
            }

            service = new WeftExecutorService( name, svc, threadCount, maxLoadFactor, metricRegistry, metricPrefix );

            // TODO: Wrapper ThreadPoolExecutor that wraps Runnables to store/copy MDC when it gets created/started.

            if ( existing == null )
            {
                addPool( key, service );
            }
        }

        return service;
    }


    private void registerMetrics( MetricRegistry registry, String prefix, ThreadPoolExecutor executor )
    {
        registry.register( name( prefix, "corePoolSize" ), (Gauge<Integer>) () -> executor.getCorePoolSize() );
        registry.register( name( prefix, "activeThreads" ), (Gauge<Integer>) () -> executor.getActiveCount() );
        registry.register( name( prefix, "maxPoolSize" ), (Gauge<Integer>) () -> executor.getMaximumPoolSize() );
        registry.register( name( prefix, "queueSize" ), (Gauge<Integer>) () -> executor.getQueue().size() );
    }

    public Set<WeftExecutorService> getPools()
    {
        return Collections.unmodifiableSet( new HashSet<>( pools.values() ) );
    }
}
