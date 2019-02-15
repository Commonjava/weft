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
