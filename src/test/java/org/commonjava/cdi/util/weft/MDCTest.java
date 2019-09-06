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

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@ApplicationScoped
public class MDCTest {

    @Inject
    @WeftManaged
    @ExecutorConfig( named = "weft-test", threads = 2, loadSensitive = ExecutorConfig.BooleanLiteral.TRUE )
    private WeftExecutorService executor;

    ExecutorService getExecutorService() {
        return executor;
    }

    @Inject
    @WeftManaged
    @ExecutorConfig( named = "weft-embedded", threads = 2, loadSensitive = ExecutorConfig.BooleanLiteral.TRUE )
    private WeftExecutorService embedded;

    ExecutorService getEmbedded() {
        return embedded;
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
     * Inject two WeftExecutorService instances using @WeftManaged that one is embedded in the another one;
     * and set the values in the MDC in each thread, Then, we verify that all of the values are available
     * in the leaf thread.
     */
    @Test
    public void run()
    {
        MDCTest client = container.instance().select(MDCTest.class).get();

        Logger logger = LoggerFactory.getLogger( getClass() );
        MDC.put("requestID", "master-indy-01");

        ThreadContext ctx = ThreadContext.getContext(true);

        DrainingExecutorCompletionService<Exception> svc =
                        new DrainingExecutorCompletionService<>( client.getExecutorService() );

        svc.submit( () -> {

            DrainingExecutorCompletionService<Exception> svc2 =
                            new DrainingExecutorCompletionService<>( client.getEmbedded() );

            logger.debug("Start processing...");
            assertThat(MDC.get("requestID"), equalTo("master-indy-01"));

            MDC.put( "promoteID", "promote-id-01" );

            svc2.submit( () -> {
                MDC.put( "ruleFile", "validation.groovy" );

                logger.debug("Start embedded processing...");
                logger.debug("MDC ContextMap: {}", MDC.getCopyOfContextMap());

                assertThat( MDC.get( "requestID" ), equalTo( "master-indy-01" ) );
                assertThat( MDC.get( "promoteID" ), equalTo( "promote-id-01" ) );
                assertThat( MDC.get( "ruleFile" ), equalTo( "validation.groovy" ) );
                return null;
            } );

            svc2.drain( entry -> {
                //DO NOTHING
            } );

            return null;
        } );

        try
        {
            svc.drain( entry -> {
                //DO NOTHING
            } );
        }
        catch ( InterruptedException | ExecutionException e )
        {
           fail(e.getMessage());
        }

    }

    @After
    public void shutdown()
    {
        weld.shutdown();
    }

}
