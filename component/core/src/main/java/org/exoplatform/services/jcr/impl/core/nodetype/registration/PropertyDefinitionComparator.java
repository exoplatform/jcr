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

import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.value.ValueConstraintsMatcher;
import org.exoplatform.services.jcr.impl.dataflow.AbstractValueData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class PropertyDefinitionComparator extends AbstractDefinitionComparator<PropertyDefinitionData>
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger(PropertyDefinitionComparator.class);

   private final LocationFactory locationFactory;

   /**
    * @param nodeTypeDataManager
    * @param persister
    * @param locationFactory
    */
   public PropertyDefinitionComparator(NodeTypeDataManager nodeTypeDataManager, DataManager persister,
      LocationFactory locationFactory)
   {
      super(nodeTypeDataManager, persister);
      this.locationFactory = locationFactory;
   }

   public PlainChangesLog compare(NodeTypeData registeredNodeType, PropertyDefinitionData[] ancestorDefinition,
      PropertyDefinitionData[] recipientDefinition) throws RepositoryException
   {

      List<PropertyDefinitionData> sameDefinitionData = new ArrayList<PropertyDefinitionData>();

      List<RelatedDefinition<PropertyDefinitionData>> changedDefinitionData =
         new ArrayList<RelatedDefinition<PropertyDefinitionData>>();
      List<PropertyDefinitionData> newDefinitionData = new ArrayList<PropertyDefinitionData>();
      List<PropertyDefinitionData> removedDefinitionData = new ArrayList<PropertyDefinitionData>();
      init(ancestorDefinition, recipientDefinition, sameDefinitionData, changedDefinitionData, newDefinitionData,
         removedDefinitionData);

      // create changes log
      PlainChangesLog changesLog = new PlainChangesLogImpl();

      // DataManager dm = persister.getDataManager();
      // removing properties
      validateRemoved(registeredNodeType, removedDefinitionData, recipientDefinition);

      // new property definition
      validateAdded(registeredNodeType, newDefinitionData, recipientDefinition);

      Set<String> nodes = nodeTypeDataManager.getNodes(registeredNodeType.getName());
      // changed
      validateChanged(registeredNodeType, changedDefinitionData, nodes, recipientDefinition);

      //
      doAdd(newDefinitionData, changesLog, nodes, registeredNodeType);
      return changesLog;

   }

   /**
    * @param registeredNodeType
    * @param recipientDefinitionData
    * @param allRecipientDefinition
    * @throws RepositoryException
    * @throws ConstraintViolationException
    */
   private void checkIsMultiple(NodeTypeData registeredNodeType, PropertyDefinitionData recipientDefinitionData,
      PropertyDefinitionData[] allRecipientDefinition) throws RepositoryException, ConstraintViolationException
   {
      Set<String> nodes2;
      if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
      {
         nodes2 = nodeTypeDataManager.getNodes(registeredNodeType.getName());
      }
      else
      {
         nodes2 =
            nodeTypeDataManager.getNodes(registeredNodeType.getName(), new InternalQName[]{recipientDefinitionData
               .getName()}, new InternalQName[]{});
      }
      for (String uuid : nodes2)
      {
         NodeData nodeData = (NodeData)persister.getItemData(uuid);
         if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
         {
            List<PropertyData> propertyDatas = persister.getChildPropertiesData(nodeData);
            for (PropertyData propertyData : propertyDatas)
            {
               // skip mixin and primary type
               if (isResidualMatch(propertyData.getQPath().getName(), allRecipientDefinition))
               {
                  if (propertyData.getValues().size() > 1)
                  {
                     throw new ConstraintViolationException("Can't change property definition "
                        + recipientDefinitionData.getName().getAsString() + " to isMultiple = false because property "
                        + propertyData.getQPath().getAsString() + " contains more then one value");
                  }
               }
            }
         }
         else
         {
            PropertyData propertyData =
               (PropertyData)persister.getItemData(nodeData, new QPathEntry(recipientDefinitionData.getName(), 0));
            if (propertyData.getValues().size() > 1)
            {
               throw new ConstraintViolationException("Can't change property definition "
                  + recipientDefinitionData.getName().getAsString() + " to isMultiple = false because property "
                  + propertyData.getQPath().getAsString() + " contains more then one value");
            }

         }
      }
   }

   /**
    * @param registeredNodeType
    * @param nodes
    * @param recipientDefinitionData
    * @throws RepositoryException
    */
   private void checkMandatory(NodeTypeData registeredNodeType, Set<String> nodes,
      PropertyDefinitionData recipientDefinitionData) throws RepositoryException
   {
      if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()) && recipientDefinitionData.isMandatory())
         throw new ConstraintViolationException("Invalid property definition " + recipientDefinitionData.getName()
            + ". Residual definition can't be mandatory");
      Set<String> nodes2 =
         nodeTypeDataManager.getNodes(registeredNodeType.getName(), new InternalQName[]{},
            new InternalQName[]{recipientDefinitionData.getName()});
      if (nodes2.size() > 0)
      {
         String message =
            "Can not change " + recipientDefinitionData.getName().getAsString()
               + " property definition from mandatory=false to mandatory = true , because " + " the following nodes ";
         for (String uuids : nodes)
         {
            message += uuids + " ";
         }
         message += "  doesn't have these properties ";

         throw new ConstraintViolationException(message);
      }
   }

   /**
    * @param registeredNodeType
    * @param nodes
    * @param recipientDefinitionData
    * @throws RepositoryException
    */
   private void checkProtected(NodeTypeData registeredNodeType, Set<String> nodes,
      PropertyDefinitionData recipientDefinitionData) throws RepositoryException
   {
      if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()) && recipientDefinitionData.isMandatory())
         throw new ConstraintViolationException("Invalid property definition " + recipientDefinitionData.getName()
            + ". Residual definition can't be protected");

      Set<String> nodes2 =
         nodeTypeDataManager.getNodes(registeredNodeType.getName(), new InternalQName[]{},
            new InternalQName[]{recipientDefinitionData.getName()});
      if (nodes2.size() > 0)
      {
         String message =
            "Can not change " + recipientDefinitionData.getName().getAsString()
               + " property definition from Protected=false to Protected = true , because " + " the following nodes ";
         for (String uuids : nodes)
         {
            message += uuids + " ";
         }
         message += "  doesn't have these properties ";

         throw new ConstraintViolationException(message);
      }
   }

   /**
    * @param registeredNodeType
    * @param recipientDefinitionData
    * @param allRecipientDefinition
    * @throws RepositoryException
    */
   private void checkRequiredType(NodeTypeData registeredNodeType, PropertyDefinitionData recipientDefinitionData,
      PropertyDefinitionData[] allRecipientDefinition) throws RepositoryException
   {
      Set<String> nodes2;
      if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
      {
         nodes2 = nodeTypeDataManager.getNodes(registeredNodeType.getName());
      }
      else
      {
         nodes2 =
            nodeTypeDataManager.getNodes(registeredNodeType.getName(), new InternalQName[]{recipientDefinitionData
               .getName()}, new InternalQName[]{});
      }
      for (String uuid : nodes2)
      {
         NodeData nodeData = (NodeData)persister.getItemData(uuid);
         if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
         {
            List<PropertyData> propertyDatas = persister.getChildPropertiesData(nodeData);
            for (PropertyData propertyData : propertyDatas)
            {
               // skip mixin and primary type
               if (isResidualMatch(propertyData.getQPath().getName(), allRecipientDefinition))
               {
                  if (propertyData.getType() != recipientDefinitionData.getRequiredType())
                  {
                     throw new ConstraintViolationException("Can not change  requiredType to "
                        + ExtendedPropertyType.nameFromValue(recipientDefinitionData.getRequiredType()) + " in "
                        + recipientDefinitionData.getName().getAsString() + "  because "
                        + propertyData.getQPath().getAsString() + " have "
                        + ExtendedPropertyType.nameFromValue(propertyData.getType()));

                  }
               }
            }

         }
         else
         {
            PropertyData propertyData =
               (PropertyData)persister.getItemData(nodeData, new QPathEntry(recipientDefinitionData.getName(), 0));
            if (propertyData.getType() != recipientDefinitionData.getRequiredType())
            {
               throw new ConstraintViolationException("Can not change  requiredType to "
                  + ExtendedPropertyType.nameFromValue(recipientDefinitionData.getRequiredType()) + " in "
                  + recipientDefinitionData.getName().getAsString() + "  because "
                  + propertyData.getQPath().getAsString() + " have "
                  + ExtendedPropertyType.nameFromValue(propertyData.getType()));
            }
         }

      }
   }

   /**
    * @param registeredNodeType
    * @param recipientDefinitionData
    * @param allRecipientDefinition
    * @throws RepositoryException
    * @throws ConstraintViolationException
    */
   private void checkValueConstraints(NodeTypeData registeredNodeType, PropertyDefinitionData recipientDefinitionData,
      PropertyDefinitionData[] allRecipientDefinition) throws RepositoryException, ConstraintViolationException
   {
      Set<String> nodes2;
      if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
      {
         nodes2 = nodeTypeDataManager.getNodes(registeredNodeType.getName());
      }
      else
      {
         nodes2 =
            nodeTypeDataManager.getNodes(registeredNodeType.getName(), new InternalQName[]{recipientDefinitionData
               .getName()}, new InternalQName[]{});
      }
      for (String uuid : nodes2)
      {
         NodeData nodeData = (NodeData)persister.getItemData(uuid);
         if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
         {
            List<PropertyData> propertyDatas = persister.getChildPropertiesData(nodeData);
            for (PropertyData propertyData : propertyDatas)
            {
               // skip mixin and primary type
               if (isResidualMatch(propertyData.getQPath().getName(), allRecipientDefinition))
               {
                  checkValueConstraints(recipientDefinitionData, propertyData);
               }
            }
         }
         else
         {
            PropertyData propertyData =
               (PropertyData)persister.getItemData(nodeData, new QPathEntry(recipientDefinitionData.getName(), 0));
            checkValueConstraints(recipientDefinitionData, propertyData);
         }
      }
   }

   private void checkValueConstraints(PropertyDefinitionData def, PropertyData propertyData)
      throws ConstraintViolationException, RepositoryException
   {

      ValueConstraintsMatcher constraints =
         new ValueConstraintsMatcher(def.getValueConstraints(), locationFactory, persister, nodeTypeDataManager);

      for (ValueData value : propertyData.getValues())
      {
         if (!constraints.match(((AbstractValueData)value).createTransientCopy(), propertyData.getType()))
         {
            String strVal = null;
            try
            {
               if (propertyData.getType() != PropertyType.BINARY)
               {
                  // may have large size
                  strVal = new String(value.getAsByteArray());
               }
               else
               {
                  strVal = "PropertyType.BINARY";
               }
            }
            catch (Throwable e)
            {
               LOG.error("Error of value read: " + e.getMessage(), e);
            }
            throw new ConstraintViolationException("Value " + strVal + " for property "
               + propertyData.getQPath().getAsString() + " doesn't match new constraint ");
         }
      }
   }

   /**
    * @param toAddList
    * @param changesLog
    * @param nodes
    * @param registeredNodeType
    * @throws RepositoryException
    */
   private void doAdd(List<PropertyDefinitionData> toAddList, PlainChangesLog changesLog, Set<String> nodes,
      NodeTypeData registeredNodeType) throws RepositoryException
   {
      for (String uuid : nodes)
      {
         NodeData nodeData = (NodeData)persister.getItemData(uuid);

         // added properties
         for (PropertyDefinitionData newPropertyDefinitionData : toAddList)
         {
            if (!newPropertyDefinitionData.getName().equals(Constants.JCR_ANY_NAME)
               && newPropertyDefinitionData.isAutoCreated())
            {
               ItemData pdata = persister.getItemData(nodeData, new QPathEntry(newPropertyDefinitionData.getName(), 0));
               if (pdata == null || (pdata != null && pdata.isNode()))
               {
                  PlainChangesLog autoCreatedChanges =
                     nodeTypeDataManager.makeAutoCreatedProperties(nodeData, registeredNodeType.getName(),
                        new PropertyDefinitionData[]{newPropertyDefinitionData}, persister, nodeData.getACL()
                           .getOwner());
                  if (autoCreatedChanges.getSize() == 0)
                  {
                     throw new ConstraintViolationException("Fail to add property by definition: "
                        + newPropertyDefinitionData.getName().getAsString() + " Possible no default values defined.");
                  }

                  changesLog.addAll(autoCreatedChanges.getAllStates());
               }
            }
         }
      }
   }

   /**
    * @param registeredNodeType
    * @param newDefinitionData
    * @param removedDefinitionData
    * @param toAddList
    * @throws RepositoryException
    */
   private void validateAdded(NodeTypeData registeredNodeType, List<PropertyDefinitionData> newDefinitionData,
      PropertyDefinitionData[] allRecipientDefinition) throws RepositoryException
   {
      if (newDefinitionData.size() > 0)
      {
         for (PropertyDefinitionData propertyDefinitionData : newDefinitionData)
         {
            if (propertyDefinitionData.getName().equals(Constants.JCR_ANY_NAME))
            {
               // Required type change
               checkRequiredType(registeredNodeType, propertyDefinitionData, allRecipientDefinition);
               // ValueConstraints
               checkValueConstraints(registeredNodeType, propertyDefinitionData, allRecipientDefinition);
               // multiple change
               checkIsMultiple(registeredNodeType, propertyDefinitionData, allRecipientDefinition);
            }
         }
      }
   }

   /**
    * @param registeredNodeType
    * @param changedDefinitionData
    * @param nodes
    * @param allRecipientDefinition
    * @throws RepositoryException
    */
   private void validateChanged(NodeTypeData registeredNodeType,
      List<RelatedDefinition<PropertyDefinitionData>> changedDefinitionData, Set<String> nodes,
      PropertyDefinitionData[] allRecipientDefinition) throws RepositoryException
   {
      for (RelatedDefinition<PropertyDefinitionData> relatedDefinitions : changedDefinitionData)
      {
         PropertyDefinitionData ancestorDefinitionData = relatedDefinitions.getAncestorDefinition();
         PropertyDefinitionData recipientDefinitionData = relatedDefinitions.getRecepientDefinition();
         // change from mandatory=false to mandatory = true
         if (!ancestorDefinitionData.isMandatory() && recipientDefinitionData.isMandatory())
         {
            checkMandatory(registeredNodeType, nodes, recipientDefinitionData);

         }
         // change from Protected=false to Protected = true
         if (!ancestorDefinitionData.isProtected() && recipientDefinitionData.isProtected())
         {
            checkProtected(registeredNodeType, nodes, recipientDefinitionData);
         }
         // Required type change
         if (ancestorDefinitionData.getRequiredType() != recipientDefinitionData.getRequiredType()
            && recipientDefinitionData.getRequiredType() != PropertyType.UNDEFINED)
         {
            checkRequiredType(registeredNodeType, recipientDefinitionData, allRecipientDefinition);
         }
         // ValueConstraints
         if (!Arrays.deepEquals(ancestorDefinitionData.getValueConstraints(), recipientDefinitionData
            .getValueConstraints()))
         {
            checkValueConstraints(registeredNodeType, recipientDefinitionData, allRecipientDefinition);
         }
         // multiple change
         if (ancestorDefinitionData.isMultiple() && !recipientDefinitionData.isMultiple())
         {
            checkIsMultiple(registeredNodeType, recipientDefinitionData, allRecipientDefinition);
         }

      }
   }

   /**
    * @param registeredNodeType
    * @param recipientDefinition
    * @param toRemoveList
    * @throws RepositoryException
    */
   private void validateRemoved(NodeTypeData registeredNodeType, List<PropertyDefinitionData> removedDefinitionData,
      PropertyDefinitionData[] recipientDefinition) throws RepositoryException
   {
      for (PropertyDefinitionData removePropertyDefinitionData : removedDefinitionData)
      {
         Set<String> nodes;
         if (removePropertyDefinitionData.getName().equals(Constants.JCR_ANY_NAME))
         {
            nodes = nodeTypeDataManager.getNodes(registeredNodeType.getName());
            for (String uuid : nodes)
            {
               NodeData nodeData = (NodeData)persister.getItemData(uuid);
               List<PropertyData> childs = persister.getChildPropertiesData(nodeData);
               // more then mixin and primary type
               // TODO it could be possible, check add definitions
               for (PropertyData propertyData : childs)
               {
                  if (!isNonResidualMatch(propertyData.getQPath().getName(), recipientDefinition))
                  {
                     throw new ConstraintViolationException("Can't remove residual property definition for "
                        + registeredNodeType.getName().getAsString() + " node type, because node "
                        + nodeData.getQPath().getAsString() + " contains property "
                        + propertyData.getQPath().getName().getAsString());
                  }
               }
            }
         }
         else if (!isResidualMatch(removePropertyDefinitionData.getName(), recipientDefinition))
         {
            // TODO more complex exception
            nodes =
               nodeTypeDataManager.getNodes(registeredNodeType.getName(),
                  new InternalQName[]{removePropertyDefinitionData.getName()}, new InternalQName[]{});
            if (nodes.size() > 0)
            {
               String message =
                  "Can not remove " + removePropertyDefinitionData.getName().getAsString()
                     + " PropertyDefinitionData, because the following nodes have these properties: ";
               for (String uuids : nodes)
               {
                  message += uuids + " ";
               }
               throw new ConstraintViolationException(message);

            }
         }
      }
   }
}
