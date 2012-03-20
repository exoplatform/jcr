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
 */
public interface NodeTypeRepository extends ComponentPersister
{

   /**
    * @param nodeType
    * @param volatileNodeTypes
    * @throws RepositoryException 
    */
   void addNodeType(NodeTypeData nodeType, Map<InternalQName, NodeTypeData> volatileNodeTypes)
      throws RepositoryException;

   /**
    * @return
    */
   NodeTypeRepository createCopy();

   /**
    * @return
    * @throws RepositoryException 
    */
   List<NodeTypeData> getAllNodeTypes() throws RepositoryException;

   /**
    * @param nodeTypeName
    * @return
    * @throws RepositoryException 
    */
   Set<InternalQName> getDeclaredSubtypes(InternalQName nodeTypeName);

   /**
    * @param nodeName
    * @param nodeTypeNames
    * @return
    * @throws RepositoryException 
    */
   NodeDefinitionData getDefaultChildNodeDefinition(InternalQName nodeName, InternalQName[] nodeTypeNames)
      throws RepositoryException;

   /**
    * @param typeName
    * @return
    * @throws RepositoryException 
    */
   NodeTypeData getNodeType(InternalQName typeName);

   /**
    * @param propertyName
    * @param nodeTypeNames
    * @return
    * @throws RepositoryException 
    */
   PropertyDefinitionDatas getPropertyDefinitions(InternalQName propertyName, InternalQName[] nodeTypeNames)
      throws RepositoryException;

   /**
    * @param nodeTypeName
    * @return
    */
   Set<InternalQName> getSubtypes(InternalQName nodeTypeName);

   /**
    * @param ntname
    * @return
    */
   Set<InternalQName> getSupertypes(InternalQName ntname);

   /**
    * @param testTypeName
    * @param primaryType
    * @return
    */
   boolean isNodeType(InternalQName testTypeName, InternalQName primaryType);

   /**
    * @param testTypeName
    * @param typesNames
    * @return
    */
   boolean isNodeType(InternalQName testTypeName, InternalQName[] typesNames);

   /**
    * @param nodeTypeName
    * @param nodeType
    */
   void removeNodeType(NodeTypeData nodeType);

   /**
    * @param nodeTypeName
    * @param nodeType
    * @exception RepositoryException
    */
   void unregisterNodeType(NodeTypeData nodeType) throws RepositoryException;

   /**
    * Write node types to stream
    * 
    * @param os output stream
    * @param nodeTypes
    * @throws RepositoryException 
    */
   void registerNodeType(final List<NodeTypeData> nodeTypes, final NodeTypeDataManager nodeTypeDataManager,
      final String accessControlPolicy, final int alreadyExistsBehaviour) throws RepositoryException;

}
