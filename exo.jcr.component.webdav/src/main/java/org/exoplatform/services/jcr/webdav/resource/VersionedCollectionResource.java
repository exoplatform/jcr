/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.webdav.resource;

import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;
import java.net.URI;

/**
 * Created by The eXo Platform SARL .<br>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class VersionedCollectionResource extends CollectionResource implements VersionedResource
{

   /**
    * 
    * @param identifier resource identifier
    * @param node node
    * @param namespaceContext namespace context
    * @throws IllegalResourceTypeException {@link IllegalResourceTypeException}
    * @throws RepositoryException {@link RepositoryException}
    */
   public VersionedCollectionResource(URI identifier, Node node, WebDavNamespaceContext namespaceContext)
      throws IllegalResourceTypeException, RepositoryException
   {
      super(VERSIONED_COLLECTION, identifier, node, namespaceContext);
   }

   /**
    * @return this resource versionhistory.
    * @throws RepositoryException {@link RepositoryException}
    * @throws IllegalResourceTypeException {@link IllegalResourceTypeException}
    */
   public VersionHistoryResource getVersionHistory() throws RepositoryException, IllegalResourceTypeException
   {
      return new VersionHistoryResource(versionHistoryURI(), node.getVersionHistory(), this, namespaceContext);
   }

   /**
    * returns versionhistory URI.
    * 
    * @return versionhistory URI
    */
   protected final URI versionHistoryURI()
   {
      return URI.create(identifier.toASCIIString() + "?vh");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public HierarchicalProperty getProperty(QName name) throws PathNotFoundException, AccessDeniedException,
      RepositoryException
   {
      if (name.equals(ISVERSIONED))
      {
         return new HierarchicalProperty(name, "1");
      }
      else if (name.equals(CHECKEDIN))
      {
         if (node.isCheckedOut())
         {
            throw new PathNotFoundException();
         }

         String checkedInHref = identifier.toASCIIString() + "?version=" + node.getBaseVersion().getName();
         HierarchicalProperty checkedIn = new HierarchicalProperty(name);
         checkedIn.addChild(new HierarchicalProperty(new QName("DAV:", "href"), checkedInHref));
         return checkedIn;

      }
      else if (name.equals(CHECKEDOUT))
      {
         if (!node.isCheckedOut())
         {
            throw new PathNotFoundException();
         }
         return new HierarchicalProperty(name);
      }
      else if (name.equals(VERSIONNAME))
      {
         return new HierarchicalProperty(name, decodeValue(node.getBaseVersion().getName()));
      }

      return super.getProperty(name);
   }

}
