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

import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataImpl;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.version.OnParentVersionAction;

/**
 * Created by The eXo Platform SAS. <br>
 * This class is designed to build NodeTypeData instances 'bit by bit'. It
 * serves as a container to store NodeType definition setting in the process of
 * reading the stream. When instance of builder is created default parameters
 * are assigned to internal fields. When method <b>build()</b> is invoked new
 * NodeTypeData instance is created using parameters from internal fields. To
 * define new child or new property method <b>
 * newNodeDefinitionDataBuilder()</b> or <b> newPropertyDefinitionDataBuilder()
 * </b> should be invoked. <b>Instances of returned Builders are automatically
 * added to internal child or property lists.</b> If some builder is not used it
 * should be removed from internal lists by using NodeTypeDataBuilder's method
 * removeNodeDefinitionDataBuilder() or removePropertyDefinitionDataBuilder.
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: $
 */
public class NodeTypeDataBuilder
{

   private InternalQName name;

   private InternalQName[] supertypes;

   private boolean isMixin;

   private boolean isOrderable;

   private InternalQName primaryItemName;

   private PropertyDefinitionData[] propertyDefinitions;

   private NodeDefinitionData[] childNodeDefinitions;

   private boolean isAbstract;

   private boolean isQueryable = true;

   private List<NodeDefinitionDataBuilder> nodeDefinitionDataBuilders;

   private List<PropertyDefinitionDataBuilder> propertyDefinitionDataBuilders;

   /**
    * This subclass represents child builder. To create instance of this class
    * you should call NodeTypeDataBuilder's method
    * <b>newNodeDefinitionDataBuilder()</b>.
    */
   public class NodeDefinitionDataBuilder
   {

      private InternalQName name;

      private InternalQName declaringType;

      private InternalQName defaultPrimaryType;

      private InternalQName[] requiredPrimaryTypes;

      private boolean isAutoCreated;

      private int onParentVersion;

      private boolean isProtected;

      private boolean isMandatory;

      private boolean allowsSameNameSiblings;

      /**
       * Constructor is private. Could to be invoked only by NodeTypeDataBuilder.
       * 
       * @param declaringType
       */
      private NodeDefinitionDataBuilder(InternalQName declaringType)
      {
         this.declaringType = declaringType;
         this.defaultPrimaryType = null;
         this.requiredPrimaryTypes = new InternalQName[]{Constants.NT_BASE};
         this.isAutoCreated = false;
         this.onParentVersion = OnParentVersionAction.COPY;
         this.isProtected = false;
         this.isMandatory = false;
         this.allowsSameNameSiblings = false;
      }

      /**
       * This method returns new instance of NodeDefinitionData using parameters
       * stored in current instance. This method is called by
       * <b>NodeTypeDataBuilder.build()</b> before creating NodeTypeData instance.
       * 
       * @return instance of NodeDefinitionData
       */
      private NodeDefinitionData build()
      {
         return new NodeDefinitionData(name, declaringType, isAutoCreated, isMandatory, onParentVersion, isProtected,
            requiredPrimaryTypes, defaultPrimaryType, allowsSameNameSiblings);
      }

      public void setName(InternalQName name)
      {
         this.name = name;
      }

      public void setDefaultPrimaryType(InternalQName defaultPrimaryType)
      {
         this.defaultPrimaryType = defaultPrimaryType;
      }

      public void setRequiredPrimaryTypes(InternalQName[] requiredPrimaryTypes)
      {
         this.requiredPrimaryTypes = requiredPrimaryTypes;
      }

      public void setAutoCreated(boolean isAutoCreated)
      {
         this.isAutoCreated = isAutoCreated;
      }

      public void setOnParentVersion(int onParentVersion)
      {
         this.onParentVersion = onParentVersion;
      }

      public void setProtected(boolean isProtected)
      {
         this.isProtected = isProtected;
      }

      public void setMandatory(boolean isMandatory)
      {
         this.isMandatory = isMandatory;
      }

      public void setAllowsSameNameSiblings(boolean allowsSameNameSiblings)
      {
         this.allowsSameNameSiblings = allowsSameNameSiblings;
      }
   }

   /**
    * This subclass represents property builder. To create instance of this class
    * you should call NodeTypeDataBuilder's method
    * <b>newPropertyDefinitionDataBuilders()</b>.
    */
   public class PropertyDefinitionDataBuilder
   {

      private InternalQName name;

      private int requiredType;

      private String[] valueConstraints;

      private String[] defaultValues;

      private boolean isAutoCreated;

      private boolean isProtected;

      private boolean isMandatory;

      private boolean isMultiple;

      private boolean isFullTextSearchable;

      private boolean isQueryOrderable;

      private String[] queryOperators;

      private int onParentVersion;

      private InternalQName declaringType;

      /**
       * Constructor is private. Could to be invoked only by NodeTypeDataBuilder.
       * 
       * @param declaringType
       */
      private PropertyDefinitionDataBuilder(InternalQName declaringType)
      {
         this.requiredType = PropertyType.STRING;
         this.valueConstraints = null;
         this.defaultValues = null;
         this.isAutoCreated = false;
         this.isProtected = false;
         this.isMandatory = false;
         this.isMultiple = false;
//         this.isFullTextSearchable = true;
//         this.isQueryOrderable = true;
//         this.queryOperators = Operator.getAllQueryOperators();
         this.onParentVersion = OnParentVersionAction.COPY;
         this.declaringType = declaringType;
      }

      /**
       * This method returns new instance of NodeDefinitionData using parameters
       * stored in current instance. This method is called by
       * <b>NodeTypeDataBuilder.build()</b> before creating NodeTypeData instance.
       * 
       * @return instance of PropertyDefinitionData
       */
      private PropertyDefinitionData build()
      {
         return new PropertyDefinitionData(name, declaringType, isAutoCreated, isMandatory, onParentVersion,
            isProtected, requiredType, valueConstraints, defaultValues, isMultiple);
      }

      public void setName(InternalQName name)
      {
         this.name = name;
      }

      public void setRequiredType(int requiredType)
      {
         this.requiredType = requiredType;
      }

      public void setValueConstraints(String[] valueConstraints)
      {
         this.valueConstraints = valueConstraints;
      }

      public void setDefaultValues(String[] defaultValues)
      {
         this.defaultValues = defaultValues;
      }

      public void setAutoCreated(boolean isAutoCreated)
      {
         this.isAutoCreated = isAutoCreated;
      }

      public void setProtected(boolean isProtected)
      {
         this.isProtected = isProtected;
      }

      public void setMandatory(boolean isMandatory)
      {
         this.isMandatory = isMandatory;
      }

      public void setMultiple(boolean isMultiple)
      {
         this.isMultiple = isMultiple;
      }

      public void setFullTextSearchable(boolean fullTextSearchable)
      {
         this.isFullTextSearchable = fullTextSearchable;
      }

      public void setQueryOrderable(boolean isQueryOrderable)
      {
         this.isQueryOrderable = isQueryOrderable;
      }

      public void setQueryOperators(String[] queryOperators)
      {
         this.queryOperators = queryOperators;
      }

      public void setOnParentVersion(int onParentVersion)
      {
         this.onParentVersion = onParentVersion;
      }
   }

   /**
    * Creates new instance of NodeTypeData builder with default parameters.
    */
   public NodeTypeDataBuilder()
   {
      this.isMixin = false;
      this.isOrderable = false;
      this.isAbstract = false;
      this.isQueryable = true;
      this.primaryItemName = null;
      this.supertypes = new InternalQName[]{Constants.NT_BASE};
      this.propertyDefinitions = new PropertyDefinitionData[0];
      this.childNodeDefinitions = new NodeDefinitionData[0];
      this.nodeDefinitionDataBuilders = new LinkedList<NodeDefinitionDataBuilder>();
      this.propertyDefinitionDataBuilders = new LinkedList<PropertyDefinitionDataBuilder>();
   }

   /**
    * Creates child builder. This builder is automatically added to list of child
    * builders in order to fill nodetType's child declaration array. If this
    * instance is not used, it should be removed from internal list by calling
    * NodeTypeData's method <b>removeNodeDefinitionDataBuilder()</b>.
    * 
    * @return new NodeDefinitionBuilder
    */
   public NodeDefinitionDataBuilder newNodeDefinitionDataBuilder()
   {
      NodeDefinitionDataBuilder child = new NodeDefinitionDataBuilder(this.name);
      this.nodeDefinitionDataBuilders.add(child);
      return child;
   }

   /**
    * Creates property builder. This builder is automatically added to list of
    * property builders in order to fill nodetType's property declaration array.
    * If this instance is not used, it should be removed from internal list by
    * calling NodeTypeData's method <b>removePropertyDefinitionDataBuilder()</b>.
    * 
    * @return new NodeDefinitionBuilder
    */
   public PropertyDefinitionDataBuilder newPropertyDefinitionDataBuilder()
   {
      PropertyDefinitionDataBuilder property = new PropertyDefinitionDataBuilder(this.name);
      this.propertyDefinitionDataBuilders.add(property);
      return property;
   }

   /**
    * Removes given NodeDefinitionDataBuilder from internal list of NodeType's
    * children.
    * 
    * @param NodeDefinitionDataBuilder
    * @return result flag
    */
   public boolean removeNodeDefinitionDataBuilder(NodeDefinitionDataBuilder childBuilder)
   {
      return nodeDefinitionDataBuilders.remove(childBuilder);
   }

   /**
    * Removes given PropertyDefinitionDataBuilder from internal list of
    * NodeType's properties.
    * 
    * @param PropertyDefinitionDataBuilder
    * @return result flag
    */
   public boolean removePropertyDefinitionDataBuilder(PropertyDefinitionDataBuilder propertyBuilder)
   {
      return propertyDefinitionDataBuilders.remove(propertyBuilder);
   }

   /**
    * Creates instance of NodeTypeData using parameters stored in this object.
    * 
    * @return NodeTypeData
    */
   public NodeTypeData build()
   {
      if (nodeDefinitionDataBuilders.size() > 0)
      {
         childNodeDefinitions = new NodeDefinitionData[nodeDefinitionDataBuilders.size()];
         for (int i = 0; i < childNodeDefinitions.length; i++)
         {
            childNodeDefinitions[i] = nodeDefinitionDataBuilders.get(i).build();
         }
      }
      if (propertyDefinitionDataBuilders.size() > 0)
      {
         propertyDefinitions = new PropertyDefinitionData[propertyDefinitionDataBuilders.size()];
         for (int i = 0; i < propertyDefinitions.length; i++)
         {
            propertyDefinitions[i] = propertyDefinitionDataBuilders.get(i).build();
         }
      }
      return new NodeTypeDataImpl(name, primaryItemName, isMixin, isOrderable,  supertypes,
         propertyDefinitions, childNodeDefinitions);
   }

   public void setName(InternalQName name)
   {
      this.name = name;
   }

   public void setSupertypes(InternalQName[] supertypes)
   {
      this.supertypes = supertypes;
   }

   public void setMixin(boolean isMixin)
   {
      this.isMixin = isMixin;
   }

   public void setOrderable(boolean isOrderable)
   {
      this.isOrderable = isOrderable;
   }

   public void setPrimaryItemName(InternalQName primaryItemName)
   {
      this.primaryItemName = primaryItemName;
   }

   public void setAbstract(boolean isAbstract)
   {
      this.isAbstract = isAbstract;
   }

   public void setQueryable(boolean isQueryable)
   {
      this.isQueryable = isQueryable;
   }

}
