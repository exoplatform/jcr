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

import org.exoplatform.commons.utils.PropertyManager;
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

   private static void help(String errorMessage)
   {
      StringBuilder sb = new StringBuilder();
      if (errorMessage != null)
      {
         sb.append(errorMessage + ", t");
      }
      else
      {
         sb.append("T");
      }
      sb.append("he expected arguments are: help|?|<configuration-file-path>|udp|tcp <initial-hosts>");
      System.err.println(sb.toString());//NOSONAR
   }

   /**
    * @param args
    */
   public static void main(String[] args) throws Exception
   {
      String configPath;
      if (args == null || args.length == 0)
      {
         configPath = DEFAULT_CONFIG_FILE_PATH;
      }
      else if (args.length == 1)
      {
         String arg = args[0];
         if ("help".equals(arg) || "?".equals(arg))
         {
            help(null);
            return;
         }
         else if ("udp".equals(arg))
         {
            configPath = DEFAULT_CONFIG_FILE_PATH;
         }
         else if ("tcp".equals(arg))
         {
            configPath = DEFAULT_CONFIG_FILE_PATH;
            addTCP2ProfileList();
            PropertyManager.setProperty("jgroups.bind_addr", "127.0.0.1");
            System.out.println("No initial hosts have been configured so the bind address "//NOSONAR
               + "has been automatically set to 127.0.0.1 assuming that it has been properly"
               + " configured to map to localhost");
         }
         else
         {
            configPath = arg;
         }
      }
      else if (args.length == 2)
      {
         String arg = args[0];
         if ("tcp".equals(arg))
         {
            configPath = DEFAULT_CONFIG_FILE_PATH;
            addTCP2ProfileList();
            PropertyManager.setProperty("jgroups.tcpping.initial_hosts", args[1]);
            System.out.println("The initial hosts have been configured to:" + args[1]);//NOSONAR
         }
         else
         {
            help("Unexpected syntax");
            return;
         }
      }
      else
      {
         help("Too many arguments");
         return;
      }
      System.out.println("The configuration file will be loaded from '" + configPath + "'");//NOSONAR

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

   private static void addTCP2ProfileList()
   {
      String profiles = PropertyManager.getProperty(PropertyManager.RUNTIME_PROFILES);
      StringBuilder sb = new StringBuilder();
      if (profiles != null && !profiles.isEmpty())
      {
         sb.append(profiles);
         sb.append(',');
      }
      sb.append("tcp");
      PropertyManager.setProperty(PropertyManager.RUNTIME_PROFILES, sb.toString());
      System.out.println("The tcp stack has been enabled");//NOSONAR            
   }

}
