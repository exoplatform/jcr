/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.ext.distribution;

/**
 * The existing data distribution modes are the following:
 * <ul>
 *    <li>Readable mode that is used to store the data in an understandable way for a human
 *    being, corresponding to <code>DataDistributionMode.READABLE</code>.</li>
 *    <li>Optimized mode that is used when we don't need to be able to understand
 *    how it is stored, we only want to have the best possible performances, 
 *    corresponding to <code>DataDistributionMode.OPTIMIZED</code>.</li>
 *    <li>None mode that is used when we just want to store the node
 *    as it is without using any distribution algorithm. This is mostly
 *    interesting as helper to create a hierarchy of nodes in a reliable way
 *    when we already know that we have only few nodes to store, 
 *    corresponding to <code>DataDistributionMode.NONE</code>.</li>
 * </ul>
 * 
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public enum DataDistributionMode 
{
   READABLE, OPTIMIZED, NONE
}
