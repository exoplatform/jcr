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
package org.exoplatform.services.jcr.webdav.lnkproducer;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Created by The eXo Platform SAS.
 *
 * Link Producer Service is a simple service, which generates an .lnk file,
 * that is compatible with the Microsoft link file format.
 *
 * @author : <a href="gavrikvetal@gmail.com">Vitaly Guly</a>.
 *
 */

@Path("/lnkproducer/")
public class LnkProducer implements ResourceContainer
{

   /**
    * logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.webdav.LnkProducer");

   private static final String URL_SUFFIX=".url";

   /**
    * Default constructor.
    */
   public LnkProducer()
   {
   }

   /**
    * Produces a link.
    * 
    * @param linkFilePath link file path
    * @param path path to resource
    * @param uriInfo uriInfo
    * @return generated link
    * @response
    * {code}
    *  "content" : the generated link file.
    * {code}
    * @LevelAPI Experimental
    */
   @GET
   @Path("/{linkFilePath}/")
   @Produces("application/octet-stream")
   public Response produceLink(@PathParam("linkFilePath") String linkFilePath, @QueryParam("path") String path,
      @Context UriInfo uriInfo)
   {

      String host = uriInfo.getRequestUri().getHost();
      String uri = uriInfo.getBaseUri().toString();

      try
      {
         byte[] content;
         if (linkFilePath != null && linkFilePath.endsWith(URL_SUFFIX))
         {
            URLGenerator urlGenerator = new URLGenerator(uri, path);
            content = urlGenerator.generateLinkContent();
         }
         else
         {
            LinkGenerator linkGenerator = new LinkGenerator(host, uri, path);
            content = linkGenerator.generateLinkContent();
         }

         return Response.ok(content, MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_LENGTH,
            Integer.toString(content.length)).build();

      }
      catch (IOException exc)
      {
         LOG.error(exc.getMessage(), exc);
         throw new WebApplicationException(exc);
      }

   }
}
