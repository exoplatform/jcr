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
import org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeTypeReadException;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id$
 *
 */
public interface NodeTypeData
{

   /**
    * 
    * @return
    * @throws RepositoryException
    * @throws NodeTypeReadException
    */
   public NodeDefinitionData[] getDeclaredChildNodeDefinitions();;

   /**
    * 
    * @return
    * @throws RepositoryException
    * @throws NodeTypeReadException
    */
   public PropertyDefinitionData[] getDeclaredPropertyDefinitions();

   /**
    * 
    * @return
    * @throws RepositoryException
    * @throws NodeTypeReadException
    */
   public InternalQName[] getDeclaredSupertypeNames();

   /**
    * 
    * @return
    * @throws RepositoryException
    * @throws NodeTypeReadException
    */
   public InternalQName getPrimaryItemName();

   /**
    * 
    * @return
    * @throws RepositoryException
    * @throws NodeTypeReadException
    */
   public InternalQName getName();

   /**
    * 
    * @return
    * @throws RepositoryException
    * @throws NodeTypeReadException
    */
   public boolean hasOrderableChildNodes();

   /**
    * 
    * @return
    * @throws RepositoryException
    * @throws NodeTypeReadException
    */
   public boolean isMixin();
}