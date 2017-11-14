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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Effectively a multi-threaded implementation of ThreadLocal, where threads can pass on context to one another starting
 * with some "top" thread, and traversing to each "child" thread started via a {@link ContextSensitiveExecutorService}
 * instance, as is injected by Weft's @{@link WeftManaged} annotation.
 *
 * This {@link ThreadContext} keeps a count of the number of threads referencing it, and can run finalization logic
 * when that number hits 0.
 *
 * Created by jdcasey on 1/3/17.
 */
public class ThreadContext implements Map<String, Object>
{
    private static ThreadLocal<ThreadContext> THREAD_LOCAL = new ThreadLocal<>();

    private final Map<String, Object> contextMap = new ConcurrentHashMap<>();

    private Map<String, String> mdcMap; // mapped diagnostic context

    private int refs = 1;

    private List<Consumer<ThreadContext>> finalizers = new ArrayList<>();

    public static ThreadContext getContext( boolean create )
    {
        ThreadContext threadContext = THREAD_LOCAL.get();
        if ( threadContext == null && create )
        {
            threadContext = new ThreadContext();
            threadContext.mdcMap = MDC.getCopyOfContextMap();
            THREAD_LOCAL.set( threadContext );
        }

        return threadContext;
    }

    public static ThreadContext setContext( ThreadContext ctx )
    {
        ThreadContext oldCtx = swapContext( ctx );
        if ( ctx != null && ctx.mdcMap != null  )
        {
            MDC.setContextMap(ctx.mdcMap);
        }
        return oldCtx;
    }

    private static ThreadContext swapContext( final ThreadContext ctx )
    {
        ThreadContext oldCtx = THREAD_LOCAL.get();
        if ( oldCtx != null )
        {
            Logger logger = LoggerFactory.getLogger( ThreadContext.class );
            oldCtx.refs--;
            logger.trace( "context refs: {}", oldCtx.refs );
            oldCtx.runFinalizersIfDone();
        }

        THREAD_LOCAL.set( ctx );
        if ( ctx != null )
        {
            ctx.refs++;
        }

        return oldCtx;
    }

    /**
     * Provide some finalizer logic to handle the scenario where the number of "live" threads referencing this context
     * drops to 0. Before this happens, any contextual information in this ThreadContext may be needed by running threads,
     * and it's not safe to clean up. However, since the context may contain {@link java.io.Closeable} instances and
     * the like, it's important to have some point where they will be cleaned up.
     * @since 1.5
     * @param finalizer
     */
    public synchronized void registerFinalizer( Consumer<ThreadContext> finalizer )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Registering finalizer: {} on ThreadContext: {}", finalizer, this );
        if ( !this.finalizers.contains( finalizer ) )
        {
            this.finalizers.add( finalizer );
        }
    }

    /**
     * If the thread reference count on this context drops to zero, run any finalization logic that might be registered.
     */
    private synchronized void runFinalizersIfDone()
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( refs < 1 && finalizers != null )
        {
            logger.debug( "Running finalizers for ThreadContext: {}", this );
            finalizers.forEach( c->{
                if ( c != null )
                {
                    logger.debug( "Running finalizer: {} for ThreadContext: {}", c, this );

                    try
                    {
                        c.accept( this );
                    }
                    catch ( Throwable t )
                    {
                        logger.error( "Caught error while running finalizer: " + c + " on ThreadContext: " + this, t );
                    }

                    logger.trace( "Finalizer: {} done for ThreadContext: {}", c, this );
                }
            } );
        }
    }

    public static void clearContext()
    {
        swapContext( null );
        MDC.clear();
    }

    private ThreadContext(){}

    public int size()
    {
        return contextMap.size();
    }

    public boolean isEmpty()
    {
        return contextMap.isEmpty();
    }

    public void putAll( Map<? extends String, ?> map )
    {
        contextMap.putAll( map );
    }

    public Collection<Object> values()
    {
        return contextMap.values();
    }

    public Object merge( String key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction )
    {
        return contextMap.merge( key, value, remappingFunction );
    }

    public Set<String> keySet()
    {
        return contextMap.keySet();
    }

    public void forEach( BiConsumer<? super String, ? super Object> action )
    {
        contextMap.forEach( action );
    }

    public Object computeIfPresent( String key, BiFunction<? super String, ? super Object, ?> remappingFunction )
    {
        return contextMap.computeIfPresent( key, remappingFunction );
    }

    public void clear()
    {
        contextMap.clear();
    }

    public boolean containsValue( Object o )
    {
        return contextMap.containsValue( o );
    }

    public Object put( String s, Object o )
    {
        return contextMap.put( s, o );
    }

    public void replaceAll( BiFunction<? super String, ? super Object, ?> function )
    {
        contextMap.replaceAll( function );
    }

    public Object get( Object o )
    {
        return contextMap.get( o );
    }

    public boolean containsKey( Object o )
    {
        return contextMap.containsKey( o );
    }

    public Set<Map.Entry<String, Object>> entrySet()
    {
        return contextMap.entrySet();
    }

    public boolean replace( String key, Object oldValue, Object newValue )
    {
        return contextMap.replace( key, oldValue, newValue );
    }

    public Object computeIfAbsent( String key, Function<? super String, ?> mappingFunction )
    {
        return contextMap.computeIfAbsent( key, mappingFunction );
    }

    public Object compute( String key, BiFunction<? super String, ? super Object, ?> remappingFunction )
    {
        return contextMap.compute( key, remappingFunction );
    }

    public Object putIfAbsent( String key, Object value )
    {
        return contextMap.putIfAbsent( key, value );
    }

    public Object remove( Object o )
    {
        return contextMap.remove( o );
    }

    public Object getOrDefault( Object key, Object defaultValue )
    {
        return contextMap.getOrDefault( key, defaultValue );
    }

    public boolean remove( Object key, Object value )
    {
        return contextMap.remove( key, value );
    }

    public Object replace( String key, Object value )
    {
        return contextMap.replace( key, value );
    }

}
