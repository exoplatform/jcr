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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.dataflow.version.VersionHistoryDataHelper;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

public class ItemDataCopyVisitor extends DefaultItemDataCopyVisitor
{

   private Log log = ExoLogger.getLogger("jcr.ItemDataCopyVisitor");

   public ItemDataCopyVisitor(NodeData parent, InternalQName destNodeName, NodeTypeDataManager nodeTypeManager,
      SessionDataManager dataManager, boolean keepIdentifiers)
   {
      super(parent, destNodeName, nodeTypeManager, dataManager, keepIdentifiers);
   }

   @Override
   protected void entering(PropertyData property, int level) throws RepositoryException
   {

      if (log.isDebugEnabled())
         log.debug("entering " + property.getQPath().getAsString());

      // don't using super
      InternalQName qname = property.getQPath().getName();

      List<ValueData> values;

      if (ntManager.isNodeType(Constants.MIX_VERSIONABLE, curParent().getPrimaryTypeName(), curParent()
         .getMixinTypeNames()))
      {
         // versionable node copy
         // [mix:versionable] > mix:referenceable
         // mixin
         // - jcr:versionHistory (reference) mandatory protected
         // < 'nt:versionHistory'
         // - jcr:baseVersion (reference) mandatory protected
         // ignore
         // < 'nt:version'
         // - jcr:isCheckedOut (boolean) = 'true' mandatory
         // autocreated protected ignore
         // - jcr:predecessors (reference) mandatory protected
         // multiple
         // < 'nt:version'
         // - jcr:mergeFailed (reference) protected multiple abort
         // < 'nt:version'

         // before manipulate with version stuuf we have create a one new VH right
         // here!
         QPath vhpPath = QPath.makeChildPath(curParent().getQPath(), Constants.JCR_VERSIONHISTORY);
         ItemState vhpState = findLastItemState(vhpPath);
         if (vhpState == null)
         {
            // need create a new VH
            PlainChangesLogImpl changes = new PlainChangesLogImpl();
            VersionHistoryDataHelper vh = new VersionHistoryDataHelper(curParent(), changes, dataManager, ntManager);
            itemAddStates.addAll(changes.getAllStates());
         }

         values = new ArrayList<ValueData>(1);
         if (qname.equals(Constants.JCR_LOCKISDEEP))
         {
            return;
         }
         else if (qname.equals(Constants.JCR_LOCKOWNER))
         {
            return;
         }
         else if (qname.equals(Constants.JCR_VERSIONHISTORY))
         {
            return; // added in VH create
         }
         else if (qname.equals(Constants.JCR_PREDECESSORS))
         {
            return; // added in VH create
         }
         else if (qname.equals(Constants.JCR_BASEVERSION))
         {
            return; // added in VH create
         }
         else if (qname.equals(Constants.JCR_ISCHECKEDOUT))
         {
            values.add(new TransientValueData(true));
         }
         else if (qname.equals(Constants.JCR_MERGEFAILED))
         {
            return; // skip it
         }
         else if (qname.equals(Constants.JCR_UUID))
         {
            values.add(new TransientValueData(curParent().getIdentifier())); // uuid
            // of
            // the
            // parent
         }
         else
         {
            values = property.getValues(); // copy the property
         }
      }
      else if (ntManager.isNodeType(Constants.MIX_REFERENCEABLE, curParent().getPrimaryTypeName(), curParent()
         .getMixinTypeNames())
         && qname.equals(Constants.JCR_UUID))
      {

         values = new ArrayList<ValueData>(1);
         values.add(new TransientValueData(curParent().getIdentifier()));
      }
      else
      {
         // http://jira.exoplatform.org/browse/JCR-294
         if (qname.equals(Constants.JCR_LOCKISDEEP))
         {
            return;
         }
         else if (qname.equals(Constants.JCR_LOCKOWNER))
         {
            return;
         }
         values = property.getValues();
      }

      TransientPropertyData newProperty =
         new TransientPropertyData(QPath.makeChildPath(curParent().getQPath(), qname), keepIdentifiers ? property
            .getIdentifier() : IdGenerator.generate(), -1, property.getType(), curParent().getIdentifier(), property
            .isMultiValued());

      newProperty.setValues(values);

      if (log.isDebugEnabled())
         log.debug("entering COPY " + newProperty.getQPath().getAsString() + "; pidentifier: "
            + newProperty.getParentIdentifier() + "; identifier: " + newProperty.getIdentifier());

      itemAddStates.add(new ItemState(newProperty, ItemState.ADDED, true, ancestorToSave, level != 0));
   }
}
