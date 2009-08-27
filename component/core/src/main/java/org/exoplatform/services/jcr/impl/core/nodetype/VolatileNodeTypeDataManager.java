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
package org.exoplatform.services.jcr.impl.core.nodetype;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class VolatileNodeTypeDataManager
   extends NodeTypeDataManagerImpl
{

   public VolatileNodeTypeDataManager(NodeTypeDataManagerImpl nodeTypeDataManagerImpl) throws RepositoryException
   {
      super(nodeTypeDataManagerImpl.accessControlPolicy, nodeTypeDataManagerImpl.locationFactory,
               nodeTypeDataManagerImpl.namespaceRegistry, nodeTypeDataManagerImpl.persister);
      this.superNodeTypeDataManager = nodeTypeDataManagerImpl;
      this.queryHandlers = new HashSet<QueryHandler>(nodeTypeDataManagerImpl.queryHandlers);

      registerVolatileNodeTypes(superNodeTypeDataManager.getAllNodeTypes());
   }

   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger(VolatileNodeTypeDataManager.class);

   private final NodeTypeDataManagerImpl superNodeTypeDataManager;

   public void registerVolatileNodeTypes(Collection<NodeTypeData> volatileNodeTypes) throws RepositoryException
   {
      Map<InternalQName, NodeTypeData> map = new HashMap<InternalQName, NodeTypeData>();
      for (NodeTypeData nodeTypeData : volatileNodeTypes)
      {
         map.put(nodeTypeData.getName(), nodeTypeData);
      }
      registerVolatileNodeTypes(map);
   }

   public void registerVolatileNodeTypes(Map<InternalQName, NodeTypeData> volatileNodeTypes) throws RepositoryException
   {
      for (Map.Entry<InternalQName, NodeTypeData> entry : volatileNodeTypes.entrySet())
      {
         if (findNodeType(entry.getKey()) != null)
            internalUnregister(entry.getKey(), entry.getValue());
         internalRegister(entry.getValue(), volatileNodeTypes);
      }
   }

}
