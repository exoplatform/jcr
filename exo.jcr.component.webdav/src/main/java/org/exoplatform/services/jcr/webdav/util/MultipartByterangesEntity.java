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
package org.exoplatform.services.jcr.webdav.util;

import org.exoplatform.services.jcr.webdav.Range;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.jcr.webdav.resource.FileResource;
import org.exoplatform.services.jcr.webdav.resource.Resource;
import org.exoplatform.services.jcr.webdav.resource.VersionResource;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ExtHttpHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.StreamingOutput;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class MultipartByterangesEntity implements StreamingOutput
{

   /**
    * logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.webdav.MultipartByterangesEntity");

   /**
    * resource.
    */
   private final Resource resource_;

   /**
    * ranges.
    */
   private final List<Range> ranges_;

   /**
    * content-length.
    */
   private final long contentLength_;

   /**
    * content-type.
    */
   private final String contentType_;

   /**
    * @param resource resource
    * @param ranges ranges
    * @param contentType content-type
    * @param contentLength content-length
    */
   public MultipartByterangesEntity(Resource resource, List<Range> ranges, String contentType, long contentLength)
   {
      resource_ = resource;
      ranges_ = ranges;
      contentLength_ = contentLength;
      contentType_ = contentType;
   }

   /**
    * {@inheritDoc}
    */
   public void write(OutputStream ostream) throws IOException
   {
      try
      {
         for (Range range : ranges_)
         {
            InputStream istream = null;
            if (resource_ instanceof VersionResource)
               istream = ((VersionResource)resource_).getContentAsStream();
            else
               istream = ((FileResource)resource_).getContentAsStream();

            println(ostream);
            // boundary
            print("--" + WebDavConst.BOUNDARY, ostream);
            println(ostream);
            // content-type
            print(ExtHttpHeaders.CONTENT_TYPE + ": " + contentType_, ostream);
            println(ostream);
            // current range
            print(ExtHttpHeaders.CONTENTRANGE + ": bytes " + range.getStart() + "-" + range.getEnd() + "/"
               + contentLength_, ostream);
            println(ostream);
            println(ostream);
            // range data
            RangedInputStream rangedInputStream = new RangedInputStream(istream, range.getStart(), range.getEnd());

            byte buff[] = new byte[0x1000];
            int rd = -1;
            while ((rd = rangedInputStream.read(buff)) != -1)
               ostream.write(buff, 0, rd);
            rangedInputStream.close();
         }
         println(ostream);
         print("--" + WebDavConst.BOUNDARY + "--", ostream);
         println(ostream);
      }
      catch (IOException exc)
      {
         LOG.error(exc.getMessage(), exc);
         throw new IOException("Can't write to stream, caused " + exc, exc);
      }
      catch (RepositoryException exc)
      {
         LOG.error(exc.getMessage(), exc);
         throw new IOException("Can't write to stream, caused " + exc, exc);
      }
   }

   /**
    * Writes string into stream.
    * 
    * @param s string
    * @param ostream stream
    * @throws IOException {@link IOException}
    */
   private void print(String s, OutputStream ostream) throws IOException
   {
      int length = s.length();
      for (int i = 0; i < length; i++)
      {
         char c = s.charAt(i);
         ostream.write(c);
      }
   }

   /**
    * Writes a new line into stream.
    * @param ostream stream
    * @throws IOException {@link IOException}
    */
   private void println(OutputStream ostream) throws IOException
   {
      ostream.write('\r');
      ostream.write('\n');
   }

}
