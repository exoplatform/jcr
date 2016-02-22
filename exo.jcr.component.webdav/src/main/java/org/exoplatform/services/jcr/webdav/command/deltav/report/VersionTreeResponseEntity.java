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
package org.exoplatform.services.jcr.webdav.command.deltav.report;

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.webdav.resource.IllegalResourceTypeException;
import org.exoplatform.services.jcr.webdav.resource.VersionResource;
import org.exoplatform.services.jcr.webdav.resource.VersionedResource;
import org.exoplatform.services.jcr.webdav.xml.PropertyWriteUtil;
import org.exoplatform.services.jcr.webdav.xml.PropstatGroupedRepresentation;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Created by The eXo Platform SAS
 * @author Vitaly Guly - gavrikvetal@gmail.com
 * 
 * @version $Id: $
 */

public class VersionTreeResponseEntity implements StreamingOutput
{

   /**
    * logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.webdav.VersionTreeResponseEntity");

   /**
    * XML writer.
    */
   protected XMLStreamWriter xmlStreamWriter;

   /**
    * Name space context.
    */
   protected final WebDavNamespaceContext namespaceContext;

   /**
    * The list of versions.
    */
   private Set<VersionResource> versions;

   /**
    * The list of properties.
    */
   private Set<QName> properties;

   /**
    * Constructor.
    * 
    * @param namespaceContext namespace context
    * @param versionedResource resource 
    * @param properties list of properties
    * @throws RepositoryException {@link RepositoryException}
    * @throws IllegalResourceTypeException {@link IllegalResourceTypeException}
    */
   public VersionTreeResponseEntity(WebDavNamespaceContext namespaceContext, VersionedResource versionedResource,
      Set<QName> properties) throws RepositoryException, IllegalResourceTypeException
   {
      this.namespaceContext = namespaceContext;
      this.properties = properties;
      versions = versionedResource.getVersionHistory().getVersions();
   }

   /**
    * {@inheritDoc}
    */
   public void write(OutputStream outputStream) throws IOException
   {
      try
      {
         this.xmlStreamWriter =
            XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, Constants.DEFAULT_ENCODING);
         xmlStreamWriter.setNamespaceContext(namespaceContext);

         xmlStreamWriter.writeStartDocument();
         xmlStreamWriter.writeStartElement("D", "multistatus", "DAV:");
         xmlStreamWriter.writeNamespace("D", "DAV:");

         xmlStreamWriter.writeAttribute("xmlns:b", "urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882/");

         Iterator<VersionResource> versionIterator = versions.iterator();
         while (versionIterator.hasNext())
         {
            VersionResource versionResource = versionIterator.next();
            xmlStreamWriter.writeStartElement("DAV:", "response");

            xmlStreamWriter.writeStartElement("DAV:", "href");
            xmlStreamWriter.writeCharacters(versionResource.getIdentifier().toASCIIString());
            xmlStreamWriter.writeEndElement();

            PropstatGroupedRepresentation propstat =
               new PropstatGroupedRepresentation(versionResource, properties, false);
            PropertyWriteUtil.writePropStats(xmlStreamWriter, propstat.getPropStats());

            xmlStreamWriter.writeEndElement();
         }

         xmlStreamWriter.writeEndElement();
         xmlStreamWriter.writeEndDocument();
      }
      catch (XMLStreamException exc)
      {
         log.error(exc.getMessage(), exc);
         throw new IOException(exc.getMessage(), exc);
      }
      catch (RepositoryException exc)
      {
         log.error(exc.getMessage(), exc);
         throw new IOException(exc.getMessage(), exc);
      }
   }

}
