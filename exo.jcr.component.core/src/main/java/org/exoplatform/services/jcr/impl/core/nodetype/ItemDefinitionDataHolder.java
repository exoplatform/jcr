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
import java.util.Map;
import java.util.Map.Entry;

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

   private final Map<InternalQName, Map<InternalQName, Map<InternalQName, NodeDefinitionData>>> nodeDefinitions;

   private final Map<InternalQName, Map<InternalQName, Map<Boolean, PropertyDefinitionData>>> propertyDefinitions;

   private final Map<InternalQName, Map<InternalQName, NodeDefinitionData>> defNodeDefinitions;

   public ItemDefinitionDataHolder()
   {
      this.nodeDefinitions = new HashMap<InternalQName, Map<InternalQName, Map<InternalQName, NodeDefinitionData>>>();
      this.propertyDefinitions = new HashMap<InternalQName, Map<InternalQName, Map<Boolean, PropertyDefinitionData>>>();
      this.defNodeDefinitions = new HashMap<InternalQName, Map<InternalQName, NodeDefinitionData>>();
   }

   private ItemDefinitionDataHolder(
      Map<InternalQName, Map<InternalQName, Map<InternalQName, NodeDefinitionData>>> nodeDefinitions,
      Map<InternalQName, Map<InternalQName, Map<Boolean, PropertyDefinitionData>>> propertyDefinitions,
      Map<InternalQName, Map<InternalQName, NodeDefinitionData>> defNodeDefinitions)
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
         NodeDefinitionData def = getNodeDefinitionDataInternal(parentNodeType, childName);
         if (def != null)
            return def;
      }

      // residual
      for (InternalQName parentNodeType : nodeTypes)
      {
         NodeDefinitionData def = getNodeDefinitionDataInternal(parentNodeType, Constants.JCR_ANY_NAME);
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

      PropertyDefinitionData def = getPropertyDefinitionInternal(parentNodeType, childName, multiValued);

      // try residual def
      if (def == null)
      {
         return getPropertyDefinitionInternal(parentNodeType, Constants.JCR_ANY_NAME, multiValued);
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
         PropertyDefinitionData def = getPropertyDefinitionInternal(nt, propertyName, false);
         if (def != null && pdefs.getDefinition(def.isMultiple()) == null)
            pdefs.setDefinition(def); // set if same is not exists

         // multi-valued
         def = getPropertyDefinitionInternal(nt, propertyName, true);
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
            addNodeDefinitionDataInternal(name, nodeDef.getName(), rnt, nodeDef);
            if (LOG.isDebugEnabled())
            {
               LOG.debug("NodeDef added: parent NT: " + name.getAsString() + " child nodeName: "
                  + nodeDef.getName().getAsString() + " childNT: " + rnt.getAsString());
            }
         }

         // put default node definition
         addNodeDefinitionDataInternal(name, nodeDef.getName(), nodeDef);

         if (LOG.isDebugEnabled())
         {
            LOG.debug("Default NodeDef added: parent NT: " + name.getAsString() + " child nodeName: "
               + nodeDef.getName());
         }
      }

      // put prop defs
      PropertyDefinitionData[] propDefs = nodeType.getDeclaredPropertyDefinitions();
      for (PropertyDefinitionData propDef : propDefs)
      {
         addPropertyDefinitionInternal(name, propDef.getName(), propDef.isMultiple(), propDef);
         
         if (LOG.isDebugEnabled())
         {
            LOG.debug("PropDef added: parent NT: " + name.getAsString() + " child propName: "
               + propDef.getName().getAsString() + " isMultiple: " + propDef.isMultiple());
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
            removeNodeDefinitionDataInternal(name, nodeDef.getName(), rnt);
            if (LOG.isDebugEnabled())
            {
               LOG.debug("NodeDef removed: parent NT: " + name.getAsString() + " child nodeName: "
                  + nodeDef.getName().getAsString() + " childNT: " + rnt.getAsString());
            }
         }

         // remove default node definition
         removeNodeDefinitionDataInternal(name, nodeDef.getName());
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Default NodeDef removed: parent NT: " + name.getAsString() + " child nodeName: "
               + nodeDef.getName());
         }         
      }

      // remove defs
      PropertyDefinitionData[] propDefs = nodeType.getDeclaredPropertyDefinitions();
      for (PropertyDefinitionData propDef : propDefs)
      {
         removePropertyDefinitionInternal(name, propDef.getName(), propDef.isMultiple());
         if (LOG.isDebugEnabled())
         {
            LOG.debug("PropDef remode: parent NT: " + name.getAsString() + " child propName: "
               + propDef.getName().getAsString() + " isMultiple: " + propDef.isMultiple());
         }
      }
   }

   private NodeDefinitionData getNodeDefinitionFromThisOrSupertypes(InternalQName parentNodeType,
      InternalQName childName, InternalQName childNodeType)
   {

      NodeDefinitionData def = getNodeDefinitionDataInternal(parentNodeType, childName, childNodeType);
      if (def != null)
         return def;

      return def;
   }

   private NodeDefinitionData getNodeDefinitionDataInternal(InternalQName parentNodeType, InternalQName childName)
   {
      Map<InternalQName, NodeDefinitionData> defs = defNodeDefinitions.get(parentNodeType);
      if (defs == null)
      {
         return null;
      }
      return defs.get(childName);
   }

   private void addNodeDefinitionDataInternal(InternalQName parentNodeType, InternalQName childName,
      NodeDefinitionData nodeDef)
   {
      Map<InternalQName, NodeDefinitionData> defs = defNodeDefinitions.get(parentNodeType);
      if (defs == null)
      {
         defs = new HashMap<InternalQName, NodeDefinitionData>();
         defNodeDefinitions.put(parentNodeType, defs);
      }

      defs.put(childName, nodeDef);
   }

   private void removeNodeDefinitionDataInternal(InternalQName parentNodeType, InternalQName childName)
   {
      Map<InternalQName, NodeDefinitionData> defs = defNodeDefinitions.get(parentNodeType);
      if (defs == null)
      {
         return;
      }
      defs.remove(childName);
   }

   private PropertyDefinitionData getPropertyDefinitionInternal(InternalQName parentNodeType, InternalQName childName,
      boolean multiValued)
   {
      Map<InternalQName, Map<Boolean, PropertyDefinitionData>> defs = propertyDefinitions.get(parentNodeType);
      if (defs == null)
      {
         return null;
      }
      Map<Boolean, PropertyDefinitionData> def = defs.get(childName);
      if (def == null)
      {
         return null;
      }
      return def.get(multiValued);
   }

   private void addPropertyDefinitionInternal(InternalQName parentNodeType, InternalQName childName,
      boolean multiValued, PropertyDefinitionData propDef)
   {
      Map<InternalQName, Map<Boolean, PropertyDefinitionData>> defs = propertyDefinitions.get(parentNodeType);
      if (defs == null)
      {
         defs = new HashMap<InternalQName, Map<Boolean, PropertyDefinitionData>>();
         propertyDefinitions.put(parentNodeType, defs);
      }
      Map<Boolean, PropertyDefinitionData> def = defs.get(childName);
      if (def == null)
      {
         def = new HashMap<Boolean, PropertyDefinitionData>();
         defs.put(childName, def);         
      }

      def.put(multiValued, propDef);      
   }
   
   private void removePropertyDefinitionInternal(InternalQName parentNodeType, InternalQName childName,
      boolean multiValued)
   {
      Map<InternalQName, Map<Boolean, PropertyDefinitionData>> defs = propertyDefinitions.get(parentNodeType);
      if (defs == null)
      {
         return;
      }
      Map<Boolean, PropertyDefinitionData> def = defs.get(childName);
      if (def == null)
      {
         return;
      }
      def.remove(multiValued);      
   }
   
   private NodeDefinitionData getNodeDefinitionDataInternal(InternalQName parentNodeType, InternalQName childName,
      InternalQName childNodeType)
   {
      Map<InternalQName, Map<InternalQName, NodeDefinitionData>> defs = nodeDefinitions.get(parentNodeType);
      if (defs == null)
      {
         return null;
      }
      Map<InternalQName, NodeDefinitionData> def = defs.get(childName);
      if (def == null)
      {
         return null;
      }
      return def.get(childNodeType);
   }
   
   private void addNodeDefinitionDataInternal(InternalQName parentNodeType, InternalQName childName,
      InternalQName childNodeType, NodeDefinitionData nodeDef)
   {
      Map<InternalQName, Map<InternalQName, NodeDefinitionData>> defs = nodeDefinitions.get(parentNodeType);
      if (defs == null)
      {
         defs = new HashMap<InternalQName, Map<InternalQName, NodeDefinitionData>>();
         nodeDefinitions.put(parentNodeType, defs);
      }
      Map<InternalQName, NodeDefinitionData> def = defs.get(childName);
      if (def == null)
      {
         def = new HashMap<InternalQName, NodeDefinitionData>();
         defs.put(childName, def);         
      }

      def.put(childNodeType, nodeDef);           
   }
   
   private void removeNodeDefinitionDataInternal(InternalQName parentNodeType, InternalQName childName,
      InternalQName childNodeType)
   {
      Map<InternalQName, Map<InternalQName, NodeDefinitionData>> defs = nodeDefinitions.get(parentNodeType);
      if (defs == null)
      {
         return;
      }
      Map<InternalQName, NodeDefinitionData> def = defs.get(childName);
      if (def == null)
      {
         return;
      }
      def.remove(childNodeType);      
   }
   
   /**
    * Create copy of holder.
    * 
    * @return
    */
   protected ItemDefinitionDataHolder createCopy()
   {
      return new ItemDefinitionDataHolder(cloneMap(nodeDefinitions), cloneMap(propertyDefinitions),
         cloneMap(defNodeDefinitions));
   }
   
   @SuppressWarnings("unchecked")
   private static <K,V> Map<K,V> cloneMap(Map<? extends K, ? extends V> map) 
   {
      Map<K,V> copyMap = (Map<K,V>)((HashMap<K, V>)map).clone();
      
      for (Entry<K, V> entry : copyMap.entrySet())
      {
         if (entry.getValue() instanceof Map<?, ?>)
         {
            entry.setValue((V)cloneMap((Map<?, ?>)entry.getValue()));
         }
      }
      return copyMap;
   }
}
