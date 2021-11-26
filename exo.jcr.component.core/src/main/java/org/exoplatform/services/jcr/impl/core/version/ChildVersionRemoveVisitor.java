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

package org.exoplatform.services.jcr.impl.core.version;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemDataTraversingVisitor;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Stack;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS 22.06.2007 Traverse through all versions in
 * the version history and check if visited child histories isn't used in
 * repository. If the child version isn't used it will be removed immediately.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: ChildVersionRemoveVisitor.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class ChildVersionRemoveVisitor extends ItemDataTraversingVisitor
{

   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.ChildVersionRemoveVisitor");

   protected final Stack<NodeData> parents = new Stack<NodeData>();

   protected final NodeTypeDataManager nodeTypeDataManager;

   protected final QPath ancestorToSave;

   protected final QPath containingHistory;

   public ChildVersionRemoveVisitor(ItemDataConsumer dataManager, NodeTypeDataManager nodeTypeDataManager,
      QPath containingHistory, QPath ancestorToSave)
   {
      super(dataManager);

      this.ancestorToSave = ancestorToSave;
      this.containingHistory = containingHistory;
      this.nodeTypeDataManager = nodeTypeDataManager;
   }

   protected SessionDataManager dataManager()
   {
      return (SessionDataManager)dataManager;
   }

   @Override
   protected void entering(PropertyData property, int level) throws RepositoryException
   {
      if (property.getQPath().getName().equals(Constants.JCR_CHILDVERSIONHISTORY)
         && nodeTypeDataManager.isNodeType(Constants.NT_VERSIONEDCHILD, parents.peek().getPrimaryTypeName(), parents
            .peek().getMixinTypeNames()))
      {
         // check and remove child VH
         String vhID = ValueDataUtil.getString(property.getValues().get(0));

         dataManager().removeVersionHistory(vhID, containingHistory, ancestorToSave);
      }
   }

   @Override
   protected void entering(NodeData node, int level) throws RepositoryException
   {
      parents.push(node);
   }

   @Override
   protected void leaving(PropertyData property, int level) throws RepositoryException
   {
   }

   @Override
   protected void leaving(NodeData node, int level) throws RepositoryException
   {
      parents.pop();
   }

}
