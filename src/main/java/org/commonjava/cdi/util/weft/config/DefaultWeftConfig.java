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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefaultWeftConfig
    implements WeftConfig
{

    public static final String THREADS_SUFFIX = "t";

    public static final String PRIORITY_SUFFIX = "p";

    private static final int DEFAULT_THREADS = Runtime.getRuntime()
                                                      .availableProcessors() * 2;

    private static final int DEFAULT_PRIORITY = 8;

    private static final float DEFAULT_MAX_LOAD_FACTOR = 10.0f;

    private boolean enabled = true;

    private final Map<String, Boolean> enabledPools = new HashMap<>();

    private final Map<String, Integer> config = new HashMap<String, Integer>();

    private final Map<String, Float> maxLoadFactors = new HashMap<>();

    private final Map<String, Boolean> loadSensitivePools = new HashMap<>();

    private boolean defaultLoadSensitive;

    private int defaultThreads = DEFAULT_THREADS;

    private int defaultPriority = DEFAULT_PRIORITY;

    private float defaultMaxLoadFactor = DEFAULT_MAX_LOAD_FACTOR;

    private String nodePrefix;

    private Set<String> knownPools = new HashSet<>();

    public DefaultWeftConfig()
    {
    }

    public DefaultWeftConfig( final Map<String, Integer> config, final Map<String, Float> maxLoadFactors )
    {
        this.config.putAll( config );
        this.maxLoadFactors.putAll( maxLoadFactors );
    }

    public DefaultWeftConfig( final DefaultWeftConfig config )
    {
        this.config.putAll( config.config );
        this.maxLoadFactors.putAll( config.maxLoadFactors );
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

    public DefaultWeftConfig configureDefaultMaxLoadFactor( final float maxLoadFactor )
    {
        this.defaultMaxLoadFactor = maxLoadFactor;
        return this;
    }

    public DefaultWeftConfig configureDefaultLoadSensitive( final boolean defaultLoadSensitive )
    {
        this.defaultLoadSensitive = defaultLoadSensitive;
        return this;
    }

    public DefaultWeftConfig configurePool( final String name, final int threads, final int priority )
    {
        return configurePool( name, threads, priority, 0f );
    }

    public DefaultWeftConfig configurePool( final String name, final int threads, final int priority, final float maxLoadFactor )
    {
        knownPools.add( name );

        config.put( name + THREADS_SUFFIX, threads );
        config.put( name + PRIORITY_SUFFIX, priority );
        if ( maxLoadFactor > 0f )
        {
            maxLoadFactors.put( name, maxLoadFactor );
        }

        return this;
    }

    public DefaultWeftConfig configureThreads( final String name, final int threads )
    {
        knownPools.add( name );

        config.put( name + THREADS_SUFFIX, threads );

        return this;
    }

    public DefaultWeftConfig configurePriority( final String name, final int priority )
    {
        knownPools.add( name );

        config.put( name + PRIORITY_SUFFIX, priority );

        return this;
    }

    public DefaultWeftConfig configureMaxLoadFactor( final String name, final float maxLoadFactor )
    {
        knownPools.add( name );

        maxLoadFactors.put( name, maxLoadFactor );
        return this;
    }

    public DefaultWeftConfig configureLoadSensitive( final String name, final boolean sensitive )
    {
        knownPools.add( name );

        loadSensitivePools.put( name, sensitive );
        return this;
    }

    public DefaultWeftConfig configureEnabled( final String name, final boolean enabled )
    {
        knownPools.add( name );

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
        return defaultThreads < 1 ? 1 : defaultThreads;
    }

    @Override
    public int getDefaultPriority()
    {
        return defaultPriority;
    }

    @Override
    public float getDefaultMaxLoadFactor()
    {
        return defaultMaxLoadFactor;
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
        return getWithDefaultAndFailover( poolName, PRIORITY_SUFFIX, 1, getDefaultPriority() );
    }

    @Override
    public float getMaxLoadFactor( final String poolName )
    {
        return getWithDefaultAndFailover( poolName, 10f, getDefaultMaxLoadFactor() );
    }

    @Override
    public int getPriority( final String poolName, final Integer defaultValue )
    {
        return getWithDefaultAndFailover( poolName, PRIORITY_SUFFIX, defaultValue, getDefaultPriority() );
    }

    @Override
    public float getMaxLoadFactor( final String poolName, final Float defaultMax )
    {
        return getWithDefaultAndFailover( poolName, defaultMax, getDefaultMaxLoadFactor() );
    }

    @Override
    public boolean isLoadSensitive( final String poolName, final Boolean defaultValue )
    {
        return getWithDefaultAndFailover( poolName, defaultValue, isDefaultLoadSensitive() );
    }

    @Override
    public boolean isDefaultLoadSensitive()
    {
        return defaultLoadSensitive;
    }

    @Override
    public Set<String> getKnownPools()
    {
        return Collections.unmodifiableSet( knownPools );
    }

    private int getWithDefaultAndFailover( final String poolName, final String suffix, final Integer defaultValue, final int failover )
    {
        final Integer v = config.get( poolName + suffix );
        if ( v == null || v < 1 )
        {
            return defaultValue == null || defaultValue < 0 ? failover : defaultValue;
        }

        return v;
    }

    private float getWithDefaultAndFailover( final String poolName, final Float defaultValue, final float failover )
    {
        final Float v = maxLoadFactors.get( poolName );
        if ( v == null )
        {
            return defaultValue == null  || defaultValue == 0 ? failover : defaultValue;
        }

        return v;
    }

    private boolean getWithDefaultAndFailover( String poolName, Boolean defaultValue, Boolean failover )
    {
        Boolean v = loadSensitivePools.get( poolName );
        if ( v == null )
        {
            return defaultValue == null ? failover : defaultValue;
        }

        return v;
    }

}
