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
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeTypeDataPersister;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class InmemoryNodeTypeRepository extends AbstractNodeTypeRepository
{
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.InmemoryNodeTypeRepository");

   private final ItemDefinitionDataHolder defsHolder;

   private final NodeTypeDataHierarchyHolder hierarchy;

   private boolean haveTypes = false;

   /**
    * @param defsHolder
    * @param hierarchy
    */
   public InmemoryNodeTypeRepository(ItemDefinitionDataHolder defsHolder, NodeTypeDataHierarchyHolder hierarchy,
      NodeTypeDataPersister nodeTypeDataPersister)
   {
      super(nodeTypeDataPersister);
      this.defsHolder = defsHolder;
      this.hierarchy = hierarchy;
   }

   /**
    * @param defsHolder
    * @param hierarchy
    */
   public InmemoryNodeTypeRepository(NodeTypeDataPersister nodeTypeDataPersister)
   {
      super(nodeTypeDataPersister);
      this.hierarchy = new NodeTypeDataHierarchyHolder();
      this.defsHolder = new ItemDefinitionDataHolder();
   }

   /**
    * {@inheritDoc}
    */
   public void addNodeType(NodeTypeData nodeType, Map<InternalQName, NodeTypeData> volatileNodeTypes)
      throws RepositoryException
   {

      hierarchy.addNodeType(nodeType, volatileNodeTypes);

      // put supers
      final Set<InternalQName> supers = hierarchy.getSupertypes(nodeType.getName(), volatileNodeTypes);

      for (final InternalQName superName : supers)
      {
         defsHolder.putDefinitions(nodeType.getName(), hierarchy.getNodeType(superName, volatileNodeTypes));
      }
      haveTypes = true;

      // put prop def
      defsHolder.putDefinitions(nodeType.getName(), nodeType);
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeTypeData> getAllNodeTypes()
   {
      if (!haveTypes)
         try
         {
            return super.getAllNodeTypes();
         }
         catch (RepositoryException e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }

      return hierarchy.getAllNodeTypes();
   }

   /**
    * {@inheritDoc}
    */
   public NodeDefinitionData getChildNodeDefinition(InternalQName parentTypeName, InternalQName nodeName,
      InternalQName nodeTypeName)
   {
      return defsHolder.getChildNodeDefinition(parentTypeName, nodeName, nodeTypeName);
   }

   /**
    * {@inheritDoc}
    */
   public Set<InternalQName> getDeclaredSubtypes(InternalQName nodeTypeName)
   {

      return hierarchy.getDeclaredSubtypes(nodeTypeName);
   }

   /**
    * {@inheritDoc}
    */
   public NodeDefinitionData getDefaultChildNodeDefinition(InternalQName nodeName, InternalQName[] nodeTypeNames)
   {

      return defsHolder.getDefaultChildNodeDefinition(nodeName, nodeTypeNames);
   }

   /**
    * {@inheritDoc}
    */
   public NodeTypeData getNodeType(InternalQName typeName)
   {

      return hierarchy.getNodeType(typeName);
   }

   /**
    * {@inheritDoc}
    */
   public NodeTypeData getNodeType(InternalQName superName, Map<InternalQName, NodeTypeData> volatileNodeTypes)
   {

      return hierarchy.getNodeType(superName, volatileNodeTypes);
   }

   /**
    * {@inheritDoc}
    */
   public PropertyDefinitionDatas getPropertyDefinitions(InternalQName propertyName, InternalQName[] nodeTypeNames)
   {

      return defsHolder.getPropertyDefinitions(propertyName, nodeTypeNames);
   }

   /**
    * {@inheritDoc}
    */
   public Set<InternalQName> getSubtypes(InternalQName nodeTypeName)
   {

      return hierarchy.getSubtypes(nodeTypeName);
   }

   /**
    * {@inheritDoc}
    */
   public Set<InternalQName> getSupertypes(InternalQName ntname)
   {

      return hierarchy.getSupertypes(ntname);
   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public Set<InternalQName> getSupertypes(InternalQName name, Map<InternalQName, NodeTypeData> volatileNodeTypes)
      throws RepositoryException
   {

      return hierarchy.getSupertypes(name, volatileNodeTypes);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNodeType(InternalQName testTypeName, InternalQName[] typesNames)
   {

      return hierarchy.isNodeType(testTypeName, typesNames);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNodeType(InternalQName testTypeName, InternalQName primaryType)
   {
      return hierarchy.isNodeType(testTypeName, primaryType);
   }

   /**
    * {@inheritDoc}
    */
   public NodeTypeRepository createCopy()
   {

      return new InmemoryNodeTypeRepository(defsHolder.createCopy(), hierarchy.createCopy(), nodeTypeDataPersister);
   }

   /**
    * {@inheritDoc}
    */
   public void removeNodeType(NodeTypeData nodeType)
   {
      InternalQName nodeTypeName = nodeType.getName();
      // put supers
      final Set<InternalQName> supers = hierarchy.getSubtypes(nodeTypeName);

      // remove from internal lists
      hierarchy.removeNodeType(nodeTypeName);

      // remove supers
      if (supers != null)
      {
         for (final InternalQName superName : supers)
         {
            defsHolder.removeDefinitions(nodeTypeName, hierarchy.getNodeType(superName));
         }
      }
      // remove it self
      defsHolder.removeDefinitions(nodeTypeName, nodeType);
      haveTypes = hierarchy.getAllNodeTypes().size() > 0;
   }
}
