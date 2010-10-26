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

   public JcrGroovyResourceLoader(URL[] roots) throws MalformedURLException
   {
      super(normalizeJcrURL(roots));
   }

   @Override
   protected URL getResource(String filename) throws MalformedURLException
   {
      filename = filename.intern();
      URL resource = null;
      synchronized (filename)
      {
         resource = resources.get(filename);
         boolean inCache = resource != null;
         for (URL root : roots)
         {
            if (resource == null)
            {
               if ("jcr".equals(root.getProtocol()))
               {
                  // In JCR URL path represented by fragment
                  // jcr://repository/workspace#/path
                  String ref = root.getRef();
                  resource = new URL(root, "#" + ref + filename);
               }
               else
               {
                  resource = new URL(root, filename);
               }
            }
            URLConnection connection = null;
            try
            {
               if (GroovyScript2RestLoader.LOG.isDebugEnabled())
                  GroovyScript2RestLoader.LOG.debug("Try to load resource from URL : " + resource);

               connection = resource.openConnection();
               connection.getInputStream().close();

               break;
            }
            catch (IOException e)
            {
               if (GroovyScript2RestLoader.LOG.isDebugEnabled())
                  GroovyScript2RestLoader.LOG.debug("Can't open URL : " + resource);

               resource = null;
            }
            finally
            {
               if (connection != null && resource != null && "jcr".equals(resource.getProtocol()))
               {
                  ((JcrURLConnection)connection).disconnect();
               }
            }
         }
         if (resource != null)
         {
            resources.put(filename, resource);
         }
         else if (inCache)
         {
            // Remove from map if resource is unreachable
            resources.remove(filename);
         }
      }
      return resource;
   }
}