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

package org.exoplatform.services.jcr.webdav.command;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.jcr.webdav.resource.FileResource;
import org.exoplatform.services.jcr.webdav.resource.Resource;
import org.exoplatform.services.jcr.webdav.resource.ResourceUtil;
import org.exoplatform.services.jcr.webdav.util.PropertyConstants;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ExtHttpHeaders;

import java.net.URI;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

/**
 * Created by The eXo Platform SAS.
 * @author Vitaly Guly - gavrikvetal@gmail.com
 * 
 * @version $Id: $
 */

public class HeadCommand
{

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.webdav.HeadCommand");

   /**
    * Webdav Head method implementation.
    * 
    * @param session current session
    * @param path resouce path
    * @param baseURI base uri
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response head(Session session, String path, String baseURI)
   {
      try
      {
         Node node = (Node)session.getItem(path);

         WebDavNamespaceContext nsContext = new WebDavNamespaceContext(session);
         URI uri = new URI(TextUtil.escape(baseURI + node.getPath(), '%', true));

         if (ResourceUtil.isFile(node))
         {
            Resource resource = new FileResource(uri, node, nsContext);

            String lastModified = resource.getProperty(PropertyConstants.GETLASTMODIFIED).getValue();
            String contentType = resource.getProperty(PropertyConstants.GETCONTENTTYPE).getValue();
            String contentLength = resource.getProperty(PropertyConstants.GETCONTENTLENGTH).getValue();

            return Response.ok().header(ExtHttpHeaders.LAST_MODIFIED, lastModified).header(ExtHttpHeaders.CONTENT_TYPE,
               contentType).header(ExtHttpHeaders.CONTENT_LENGTH, contentLength).build();
         }

         return Response.ok().build();
      }
      catch (PathNotFoundException exc)
      {
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         LOG.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

}
