/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import org.exoplatform.services.jcr.core.ComponentPersister;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.datamodel.InternalQName;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public interface NodeTypeDataPersister extends ComponentPersister
{
   /**
    * Add new node type
    * 
    * @param nodeType
    * @throws RepositoryException
    */
   public void addNodeType(NodeTypeData nodeType) throws RepositoryException;

   /**
    * Check node type
    * 
    * @param nodeTypeName
    * @return
    * @throws RepositoryException
    */
   public boolean hasNodeType(InternalQName nodeTypeName) throws RepositoryException;

   /**
    * Write node types to stream
    * 
    * @param nodeTypes
    * @throws RepositoryException
    */
   public void addNodeTypes(List<NodeTypeData> nodeTypes) throws RepositoryException;

   /**
    * Remove node type
    * 
    * @param nodeType
    * @throws RepositoryException
    */
   public void removeNodeType(NodeTypeData nodeType) throws RepositoryException;

   /**
    * Read node types.
    * 
    * @return
    * @throws RepositoryException
    */
   public List<NodeTypeData> getAllNodeTypes() throws RepositoryException;

   /**
    * Read node types.
    * 
    * @param nodeTypeName
    * @return
    * @throws RepositoryException
    */
   public NodeTypeData getNodeType(InternalQName nodeTypeName) throws RepositoryException;

   /**
   * Write node types to stream
   * 
   * @param nodeTypes
   * @param observer
   * @throws RepositoryException
   */
   public void update(List<NodeTypeData> nodeTypes, UpdateNodeTypeObserver observer) throws RepositoryException;

}
