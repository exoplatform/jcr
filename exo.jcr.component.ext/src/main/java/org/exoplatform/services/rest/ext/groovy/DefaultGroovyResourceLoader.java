/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.services.rest.ext.groovy;

import groovy.lang.GroovyResourceLoader;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: DefaultGroovyResourceLoader.java 2680 2010-06-22 11:43:00Z
 *          aparfonov $
 */
public class DefaultGroovyResourceLoader implements GroovyResourceLoader
{
   private static final String DEFAULT_SOURCE_FILE_EXTENSION = ".groovy";

   private static final Log LOG = ExoLogger.getLogger("exo.ws.rest.ext.DefaultGroovyResourceLoader");

   public final ConcurrentMap<String, Future<URL>> findResourceURLTasks = new ConcurrentHashMap<String, Future<URL>>();
   
   private int maxEntries = 512;

   protected final Map<String, URL> resources;

   protected final URL[] roots;

   @SuppressWarnings("serial")
   public DefaultGroovyResourceLoader(URL[] roots) throws MalformedURLException
   {
      this.roots = new URL[roots.length];
      for (int i = 0; i < roots.length; i++)
      {
         String str = roots[i].toString();
         if (str.charAt(str.length() - 1) != '/')
            this.roots[i] = new URL(str + '/');
         else
            this.roots[i] = roots[i];
      }
      resources = Collections.synchronizedMap(new LinkedHashMap<String, URL>()
      {
         protected boolean removeEldestEntry(Entry<String, URL> eldest)
         {
            return size() > maxEntries;
         }
      });
   }

   public DefaultGroovyResourceLoader(URL root) throws MalformedURLException
   {
      this(new URL[]{root});
   }

   /**
    * {@inheritDoc}
    */
   public final URL loadGroovySource(String filename) throws MalformedURLException
   {
      URL resource = null;
      final String ffilename = filename.replace('.', '/') + getSourceFileExtension();
      return getResource(ffilename);
   }

   protected URL getResource(final String filename) throws MalformedURLException
   {
      // First we check the cache outside the synchronized block
      URL resource = resources.get(filename);
      if (resource != null && checkResource(resource))
      {
         // The resource could be found in the cache and is reachable
         return resource;
      }
      // The resource cannot be found or is unreachable
      // Check if a corresponding findResourceURL task exists
      Future<URL> findResourceURLTask = findResourceURLTasks.get(filename);
      if (findResourceURLTask == null)
      {
         // The task doesn't exist so we create it
         FutureTask<URL> f = new FutureTask<URL>(new Callable<URL>()
         {
            public URL call() throws Exception
            {
               return findResourceURL(filename);
            }
         });
         // We add the new task to the existing tasks
         findResourceURLTask = findResourceURLTasks.putIfAbsent(filename, f);
         if (findResourceURLTask == null)
         {
            // The task has not be registered so we launch it
            findResourceURLTask = f;
            f.run();
         }
      }
      try
      {
         return findResourceURLTask.get();
      }
      catch (CancellationException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      catch (ExecutionException e)
      {
         throw (MalformedURLException)e.getCause();
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
      finally
      {
         findResourceURLTasks.remove(filename, findResourceURLTask);
      }
      return null;
   }

   protected URL findResourceURL(String filename) throws MalformedURLException
   {
      URL resource = resources.get(filename);
      boolean inCache = resource != null;
      if (inCache && !checkResource(resource))
         resource = null; // Resource in cache is unreachable.
      for (int i = 0; i < roots.length && resource == null; i++)
      {
         URL tmp = createURL(roots[i], filename);
         if (checkResource(tmp))
            resource = tmp;
      }
      if (resource != null)
         resources.put(filename, resource);
      else if (inCache)
         resources.remove(filename);
      return resource;
   }
   
   protected URL createURL(URL root, String filename) throws MalformedURLException
   {
      return new URL(root, filename);
   }
   
   protected String getSourceFileExtension()
   {
      return DEFAULT_SOURCE_FILE_EXTENSION;
   }

   protected boolean checkResource(URL resource)
   {
      try
      {
         resource.openStream().close();
         return true;
      }
      catch (IOException e)
      {
         return false;
      }
   }
}
