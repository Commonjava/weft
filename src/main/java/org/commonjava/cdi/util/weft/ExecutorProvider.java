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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;

@ApplicationScoped
public class ExecutorProvider
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private WeftPoolBoy poolBoy = new WeftPoolBoy();

    @Produces
    @WeftManaged
    public WeftExecutorService getExecutorService( final InjectionPoint ip )
    {
        return getExec( ip, false );
    }

    @Produces
    @WeftScheduledExecutor
    public ScheduledExecutorService getScheduledExecutorService( final InjectionPoint ip )
    {
        return (ScheduledExecutorService) getExec( ip, true );
    }

    private WeftExecutorService getExec( final InjectionPoint ip, final boolean scheduled )
    {
        final ExecutorConfig ec = ip.getAnnotated()
                                    .getAnnotation( ExecutorConfig.class );

        return poolBoy.getPool( ec, scheduled );
    }

}
