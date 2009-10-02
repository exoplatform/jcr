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
package org.exoplatform.services.jcr.core.nodetype;

import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 25.11.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: NodeTypeDataManager.java 24494 2008-12-05 12:26:49Z pnedonosko
 *          $
 */
public interface NodeTypeDataManager
{

   /**
    * Returns all supertypes of this node type in the node type inheritance
    * hierarchy. For primary types apart from <code>nt:base</code>, this list
    * will always include at least <code>nt:base</code>. For mixin types, there
    * is no required supertype.
    */
   public Set<InternalQName> getSupertypes(final InternalQName nodeTypeName);

   //void addQueryHandler(QueryHandler queryHandler);

   /**
    * @param nodeName
    * @param nodeTypeNames
    * @return
    */
   NodeDefinitionData findChildNodeDefinition(InternalQName nodeName, InternalQName... nodeTypeNames);

   /**
    * @param nodeName
    * @param primaryNodeType
    * @param mixinTypes
    * @return
    */
   NodeDefinitionData findChildNodeDefinition(InternalQName nodeName, InternalQName primaryNodeType,
      InternalQName[] mixinTypes);

   /**
    * @param typeName
    * @return
    */
   NodeTypeData findNodeType(InternalQName typeName);

   /**
    * @param propertyName
    * @param primaryNodeType
    * @param mixinTypes
    * @return
    */
   PropertyDefinitionDatas findPropertyDefinitions(InternalQName propertyName, InternalQName primaryNodeType,
      InternalQName[] mixinTypes);

   /**
    * @param nodeTypeNames
    * @return
    */
   NodeDefinitionData[] getAllChildNodeDefinitions(InternalQName... nodeTypeNames);

   /**
    * Return all NodeTypes.
    * 
    * @return List of NodeTypeData
    * @throws RepositoryException in case of error
    */
   List<NodeTypeData> getAllNodeTypes();

   /**
    * @param nodeTypeNames
    * @return
    */
   PropertyDefinitionData[] getAllPropertyDefinitions(InternalQName... nodeTypeNames);

   /**
    * @param nodeName
    * @param nodeTypeName
    * @param parentTypeName
    * @return
    */
   NodeDefinitionData getChildNodeDefinition(InternalQName nodeName, InternalQName nodeTypeName,
      InternalQName parentTypeName);

   /**
    * Returns the <i>direct</i> subtypes of this node type in the node type
    * inheritance hierarchy, that is, those which actually declared this node
    * type in their list of supertypes.
    * 
    * @return
    */
   Set<InternalQName> getDeclaredSubtypes(final InternalQName nodeTypeName);

   /**
    * @param primaryNodeType
    * @param mixinTypes
    * @return
    */
   List<ItemDefinitionData> getManadatoryItemDefs(InternalQName primaryNodeType, InternalQName[] mixinTypes);

   /**
    * @param name
    * @return
    * @throws RepositoryException
    */
   Set<String> getNodes(InternalQName name) throws RepositoryException;

   /**
    * @param name
    * @param internalQNames
    * @param internalQNames2
    * @return
    * @throws RepositoryException
    */
   Set<String> getNodes(InternalQName name, InternalQName[] includeProperties, InternalQName[] excludeProperties)
      throws RepositoryException;

   /**
    * @param propertyName
    * @param nodeTypeNames
    * @return
    */
   PropertyDefinitionDatas getPropertyDefinitions(InternalQName propertyName, InternalQName... nodeTypeNames);

   /**
    * Returns all subtypes of this node type in the node type inheritance
    * hierarchy.
    * 
    * @param nodeTypeName
    * @return
    */
   Set<InternalQName> getSubtypes(final InternalQName nodeTypeName);

   /**
    * @param childNodeTypeName
    * @param parentNodeType
    * @param parentMixinNames
    * @return
    */
   boolean isChildNodePrimaryTypeAllowed(InternalQName childNodeTypeName, InternalQName parentNodeType,
      InternalQName[] parentMixinNames);

   /**
    * @param testTypeName
    * @param typeNames
    * @return
    */
   boolean isNodeType(InternalQName testTypeName, InternalQName... typeNames);

   /**
    * @param testTypeName
    * @param primaryNodeType
    * @param mixinNames
    * @return
    */
   boolean isNodeType(InternalQName testTypeName, InternalQName primaryNodeType, InternalQName[] mixinNames);

   /**
    * @param primaryNodeType
    * @param mixinTypes
    * @return
    */
   boolean isOrderableChildNodesSupported(InternalQName primaryNodeType, InternalQName[] mixinTypes);

   /**
    * Create PlainChangesLog of autocreated items to this node. No checks will be
    * passed for autocreated items.
    * 
    * @throws RepositoryException
    */
   PlainChangesLog makeAutoCreatedItems(NodeData parent, InternalQName nodeTypeName, ItemDataConsumer dataManager,
      String owner) throws RepositoryException;

   /**
    * @param nodeData
    * @param name
    * @param nodeDefinitionDatas
    * @param persister
    * @param owner
    * @return
    * @throws RepositoryException
    */
   ItemStateChangesLog makeAutoCreatedNodes(NodeData nodeData, InternalQName name,
      NodeDefinitionData[] nodeDefinitionDatas, ItemDataConsumer dataManager, String owner) throws RepositoryException;

   /**
    * @param nodeData
    * @param name
    * @param propertyDefinitionDatas
    * @param persister
    * @param owner
    * @return
    * @throws RepositoryException
    */
   PlainChangesLog makeAutoCreatedProperties(NodeData nodeData, InternalQName name,
      PropertyDefinitionData[] propertyDefinitionDatas, ItemDataConsumer dataManager, String owner)
      throws RepositoryException;

   /**
    * @param xml
    * @param alreadyExistsBehaviour
    * @return
    * @throws RepositoryException
    */
   List<NodeTypeData> registerNodeTypes(InputStream xml, int alreadyExistsBehaviour) throws RepositoryException;

   /**
    * @param ntValues
    * @param alreadyExistsBehaviour
    * @return
    * @throws RepositoryException
    */
   List<NodeTypeData> registerNodeTypes(List<NodeTypeValue> ntValues, int alreadyExistsBehaviour)
      throws RepositoryException;

   /**
    * @param nodeTypeName
    * @throws RepositoryException
    */
   void unregisterNodeType(InternalQName nodeTypeName) throws RepositoryException;
}
