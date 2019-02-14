package org.commonjava.cdi.util.weft;

import java.util.concurrent.ExecutorService;

public interface WeftExecutorService
        extends ExecutorService
{
    String getName();

    boolean isHealthy();

    double getLoadFactor();

    long getCurrentLoad();

    Integer getThreadCount();

    int getCorePoolSize();

    int getMaximumPoolSize();

    int getActiveCount();

    long getTaskCount();
}
