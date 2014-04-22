/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
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
