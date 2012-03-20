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

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.value.BaseValue;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.version.VersionHistoryDataHelper;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class ItemAutocreator
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ItemAutocreator");

   private final NodeTypeDataManager nodeTypeDataManager;

   private final ItemDataConsumer dataConsumer;

   private final ValueFactory valueFactory;

   private final boolean avoidCheckExistedChildItems;

   /**
    * @param nodeTypeDataManager
    */
   public ItemAutocreator(NodeTypeDataManager nodeTypeDataManager, ValueFactory valueFactory,
      ItemDataConsumer dataConsumer, boolean avoidCheckExistedChildItems)
   {
      super();
      this.nodeTypeDataManager = nodeTypeDataManager;
      this.valueFactory = valueFactory;
      this.dataConsumer = dataConsumer;
      this.avoidCheckExistedChildItems = avoidCheckExistedChildItems;
   }

   public PlainChangesLog makeAutoCreatedItems(final NodeData parent, final InternalQName nodeTypeName,
      final ItemDataConsumer targetDataManager, final String owner) throws RepositoryException
   {
      final PlainChangesLogImpl changes = new PlainChangesLogImpl();
      final NodeTypeData type = nodeTypeDataManager.getNodeType(nodeTypeName);

      changes.addAll(makeAutoCreatedProperties(parent, nodeTypeName,
         nodeTypeDataManager.getAllPropertyDefinitions(nodeTypeName), targetDataManager, owner).getAllStates());
      // Add autocreated child nodes
      changes.addAll(makeAutoCreatedNodes(parent, nodeTypeName,
         nodeTypeDataManager.getAllChildNodeDefinitions(nodeTypeName), targetDataManager, owner).getAllStates());

      // versionable
      if (nodeTypeDataManager.isNodeType(Constants.MIX_VERSIONABLE, new InternalQName[]{type.getName()}))
      {

         // using VH helper as for one new VH, all changes in changes log
         changes.addAll(makeMixVesionableChanges(parent).getAllStates());
      }
      return changes;
   }

   public PlainChangesLog makeAutoCreatedNodes(final NodeData parent, final InternalQName typeName,
      final NodeDefinitionData[] nodeDefs, final ItemDataConsumer targetDataManager, final String owner)
      throws RepositoryException
   {
      final PlainChangesLogImpl changes = new PlainChangesLogImpl();
      final Set<InternalQName> addedNodes = new HashSet<InternalQName>();
      for (final NodeDefinitionData ndef : nodeDefs)
      {
         if (ndef.isAutoCreated())
         {
            final ItemData pdata =
               avoidCheckExistedChildItems ? null : targetDataManager.getItemData(parent, new QPathEntry(
                  ndef.getName(), 0), ItemType.NODE, false);
            if (pdata == null && !addedNodes.contains(ndef.getName()) || pdata != null && !pdata.isNode())
            {

               final TransientNodeData childNodeData =
                  TransientNodeData.createNodeData(parent, ndef.getName(), ndef.getDefaultPrimaryType(),
                     IdGenerator.generate());
               changes.add(ItemState.createAddedState(childNodeData, false));
               changes.addAll(makeAutoCreatedItems(childNodeData, childNodeData.getPrimaryTypeName(),
                  targetDataManager, owner).getAllStates());
               addedNodes.add(ndef.getName());
            }
            else
            {
               if (this.LOG.isDebugEnabled())
               {
                  this.LOG.debug("Skipping existed node " + ndef.getName() + " in " + parent.getQPath().getAsString()
                     + "   during the automatic creation of items for " + typeName.getAsString()
                     + " nodetype or mixin type");
               }
            }
         }
      }
      return changes;

   }

   public PlainChangesLog makeAutoCreatedProperties(final NodeData parent, final InternalQName typeName,
      final PropertyDefinitionData[] propDefs, final ItemDataConsumer targetDataManager, final String owner)
      throws RepositoryException
   {
      final PlainChangesLogImpl changes = new PlainChangesLogImpl();

      final Set<InternalQName> addedProperties = new HashSet<InternalQName>();

      // Add autocreated child properties

      for (final PropertyDefinitionData pdef : propDefs)
      {

         if (pdef.isAutoCreated())
         {

            final ItemData pdata =
               avoidCheckExistedChildItems ? null : targetDataManager.getItemData(parent, new QPathEntry(
                  pdef.getName(), 0), ItemType.PROPERTY, false);
            if (pdata == null && !addedProperties.contains(pdef.getName()) || pdata != null && pdata.isNode())
            {

               final List<ValueData> listAutoCreateValue = autoCreatedValue(parent, typeName, pdef, owner);

               if (listAutoCreateValue != null)
               {
                  final TransientPropertyData propertyData =
                     TransientPropertyData.createPropertyData(parent, pdef.getName(), pdef.getRequiredType(),
                        pdef.isMultiple(), listAutoCreateValue);
                  changes.add(ItemState.createAddedState(propertyData));
                  addedProperties.add(pdef.getName());
               }
            }
            else
            {
               if (this.LOG.isDebugEnabled())
               {
                  this.LOG.debug("Skipping existed property " + pdef.getName() + " in "
                     + parent.getQPath().getAsString() + "   during the automatic creation of items for "
                     + typeName.getAsString() + " nodetype or mixin type");
               }
            }
         }
      }
      return changes;
   }

   /**
    * @param parent
    * @param dataManager
    * @param changes
    * @return 
    * @throws RepositoryException
    */
   public PlainChangesLog makeMixVesionableChanges(final NodeData parent) throws RepositoryException
   {
      PlainChangesLog changesLog = new PlainChangesLogImpl();
      new VersionHistoryDataHelper(parent, changesLog, dataConsumer, nodeTypeDataManager);
      return changesLog;
   }

   protected List<ValueData> autoCreatedValue(final NodeData parent, final InternalQName typeName,
      final PropertyDefinitionData def, final String owner) throws RepositoryException
   {
      final List<ValueData> vals = new ArrayList<ValueData>();

      if (nodeTypeDataManager.isNodeType(Constants.NT_BASE, new InternalQName[]{typeName})
         && def.getName().equals(Constants.JCR_PRIMARYTYPE))
      {
         vals.add(new TransientValueData(parent.getPrimaryTypeName()));

      }
      else if (nodeTypeDataManager.isNodeType(Constants.MIX_REFERENCEABLE, new InternalQName[]{typeName})
         && def.getName().equals(Constants.JCR_UUID))
      {
         vals.add(new TransientValueData(parent.getIdentifier()));

      }
      else if (nodeTypeDataManager.isNodeType(Constants.NT_HIERARCHYNODE, new InternalQName[]{typeName})
         && def.getName().equals(Constants.JCR_CREATED))
      {
         vals.add(new TransientValueData(Calendar.getInstance()));
      }
      else if (nodeTypeDataManager.isNodeType(Constants.NT_HIERARCHYNODE, new InternalQName[]{typeName})
         && def.getName().equals(new InternalQName(Constants.NS_JCR_URI, "createdBy")))
      {
         vals.add(new TransientValueData(owner));

      }
      else if (nodeTypeDataManager.isNodeType(Constants.EXO_OWNEABLE, new InternalQName[]{typeName})
         && def.getName().equals(Constants.EXO_OWNER))
      {
         // String owner = session.getUserID();
         vals.add(new TransientValueData(owner));
      }
      else if (nodeTypeDataManager.isNodeType(Constants.EXO_PRIVILEGEABLE, new InternalQName[]{typeName})
         && def.getName().equals(Constants.EXO_PERMISSIONS))
      {
         for (final AccessControlEntry ace : parent.getACL().getPermissionEntries())
         {
            vals.add(new TransientValueData(ace));
         }

      }
      else
      {
         final String[] propVal = def.getDefaultValues();
         // there can be null in definition but should not be null value
         if (propVal != null && propVal.length != 0)
         {
            for (final String v : propVal)
            {
               if (v != null)
               {
                  if (def.getRequiredType() == PropertyType.UNDEFINED)
                  {
                     vals.add(((BaseValue)this.valueFactory.createValue(v)).getInternalData());
                  }
                  else
                  {
                     vals.add(((BaseValue)this.valueFactory.createValue(v, def.getRequiredType())).getInternalData());
                  }
               }
               else
               {
                  vals.add(null);
               }
            }
         }
         else
         {
            return null;
         }
      }

      return vals;
   }
}
