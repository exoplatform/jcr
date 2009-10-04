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

import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;

/**
 * Created by The eXo Platform SAS. <br/>
 * Per-repository component holding all Child Nodes and Properties Definitions
 * as flat Map For ex definition for jcr:primaryType will be repeated as many
 * times as many primary nodetypes is registered (as each primary nodetype
 * extends nt:base directly or indirectly) and so on.
 * 
 * @author Gennady Azarenkov
 * @version $Id: ItemDefinitionDataHolder.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class ItemDefinitionDataHolder
{

   private static Log LOG = ExoLogger.getLogger("jcr.ItemDefinitionDataHolder");

   private final HashMap<ChildNodeDefKey, NodeDefinitionData> nodeDefinitions;

   private final HashMap<PropertyDefKey, PropertyDefinitionData> propertyDefinitions;

   private final HashMap<DefaultNodeDefKey, NodeDefinitionData> defNodeDefinitions;

   public ItemDefinitionDataHolder()
   {
      this.nodeDefinitions = new HashMap<ChildNodeDefKey, NodeDefinitionData>();
      this.propertyDefinitions = new HashMap<PropertyDefKey, PropertyDefinitionData>();
      this.defNodeDefinitions = new HashMap<DefaultNodeDefKey, NodeDefinitionData>();
   }

   private ItemDefinitionDataHolder(HashMap<ChildNodeDefKey, NodeDefinitionData> nodeDefinitions,
      HashMap<PropertyDefKey, PropertyDefinitionData> propertyDefinitions,
      HashMap<DefaultNodeDefKey, NodeDefinitionData> defNodeDefinitions)
   {
      this.nodeDefinitions = nodeDefinitions;
      this.propertyDefinitions = propertyDefinitions;
      this.defNodeDefinitions = defNodeDefinitions;
   }

   /**
    * @param parentNodeType - name of parent node type
    * @param childName name of child node
    * @param childNodeType name of child node type
    * @return Child NodeDefinition or null if not found
    */
   public NodeDefinitionData getChildNodeDefinition(InternalQName parentNodeType, InternalQName childName,
      InternalQName childNodeType)
   {

      NodeDefinitionData def = getNodeDefinitionFromThisOrSupertypes(parentNodeType, childName, childNodeType);

      return def;
   }

   /**
    * @param pr name of parent node types
    * @param childName name of child node
    * @return default ChildNodeDefinition or null if not found
    */
   public NodeDefinitionData getDefaultChildNodeDefinition(InternalQName childName, InternalQName... nodeTypes)
   {

      for (InternalQName parentNodeType : nodeTypes)
      {
         NodeDefinitionData def = defNodeDefinitions.get(new DefaultNodeDefKey(parentNodeType, childName));
         if (def != null)
            return def;
      }

      // residual
      for (InternalQName parentNodeType : nodeTypes)
      {
         NodeDefinitionData def = defNodeDefinitions.get(new DefaultNodeDefKey(parentNodeType, Constants.JCR_ANY_NAME));
         if (def != null)
            return def;
      }

      return null;
   }

   /**
    * @param parentNodeType name of parent node type
    * @param childName name of child property
    * @param multiValued
    * @return Child PropertyDefinition or null if not found
    */
   public PropertyDefinitionData getPropertyDefinition(InternalQName childName, boolean multiValued,
      InternalQName parentNodeType)
   {

      PropertyDefKey key = new PropertyDefKey(parentNodeType, childName, multiValued);
      PropertyDefinitionData def = propertyDefinitions.get(key);

      // try residual def
      if (def == null)
      {
         return propertyDefinitions.get(new PropertyDefKey(parentNodeType, Constants.JCR_ANY_NAME, multiValued));
      }

      return def;
   }

   /**
    * @param parentNodeType name of parent node type
    * @param propertyName name of child property
    * @param multiValued
    * @return Child PropertyDefinition or null if not found
    */
   public PropertyDefinitionDatas getPropertyDefinitions(final InternalQName propertyName,
      final InternalQName... nodeTypes)
   {

      PropertyDefinitionDatas pdefs = new PropertyDefinitionDatas();

      for (InternalQName nt : nodeTypes)
      {
         // single-valued
         PropertyDefinitionData def = propertyDefinitions.get(new PropertyDefKey(nt, propertyName, false));
         if (def != null && pdefs.getDefinition(def.isMultiple()) == null)
            pdefs.setDefinition(def); // set if same is not exists

         // multi-valued
         def = propertyDefinitions.get(new PropertyDefKey(nt, propertyName, true));
         if (def != null && pdefs.getDefinition(def.isMultiple()) == null)
            pdefs.setDefinition(def); // set if same is not exists

         // try residual

      }

      return pdefs.getAnyDefinition() != null ? pdefs : null;
   }

   /**
    * adds Child Node/Property Definitions for incoming NodeType (should be
    * called by NodeTypeManager in register method)
    * 
    * @param nodeType
    */
   void putDefinitions(InternalQName name, NodeTypeData nodeType)
   {

      // put child node defs
      NodeDefinitionData[] nodeDefs = nodeType.getDeclaredChildNodeDefinitions();
      for (NodeDefinitionData nodeDef : nodeDefs)
      {
         // put required node type defs
         for (InternalQName rnt : nodeDef.getRequiredPrimaryTypes())
         {
            ChildNodeDefKey nodeDefKey = new ChildNodeDefKey(name, nodeDef.getName(), rnt);
            nodeDefinitions.put(nodeDefKey, nodeDef);

            if (LOG.isDebugEnabled())
            {
               LOG.debug("NodeDef added: parent NT: " + name.getAsString() + " child nodeName: "
                  + nodeDef.getName().getAsString() + " childNT: " + rnt.getAsString() + " hash: "
                  + nodeDefKey.hashCode());
            }
         }

         // put default node definition
         DefaultNodeDefKey defNodeDefKey = new DefaultNodeDefKey(name, nodeDef.getName());
         defNodeDefinitions.put(defNodeDefKey, nodeDef);

         if (LOG.isDebugEnabled())
         {
            LOG.debug("Default NodeDef added: parent NT: " + name.getAsString() + " child nodeName: "
               + nodeDef.getName() + " hash: " + defNodeDefKey.hashCode());
         }
      }

      // put prop defs
      PropertyDefinitionData[] propDefs = nodeType.getDeclaredPropertyDefinitions();
      for (PropertyDefinitionData propDef : propDefs)
      {
         PropertyDefKey propDefKey = new PropertyDefKey(name, propDef.getName(), propDef.isMultiple());
         propertyDefinitions.put(propDefKey, propDef);

         if (LOG.isDebugEnabled())
         {
            LOG.debug("PropDef added: parent NT: " + name.getAsString() + " child propName: "
               + propDef.getName().getAsString() + " isMultiple: " + propDef.isMultiple() + " hash: "
               + propDefKey.hashCode());
         }
      }

   }

   void removeDefinitions(InternalQName name, NodeTypeData nodeType)
   {
      // remove child node defs
      NodeDefinitionData[] nodeDefs = nodeType.getDeclaredChildNodeDefinitions();
      for (NodeDefinitionData nodeDef : nodeDefs)
      {
         // remove required node type defs
         for (InternalQName rnt : nodeDef.getRequiredPrimaryTypes())
         {
            ChildNodeDefKey nodeDefKey = new ChildNodeDefKey(name, nodeDef.getName(), rnt);
            nodeDefinitions.remove(nodeDefKey);

            if (LOG.isDebugEnabled())
            {
               LOG.debug("NodeDef removed: parent NT: " + name.getAsString() + " child nodeName: "
                  + nodeDef.getName().getAsString() + " childNT: " + rnt.getAsString() + " hash: "
                  + nodeDefKey.hashCode());
            }
         }

         // remove default node definition
         DefaultNodeDefKey defNodeDefKey = new DefaultNodeDefKey(name, nodeDef.getName());
         defNodeDefinitions.remove(defNodeDefKey);

         if (LOG.isDebugEnabled())
         {
            LOG.debug("Default NodeDef removed: parent NT: " + name.getAsString() + " child nodeName: "
               + nodeDef.getName() + " hash: " + defNodeDefKey.hashCode());
         }
      }

      // remove defs
      PropertyDefinitionData[] propDefs = nodeType.getDeclaredPropertyDefinitions();
      for (PropertyDefinitionData propDef : propDefs)
      {
         PropertyDefKey propDefKey = new PropertyDefKey(name, propDef.getName(), propDef.isMultiple());
         propertyDefinitions.remove(propDefKey);

         if (LOG.isDebugEnabled())
         {
            LOG.debug("PropDef remode: parent NT: " + name.getAsString() + " child propName: "
               + propDef.getName().getAsString() + " isMultiple: " + propDef.isMultiple() + " hash: "
               + propDefKey.hashCode());
         }
      }
   }

   private NodeDefinitionData getNodeDefinitionFromThisOrSupertypes(InternalQName parentNodeType,
      InternalQName childName, InternalQName childNodeType)
   {

      NodeDefinitionData def = nodeDefinitions.get(new ChildNodeDefKey(parentNodeType, childName, childNodeType));
      if (def != null)
         return def;

      return def;
   }

   /**
    * Create copy of holder.
    * 
    * @return
    */
   protected ItemDefinitionDataHolder createCopy()
   {
      return new ItemDefinitionDataHolder(new HashMap<ChildNodeDefKey, NodeDefinitionData>(nodeDefinitions),
         new HashMap<PropertyDefKey, PropertyDefinitionData>(propertyDefinitions),
         new HashMap<DefaultNodeDefKey, NodeDefinitionData>(defNodeDefinitions));
   }

   private class ChildNodeDefKey extends ItemDefKey
   {
      private int hashCode = -1;

      private final InternalQName childNodeType;

      private ChildNodeDefKey(InternalQName parentNodeType, InternalQName childName, InternalQName childNodeType)
      {
         super(parentNodeType, childName);
         this.childNodeType = childNodeType;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
         {
            return true;
         }
         if (!super.equals(obj))
         {
            return false;
         }
         if (!(obj instanceof ChildNodeDefKey))
         {
            return false;
         }
         ChildNodeDefKey other = (ChildNodeDefKey)obj;
         if (!getOuterType().equals(other.getOuterType()))
         {
            return false;
         }
         if (childNodeType == null)
         {
            if (other.childNodeType != null)
            {
               return false;
            }
         }
         else if (!childNodeType.equals(other.childNodeType))
         {
            return false;
         }
         return true;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int hashCode()
      {
         if (hashCode == -1)
         {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((childNodeType == null) ? 0 : childNodeType.hashCode());
            hashCode = result;
         }

         return hashCode;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String toString()
      {
         String result = super.toString();
         result += ((childNodeType == null) ? "" : childNodeType.getAsString());

         return result;
      }

      private ItemDefinitionDataHolder getOuterType()
      {
         return ItemDefinitionDataHolder.this;
      }

   }

   private class DefaultNodeDefKey extends ItemDefKey
   {

      private DefaultNodeDefKey(InternalQName parentNodeType, InternalQName childName)
      {
         super(parentNodeType, childName);
      }
   }

   /**
    * @see about hash code generation:
    *      http://www.geocities.com/technofundo/tech/java/equalhash.html
    */
   private abstract class ItemDefKey
   {

      private final InternalQName parentNodeType;

      private final InternalQName childName;

      private int hashCode = -1;

      protected ItemDefKey(InternalQName parentNodeType, InternalQName childName)
      {
         this.parentNodeType = parentNodeType;
         this.childName = childName;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
         {
            return true;
         }
         if (obj == null)
         {
            return false;
         }
         if (!(obj instanceof ItemDefKey))
         {
            return false;
         }
         ItemDefKey other = (ItemDefKey)obj;
         if (!getOuterType().equals(other.getOuterType()))
         {
            return false;
         }
         if (childName == null)
         {
            if (other.childName != null)
            {
               return false;
            }
         }
         else if (!childName.equals(other.childName))
         {
            return false;
         }
         if (parentNodeType == null)
         {
            if (other.parentNodeType != null)
            {
               return false;
            }
         }
         else if (!parentNodeType.equals(other.parentNodeType))
         {
            return false;
         }
         return true;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int hashCode()
      {
         if (hashCode == -1)
         {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((childName == null) ? 0 : childName.hashCode());
            result = prime * result + ((parentNodeType == null) ? 0 : parentNodeType.hashCode());
            hashCode = result;
         }
         return hashCode;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String toString()
      {
         String result = "";
         result += ((childName == null) ? 0 : childName.getAsString());
         result += ((parentNodeType == null) ? 0 : parentNodeType.getAsString());
         return result;
      }

      private ItemDefinitionDataHolder getOuterType()
      {
         return ItemDefinitionDataHolder.this;
      }
   }

   private class PropertyDefKey extends ItemDefKey
   {

      private final boolean multiValued;

      private int hashCode = -1;

      private PropertyDefKey(InternalQName parentNodeType, InternalQName childName, boolean multiValued)
      {
         super(parentNodeType, childName);
         this.multiValued = multiValued;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
         {
            return true;
         }
         if (!super.equals(obj))
         {
            return false;
         }
         if (!(obj instanceof PropertyDefKey))
         {
            return false;
         }
         PropertyDefKey other = (PropertyDefKey)obj;
         if (!getOuterType().equals(other.getOuterType()))
         {
            return false;
         }
         if (multiValued != other.multiValued)
         {
            return false;
         }
         return true;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int hashCode()
      {
         if (hashCode == -1)
         {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + getOuterType().hashCode();
            result = prime * result + (multiValued ? 1231 : 1237);
            hashCode = result;
         }

         return hashCode;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String toString()
      {
         String result = super.toString();
         result += " multiValued=" + multiValued;
         return result;
      }

      private ItemDefinitionDataHolder getOuterType()
      {
         return ItemDefinitionDataHolder.this;
      }
   }

}
