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
import org.exoplatform.services.jcr.webdav.util.DeltaVConstants;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;

import javax.jcr.*;
import javax.jcr.version.Version;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;

/**
 * Created by The eXo Platform SARL .<br>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class VersionResource extends GenericResource
{

   /**
    * resource version.
    */
   protected final Version version;

   /**
    * versioned resource.
    */
   protected final VersionedResource versionedResource;

   /**
    * @param identifier resource identifier
    * @param versionedResource resource
    * @param version version
    * @param namespaceContext namespace context
    */
   public VersionResource(final URI identifier, VersionedResource versionedResource, Version version,
      final WebDavNamespaceContext namespaceContext)
   {
      super(VERSION, identifier, namespaceContext);
      this.version = version;
      this.versionedResource = versionedResource;
   }

   /**
    * {@inheritDoc}
    */
   public HierarchicalProperty getProperty(QName name) throws PathNotFoundException, AccessDeniedException,
      RepositoryException
   {
      if (DeltaVConstants.VERSIONNAME.equals(name))
      {
         return new HierarchicalProperty(name, version.getName());
      }
      else if (DeltaVConstants.DISPLAYNAME.equals(name))
      {
         return new HierarchicalProperty(name, decodeValue(version.getName()));
      }
      else if (DeltaVConstants.VERSIONHISTORY.equals(name))
      {
         return new HierarchicalProperty(name);
      }
      else if (DeltaVConstants.CHECKEDIN.equals(name))
      {

         HierarchicalProperty checkedInProperty = new HierarchicalProperty(name);
         HierarchicalProperty href = checkedInProperty.addChild(new HierarchicalProperty(new QName("DAV:", "href")));
         href.setValue(identifier.toASCIIString());
         return checkedInProperty;

      }
      else if (DeltaVConstants.PREDECESSORSET.equals(name))
      {
         Version[] predecessors = version.getPredecessors();
         HierarchicalProperty predecessorsProperty = new HierarchicalProperty(name);
         for (Version curVersion : predecessors)
         {
            if ("jcr:rootVersion".equals(curVersion.getName()))
            {
               continue;
            }

            String versionHref =
               versionedResource.getIdentifier().toASCIIString() + "/?version=" + curVersion.getName();
            HierarchicalProperty href =
               predecessorsProperty.addChild(new HierarchicalProperty(new QName("DAV:", "href")));
            href.setValue(versionHref);
         }
         return predecessorsProperty;

      }
      else if (DeltaVConstants.SUCCESSORSET.equals(name))
      {
         Version[] successors = version.getSuccessors();
         HierarchicalProperty successorsProperty = new HierarchicalProperty(name);
         for (Version curVersion : successors)
         {
            String versionHref =
               versionedResource.getIdentifier().toASCIIString() + "/?version=" + curVersion.getName();
            HierarchicalProperty href =
               successorsProperty.addChild(new HierarchicalProperty(new QName("DAV:", "href")));
            href.setValue(versionHref);
         }
         return successorsProperty;

      }
      else if (DeltaVConstants.RESOURCETYPE.equals(name))
      {
         HierarchicalProperty resourceType = new HierarchicalProperty(name);
         if (versionedResource.isCollection())
         {
            // new HierarchicalProperty("DAV:", "collection")
            resourceType.addChild(new HierarchicalProperty(new QName("DAV:", "collection")));
         }
         return resourceType;

      }
      else if (DeltaVConstants.GETCONTENTLENGTH.equals(name))
      {
         if (versionedResource.isCollection())
         {
            throw new PathNotFoundException();
         }
         HierarchicalProperty getContentLength = new HierarchicalProperty(name);
         Property jcrDataProperty = contentNode().getProperty("jcr:data");
         getContentLength.setValue("" + jcrDataProperty.getLength());
         return getContentLength;

      }
      else if (DeltaVConstants.GETCONTENTTYPE.equals(name))
      {
         if (versionedResource.isCollection())
         {
            throw new PathNotFoundException();
         }

         HierarchicalProperty getContentType = new HierarchicalProperty(name);
         Property mimeType = contentNode().getProperty("jcr:mimeType");
         getContentType.setValue(mimeType.getString());
         return getContentType;

      }
      else if (DeltaVConstants.CREATIONDATE.equals(name))
      {
         Calendar created = version.getNode("jcr:frozenNode").getProperty("jcr:created").getDate();
         HierarchicalProperty creationDate = new HierarchicalProperty(name, created, CREATION_PATTERN);
         creationDate.setAttribute("b:dt", "dateTime.tz");
         return creationDate;

      }
      else if (DeltaVConstants.GETLASTMODIFIED.equals(name))
      {
         Calendar created = version.getNode("jcr:frozenNode").getProperty("jcr:created").getDate();
         HierarchicalProperty creationDate = new HierarchicalProperty(name, created, MODIFICATION_PATTERN);
         creationDate.setAttribute("b:dt", "dateTime.1123");
         return creationDate;

      }
      else
      {
         throw new PathNotFoundException();
      }

   }

   /**
    * {@inheritDoc}
    */
   public final boolean isCollection()
   {
      return false;
   }

   /**
    * Returns content node.
    * 
    * @return content node
    * @throws RepositoryException {@link RepositoryException}
    */
   public Node contentNode() throws RepositoryException
   {
      return version.getNode("jcr:frozenNode").getNode("jcr:content");
   }

   /**
    * Returns the content of node as a stream.
    * 
    * @return content as stream
    * @throws RepositoryException {@link RepositoryException}
    */
   public InputStream getContentAsStream() throws RepositoryException
   {
      return contentNode().getProperty("jcr:data").getStream();
   }

}
