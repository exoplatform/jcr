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
import org.exoplatform.services.jcr.webdav.Range;
import org.exoplatform.services.jcr.webdav.WebDavConst.CacheConstants;
import org.exoplatform.services.jcr.webdav.resource.CollectionResource;
import org.exoplatform.services.jcr.webdav.resource.FileResource;
import org.exoplatform.services.jcr.webdav.resource.Resource;
import org.exoplatform.services.jcr.webdav.resource.ResourceUtil;
import org.exoplatform.services.jcr.webdav.resource.VersionResource;
import org.exoplatform.services.jcr.webdav.resource.VersionedFileResource;
import org.exoplatform.services.jcr.webdav.resource.VersionedResource;
import org.exoplatform.services.jcr.webdav.util.MultipartByterangesEntity;
import org.exoplatform.services.jcr.webdav.util.RangedInputStream;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.ext.provider.XSLTStreamingOutput;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.stream.StreamSource;

/**
 * Created by The eXo Platform SAS Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a>.
 * 
 * @version $Id: $
 */

public class GetCommand
{

   /**
    * Logger.
    */
   private static Log log = ExoLogger.getLogger(GetCommand.class);

   /**
    * GET content of the resource. Can be return content of the file. The content
    * returns in the XML type. If version parameter is present, returns the
    * content of the version of the resource.
    * 
    * @param session current session
    * @param path resource path
    * @param version version name
    * @param baseURI base uri
    * @param ranges ranges
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response get(Session session, String path, String version, String baseURI, List<Range> ranges, String ifModifiedSince)
   {

      if (version == null)
      {
         if (path.indexOf("?version=") > 0)
         {
            version = path.substring(path.indexOf("?version=") + "?version=".length());
            path = path.substring(0, path.indexOf("?version="));
         }
      }

      try
      {

         Node node = (Node)session.getItem(path);

         WebDavNamespaceContext nsContext = new WebDavNamespaceContext(session);
         URI uri = new URI(TextUtil.escape(baseURI + node.getPath(), '%', true));

         Resource resource;
         InputStream istream;

         if (ResourceUtil.isFile(node))
         {

            if (version != null)
            {
               VersionedResource versionedFile = new VersionedFileResource(uri, node, nsContext);
               resource = versionedFile.getVersionHistory().getVersion(version);
               istream = ((VersionResource)resource).getContentAsStream();
            }
            else
            {
               resource = new FileResource(uri, node, nsContext);
               istream = ((FileResource)resource).getContentAsStream();
            }

            HierarchicalProperty contentLengthProperty = resource.getProperty(FileResource.GETCONTENTLENGTH);
            long contentLength = new Long(contentLengthProperty.getValue());

            HierarchicalProperty mimeTypeProperty = resource.getProperty(FileResource.GETCONTENTTYPE);
            String contentType = mimeTypeProperty.getValue();

            FileResource fileResource = new FileResource(uri, node, nsContext);
            HierarchicalProperty lastModifiedProperty = fileResource.getProperty(FileResource.GETLASTMODIFIED);
            
            if((ifModifiedSince != null) && (ifModifiedSince.equals(lastModifiedProperty.getValue()))){
               return Response.notModified().build();
            }

            // content length is not present
            if (contentLength == 0)
            {
               return Response.ok().header(ExtHttpHeaders.ACCEPT_RANGES, "bytes").entity(istream).build();
            }

            // no ranges request

            if (ranges.size() == 0)
            {

               return Response.ok().header(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength)).header(
                  ExtHttpHeaders.ACCEPT_RANGES, "bytes").header(ExtHttpHeaders.LAST_MODIFIED,
                  lastModifiedProperty.getValue()).header(ExtHttpHeaders.CACHE_CONTROL,
                  generateCacheControl(contentType)).entity(istream).type(contentType).build();

            }

            // one range
            if (ranges.size() == 1)
            {
               Range range = ranges.get(0);
               if (!validateRange(range, contentLength))
                  return Response.status(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE).header(
                     ExtHttpHeaders.CONTENTRANGE, "bytes */" + contentLength).build();

               long start = range.getStart();
               long end = range.getEnd();
               long returnedContentLength = (end - start + 1);

               RangedInputStream rangedInputStream = new RangedInputStream(istream, start, end);

               return Response.status(HTTPStatus.PARTIAL).header(HttpHeaders.CONTENT_LENGTH,
                  Long.toString(returnedContentLength)).header(ExtHttpHeaders.ACCEPT_RANGES, "bytes").header(
                  ExtHttpHeaders.CONTENTRANGE, "bytes " + start + "-" + end + "/" + contentLength).entity(
                  rangedInputStream).build();
            }

            // multipart byte ranges as byte:0-100,80-150,210-300
            for (int i = 0; i < ranges.size(); i++)
            {
               Range range = ranges.get(i);
               if (!validateRange(range, contentLength))
                  return Response.status(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE).header(
                     ExtHttpHeaders.CONTENTRANGE, "bytes */" + contentLength).build();
               ranges.set(i, range);
            }

            MultipartByterangesEntity mByterangesEntity =
               new MultipartByterangesEntity(resource, ranges, contentType, contentLength);

            return Response.status(HTTPStatus.PARTIAL).header(ExtHttpHeaders.ACCEPT_RANGES, "bytes").entity(
               mByterangesEntity).build();
         }
         else
         {
            // Collection processing;
            resource = new CollectionResource(uri, node, nsContext);
            istream = ((CollectionResource)resource).getContentAsStream(baseURI);

            XSLTStreamingOutput entity = new XSLTStreamingOutput("get.method.template", new StreamSource(istream));

            return Response.ok(entity, MediaType.TEXT_HTML).build();

         }

      }
      catch (PathNotFoundException exc)
      {
         return Response.status(HTTPStatus.NOT_FOUND).build();
      }
      catch (RepositoryException exc)
      {
         return Response.serverError().build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().build();
      }
   }

   /**
    * Checks is the range is valid.
    * 
    * @param range range
    * @param contentLength coontent length
    * @return true if the range is valid else false
    */
   private boolean validateRange(Range range, long contentLength)
   {
      long start = range.getStart();
      long end = range.getEnd();

      // range set as bytes:-100
      // take 100 bytes from end
      if (start < 0 && end == -1)
      {
         if ((-1 * start) >= contentLength)
         {
            start = 0;
            end = contentLength - 1;
         }
         else
         {
            start = contentLength + start;
            end = contentLength - 1;
         }
      }

      // range set as bytes:100-
      // take from 100 to the end
      if (start >= 0 && end == -1)
         end = contentLength - 1;

      // normal range set as bytes:100-200
      // end can be greater then content-length
      if (end >= contentLength)
         end = contentLength - 1;

      if (start >= 0 && end >= 0 && start <= end)
      {
         range.setStart(start);
         range.setEnd(end);
         return true;
      }
      return false;
   }

   /**
    * Generates the value of Cache-Control header according to the content type.
    * 
    * @param contentType content type
    * @return Cache-Control value
    */
   private String generateCacheControl(String contentType)
   {
      if (contentType.contains("image"))
      {
         return CacheConstants.IMAGE_CACHE;
      }
      else if (contentType.contains("audio"))
      {
         return CacheConstants.AUDIO_CACHE;
      }
      return CacheConstants.NO_CACHE;
   }

}
