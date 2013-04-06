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

import org.exoplatform.services.jcr.datamodel.InternalQName;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id$
 * @LevelAPI Unsupported
 */
public interface NodeTypeData
{
   /**
    * @return returns the node definitions of all the children nodes
    */
   public NodeDefinitionData[] getDeclaredChildNodeDefinitions();
   /**
    * @return returns the property definitions of the children properties
    */
   public PropertyDefinitionData[] getDeclaredPropertyDefinitions();
   /**
    * @return returns the name of all the super types
    */
   public InternalQName[] getDeclaredSupertypeNames();
   /**
    * @return returns the primary item name
    */
   public InternalQName getPrimaryItemName();
   /**
    * @return returns the name of the node type
    */
   public InternalQName getName();
   /**
    * @return returns <code>true</code> if orderable child nodes is supported, <code>false</code> otherwise
    */
   public boolean hasOrderableChildNodes();
   /**
    * @return returns <code>true</code> if the node type is a mixin, <code>false</code> otherwise
    */
   public boolean isMixin();
}