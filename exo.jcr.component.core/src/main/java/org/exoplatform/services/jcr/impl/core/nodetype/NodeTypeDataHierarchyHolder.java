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

package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: NodeTypesHierarchyHolder.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class NodeTypeDataHierarchyHolder
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.NodeTypeDataHierarchyHolder");

   private volatile Map<InternalQName, NodeTypeHolder> nodeTypes;

   public NodeTypeDataHierarchyHolder()
   {
      this(new HashMap<InternalQName, NodeTypeHolder>());
   }

   /**
    * Helper counstructor for create copy method.
    * 
    * @param nodeTypes
    */
   public NodeTypeDataHierarchyHolder(Map<InternalQName, NodeTypeHolder> nodeTypes)
   {
      this.nodeTypes = Collections.unmodifiableMap(nodeTypes);
   }

   /**
    * @return
    */
   public List<NodeTypeData> getAllNodeTypes()
   {
      Collection<NodeTypeHolder> hs = nodeTypes.values();
      List<NodeTypeData> nts = new ArrayList<NodeTypeData>(hs.size());
      for (NodeTypeHolder nt : hs)
      {
         nts.add(nt.nodeType);
      }
      return nts;
   }

   /**
    * Returns the <i>direct</i> subtypes of this node type in the node type
    * inheritance hierarchy, that is, those which actually declared this node
    * type in their list of supertypes.
    * 
    * @return
    */
   public Set<InternalQName> getDeclaredSubtypes(final InternalQName nodeTypeName)
   {
      Set<InternalQName> resultSet = new HashSet<InternalQName>();
      for (Map.Entry<InternalQName, NodeTypeHolder> entry : nodeTypes.entrySet())
      {
         InternalQName[] declaredSupertypeNames = entry.getValue().nodeType.getDeclaredSupertypeNames();
         for (int i = 0; i < declaredSupertypeNames.length; i++)
         {
            if (nodeTypeName.equals(declaredSupertypeNames[i]))
               resultSet.add(entry.getKey());
         }
      }
      return resultSet;
   }

   /**
    * @param nodeTypeName
    * @return
    */
   public NodeTypeData getNodeType(final InternalQName nodeTypeName)
   {
      if (nodeTypeName != null)
      {
         final NodeTypeHolder nt = nodeTypes.get(nodeTypeName);
         if (nt != null)
            return nt.nodeType;
      }
      return null;
   }

   /**
    * @param nodeTypeName
    * @param volatileNodeTypes
    * @return
    */
   public NodeTypeData getNodeType(final InternalQName nodeTypeName, Map<InternalQName, NodeTypeData> volatileNodeTypes)
   {
      NodeTypeData nt = volatileNodeTypes.get(nodeTypeName);
      if (nt == null)
      {
         final NodeTypeHolder nth = nodeTypes.get(nodeTypeName);
         nt = nth != null ? nth.nodeType : null;
      }
      return nt;
   }

   /**
    * Returns all subtypes of this node type in the node type inheritance
    * hierarchy.
    * 
    * @param nodeTypeName
    * @return
    */
   public Set<InternalQName> getSubtypes(final InternalQName nodeTypeName)
   {
      Set<InternalQName> resultSet = new HashSet<InternalQName>();
      for (InternalQName ntName : nodeTypes.keySet())
      {
         if (getSupertypes(ntName).contains(nodeTypeName))
         {
            resultSet.add(ntName);
         }
      }
      return resultSet;
   }

   /**
    * @param nodeTypeName
    * @return
    */
   public Set<InternalQName> getSupertypes(final InternalQName nodeTypeName)
   {
      if (nodeTypeName != null)
      {
         final NodeTypeHolder nt = nodeTypes.get(nodeTypeName);
         if (nt != null)
            return nt.superTypes;
      }
      return new HashSet<InternalQName>();
   }

   /**
    * @param nodeTypeName
    * @param volatileNodeTypes
    * @return
    * @throws RepositoryException
    */
   public Set<InternalQName> getSupertypes(final InternalQName nodeTypeName,
      Map<InternalQName, NodeTypeData> volatileNodeTypes) throws RepositoryException
   {
      final NodeTypeHolder nt = nodeTypes.get(nodeTypeName);
      if (nt == null)
         throw new RepositoryException("Node type " + nodeTypeName.getAsString() + " not found");
      final Set<InternalQName> supers = new HashSet<InternalQName>();
      mergeAllSupertypes(supers, nt.nodeType.getDeclaredSupertypeNames(), volatileNodeTypes);
      return supers;
   }

   /**
    * @param testTypeName
    * @param typesNames
    * @return
    */
   public boolean isNodeType(final InternalQName testTypeName, final InternalQName... typesNames)
   {
      for (InternalQName typeName : typesNames)
      {
         if (testTypeName.equals(typeName))
            return true;

         NodeTypeHolder nt = nodeTypes.get(typeName);
         if (nt != null && (nt.superTypes.contains(testTypeName)))
            return true;
      }

      return false;
   }

   void addNodeType(final NodeTypeData nodeType, Map<InternalQName, NodeTypeData> volatileNodeTypes)
      throws RepositoryException
   {
      final Set<InternalQName> supers = new HashSet<InternalQName>();
      mergeAllSupertypes(supers, nodeType.getDeclaredSupertypeNames(), volatileNodeTypes);
      synchronized (this)
      {
         Map<InternalQName, NodeTypeHolder> nodeTypesTmp = new HashMap<InternalQName, NodeTypeHolder>(nodeTypes);
         nodeTypesTmp.put(nodeType.getName(), new NodeTypeHolder(nodeType, supers));
         this.nodeTypes = Collections.unmodifiableMap(nodeTypesTmp);         
      }
   }

   synchronized void removeNodeType(final InternalQName nodeTypeName)
   {
      Map<InternalQName, NodeTypeHolder> nodeTypesTmp = new HashMap<InternalQName, NodeTypeHolder>(nodeTypes);
      nodeTypesTmp.remove(nodeTypeName);
      this.nodeTypes = Collections.unmodifiableMap(nodeTypesTmp);         
   }

   protected synchronized void mergeAllSupertypes(Set<InternalQName> list, final InternalQName[] supers,
      Map<InternalQName, NodeTypeData> volatileNodeTypes) throws RepositoryException
   {

      if (supers != null)
      {
         for (InternalQName su : supers)
         {
            if (list.contains(su))
               continue;
            list.add(su);

            NodeTypeData volatileSuper = volatileNodeTypes.get(su);
            NodeTypeHolder ntSuper = nodeTypes.get(su);
            if (volatileSuper == null && ntSuper == null)
            {
               throw new RepositoryException("Node type " + su.getAsString() + " not found");
            }
            if (volatileSuper != null)
            {

               mergeAllSupertypes(list, volatileSuper.getDeclaredSupertypeNames(), volatileNodeTypes);

            }
            else
            {
               mergeAllSupertypes(list, ntSuper.superTypes.toArray(new InternalQName[ntSuper.superTypes.size()]),
                  volatileNodeTypes);
            }
         }
      }

   }

   /**
    * @return copy of holder.
    */
   protected NodeTypeDataHierarchyHolder createCopy()
   {
      return new NodeTypeDataHierarchyHolder(new HashMap<InternalQName, NodeTypeHolder>(nodeTypes));
   }

   class NodeTypeHolder
   {

      final NodeTypeData nodeType;

      final Set<InternalQName> superTypes;

      NodeTypeHolder(NodeTypeData nodeType, Set<InternalQName> superTypes)
      {
         this.nodeType = nodeType;
         this.superTypes = superTypes;
      }
   }
}
