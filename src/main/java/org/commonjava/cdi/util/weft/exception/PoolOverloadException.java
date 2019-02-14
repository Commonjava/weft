package org.commonjava.cdi.util.weft.exception;

public class PoolOverloadException
                extends RuntimeException
{
    private final String poolName;

    private final double loadFactor;

    private final double currentLoad;

    private Float maxLoadFactor;

    private final int threadCount;

    public PoolOverloadException( String poolName, double loadFactor, double currentLoad, final Float maxLoadFactor, int threadCount )
    {
        this.poolName = poolName;
        this.loadFactor = loadFactor;
        this.currentLoad = currentLoad;
        this.maxLoadFactor = maxLoadFactor;
        this.threadCount = threadCount;
    }

    public String getPoolName()
    {
        return poolName;
    }

    public double getLoadFactor()
    {
        return loadFactor;
    }

    public Float getMaxLoadFactor()
    {
        return maxLoadFactor;
    }

    public double getCurrentLoad()
    {
        return currentLoad;
    }

    public int getThreadCount()
    {
        return threadCount;
    }

    @Override
    public String getMessage()
    {
        return "PoolOverloadException{" + "poolName='" + poolName + '\'' + ", loadFactor=" + loadFactor
                        + ", currentLoad=" + currentLoad + ", threadCount=" + threadCount + '}';
    }
}
