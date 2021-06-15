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

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import static org.commonjava.cdi.util.weft.ContextSensitiveWeakHashMap.newSynchronizedContextSensitiveWeakHashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@ApplicationScoped
public class ContextSensitiveWeakHashMapTest
{
    /*Inject an ExecutorService instance using @WeftManaged.*/
    @WeftManaged
    @Inject
    private ExecutorService executor;

    ExecutorService getExecutorService()
    {
        return executor;
    }

    private Weld weld;

    private WeldContainer container;

    @Before
    public void init()
    {
        weld = new Weld();
        container = weld.initialize();
    }

    /**
     * We create a ThreadContext in the main thread, then create a ContextSensitiveWeakHashMap
     * and a normal WeakHashMap for comparision.
     *
     * Then we start a new Runnable via the ExecutorService. In this runnable, we do:
     * 1. add an entry to both maps.
     * 2. run GC to clear weak entries.
     * 3. verify that the value is available in the ContextSensitiveWeakHashMap but NOT in normal WeakHashMap.
     *
     * After the execution is done, we clearContext in main thread, run GC and verify the entry
     * in ContextSensitiveWeakHashMap is cleared.
     */
    @Test
    public void run()
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        ContextSensitiveWeakHashMapTest client =
                        container.select( ContextSensitiveWeakHashMapTest.class ).get();

        String path = "foo/bar/bar-1.0.pom";

        ThreadContext ctx = ThreadContext.getContext( true ); // create ctx

        /* ReentrantLock as value has not special meaning. Anything else is equally fine. */
        final Map<String, ReentrantLock> contextSensitiveWeakHashMap = newSynchronizedContextSensitiveWeakHashMap();

        final ReentrantLock parentLock =
                        contextSensitiveWeakHashMap.computeIfAbsent( new String( path ), k -> new ReentrantLock() );

        final Map<String, ReentrantLock> weakHashMap = new WeakHashMap<>(); // normal WeakHashMap

        Exception err = null;

        try
        {
            /* @formatter:off */
            Future<?> task = client.getExecutorService().submit( () ->
                 {
                     logger.debug( "Start processing..." );
                     ReentrantLock lock = contextSensitiveWeakHashMap.computeIfAbsent( new String( path ),
                                                     k -> new ReentrantLock() );

                     weakHashMap.computeIfAbsent( new String( path ),
                                     k -> new ReentrantLock() );

                     Runtime.getRuntime().gc();

                     assertEquals( parentLock, lock );
                     assertNull( weakHashMap.get( new String( path ) ) );
                 } );
            /* @formatter:on */
            task.get();
        }
        catch ( Exception e )
        {
            logger.error( "Something failed", e );
            err = e;
        }
        finally
        {
            ThreadContext.clearContext();
        }

        Runtime.getRuntime().gc();

        assertNull( contextSensitiveWeakHashMap.get( new String( path ) ) );
        assertNull( err );
    }

    @After
    public void shutdown()
    {
        weld.shutdown();
    }
}
