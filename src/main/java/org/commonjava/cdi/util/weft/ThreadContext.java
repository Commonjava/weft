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

import org.slf4j.MDC;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by jdcasey on 1/3/17.
 */
public class ThreadContext implements Map<String, Object>
{
    private static ThreadLocal<ThreadContext> THREAD_LOCAL = new ThreadLocal<>();

    private final Map<String, Object> contextMap = new ConcurrentHashMap<>();

    private Map<String, String> mdcMap; // mapped diagnostic context

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
        ThreadContext oldCtx = THREAD_LOCAL.get();
        THREAD_LOCAL.set( ctx );
        if ( ctx != null )
        {
            MDC.setContextMap(ctx.mdcMap);
        }
        return oldCtx;
    }

    public static void clearContext()
    {
        THREAD_LOCAL.set( null );
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
