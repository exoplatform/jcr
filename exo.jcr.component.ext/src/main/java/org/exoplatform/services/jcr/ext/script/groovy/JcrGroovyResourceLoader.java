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
package org.exoplatform.services.jcr.ext.script.groovy;

import org.exoplatform.services.jcr.ext.resource.JcrURLConnection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ext.groovy.DefaultGroovyResourceLoader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * JCR groovy resource resolver.
 */
public class JcrGroovyResourceLoader extends DefaultGroovyResourceLoader
{
   private static final Log LOG = ExoLogger.getLogger(JcrGroovyResourceLoader.class);

   private static URL[] normalizeJcrURL(URL[] src) throws MalformedURLException
   {
      URL[] res = new URL[src.length];
      for (int i = 0; i < src.length; i++)
      {
         if ("jcr".equals(src[i].getProtocol()))
         {
            String ref = src[i].getRef();
            if (ref == null)
            {
               ref = "/";
            }
            else if (ref.charAt(ref.length() - 1) != '/')
            {
               ref = ref + "/";
            }
            res[i] = new URL(src[i], "#" + ref);
         }
         else
         {
            res[i] = src[i];
         }
      }
      return res;
   }

   public JcrGroovyResourceLoader(URL[] roots, URL[] files) throws MalformedURLException
   {
      super(normalizeJcrURL(roots), files);
   }

   public JcrGroovyResourceLoader(URL[] roots) throws MalformedURLException
   {
      this(roots, new URL[0]);
   }

   public JcrGroovyResourceLoader(URL root) throws MalformedURLException
   {
      this(new URL[]{root}, new URL[0]);
   }

   /**
    * @see org.exoplatform.services.rest.ext.groovy.DefaultGroovyResourceLoader#getResource(java.lang.String)
    */
   @Override
   protected URL getResource(String filename) throws MalformedURLException
   {
      if (LOG.isDebugEnabled())
         LOG.debug("Process file: " + filename);

      URL resource = null;
      filename = filename.intern();
      synchronized (filename)
      {
         resource = resources.get(filename);
         boolean inCache = resource != null;
         if (inCache && !checkResource(resource))
            resource = null;
         for (int i = 0; i < files.length && resource == null; i++)
         {
            URL tmp = files[i];
            if (tmp.toString().endsWith(filename) && checkResource(tmp))
               resource = tmp;
         }
         for (int i = 0; i < roots.length && resource == null; i++)
         {
            // In JCR URL path represented by fragment jcr://repository/workspace#/path
            URL tmp =
               ("jcr".equals(roots[i].getProtocol())) ? new URL(roots[i], "#" + roots[i].getRef() + filename)
                  : new URL(roots[i], filename);
            if (checkResource(tmp))
               resource = tmp;
         }
         if (resource != null)
            resources.put(filename, resource);
         else if (inCache)
            resources.remove(filename);
      }
      return resource;
   }

   /**
    * @see org.exoplatform.services.rest.ext.groovy.DefaultGroovyResourceLoader#checkResource(java.net.URL)
    */
   @Override
   protected boolean checkResource(URL resource)
   {
      URLConnection connection = null;
      try
      {
         if (LOG.isDebugEnabled())
            LOG.debug("Try to load resource from URL : " + resource);

         connection = resource.openConnection();
         connection.getInputStream().close();

         return true;
      }
      catch (IOException e)
      {
         if (LOG.isDebugEnabled())
            LOG.debug("Can't open URL : " + resource);

         return false;
      }
      finally
      {
         if (connection != null && resource != null && "jcr".equals(resource.getProtocol()))
            ((JcrURLConnection)connection).disconnect();
      }
   }
}