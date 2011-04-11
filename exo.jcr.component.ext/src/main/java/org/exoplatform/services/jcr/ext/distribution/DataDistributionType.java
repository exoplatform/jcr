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

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * This interface describes a type of distribution.
 * 
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public interface DataDistributionType
{
   /**
    * Retrieves the node from the JCR under the given root node and corresponding to the given
    * data id.
    * @param rootNode the root node under which the data to find is stored
    * @param dataId the id of the data to find
    * @return the Node corresponding to the data to find
    * @throws PathNotFoundException if the data cannot be find
    * @throws RepositoryException if an error occurred while trying to get the expected data
    */
   Node getDataNode(Node rootNode, String dataId) throws PathNotFoundException, RepositoryException;
   
   /**
    * Tries to get the node from the JCR and if it cannot be found, it will create it automatically.
    * @param rootNode the root node under which the data to find is stored
    * @param dataId the id of the data to find/create
    * @return the Node corresponding to the data to find
    * @throws RepositoryException if an error occurred while trying to get or create the expected data
    */
   Node getOrCreateDataNode(Node rootNode, String dataId) throws RepositoryException;
   
   /**
    * Tries to get the node from the JCR and if it cannot be found, it will create it automatically.
    * If the node has to be created, the node will be created with the given node type.
    * @param rootNode the root node under which the data to find is stored
    * @param dataId the id of the data to find/create
    * @param nodeType the node type to use in case we need to create the node
    * @return the Node corresponding to the data to find
    * @throws RepositoryException if an error occurred while trying to get or create the expected data
    */
   Node getOrCreateDataNode(Node rootNode, String dataId, String nodeType) throws RepositoryException;
   
   /**
    * Tries to get the node from the JCR and if it cannot be found, it will create it automatically.
    * If the node has to be created, the node will be created with the given node type and given
    * mixin types.
    * @param rootNode the root node under which the data to find is stored
    * @param dataId the id of the data to find/create
    * @param nodeType the node type to use in case we need to create the node
    * @param mixinTypes the mixin types to use in case we need to create the node
    * @return the Node corresponding to the data to find
    * @throws RepositoryException if an error occurred while trying to get or create the expected data
    */
   Node getOrCreateDataNode(Node rootNode, String dataId, String nodeType, List<String> mixinTypes) throws RepositoryException;
   
   /**
    * Tries to get the node from the JCR and if it cannot be found, it will create it automatically.
    * If the node has to be created, the node will be created with the given node type, given
    * mixin types and given permissions.
    * @param rootNode the root node under which the data to find is stored
    * @param dataId the id of the data to find/create
    * @param nodeType the node type to use in case we need to create the node
    * @param mixinTypes the mixin types to use in case we need to create the node
    * @param permissions the permissions to use in case we need to create the node
    * @return the Node corresponding to the data to find
    * @throws RepositoryException if an error occurred while trying to get or create the expected data
    */
   Node getOrCreateDataNode(Node rootNode, String dataId, String nodeType, List<String> mixinTypes, Map<String, String[]> permissions) throws RepositoryException;
   
   /**
    * Remove the node from the JCR if it exists
    * @param rootNode the root node under which the data to remove is stored
    * @param dataId the id of the data to remove
    * @throws RepositoryException if an error occurred while trying to remove the expected data
    */
   void removeDataNode(Node rootNode, String dataId) throws RepositoryException;
}
