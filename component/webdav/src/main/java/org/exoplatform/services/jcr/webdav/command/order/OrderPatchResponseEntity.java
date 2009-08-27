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
package org.exoplatform.services.jcr.webdav.command.order;

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import javax.jcr.Node;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

/**
 * Created by The eXo Platform SAS. Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a>
 * 
 * @version $Id: $
 */

public class OrderPatchResponseEntity implements StreamingOutput
{

   /**
    * logger.
    */
   private static Log log = ExoLogger.getLogger(OrderPatchResponseEntity.class);

   /**
    * Namespace context.
    */
   protected final WebDavNamespaceContext nsContext;

   /**
    * URI.
    */
   protected final URI uri;

   /**
    * Node.
    */
   protected Node node;

   /**
    * Order members.
    */
   protected List<OrderMember> members;

   /**
    * Constructor.
    * 
    * @param nsContext namespace context.
    * @param uri uri
    * @param node node
    * @param members order members
    */
   public OrderPatchResponseEntity(WebDavNamespaceContext nsContext, URI uri, Node node, List<OrderMember> members)
   {
      this.nsContext = nsContext;
      this.uri = uri;
      this.node = node;
      this.members = members;
   }

   /**
    * {@inheritDoc}
    */
   public void write(OutputStream outputStream) throws IOException
   {
      try
      {
         XMLStreamWriter xmlStreamWriter =
            XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, Constants.DEFAULT_ENCODING);
         xmlStreamWriter.setNamespaceContext(nsContext);
         xmlStreamWriter.setDefaultNamespace("DAV:");

         xmlStreamWriter.writeStartDocument();
         xmlStreamWriter.writeStartElement("D", "multistatus", "DAV:");
         xmlStreamWriter.writeNamespace("D", "DAV:");

         xmlStreamWriter.writeAttribute("xmlns:b", "urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882/");

         for (int i = 0; i < members.size(); i++)
         {
            OrderMember member = members.get(i);

            xmlStreamWriter.writeStartElement("DAV:", "response");

            xmlStreamWriter.writeStartElement("DAV:", "href");
            String href = uri.toASCIIString() + "/" + TextUtil.escape(member.getSegment(), '%', true);
            xmlStreamWriter.writeCharacters(href);
            xmlStreamWriter.writeEndElement();

            xmlStreamWriter.writeStartElement("DAV:", "status");
            xmlStreamWriter.writeCharacters(WebDavConst.getStatusDescription(member.getStatus()));
            xmlStreamWriter.writeEndElement();

            xmlStreamWriter.writeEndElement();
         }

         xmlStreamWriter.writeEndElement();
         xmlStreamWriter.writeEndDocument();

      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         throw new IOException();
      }

   }

}
