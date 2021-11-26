/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.ext.distribution;


/**
 * This service is used to distribute smartly the data over the JCR. It can help you to have the best
 * possible performances in read and write accesses, especially if you have a lot of nodes of the
 * same type to store, instead of storing them yourself under the same parent node (which can 
 * affect the performances if you have a lot of nodes to store), we simply delegate data access and 
 * storage to the {@link DataDistributionType} corresponding to the expected mode that will store the
 *  data for you in an optimized and reliable way.
 * 
 * See below an example of how it can be used
 * <pre>
 * // Get the data distribution corresponding to the readable mode
 * DataDistributionType type = manager.getDataDistributionType(DataDistributionMode.READABLE);
 * // Get or create the node corresponding to "john.smith"
 * Node node = type.getOrCreateDataNode(parentNode, "john.smith");
 * </pre>
 * 
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public interface DataDistributionManager
{
   /**
    * Retrieves the data distribution type corresponding to given mode.
    * @param mode the mode of distribution to use
    * @return the expected mode if it exists <code>null</code> otherwise.
    */
   DataDistributionType getDataDistributionType(DataDistributionMode mode);
}
