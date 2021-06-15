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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;

public class SignallingLockerTest
{
    private SignallingLocker<String> locker = new SignallingLocker<>( 100 );
    private final long timeout = 100;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Test
    public void sweepStaleLocks()
            throws Exception
    {
        String key = "foo";

        ExecutorService exec = Executors.newFixedThreadPool( 2 );

        AtomicBoolean race = new AtomicBoolean( false );

        Map<String, SignallingLock> locks = new ConcurrentHashMap<>();

        ExecutorCompletionService<Void> service = new ExecutorCompletionService<>( exec );

        service.submit( op( "1", key, race, locks ) );
        service.submit( op( "2", key, race, locks ) );

        race.set( true );

        for(int i=0; i<2; i++)
        {
            service.take().get();
        }

        logger.info( "Waiting for sweep" );

        Thread.sleep( 200 );

        logger.info( "Retrying third lock" );

        op( "3", key, race, locks ).call();

        logger.info( "Locks: \n\n{}\n\n", locks );

        assertThat( "threaded locks should be the same instance", locks.get("1") == locks.get("2"));
        assertThat( "threaded locks should NOT be the same as the third lock instance", locks.get("1") != locks.get("3)"));
    }

    private Callable<Void> op( String id, String key, AtomicBoolean race, Map<String, SignallingLock> locks )
    {
        return () -> {
            // loop until we're ready.
            while ( !race.get() )
            {
                logger.info( "{} waiting.", id );
                try
                {
                    Thread.sleep( 10 );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
            }

            locker.lockAnd( key, ( k, lock ) -> {
                logger.info( "{} locked and running.", id );
                locks.put( id, lock );

                try
                {
                    Thread.sleep( timeout );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }

                return null;
            } );

            return null;
        };
    }
}
