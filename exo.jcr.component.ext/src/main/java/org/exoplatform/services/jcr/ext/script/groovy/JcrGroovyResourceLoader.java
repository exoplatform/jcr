/**
 * Copyright (C) 2010 eXo Platform SAS.
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

import groovy.lang.GroovyResourceLoader;

import org.exoplatform.services.jcr.ext.resource.JcrURLConnection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
class JcrGroovyResourceLoader implements GroovyResourceLoader
{

   private static final Log LOG = ExoLogger.getLogger(JcrGroovyResourceLoader.class);

   private int maxEntries = 512;

   private final Map<String, URL> resources = Collections.synchronizedMap(new LinkedHashMap<String, URL>()
   {
      protected boolean removeEldestEntry(Entry<String, URL> eldest)
      {
         return size() > maxEntries;
      }
   });

   private URL[] roots;

   public JcrGroovyResourceLoader(URL[] roots) throws MalformedURLException
   {
      this.roots = normalizeJcrURL(roots);
   }

   /**
    * {@inheritDoc}
    */
   public URL loadGroovySource(String classname) throws MalformedURLException
   {
      final String filename = (classname.replace('.', '/') + ".groovy").intern();
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
               if (LOG.isDebugEnabled())
                  LOG.debug("Try to load resource from URL : " + resource);

               connection = resource.openConnection();
               resource.openStream().close();

               break;
            }
            catch (IOException e)
            {
               if (LOG.isDebugEnabled())
                  LOG.debug("Can't open URL : " + resource);

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

}
