package org.commonjava.cdi.util.weft;

import com.codahale.metrics.health.HealthCheck;

public class WeftPoolHealthCheck
        extends HealthCheck
{
    private static final String POOL_SIZE = "pool-size";

    private static final String CURRENT_LOAD = "current-load";

    private static final String LOAD_FACTOR = "load-factor";

    private WeftExecutorService pool;

    public WeftPoolHealthCheck( final WeftExecutorService pool )
    {
        this.pool = pool;
    }

    @Override
    protected Result check()
            throws Exception
    {

        ResultBuilder builder = Result.builder();
        if ( pool == null || pool.isHealthy() )
        {
            builder = builder.healthy();
        }
        else
        {
            builder = builder.unhealthy();
        }

        if ( pool != null )
        {
            builder = builder.withDetail( POOL_SIZE, pool.getThreadCount() )
                             .withDetail( CURRENT_LOAD, pool.getCurrentLoad() )
                             .withDetail( LOAD_FACTOR, pool.getLoadFactor() );
        }
        else
        {
            builder = builder.withDetail( POOL_SIZE, 0 )
                             .withDetail( CURRENT_LOAD, 0 )
                             .withDetail( LOAD_FACTOR, 0.0 );
        }

        return builder.build();
    }
}
