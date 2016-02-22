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
package org.exoplatform.services.jcr.ext.resource;

import org.exoplatform.common.util.HierarchicalProperty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public interface NodeRepresentation
{

   /**
    * @return Mimetype for this representation.
    */
   String getMediaType() throws RepositoryException;

   /**
    * @return the content length or -1 if content length unknown
    * @throws RepositoryException
    */
   long getContentLenght() throws RepositoryException;

   /**
    * @return the content encoding or null if it unknown.
    */
   String getContentEncoding();

   /**
    * @return the stream.
    * @throws IOException
    * @throws RepositoryException
    */
   InputStream getInputStream() throws IOException, RepositoryException;

   /**
    * @return the collection of node properties name.
    */
   Collection<String> getPropertyNames() throws RepositoryException;

   /**
    * @param name
    *          the name of properties.
    * @return the property with specified name. Note that there can be multiple same name properties,
    *         in this case any one will be returned.
    */
   HierarchicalProperty getProperty(String name) throws RepositoryException;

   /**
    * @param name
    *          the name of properties.
    * @return the properties with specified name.
    */
   Collection<HierarchicalProperty> getProperties(String name) throws RepositoryException;

   /**
    * adds single property.
    * 
    * @param property
    *          .
    */
   void addProperty(HierarchicalProperty property) throws UnsupportedRepositoryOperationException;

   /**
    * adds multivalued property.
    * 
    * @param properties
    *          .
    */
   void addProperties(Collection<HierarchicalProperty> properties) throws UnsupportedRepositoryOperationException;

   /**
    * removes property.
    * 
    * @param name
    *          .
    */
   void removeProperty(String name) throws UnsupportedRepositoryOperationException;

   /**
    * Get date of last modified if available.
    * 
    * @return the date of last modified.
    * @throws RepositoryException
    */
   long getLastModified() throws RepositoryException;

   /**
    * @return the underlying node.
    */
   Node getNode();

}
