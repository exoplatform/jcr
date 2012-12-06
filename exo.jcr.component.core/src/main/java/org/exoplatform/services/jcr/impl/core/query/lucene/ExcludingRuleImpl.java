/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.jcr.RepositoryException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ExcludingRuleImpl.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class ExcludingRuleImpl implements ExcludingRule
{

   /**
    * Attribute name nodeType
    */
   private static final String NODE_TYPE = "nodeType";

   /**
    * Attribute name path
    */
   private static final String PATH = "path";

   /**
    * Exclude path from the rule
    */
   private final QPath excludePath;

   /**
    * Exclude nodeType from the rule
    */
   private final InternalQName excludeNodeType;

   private final NodeTypeDataManager ntReg;

   /**
    * ExcludingRuleImpl constructor.
    */
   ExcludingRuleImpl(Node configNode, NodeTypeDataManager ntReg, LocationFactory resolver) throws IllegalNameException,
      RepositoryException
   {
      NamedNodeMap attributes = configNode.getAttributes();

      Node path = attributes.getNamedItem(PATH);
      this.excludePath =
         path == null ? null : new QPath(resolver.parseAbsPath(path.getNodeValue()).getInternalPath().getEntries());

      Node nodeType = attributes.getNamedItem(NODE_TYPE);
      this.excludeNodeType = nodeType == null ? null : resolver.parseJCRName(nodeType.getNodeValue()).getInternalName();

      this.ntReg = ntReg;
   }

   /**
    * {@inheritDoc}
    */
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((excludeNodeType == null) ? 0 : excludeNodeType.hashCode());
      result = prime * result + ((excludePath == null) ? 0 : excludePath.hashCode());

      return result;
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }

      if (obj == null || getClass() != obj.getClass())
      {
         return false;
      }

      ExcludingRuleImpl other = (ExcludingRuleImpl)obj;
      if (excludeNodeType == null && other.excludeNodeType != null)
      {
         return false;
      }
      else if (excludeNodeType != null && !excludeNodeType.equals(other.excludeNodeType))
      {
         return false;
      }

      if (excludePath == null && other.excludePath != null)
      {
         return false;
      }
      else if (excludePath != null && !excludePath.equals(other.excludePath))
      {
         return false;
      }

      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean suiteFor(NodeData state)
   {
      boolean suiteForPath = excludePath == null ? true : validateByPath(state);
      boolean suiteForNodeType = excludeNodeType == null ? true : validateByNodeType(state);

      return suiteForPath && suiteForNodeType;
   }

   private boolean validateByPath(NodeData state)
   {
      return state.getQPath().isDescendantOf(excludePath) || state.getQPath().equals(excludePath);
   }

   private boolean validateByNodeType(NodeData state)
   {
      return ntReg.isNodeType(excludeNodeType, state.getPrimaryTypeName(), state.getMixinTypeNames());
   }
}
