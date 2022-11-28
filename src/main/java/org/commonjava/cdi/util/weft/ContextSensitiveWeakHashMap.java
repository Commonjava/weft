/**
 * Copyright (C) 2013-2022 Red Hat, Inc. (jdcasey@commonjava.org)
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * ContextSensitiveWeakHashMap is a subclass of WeakHashMap. But an entry will not be GC-ed until all the threads
 * ever use it clears their ThreadContext.
 *
 * An utility method newSynchronizedContextSensitiveWeakHashMap is provided as a convenient way to obtain a
 * synchronized ContextSensitiveWeakHashMap instance.
 *
 * Created by ruhan on 12/21/17.
 */
public class ContextSensitiveWeakHashMap<K, V>
                extends WeakHashMap<K, V>
{
    Logger logger = LoggerFactory.getLogger( getClass() );

    private final String uniqueId;

    private final Consumer<ThreadContext> finalizer;

    public ContextSensitiveWeakHashMap()
    {
        super();
        this.uniqueId = UUID.randomUUID().toString();

        this.finalizer = ctx ->
        {
            logger.trace( "Finalizer called for {}", uniqueId );
            ctx.remove( uniqueId );
        };
    }

    @Override
    public V put( K k, V v )
    {
        V ret = super.put( k, v );

        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            Object obj = ctx.computeIfAbsent( uniqueId, o -> new HashSet<K>() );
            Set<K> keys = (Set) obj;
            logger.trace( "Add key {} for {}", k, uniqueId );
            keys.add( k ); // the k is referenced in ctx so that it will not be GC-ed until ctx clears it

            ctx.registerFinalizer( finalizer ); // register finalizer if not present
        }

        return ret;
    }

    @Override
    public String toString()
    {
        return "ContextSensitiveWeakHashMap{" + "uniqueId='" + uniqueId + '\'' + '}';
    }

    /**
     * Mostly, ContextSensitiveWeakHashMap is used in multi-threads environment. This method creates a new
     * ContextSensitiveWeakHashMap instance and wraps it by Collections.synchronizedMap().
     * @param <K>
     * @param <V>
     * @return a synchronized ContextSensitiveWeakHashMap
     */
    public static <K, V> Map<K, V> newSynchronizedContextSensitiveWeakHashMap()
    {
        return Collections.synchronizedMap( new ContextSensitiveWeakHashMap<K, V>() );
    }

}
