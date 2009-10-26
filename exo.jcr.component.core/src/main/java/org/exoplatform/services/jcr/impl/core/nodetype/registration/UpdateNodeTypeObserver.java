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
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS. Helps to add changes in content in
 * repository in case of node type update.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public interface UpdateNodeTypeObserver
{
   /**
    * Call after update.
    * 
    * @param updatetNodetype
    * @param context
    */
   void afterUpdate(NodeTypeData updatetNodetype, Object context) throws RepositoryException;

   /**
    * Call before update.
    * 
    * @param updatetNodetype
    * @param context
    * @throws RepositoryException
    */
   void beforeUpdate(NodeTypeData updatetNodetype, Object context) throws RepositoryException;

   /**
    *  Should the registration of node type be skipped.
    *  For example if  ExtendedNodeTypeManager.IGNORE_IF_EXISTS flag is set.  
    * 
    * @param updatetNodetype
    * @param context
    * @throws RepositoryException 
    * @throws RepositoryException
    */
   boolean shouldSkip(NodeTypeData updatetNodetype, Object context) throws RepositoryException;
}
