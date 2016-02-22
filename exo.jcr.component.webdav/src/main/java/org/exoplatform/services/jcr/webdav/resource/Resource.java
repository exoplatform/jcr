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
package org.exoplatform.services.jcr.webdav.resource;

import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.webdav.util.PropertyConstants;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;

import java.net.URI;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SARL .<br>
 * WebDAV applicable abstraction of REST Resource definition (by Fielding: "Any
 * information that can be named can be a resource... In other words: any
 * concept that might be the target of an author's hypertext reference must fit
 * within the definition of a resource") Here the REST resource abstraction is
 * some narrowed to the WebDAV needs
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public interface Resource extends PropertyConstants
{

   /**
    * File nodetype index.
    */
   public static final int FILE = 1;

   /**
    * Collection nodetype index.
    */
   public static final int COLLECTION = 2;

   /**
    * Version nodetype index.
    */
   public static final int VERSION = 4;

   /**
    * Versioned file nodetype index.
    */
   public static final int VERSIONED_FILE = 5;

   /**
    * Versioned collection nodetype index.
    */
   public static final int VERSIONED_COLLECTION = 6;

   /**
    * Version history nodetype index.
    */
   public static final int VERSION_HISTORY = 8;

   /**
    * Empty nodetype index.
    */
   public static final int NULL = 0;

   /**
    * @return resource identifier
    */
   URI getIdentifier();

   /**
    * @return resource type
    */
   int getType();

   /**
    * @param name property name
    * @return property by its name
    * @throws PathNotFoundException {@link PathNotFoundException}
    * @throws AccessDeniedException {@link AccessDeniedException}
    * @throws RepositoryException {@link RepositoryException}
    */
   HierarchicalProperty getProperty(QName name) throws PathNotFoundException, AccessDeniedException,
      RepositoryException;

   /**
    * @param namesOnly - if true "empty" properties will be returned (w/o values
    *          inside)
    * @return all properties belonging to this resource
    * @throws RepositoryException {@link RepositoryException}
    */
   Set<HierarchicalProperty> getProperties(boolean namesOnly) throws RepositoryException;

   /**
    * @return true if this is collection-able resource - i.e. this resource may
    *         contain other resources
    */
   boolean isCollection();

   /**
    * @return namespace context for this resource
    */
   WebDavNamespaceContext getNamespaceContext();

}
