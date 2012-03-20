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
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.nodetype.ItemAutocreator;
import org.exoplatform.services.jcr.impl.core.value.ValueConstraintsMatcher;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.PropertyDefinitionComparator");

   private final List<NodeData> affectedNodes;

   private final LocationFactory locationFactory;

   private final NodeTypeDataManager nodeTypeDataManager;

   private final ItemDataConsumer dataConsumer;

   private final ItemAutocreator itemAutocreator;

   /**
    * @param nodeTypeDataManager
    * @param persister
    * @param locationFactory
    */
   public PropertyDefinitionComparator(NodeTypeDataManager nodeTypeDataManager, ItemDataConsumer dataConsumer,
      ItemAutocreator itemAutocreator, List<NodeData> affectedNodes, LocationFactory locationFactory)
   {
      this.nodeTypeDataManager = nodeTypeDataManager;
      this.dataConsumer = dataConsumer;
      this.itemAutocreator = itemAutocreator;
      this.affectedNodes = affectedNodes;
      this.locationFactory = locationFactory;
   }

   @Override
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

      // removing properties
      validateRemoved(registeredNodeType, removedDefinitionData, recipientDefinition, affectedNodes);

      // new property definition
      validateAdded(registeredNodeType, newDefinitionData, recipientDefinition, affectedNodes);

      // changed
      validateChanged(registeredNodeType, changedDefinitionData, affectedNodes, recipientDefinition);

      //
      doAdd(newDefinitionData, changesLog, affectedNodes, registeredNodeType);
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
      PropertyDefinitionData[] allRecipientDefinition, List<NodeData> nodesData) throws RepositoryException,
      ConstraintViolationException
   {
      List<NodeData> checkIsMultipleNodes;
      if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
      {
         checkIsMultipleNodes = nodesData;
      }
      else
      {
         checkIsMultipleNodes =
            getNodes(nodesData, new InternalQName[]{recipientDefinitionData.getName()}, new InternalQName[]{});
      }
      for (NodeData nodeData : checkIsMultipleNodes)
      {
         if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
         {
            List<PropertyData> propertyDatas = dataConsumer.getChildPropertiesData(nodeData);
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
               (PropertyData)dataConsumer.getItemData(nodeData, new QPathEntry(recipientDefinitionData.getName(), 0),
                  ItemType.PROPERTY);
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
   private void checkMandatory(NodeTypeData registeredNodeType, List<NodeData> nodesData,
      PropertyDefinitionData recipientDefinitionData) throws RepositoryException
   {
      if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()) && recipientDefinitionData.isMandatory())
         throw new ConstraintViolationException("Invalid property definition " + recipientDefinitionData.getName()
            + ". Residual definition can't be mandatory");
      List<NodeData> mandatoryNodes =
         getNodes(nodesData, new InternalQName[]{}, new InternalQName[]{recipientDefinitionData.getName()});
      if (mandatoryNodes.size() > 0)
      {
         StringBuilder message =
            new StringBuilder("Can not change ").append(recipientDefinitionData.getName().getAsString()).append(
               " property definition from mandatory=false to mandatory = true , because ").append(" the following nodes ");

         for (NodeData nodeData : mandatoryNodes)
         {
            message.append(nodeData.getQPath().getAsString()).append(" ");
         }
         message.append("  doesn't have these properties ");

         throw new ConstraintViolationException(message.toString());
      }
   }

   /**
    * @param registeredNodeType
    * @param recipientDefinitionData
    * @param allRecipientDefinition
    * @throws RepositoryException
    */
   private void checkRequiredType(NodeTypeData registeredNodeType, PropertyDefinitionData recipientDefinitionData,
      PropertyDefinitionData[] allRecipientDefinition, List<NodeData> nodesData) throws RepositoryException
   {
      List<NodeData> requiredTypeNodes;
      if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
      {
         requiredTypeNodes = nodesData;
      }
      else
      {
         requiredTypeNodes =
            getNodes(nodesData, new InternalQName[]{recipientDefinitionData.getName()}, new InternalQName[]{});
      }
      for (NodeData nodeData : requiredTypeNodes)
      {
         if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
         {
            List<PropertyData> propertyDatas = dataConsumer.getChildPropertiesData(nodeData);
            for (PropertyData propertyData : propertyDatas)
            {
               // skip mixin and primary type
               if (isResidualMatch(propertyData.getQPath().getName(), allRecipientDefinition))
               {
                  if (recipientDefinitionData.getRequiredType() != PropertyType.UNDEFINED
                     && propertyData.getType() != recipientDefinitionData.getRequiredType())
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
               (PropertyData)dataConsumer.getItemData(nodeData, new QPathEntry(recipientDefinitionData.getName(), 0),
                  ItemType.PROPERTY);
            if (recipientDefinitionData.getRequiredType() != PropertyType.UNDEFINED
               && propertyData.getType() != recipientDefinitionData.getRequiredType())
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
      PropertyDefinitionData[] allRecipientDefinition, List<NodeData> nodesData) throws RepositoryException,
      ConstraintViolationException
   {
      List<NodeData> checkValueConstraintsNodes;
      if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
      {
         checkValueConstraintsNodes = nodesData;
      }
      else
      {
         checkValueConstraintsNodes =
            getNodes(nodesData, new InternalQName[]{recipientDefinitionData.getName()}, new InternalQName[]{});
      }
      for (NodeData nodeData : checkValueConstraintsNodes)
      {

         if (Constants.JCR_ANY_NAME.equals(recipientDefinitionData.getName()))
         {
            List<PropertyData> propertyDatas = dataConsumer.getChildPropertiesData(nodeData);
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
               (PropertyData)dataConsumer.getItemData(nodeData, new QPathEntry(recipientDefinitionData.getName(), 0),
                  ItemType.PROPERTY);
            checkValueConstraints(recipientDefinitionData, propertyData);
         }
      }
   }

   private void checkValueConstraints(PropertyDefinitionData def, PropertyData propertyData) throws RepositoryException
   {

      ValueConstraintsMatcher constraints =
         new ValueConstraintsMatcher(def.getValueConstraints(), locationFactory, dataConsumer, nodeTypeDataManager);

      for (ValueData value : propertyData.getValues())
      {
         if (!constraints.match(value, propertyData.getType()))
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
            catch (IllegalStateException e)
            {
               LOG.error("Error of value read: " + e.getMessage(), e);
            }
            catch (IOException e)
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
   private void doAdd(List<PropertyDefinitionData> toAddList, PlainChangesLog changesLog, List<NodeData> nodesData,
      NodeTypeData registeredNodeType) throws RepositoryException
   {
      for (NodeData nodeData : nodesData)
      {

         // added properties
         for (PropertyDefinitionData newPropertyDefinitionData : toAddList)
         {
            if (!newPropertyDefinitionData.getName().equals(Constants.JCR_ANY_NAME)
               && newPropertyDefinitionData.isAutoCreated())
            {
               ItemData pdata =
                  dataConsumer.getItemData(nodeData, new QPathEntry(newPropertyDefinitionData.getName(), 0),
                     ItemType.UNKNOWN);
               if (pdata == null || (pdata != null && pdata.isNode()))
               {
                  PlainChangesLog autoCreatedChanges =
                     itemAutocreator.makeAutoCreatedProperties(nodeData, registeredNodeType.getName(),
                        new PropertyDefinitionData[]{newPropertyDefinitionData}, dataConsumer, nodeData.getACL()
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
    * @param nodes
    * @param includeProperties
    * @param excludeProperties
    * @return All nodes from list nodes, what include properties from
    *         includeProperties, and doesn't include properties from
    *         excludeProperties.
    * @throws RepositoryException
    */
   private List<NodeData> getNodes(List<NodeData> nodes, InternalQName[] includeProperties,
      InternalQName[] excludeProperties) throws RepositoryException
   {
      List<NodeData> result = new ArrayList<NodeData>();

      for (NodeData nodeData : nodes)
      {
         // search all properties
         List<PropertyData> childProperties = dataConsumer.listChildPropertiesData(nodeData);
         boolean toAdd = includeProperties.length == 0;
         // check included
         for (int i = 0; i < includeProperties.length; i++)
         {
            for (PropertyData propertyData : childProperties)
            {
               if (propertyData.getQPath().getName().equals(includeProperties[i]))
               {
                  toAdd = true;
                  break;
               }
            }
         }
         if (toAdd)
         {
            // check excluded
            for (int i = 0; i < excludeProperties.length; i++)
            {
               for (PropertyData propertyData : childProperties)
               {
                  if (propertyData.getQPath().getName().equals(excludeProperties[i]))
                  {
                     toAdd = false;
                     break;
                  }
               }
            }
            if (toAdd)
               result.add(nodeData);
         }
      }
      return result;
   }

   /**
    * @param registeredNodeType
    * @param newDefinitionData
    * @param removedDefinitionData
    * @param toAddList
    * @throws RepositoryException
    */
   private void validateAdded(NodeTypeData registeredNodeType, List<PropertyDefinitionData> newDefinitionData,
      PropertyDefinitionData[] allRecipientDefinition, List<NodeData> nodesData) throws RepositoryException
   {
      if (newDefinitionData.size() > 0)
      {
         for (PropertyDefinitionData propertyDefinitionData : newDefinitionData)
         {
            if (propertyDefinitionData.getName().equals(Constants.JCR_ANY_NAME))
            {
               // Required type change
               checkRequiredType(registeredNodeType, propertyDefinitionData, allRecipientDefinition, nodesData);
               // ValueConstraints
               checkValueConstraints(registeredNodeType, propertyDefinitionData, allRecipientDefinition, nodesData);
               // multiple change
               checkIsMultiple(registeredNodeType, propertyDefinitionData, allRecipientDefinition, nodesData);
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
      List<RelatedDefinition<PropertyDefinitionData>> changedDefinitionData, List<NodeData> nodesData,
      PropertyDefinitionData[] allRecipientDefinition) throws RepositoryException
   {
      for (RelatedDefinition<PropertyDefinitionData> relatedDefinitions : changedDefinitionData)
      {
         PropertyDefinitionData ancestorDefinitionData = relatedDefinitions.getAncestorDefinition();
         PropertyDefinitionData recipientDefinitionData = relatedDefinitions.getRecepientDefinition();
         // change from mandatory=false to mandatory = true
         if (!ancestorDefinitionData.isMandatory() && recipientDefinitionData.isMandatory())
         {
            checkMandatory(registeredNodeType, nodesData, recipientDefinitionData);

         }
         // No need to check protected
         // change from Protected=false to Protected = true
         // if (!ancestorDefinitionData.isProtected() &&
         // recipientDefinitionData.isProtected()) {
         // checkProtected(registeredNodeType, nodesData, recipientDefinitionData);
         // }
         // Required type change
         if (ancestorDefinitionData.getRequiredType() != recipientDefinitionData.getRequiredType()
            && recipientDefinitionData.getRequiredType() != PropertyType.UNDEFINED)
         {
            checkRequiredType(registeredNodeType, recipientDefinitionData, allRecipientDefinition, nodesData);
         }
         // ValueConstraints
         if (!Arrays.deepEquals(ancestorDefinitionData.getValueConstraints(),
            recipientDefinitionData.getValueConstraints()))
         {
            checkValueConstraints(registeredNodeType, recipientDefinitionData, allRecipientDefinition, nodesData);
         }
         // multiple change
         if (ancestorDefinitionData.isMultiple() && !recipientDefinitionData.isMultiple())
         {
            checkIsMultiple(registeredNodeType, recipientDefinitionData, allRecipientDefinition, nodesData);
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
      PropertyDefinitionData[] recipientDefinition, List<NodeData> nodesData) throws RepositoryException
   {
      for (PropertyDefinitionData removePropertyDefinitionData : removedDefinitionData)
      {
         if (removePropertyDefinitionData.getName().equals(Constants.JCR_ANY_NAME))
         {

            for (NodeData nodeData : nodesData)
            {
               List<PropertyData> childs = dataConsumer.getChildPropertiesData(nodeData);
               // more then mixin and primary type
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
            List<NodeData> nodes =
               getNodes(nodesData, new InternalQName[]{removePropertyDefinitionData.getName()}, new InternalQName[]{});
            if (nodes.size() > 0)
            {
               StringBuilder message =
                  new StringBuilder("Can not remove ").append(removePropertyDefinitionData.getName().getAsString())
                     .append(" PropertyDefinitionData, because the following nodes have these properties: ");
               for (NodeData nodeData : nodes)
               {
                  message.append(nodeData.getQPath().getAsString()).append(" ");
               }
               throw new ConstraintViolationException(message.toString());

            }
         }
      }
   }
}
