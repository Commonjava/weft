package org.commonjava.cdi.util.weft.exception;

public class PoolOverloadException
                extends RuntimeException
{
    final String poolName;

    final double loadFactor;

    final int currentLoad;

    final int threadCount;

    public PoolOverloadException( String poolName, double loadFactor, int currentLoad, int threadCount )
    {

        this.poolName = poolName;
        this.loadFactor = loadFactor;
        this.currentLoad = currentLoad;
        this.threadCount = threadCount;
    }

    @Override
    public String toString()
    {
        return "PoolOverloadException{" + "poolName='" + poolName + '\'' + ", loadFactor=" + loadFactor
                        + ", currentLoad=" + currentLoad + ", threadCount=" + threadCount + '}';
    }
}
