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

import org.exoplatform.services.jcr.access.AccessControlPolicy;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeTypeDataPersister;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.UpdateNodeTypeObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public abstract class AbstractNodeTypeRepository implements NodeTypeRepository
{
   protected final NodeTypeDataPersister nodeTypeDataPersister;

   /**
    * @param nodeTypeDataPersister
    */
   public AbstractNodeTypeRepository(NodeTypeDataPersister nodeTypeDataPersister)
   {
      super();
      this.nodeTypeDataPersister = nodeTypeDataPersister;
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeRepository#getAllNodeTypes()
    */
   public List<NodeTypeData> getAllNodeTypes() throws RepositoryException
   {
      return nodeTypeDataPersister.getAllNodeTypes();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isStorageFilled()
   {
      return nodeTypeDataPersister.isStorageFilled();
   }

   /**
    * {@inheritDoc}
    */
   public void registerNodeType(final List<NodeTypeData> nodeTypes, final NodeTypeDataManager nodeTypeDataManager,
      final String accessControlPolicy, final int alreadyExistsBehaviour) throws RepositoryException
   {
      // create map to speedUp registration process
      final Map<InternalQName, NodeTypeData> volatileNodeTypes = new HashMap<InternalQName, NodeTypeData>();
      for (final NodeTypeData nodeTypeData : nodeTypes)
      {
         volatileNodeTypes.put(nodeTypeData.getName(), nodeTypeData);
      }
      final List<NodeTypeData> wait4RegistrationNodeTypes = new ArrayList<NodeTypeData>();

      // persist changes
      if (this.nodeTypeDataPersister != null)
      {
         this.nodeTypeDataPersister.update(nodeTypes, new UpdateNodeTypeObserver()
         {

            public void afterUpdate(final NodeTypeData updatetNodetype, final Object context)
            {

            }

            public void beforeUpdate(final NodeTypeData updatetNodetype, final Object context)
               throws RepositoryException
            {
               if (updatetNodetype == null)
               {
                  throw new RepositoryException("NodeTypeData object " + updatetNodetype + " is null");
               }

               if (accessControlPolicy.equals(AccessControlPolicy.DISABLE)
                  && updatetNodetype.getName().equals("exo:privilegeable"))
               {
                  throw new RepositoryException("NodeType exo:privilegeable is DISABLED");
               }

               final InternalQName qname = updatetNodetype.getName();
               if (qname == null)
               {
                  throw new RepositoryException("NodeType implementation class " + updatetNodetype.getClass().getName()
                     + " is not supported in this method");
               }

               final NodeTypeData registeredNodeType = getNodeType(qname);
               if (registeredNodeType != null)
               {
                  switch (alreadyExistsBehaviour)
                  {
                     case ExtendedNodeTypeManager.FAIL_IF_EXISTS :
                        throw new NodeTypeExistsException("NodeType " + updatetNodetype.getName()
                           + " is already registered");
                     case ExtendedNodeTypeManager.REPLACE_IF_EXISTS :
                        ((PlainChangesLog)context).addAll(nodeTypeDataManager.updateNodeType(registeredNodeType,
                           updatetNodetype, volatileNodeTypes).getAllStates());
                        break;
                  }
               }
               else
               {
                  wait4RegistrationNodeTypes.add(updatetNodetype);
               }
            }

            public boolean shouldSkip(final NodeTypeData updatetNodetype, final Object context)
               throws RepositoryException
            {
               final NodeTypeData registeredNodeType = getNodeType(updatetNodetype.getName());
               if (registeredNodeType != null && alreadyExistsBehaviour == ExtendedNodeTypeManager.IGNORE_IF_EXISTS)
               {
                  return true;

               }
               return false;
            }
         });
      }
      // register new node types
      for (final NodeTypeData nodeTypeData : wait4RegistrationNodeTypes)
      {
         addNodeType(nodeTypeData, volatileNodeTypes);
      }
   }

   /**
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {

   }

   /**
    * @see org.picocontainer.Startable#stop()
    */
   public void stop()
   {

   }
}
