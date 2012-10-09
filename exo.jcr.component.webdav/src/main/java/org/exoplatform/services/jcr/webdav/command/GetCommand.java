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
import org.exoplatform.services.jcr.webdav.WebDavConst;
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
import org.exoplatform.services.rest.impl.header.MediaTypeHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
 * @version $Id$
 */

public class GetCommand
{

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.webdav.GetCommand");

   private Map<String, String> xsltParams;

   public GetCommand()
   {
   }

   public GetCommand(Map<String, String> xsltParams)
   {
      this.xsltParams = xsltParams;
   }

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
   public Response get(Session session, String path, String version, String baseURI, List<Range> ranges,
      String ifModifiedSince, String ifNoneMatch, Map<MediaType, String> cacheControls)
   {
      if (version == null)
      {
         if (path.indexOf("?version=") > 0)
         {
            version = path.substring(path.indexOf("?version=") + "?version=".length());
            path = path.substring(0, path.indexOf("?version="));
         }
      }

      InputStream istream = null;
      try
      {
         Node node = (Node)session.getItem(path);

         WebDavNamespaceContext nsContext = new WebDavNamespaceContext(session);
         URI uri = new URI(TextUtil.escape(baseURI + node.getPath(), '%', true));

         Resource resource;

         if (ResourceUtil.isFile(node))
         {
            HierarchicalProperty lastModifiedProperty; 
            String resourceEntityTag;

            if (version != null)
            {
               VersionedResource versionedFile = new VersionedFileResource(uri, node, nsContext);
               resource = versionedFile.getVersionHistory().getVersion(version);

               lastModifiedProperty = resource.getProperty(FileResource.GETLASTMODIFIED);
            }
            else
            {
               resource = new FileResource(uri, node, nsContext);

               lastModifiedProperty = resource.getProperty(FileResource.GETLASTMODIFIED);
            }

            resourceEntityTag = ResourceUtil.generateEntityTag(node, lastModifiedProperty.getValue());

            // check before any other reads
            if (ifNoneMatch != null)
            {
               if ("*".equals(ifNoneMatch))
               {
                  return Response.notModified().entity("Not Modified").build();
               }
               for (String eTag : ifNoneMatch.split(","))
               {
                  if (resourceEntityTag.equals(eTag))
                  {
                     return Response.notModified().entity("Not Modified").build();
                  }
               }
            }
            else if (ifModifiedSince != null)
            {
               DateFormat dateFormat = new SimpleDateFormat(WebDavConst.DateFormat.MODIFICATION, Locale.US);
               Date lastModifiedDate = dateFormat.parse(lastModifiedProperty.getValue());

               dateFormat = new SimpleDateFormat(WebDavConst.DateFormat.IF_MODIFIED_SINCE_PATTERN, Locale.US);
               Date ifModifiedSinceDate = dateFormat.parse(ifModifiedSince);

               if (ifModifiedSinceDate.getTime() >= lastModifiedDate.getTime())
               {
                  return Response.notModified().entity("Not Modified").build();
               }
            }

            HierarchicalProperty contentLengthProperty = resource.getProperty(FileResource.GETCONTENTLENGTH);
            long contentLength = new Long(contentLengthProperty.getValue());

            // content length is not present
            if (contentLength == 0)
            {
               istream = openStream(resource, version != null);
               return Response.ok().header(ExtHttpHeaders.ACCEPT_RANGES, "bytes").entity(istream).build();
            }

            HierarchicalProperty mimeTypeProperty = resource.getProperty(FileResource.GETCONTENTTYPE);
            String contentType = mimeTypeProperty.getValue();

            // no ranges request
            if (ranges.size() == 0)
            {
               istream = openStream(resource, version != null);

               return Response.ok().header(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength))
                  .header(ExtHttpHeaders.ACCEPT_RANGES, "bytes")
                  .header(ExtHttpHeaders.LAST_MODIFIED, lastModifiedProperty.getValue())
                  .header(ExtHttpHeaders.ETAG, resourceEntityTag)
                  .header(ExtHttpHeaders.CACHE_CONTROL, generateCacheControl(cacheControls, contentType))
                  .entity(istream).type(contentType).build();
            }

            // one range
            if (ranges.size() == 1)
            {
               Range range = ranges.get(0);
               if (!validateRange(range, contentLength))
               {
                  return Response.status(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                     .header(ExtHttpHeaders.CONTENTRANGE, "bytes */" + contentLength).build();
               }

               long start = range.getStart();
               long end = range.getEnd();
               long returnedContentLength = (end - start + 1);

               istream = openStream(resource, version != null);
               RangedInputStream rangedInputStream = new RangedInputStream(istream, start, end);

               return Response.status(HTTPStatus.PARTIAL)
                  .header(HttpHeaders.CONTENT_LENGTH, Long.toString(returnedContentLength))
                  .header(ExtHttpHeaders.ACCEPT_RANGES, "bytes")
                  .header(ExtHttpHeaders.LAST_MODIFIED, lastModifiedProperty.getValue())
                  .header(ExtHttpHeaders.ETAG, resourceEntityTag)
                  .header(ExtHttpHeaders.CONTENTRANGE, "bytes " + start + "-" + end + "/" + contentLength)
                  .entity(rangedInputStream).type(contentType).build();
            }

            // multipart byte ranges as byte:0-100,80-150,210-300
            for (int i = 0; i < ranges.size(); i++)
            {
               Range range = ranges.get(i);
               if (!validateRange(range, contentLength))
               {
                  return Response.status(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                     .header(ExtHttpHeaders.CONTENTRANGE, "bytes */" + contentLength).build();
               }
               ranges.set(i, range);
            }

            MultipartByterangesEntity mByterangesEntity =
               new MultipartByterangesEntity(resource, ranges, contentType, contentLength);

            return Response.status(HTTPStatus.PARTIAL).header(ExtHttpHeaders.ACCEPT_RANGES, "bytes")
               .header(ExtHttpHeaders.LAST_MODIFIED, lastModifiedProperty.getValue()).entity(mByterangesEntity)
               .header(ExtHttpHeaders.ETAG, resourceEntityTag)
               .type(ExtHttpHeaders.MULTIPART_BYTERANGES + WebDavConst.BOUNDARY).build();
         }
         else
         {
            // Collection processing;
            resource = new CollectionResource(uri, node, nsContext);
            istream = ((CollectionResource)resource).getContentAsStream(baseURI);

            XSLTStreamingOutput entity =
               new XSLTStreamingOutput("get.method.template", new StreamSource(istream), xsltParams);

            return Response.ok(entity, MediaType.TEXT_HTML).build();

         }

      }
      catch (PathNotFoundException exc)
      {
         closeStream(istream);
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
      }
      catch (RepositoryException exc)
      {
         closeStream(istream);

         LOG.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         closeStream(istream);

         LOG.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
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
   private String generateCacheControl(Map<MediaType, String> cacheControlMap, String contentType)
   {

      ArrayList<MediaType> mediaTypesList = new ArrayList<MediaType>(cacheControlMap.keySet());
      Collections.sort(mediaTypesList, MediaTypeHelper.MEDIA_TYPE_COMPARATOR);
      String cacheControlValue = "no-cache";

      if (contentType == null || contentType.equals(""))
      {
         return cacheControlValue;
      }

      for (MediaType mediaType : mediaTypesList)
      {
         if (contentType.equals(MediaType.WILDCARD))
         {
            cacheControlValue = cacheControlMap.get(MediaType.WILDCARD_TYPE);
            break;
         }
         else if (mediaType.isCompatible(new MediaType(contentType.split("/")[0], contentType.split("/")[1])))
         {
            cacheControlValue = cacheControlMap.get(mediaType);
            break;
         }
      }
      return cacheControlValue;
   }

   private InputStream openStream(Resource resource, boolean isVersionableResource) throws RepositoryException
   {
      return isVersionableResource ? ((VersionResource)resource).getContentAsStream() : ((FileResource)resource)
         .getContentAsStream();
   }

   private void closeStream(InputStream istream)
   {
      if (istream != null)
      {
         try
         {
            istream.close();
         }
         catch (IOException e)
         {
            LOG.error("Can't close the stream", e);
         }
      }
   }

}
