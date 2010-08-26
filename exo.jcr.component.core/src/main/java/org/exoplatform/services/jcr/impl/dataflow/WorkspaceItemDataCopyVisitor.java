/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;

import javax.jcr.RepositoryException;

/**
 * This ItemData copy visitor uses two data managers:
 * <UL>
 * <LI>srcDataManager - used for service data that comes from source workspace (visit all items on 
 * source workspace);
 * <LI>destDataManager - used for adding data to destination workspace (used to check like 
 * DefaultItemDataVisitor.calculateNewNodePath(), etc);
 * </UL>
 * 
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: WorkspaceItemDataCopyVisitor.java 111 2010-26-08 11:11:11Z serg $
 */
public class WorkspaceItemDataCopyVisitor extends ItemDataCopyVisitor
{
   private SessionDataManager srcDataManager;

   /**
    * Constructor.
    * 
    * @param parent - parent node, where copied item tree must be saved;
    * @param destNodeName - copied nodes destination name;
    * @param nodeTypeManager - node type manager;
    * @param srcDataManager - used for service data that comes from source workspace (visit all items
    *  on source workspace)
    * @param destDataManager - used for adding data to destination workspace (used to check like
    *  DefaultItemDataVisitor.calculateNewNodePath(), etc);
    * @param keepIdentifiers - should we keep items identifiers or not;
    */
   public WorkspaceItemDataCopyVisitor(NodeData parent, InternalQName destNodeName,
      NodeTypeDataManager nodeTypeManager, SessionDataManager srcDataManager, SessionDataManager destDataManager,
      boolean keepIdentifiers)
   {
      super(parent, destNodeName, nodeTypeManager, destDataManager, keepIdentifiers);
      this.srcDataManager = srcDataManager;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void visit(NodeData node) throws RepositoryException
   {
      try
      {
         entering(node, currentLevel);
         if (maxLevel == -1 || currentLevel < maxLevel)
         {
            currentLevel++;
            for (PropertyData data : srcDataManager.getChildPropertiesData(node))
               data.accept(this);
            for (NodeData data : srcDataManager.getChildNodesData(node))
               data.accept(this);
            currentLevel--;
         }
         leaving(node, currentLevel);
      }
      catch (RepositoryException re)
      {
         currentLevel = 0;
         throw re;
      }

   }
}
