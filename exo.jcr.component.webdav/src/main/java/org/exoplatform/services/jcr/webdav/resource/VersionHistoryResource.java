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
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SARL .<br>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class VersionHistoryResource extends GenericResource
{

   /**
    * History of versions.
    */
   protected final VersionHistory versionHistory;

   /**
    * Versioned resource.
    */
   protected final VersionedResource versionedResource;

   /**
    * @param identifier resource identifier
    * @param versionHistory history of versions
    * @param versionedResource resource
    * @param namespaceContext namespace context
    * @throws IllegalResourceTypeException {@link IllegalResourceTypeException}
    * @throws RepositoryException {@link}
    */
   public VersionHistoryResource(final URI identifier, VersionHistory versionHistory,
      final VersionedResource versionedResource, final WebDavNamespaceContext namespaceContext)
      throws IllegalResourceTypeException, RepositoryException
   {
      super(VERSION_HISTORY, identifier, namespaceContext);
      this.versionHistory = versionHistory;
      this.versionedResource = versionedResource;
   }

   /**
    * {@inheritDoc}
    */
   public HierarchicalProperty getProperty(QName name) throws PathNotFoundException, AccessDeniedException,
      RepositoryException
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public final boolean isCollection()
   {
      return false;
   }

   /**
    * Returns all versions of a resource.
    * 
    * @return all versions of a resource
    * @throws RepositoryException {@link RepositoryException}
    * @throws IllegalResourceTypeException {@link IllegalResourceTypeException}
    */
   public Set<VersionResource> getVersions() throws RepositoryException, IllegalResourceTypeException
   {
      Set<VersionResource> resources = new HashSet<VersionResource>();
      VersionIterator versions = versionHistory.getAllVersions();
      while (versions.hasNext())
      {
         Version version = versions.nextVersion();
         if ("jcr:rootVersion".equals(version.getName()))
         {

            continue;
         }
         resources
            .add(new VersionResource(versionURI(version.getName()), versionedResource, version, namespaceContext));
      }
      return resources;
   }

   /**
    * Returns the version of resouce by name.
    * 
    * @param name version name
    * @return version of a resource
    * @throws RepositoryException {@link RepositoryException}
    * @throws IllegalResourceTypeException {@link IllegalResourceTypeException}
    */
   public VersionResource getVersion(String name) throws RepositoryException, IllegalResourceTypeException
   {
      return new VersionResource(versionURI(name), versionedResource, versionHistory.getVersion(name), namespaceContext);
   }

   /**
    * Returns URI of the resource version.
    * 
    * @param versionName version name
    * @return URI of the resource version
    */
   protected final URI versionURI(String versionName)
   {
      return URI.create(versionedResource.getIdentifier().toASCIIString() + "?version=" + versionName);
   }

}
