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
package org.exoplatform.services.jcr.webdav.command;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.webdav.Depth;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.jcr.webdav.command.lock.LockRequestEntity;
import org.exoplatform.services.jcr.webdav.lock.NullResourceLocksHolder;
import org.exoplatform.services.jcr.webdav.resource.GenericResource;
import org.exoplatform.services.jcr.webdav.util.PropertyConstants;
import org.exoplatform.services.jcr.webdav.xml.PropertyWriteUtil;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

/**
 * Created by The eXo Platform SAS<br/>
 * .
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class LockCommand
{

   /**
    * logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.webdav.LockCommand");

   /**
    * Resource locks holder.
    */
   private final NullResourceLocksHolder nullResourceLocks;

   /**
    * Constructor.
    * 
    * @param nullResourceLocks resource locks
    */
   public LockCommand(final NullResourceLocksHolder nullResourceLocks)
   {
      this.nullResourceLocks = nullResourceLocks;
   }

   /**
    * Webdav Lock comand implementation.
    * 
    * @param session current session
    * @param path resource path
    * @param body request body
    * @param depth lock depth
    * @param timeout lock timeout
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response lock(Session session, String path, HierarchicalProperty body, Depth depth, String timeout)
   {

      boolean bodyIsEmpty = (body == null);
      String lockToken;
      try
      {
         WebDavNamespaceContext nsContext = new WebDavNamespaceContext(session);
         try
         {
            Node node = (Node)session.getItem(path);

            if (!node.isNodeType("mix:lockable"))
            {
               if (node.canAddMixin("mix:lockable"))
               {
                  node.addMixin("mix:lockable");
                  session.save();
               }
            }

            Lock lock;
            if (bodyIsEmpty)
            {
               lock = node.getLock();
               lock.refresh();

               body = new HierarchicalProperty(new QName("DAV", "activelock", "D"));
               HierarchicalProperty owner = new HierarchicalProperty(PropertyConstants.OWNER);
               HierarchicalProperty href = new HierarchicalProperty(new QName("D", "href"), lock.getLockOwner());
               body.addChild(owner).addChild(href);
            }
            else
            {
               lock = node.lock((depth.getIntValue() != 1), false);
            }
            lockToken = lock.getLockToken();
         }
         catch (PathNotFoundException pexc)
         {
            lockToken = nullResourceLocks.addLock(session, path);
         }

         LockRequestEntity requestEntity = new LockRequestEntity(body);

         lockToken = WebDavConst.Lock.OPAQUE_LOCK_TOKEN + ":" + lockToken;

         if (bodyIsEmpty)
         {
            return Response.ok(body(nsContext, requestEntity, depth, lockToken, requestEntity.getOwner(), timeout),
               "text/xml").build();
         }
         else
         {
            return Response
               .ok(body(nsContext, requestEntity, depth, lockToken, requestEntity.getOwner(), timeout), "text/xml")
               .header("Lock-Token", "<" + lockToken + ">").build();
         }

         // TODO 412 Precondition Failed ?
      }
      catch (LockException exc)
      {
         return Response.status(HTTPStatus.LOCKED).entity(exc.getMessage()).build();
      }
      catch (AccessDeniedException exc)
      {
         return Response.status(HTTPStatus.FORBIDDEN).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }

   }

   /**
    * Writes response body into the stream. 
    * 
    * @param nsContext name space context
    * @param input request body
    * @param depth lock depth
    * @param lockToken lock token key
    * @param lockOwner loco owner
    * @param timeout lock timeout
    * @return response body
    */
   private StreamingOutput body(WebDavNamespaceContext nsContext, LockRequestEntity input, Depth depth,
      String lockToken, String lockOwner, String timeout)
   {
      return new LockResultResponseEntity(nsContext, lockToken, lockOwner, timeout);
   }

   /**
    * @author Gennady Azarenkov
    */
   public class LockResultResponseEntity implements StreamingOutput
   {

      /**
       * Webdav namespace context.
       */
      protected WebDavNamespaceContext nsContext;

      /**
       * Lock token.
       */
      protected String lockToken;

      /**
       * Lock owner.
       */
      protected String lockOwner;

      /**
       * Lock timeout.
       */
      protected String timeOut;

      /**
       * Constructor.
       * 
       * @param nsContext namespace context
       * @param lockToken lock token
       * @param lockOwner lock owner
       * @param timeOut lock timeout
       */
      public LockResultResponseEntity(WebDavNamespaceContext nsContext, String lockToken, String lockOwner,
         String timeOut)
      {
         this.nsContext = nsContext;
         this.lockToken = lockToken;
         this.lockOwner = lockOwner;
         this.timeOut = timeOut;
      }

      /**
       * {@inheritDoc}
       */
      public void write(OutputStream stream) throws IOException
      {
         try
         {
            XMLStreamWriter xmlStreamWriter =
               XMLOutputFactory.newInstance().createXMLStreamWriter(stream, Constants.DEFAULT_ENCODING);
            xmlStreamWriter.setNamespaceContext(nsContext);

            xmlStreamWriter.writeStartDocument();

            xmlStreamWriter.writeStartElement("D", "prop", "DAV:");
            xmlStreamWriter.writeNamespace("D", "DAV:");

            HierarchicalProperty lockDiscovery = GenericResource.lockDiscovery(lockToken, lockOwner, timeOut);
            PropertyWriteUtil.writeProperty(xmlStreamWriter, lockDiscovery);

            xmlStreamWriter.writeEndElement();

            xmlStreamWriter.writeEndDocument();
         }
         catch (Exception e)
         {
            log.error(e.getMessage(), e);
            throw new IOException(e.getMessage());
         }
      }

   }

}
