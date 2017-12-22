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
import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@ApplicationScoped
public class MDCTest {

    @WeftManaged
    @Inject
    private ExecutorService executor;

    ExecutorService getExecutorService() {
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
     * Inject an ExecutorService instance using @WeftManaged and then set a value in the MDC in the main test method.
     * Then, we start a new Runnable via the ExecutorService, and verify that the value is available in the MDC map of the Runnable.
     */
    @Test
    public void run()
    {
        MDCTest client = container.instance().select(MDCTest.class).get();

        Logger logger = LoggerFactory.getLogger( getClass() );
        MDC.put("requestID", "master-indy-01");

        ThreadContext ctx = ThreadContext.getContext(true);

        client.getExecutorService().execute( () -> {
            logger.debug("Start processing...");
            assertThat(MDC.get("requestID"), equalTo("master-indy-01"));
        });
    }

    @After
    public void shutdown()
    {
        weld.shutdown();
    }

}
