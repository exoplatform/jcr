/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.infinispan;

import org.exoplatform.container.StandaloneContainer;

import java.net.URL;

/**
 * This class is used to be able to launch a cache instance as a JVM in standalone mode, it
 * will then be able to join an existing cluster of distributed caches.
 * It will be launched using the standalone container initialized thanks to the configuration
 * file whose path is expected as the first argument, by default it will
 * use <i>/conf/cache-server-configuration.xml</i> that is bundled into the current jar file.
 * 
 * The expected path is an absolute path or a relative path from the user directory
 * or from the {@link ClassLoader}.
 * 
 * Please note that this cache server should be used only for a distributed cache.
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class CacheServer
{

   private static final String DEFAULT_CONFIG_FILE_PATH = "/conf/cache-server-configuration.xml";

   /**
    * @param args
    */
   public static void main(String[] args) throws Exception
   {
      String configPath;
      if (args == null || args.length == 0)
      {
         configPath = DEFAULT_CONFIG_FILE_PATH;
         System.out.println("The configuration file will be loaded from '" + DEFAULT_CONFIG_FILE_PATH + "'");//NOSONAR
      }
      else if (args.length == 1)
      {
         configPath = args[0];
         System.out.println("The configuration file will be loaded from '" + args[0] + "'");//NOSONAR         
      }
      else
      {
         System.err.println("Too many arguments, the expected syntax is: java CacheServer <configuration-file-path>");//NOSONAR
         return;
      }

      URL configUrl = CacheServer.class.getResource(configPath);
      if (configUrl != null)
      {
         StandaloneContainer.addConfigurationURL(configUrl.toString());
      }
      else
      {
         StandaloneContainer.addConfigurationPath(configPath);
      }
      StandaloneContainer.getInstance();
   }

}
