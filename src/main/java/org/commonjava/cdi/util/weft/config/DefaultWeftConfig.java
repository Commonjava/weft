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
package org.commonjava.cdi.util.weft.config;

import java.util.HashMap;
import java.util.Map;

public class DefaultWeftConfig
    implements WeftConfig
{

    public static final String THREADS_SUFFIX = "t";

    public static final String PRIORITY_SUFFIX = "p";

    private static final int DEFAULT_THREADS = Runtime.getRuntime()
                                                      .availableProcessors() * 2;

    private static final int DEFAULT_PRIORITY = 8;

    private boolean enabled = true;

    private final Map<String, Boolean> enabledPools = new HashMap<>();

    private final Map<String, Integer> config = new HashMap<String, Integer>();

    private int defaultThreads = DEFAULT_THREADS;

    private int defaultPriority = DEFAULT_PRIORITY;

    private String nodePrefix;

    public DefaultWeftConfig()
    {
    }

    public DefaultWeftConfig( final Map<String, Integer> config )
    {
        this.config.putAll( config );
    }

    public DefaultWeftConfig( final DefaultWeftConfig config )
    {
        this.config.putAll( config.config );
    }

    public DefaultWeftConfig configureDefaultThreads( final int defaultThreads )
    {
        this.defaultThreads = defaultThreads;
        return this;
    }

    public DefaultWeftConfig configureDefaultPriority( final int defaultPriority )
    {
        this.defaultPriority = defaultPriority;
        return this;
    }

    public DefaultWeftConfig configurePool( final String name, final int threads, final int priority )
    {
        config.put( name + THREADS_SUFFIX, threads );
        config.put( name + PRIORITY_SUFFIX, priority );

        return this;
    }

    public DefaultWeftConfig configureThreads( final String name, final int threads )
    {
        config.put( name + THREADS_SUFFIX, threads );

        return this;
    }

    public DefaultWeftConfig configurePriority( final String name, final int priority )
    {
        config.put( name + PRIORITY_SUFFIX, priority );

        return this;
    }

    public DefaultWeftConfig configureEnabled( final String name, final boolean enabled )
    {
        enabledPools.put( name, enabled );
        return this;
    }

    public DefaultWeftConfig configureEnabled( boolean enabled )
    {
        this.enabled = enabled;
        return this;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public boolean isEnabled( String name )
    {
        if ( !isEnabled() )
        {
            return false;
        }

        Boolean result = enabledPools.get( name );
        return result == null ? isEnabled() : result;
    }

    @Override
    public int getDefaultThreads()
    {
        return defaultThreads;
    }

    @Override
    public int getDefaultPriority()
    {
        return defaultPriority;
    }

    public DefaultWeftConfig configureNodePrefix( String nodePrefix )
    {
        this.nodePrefix = nodePrefix;
        return this;
    }

    @Override
    public String getNodePrefix()
    {
        return nodePrefix;
    }

    @Override
    public int getThreads( final String poolName )
    {
        return getWithDefaultAndFailover( poolName, THREADS_SUFFIX, null, getDefaultThreads() );
    }

    @Override
    public int getThreads( final String poolName, final Integer defaultValue )
    {
        return getWithDefaultAndFailover( poolName, THREADS_SUFFIX, defaultValue, getDefaultThreads() );
    }

    @Override
    public int getPriority( final String poolName )
    {
        return getWithDefaultAndFailover( poolName, PRIORITY_SUFFIX, null, getDefaultPriority() );
    }

    @Override
    public int getPriority( final String poolName, final Integer defaultValue )
    {
        return getWithDefaultAndFailover( poolName, PRIORITY_SUFFIX, defaultValue, getDefaultPriority() );
    }

    private int getWithDefaultAndFailover( final String poolName, final String suffix, final Integer defaultValue, final int failover )
    {
        final Integer v = config.get( poolName + suffix );
        if ( v == null )
        {
            return defaultValue == null ? failover : defaultValue;
        }

        return v;
    }

}
