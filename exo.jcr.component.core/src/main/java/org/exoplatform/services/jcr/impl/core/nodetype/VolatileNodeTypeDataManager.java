/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class VolatileNodeTypeDataManager extends NodeTypeDataManagerImpl
{

   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.VolatileNodeTypeDataManager");

   public VolatileNodeTypeDataManager(final NodeTypeDataManagerImpl nodeTypeDataManagerImpl) throws RepositoryException
   {
      super(nodeTypeDataManagerImpl.accessControlPolicy, nodeTypeDataManagerImpl.locationFactory,
         nodeTypeDataManagerImpl.namespaceRegistry,
         null, // to be sure
         nodeTypeDataManagerImpl.dataManager, nodeTypeDataManagerImpl.indexSearcherHolder,
         nodeTypeDataManagerImpl.nodeTypeRepository.createCopy(), nodeTypeDataManagerImpl.cleanerHolder);
   }

   public void registerVolatileNodeTypes(final Collection<NodeTypeData> volatileNodeTypes) throws RepositoryException
   {
      final Map<InternalQName, NodeTypeData> map = new HashMap<InternalQName, NodeTypeData>();
      for (final NodeTypeData nodeTypeData : volatileNodeTypes)
      {
         map.put(nodeTypeData.getName(), nodeTypeData);
      }
      registerVolatileNodeTypes(map);
   }

   public void registerVolatileNodeTypes(final Map<InternalQName, NodeTypeData> volatileNodeTypes)
      throws RepositoryException
   {
      for (final Map.Entry<InternalQName, NodeTypeData> entry : volatileNodeTypes.entrySet())
      {
         if (getNodeType(entry.getKey()) != null)
         {
            this.nodeTypeRepository.removeNodeType(entry.getValue());
         }
         this.nodeTypeRepository.addNodeType(entry.getValue(), volatileNodeTypes);
      }
   }

}
