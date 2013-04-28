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

import org.exoplatform.services.jcr.core.ComponentPersister;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.datamodel.InternalQName;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 * @LevelAPI Unsupported
 */
public interface NodeTypeRepository extends ComponentPersister
{

   /**
    * Adds a new node type
    * 
    * @param nodeType the node type data to add
    * @param volatileNodeTypes a map defining all the volatile node types needed to add the new
    * node type
    * @throws RepositoryException if any error occurs
    */
   void addNodeType(NodeTypeData nodeType, Map<InternalQName, NodeTypeData> volatileNodeTypes)
      throws RepositoryException;

   /**
    * Gives a safe copy of the {{code language=java}}{@include org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeRepository}{{/code}}
    */
   NodeTypeRepository createCopy();

   /**
    * Gives all the existing node types
    * 
    * @throws RepositoryException if any error occurs
    */
   List<NodeTypeData> getAllNodeTypes() throws RepositoryException;

   /**
    * Gives all the declared super types of the given node type name
    * 
    * @param nodeTypeName the name of the node type for which we want the declared super types
    * @return the corresponding declared super type names
    * @throws RepositoryException if any error occurs
    */
   Set<InternalQName> getDeclaredSubtypes(InternalQName nodeTypeName);

   /**
    * Gives the default child node definition corresponding to the given property name 
    * and node type names
    * 
    * @param nodeName the name of the child node for which we want the definition
    * @param nodeTypeNames the node type names from which we need to find the child node definition
    * @return the corresponding default child node definition
    * @throws RepositoryException if any error occurs
    */
   NodeDefinitionData getDefaultChildNodeDefinition(InternalQName nodeName, InternalQName[] nodeTypeNames)
      throws RepositoryException;

   /**
    * Gives the node type data corresponding to the given node type name
    * 
    * @param typeName the node type name to retrieve
    * @return the {{code language=java}}{@include org.exoplatform.services.jcr.core.nodetype.NodeTypeData}{{/code}}
    * object representing the given node type name
    * @throws RepositoryException if any error occurs
    */
   NodeTypeData getNodeType(InternalQName typeName);

   /**
    * Gets the property definition corresponding to the given property name and node type names
    * 
    * @param propertyName the name of the property for which we want the definition
    * @param nodeTypeNames the node type names from which we need to find the property definition
    * @return the corresponding property definition
    * @throws RepositoryException if any error occurs
    */
   PropertyDefinitionDatas getPropertyDefinitions(InternalQName propertyName, InternalQName[] nodeTypeNames)
      throws RepositoryException;

   /**
    * Gives a set of all the node types that has the given node type name
    * as super type
    * 
    * @param nodeTypeName the name of the node type to check
    */
   Set<InternalQName> getSubtypes(InternalQName nodeTypeName);

   /**
    * Gives a set of all the super node types of the given node type name
    * 
    * @param ntname the name of the node type to check
    */
   Set<InternalQName> getSupertypes(InternalQName ntname);

   /**
    * Indicates whether a given node type name is of type of the given
    * primary type
    * 
    * @param testTypeName the node type name to test
    * @param primaryType the primary node type to check
    * @return <code>true</code> if the node type name to test is of type
    * of the given primary node type, <code>false</code> otherwise
    */
   boolean isNodeType(InternalQName testTypeName, InternalQName primaryType);

   /**
    * Indicates whether a given node type name is of type of one of the given
    * node type names
    * 
    * @param testTypeName the node type name to test
    * @param typesNames the node type names to check
    * @return <code>true</code> if the node type name to test is of type
    * of one of the given node type names, <code>false</code> otherwise
    */
   boolean isNodeType(InternalQName testTypeName, InternalQName[] typesNames);

   /**
    * Removes a node type from memory
    * 
    * @param nodeType the node type to remove
    */
   void removeNodeType(NodeTypeData nodeType);

   /**
    * Removes a node type from memory and persists the change
    * 
    * @param nodeType the node type to unregister
    * @exception RepositoryException if an error occurs
    */
   void unregisterNodeType(NodeTypeData nodeType) throws RepositoryException;

   /**
    * Registers new node types
    *
    * @param nodeTypes the list of node types
    * @param nodeTypeDataManager the node type data manager
    * @param accessControlPolicy  the access control policy
    * @param alreadyExistsBehavior the behavior to apply in case the node type already exists
    * @throws RepositoryException if an error occurs
    */
   void registerNodeType(List<NodeTypeData> nodeTypes, NodeTypeDataManager nodeTypeDataManager,
       String accessControlPolicy, int alreadyExistsBehavior) throws RepositoryException;

}
