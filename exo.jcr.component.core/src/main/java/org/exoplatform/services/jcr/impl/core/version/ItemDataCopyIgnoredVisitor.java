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
package org.exoplatform.services.jcr.impl.core.version;

import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.dataflow.DefaultItemDataCopyVisitor;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;

/**
 * Created by The eXo Platform SAS 14.12.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: ItemDataCopyIgnoredVisitor.java 12306 2008-03-24 10:25:55Z ksm
 *          $
 */
public class ItemDataCopyIgnoredVisitor extends DefaultItemDataCopyVisitor
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ItemDataCopyIgnoredVisitor");

   protected final SessionChangesLog restoredChanges;

   public ItemDataCopyIgnoredVisitor(NodeData context, InternalQName destNodeName, NodeTypeDataManager nodeTypeManager,
      SessionDataManager dataManager, SessionChangesLog changes)
   {
      super(context, destNodeName, nodeTypeManager, dataManager, dataManager, true);
      this.restoredChanges = changes;
   }

   @Override
   protected void entering(PropertyData property, int level) throws RepositoryException
   {

      if (level == 1
         && (property.getQPath().getName().equals(Constants.JCR_BASEVERSION) || property.getQPath().getName().equals(
            Constants.JCR_ISCHECKEDOUT)))
      {
         // skip versionable specific props
         return;
      }

      if (curParent() == null)
      {
         NodeData existedParent = (NodeData)dataManager.getItemData(property.getParentIdentifier());

         PropertyDefinitionData pdef =
            ntManager.getPropertyDefinitions(property.getQPath().getName(), existedParent.getPrimaryTypeName(),
               existedParent.getMixinTypeNames()).getAnyDefinition();

         if (pdef.getOnParentVersion() == OnParentVersionAction.IGNORE)
         {
            // parent is not exists as this copy context current parent
            // i.e. it's a IGNOREd property elsewhere at a versionable node
            // descendant.
            // So, we have to know that this parent WILL exists after restore
            ItemState contextState = restoredChanges.getItemState(property.getParentIdentifier());
            if (contextState != null && !contextState.isDeleted())
            {
               // the node can be stored as IGNOREd in restore set, check an action

               if (LOG.isDebugEnabled())
                  LOG.debug("A property " + property.getQPath().getAsString() + " is IGNOREd");

               // set context current parent to existed in restore set
               parents.push((NodeData)contextState.getData());
               super.entering(property, level);
               parents.pop();
            }
         }
      }
      else
      {
         // copy as IGNOREd parent child, i.e. OnParentVersionAction is any
         if (LOG.isDebugEnabled())
         {
            LOG.debug("A property " + property.getQPath().getAsString() + " is IGNOREd node descendant");
         }
         super.entering(property, level);
      }
   }

   @Override
   protected void entering(NodeData node, int level) throws RepositoryException
   {

      if (level == 0)
      {
         parents.pop(); // remove context parent (redo superclass constructor
         // work)
      }
      else if (level > 0)
      {
         if (curParent() == null)
         {
            NodeData existedParent = (NodeData)dataManager.getItemData(node.getParentIdentifier());
            NodeDefinitionData ndef =
               ntManager.getChildNodeDefinition(node.getQPath().getName(), node.getPrimaryTypeName(),
                  existedParent.getPrimaryTypeName(), existedParent.getMixinTypeNames());

            // the node can be stored as IGNOREd in restore set, check an action
            if (ndef.getOnParentVersion() == OnParentVersionAction.IGNORE)
            {
               // parent is not exists as this copy context current parent
               // i.e. it's a IGNOREd node elsewhere at a versionable node
               // descendant.
               // So, we have to know that this parent WILL exists after restore
               ItemState contextState = restoredChanges.getItemState(node.getParentIdentifier());
               if (contextState != null && !contextState.isDeleted())
               {
                  if (LOG.isDebugEnabled())
                     LOG.debug("A node " + node.getQPath().getAsString() + " is IGNOREd");

                  // set context current parent to existed in restore set
                  parents.push((NodeData)contextState.getData());
                  super.entering(node, level);
                  NodeData thisNode = parents.pop(); // copied
                  parents.pop(); // contextParent
                  parents.push(thisNode);
                  return;
               }
            }
         }
         else
         {
            // copy as IGNOREd parent child, i.e. OnParentVersionAction is any
            if (LOG.isDebugEnabled())
            {
               LOG.debug("A node " + node.getQPath().getAsString() + " is IGNOREd node descendant");
            }

            super.entering(node, level);
            return;
         }
      }

      parents.push(null); // skip this node as we hasn't parent in restore result
   }

   @Override
   protected void leaving(NodeData node, int level) throws RepositoryException
   {
      if (parents.size() > 0)
      {
         parents.pop();
      }
   }
}
