/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
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

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory
    implements ThreadFactory
{

    private int counter = 0;

    private final ClassLoader ccl;

    private final String name;

    private final boolean daemon;

    private final int priority;

    public NamedThreadFactory( final String name, final boolean daemon, final int priority )
    {
        this.ccl = Thread.currentThread()
                         .getContextClassLoader();
        this.name = name;
        this.daemon = daemon;
        this.priority = priority;
    }

    @Override
    public Thread newThread( final Runnable runnable )
    {
        final Thread t = new Thread( runnable );
        t.setContextClassLoader( ccl );
        t.setName( name + "-" + counter++ );
        t.setDaemon( daemon );
        t.setPriority( priority );

        return t;
    }

}
