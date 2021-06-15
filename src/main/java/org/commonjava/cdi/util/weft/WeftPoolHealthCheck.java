/**
 * Copyright (C) 2013-2021 Red Hat, Inc. (jdcasey@commonjava.org)
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

import org.commonjava.o11yphant.metrics.api.healthcheck.HealthCheck;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WeftPoolHealthCheck
                implements HealthCheck
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
    public Result check() throws Exception
    {
        boolean healthy = false;
        final String timestamp = new Date().toString();
        final Map<String, Object> details = new HashMap<>();
        if ( pool != null )
        {
            healthy = pool.isHealthy();
            details.put( POOL_SIZE, pool.getThreadCount() );
            details.put( CURRENT_LOAD, pool.getCurrentLoad() );
            details.put( LOAD_FACTOR, pool.getLoadFactor() );
        }

        final boolean isHealthy = healthy;
        return new Result()
        {
            @Override
            public boolean isHealthy()
            {
                return isHealthy;
            }

            @Override
            public String getMessage()
            {
                return null;
            }

            @Override
            public Throwable getError()
            {
                return null;
            }

            @Override
            public String getTimestamp()
            {
                return timestamp;
            }

            @Override
            public Map<String, Object> getDetails()
            {
                return details;
            }
        };
    }
}
