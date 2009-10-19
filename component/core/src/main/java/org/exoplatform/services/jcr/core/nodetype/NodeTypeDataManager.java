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

import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

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
    * The standard XML content type to be used with XML-formatted node type
    * streams.
    */
   String TEXT_XML = "text/xml";

   /**
    * The experimental content type for the compact node type definition files.
    */
   String TEXT_X_JCR_CND = "text/x-jcr-cnd";

   /**
    * Returns all supertypes of this node type in the node type inheritance
    * hierarchy. For primary types apart from <code>nt:base</code>, this list
    * will always include at least <code>nt:base</code>. For mixin types, there
    * is no required supertype.
    */
   public Set<InternalQName> getSupertypes(final InternalQName nodeTypeName);

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
    * @param nodeTypeNames
    * @return
    * @throws RepositoryException 
    */
   NodeDefinitionData getChildNodeDefinition(InternalQName nodeName, InternalQName... nodeTypeNames)
      throws RepositoryException;

   /**
    * @param nodeName
    * @param primaryNodeType
    * @param mixinTypes
    * @return
    */
   NodeDefinitionData getChildNodeDefinition(InternalQName nodeName, InternalQName primaryNodeType,
      InternalQName[] mixinTypes) throws RepositoryException;

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
   List<ItemDefinitionData> getManadatoryItemDefs(InternalQName primaryNodeType, InternalQName[] mixinTypes)
      throws RepositoryException;

   /**
    * @param typeName
    * @return
    */
   NodeTypeData getNodeType(InternalQName typeName);

   /**
    * @param propertyName
    * @param nodeTypeNames
    * @return
    */
   PropertyDefinitionDatas getPropertyDefinitions(InternalQName propertyName, InternalQName... nodeTypeNames)
      throws RepositoryException;

   /**
    * @param propertyName
    * @param primaryNodeType
    * @param mixinTypes
    * @return
    */
   PropertyDefinitionDatas getPropertyDefinitions(InternalQName propertyName, InternalQName primaryNodeType,
      InternalQName[] mixinTypes) throws RepositoryException;

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
      InternalQName[] parentMixinNames) throws RepositoryException;

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
   boolean isOrderableChildNodesSupported(InternalQName primaryNodeType, InternalQName[] mixinTypes)
      throws RepositoryException;

   /**
    * @param xml
    * @param alreadyExistsBehaviour
    * @return
    * @throws RepositoryException
    */
   List<NodeTypeData> registerNodeTypes(InputStream xml, int alreadyExistsBehaviour, String contentType)
      throws RepositoryException;

   /**
    * @param ntValues
    * @param alreadyExistsBehaviour
    * @return
    * @throws RepositoryException
    */
   List<NodeTypeData> registerNodeTypes(List<NodeTypeValue> ntValues, int alreadyExistsBehaviour)
      throws RepositoryException;

   /**
    * Changes the primary node type of this node to nodeTypeName.
    * 
    * @param nodeData
    * @param nodeTypeName
    * @return
    * @throws RepositoryException
    */
   PlainChangesLog setPrimaryType(NodeData nodeData, InternalQName nodeTypeName) throws RepositoryException;

   /**
    * @param nodeTypeName
    * @throws RepositoryException
    */
   void unregisterNodeType(InternalQName nodeTypeName) throws RepositoryException;

   PlainChangesLog updateNodeType(NodeTypeData ancestorDefinition, NodeTypeData recipientDefinition,
      Map<InternalQName, NodeTypeData> volatileNodeTypes) throws ConstraintViolationException, RepositoryException;

}
