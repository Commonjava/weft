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
