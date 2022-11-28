/**
 * Copyright (C) 2013-2022 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.cdi.util.weft.config;

import java.util.Set;

public interface WeftConfig
{

    boolean isEnabled();

    boolean isEnabled( String poolName );

    int getThreads( String poolName );

    int getPriority( String poolName );

    float getMaxLoadFactor( String poolName );

    int getThreads( String poolName, Integer defaultThreads );

    int getPriority( String poolName, Integer defaultPriority );

    float getMaxLoadFactor( String poolName, Float defaultMax );

    int getDefaultThreads();

    int getDefaultPriority();

    float getDefaultMaxLoadFactor();

    boolean isLoadSensitive( String poolName, Boolean defaultLoadSensitive );

    boolean isDefaultLoadSensitive();

    String getNodePrefix(); // for cluster env

    Set<String> getKnownPools();
}
