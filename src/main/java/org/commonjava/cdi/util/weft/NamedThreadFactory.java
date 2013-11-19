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
