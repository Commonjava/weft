package org.commonjava.cdi.util.weft.config;

public interface WeftConfig
{

    int getThreads( String poolName );

    int getPriority( String poolName );

    int getThreads( String poolName, Integer defaultThreads );

    int getPriority( String poolName, Integer defaultPriority );

    int getDefaultThreads();

    int getDefaultPriority();

}
