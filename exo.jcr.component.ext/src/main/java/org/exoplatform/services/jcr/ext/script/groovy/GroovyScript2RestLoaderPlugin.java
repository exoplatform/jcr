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

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: GroovyScript2RestLoaderPlugin.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class GroovyScript2RestLoaderPlugin extends BaseComponentPlugin
{

   /** Logger. */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.GroovyScript2RestLoaderPlugin");

   /** Repository service **/
   private final RepositoryService repositoryService;

   /** Configurations for scripts what were got from XML. */
   private List<XMLGroovyScript2Rest> l = new ArrayList<XMLGroovyScript2Rest>();

   /** Repository. */
   private String repository;

   /** Workspace. */
   private String workspace;

   /** Root node for scripts. If it does not exist new one will be created. */
   private String node;

   @SuppressWarnings("unchecked")
   public GroovyScript2RestLoaderPlugin(InitParams params, RepositoryService repoServiceo)
   {
      this.repositoryService = repoServiceo;

      repository = params.containsKey("repository") ? params.getValueParam("repository").getValue() : null;

      workspace = params.getValueParam("workspace").getValue();
      node = params.getValueParam("node").getValue();
      Iterator<PropertiesParam> iterator = params.getPropertiesParamIterator();
      while (iterator.hasNext())
      {
         PropertiesParam p = iterator.next();
         String name = p.getName();
         boolean autoload = Boolean.valueOf(p.getProperty("autoload"));
         String path = p.getProperty("path");
         if (LOG.isDebugEnabled())
            LOG.debug("Read new script configuration " + name);
         l.add(new XMLGroovyScript2Rest(name, path, autoload));
      }
   }

   public List<XMLGroovyScript2Rest> getXMLConfigs()
   {
      return l;
   }

   /**
    * Get working repository name. Returns the repository name from configuration 
    * if it previously configured and returns the name of current repository in other case.
    *
    * @return String
    *           repository name
    * @throws RepositoryException
    */
   public String getRepository()
   {
      if (repository == null)
      {
         try
         {
            return repositoryService.getCurrentRepository().getConfiguration().getName();
         }
         catch (RepositoryException e)
         {
            throw new RuntimeException("Can not get current repository and repository name was not configured", e);
         }
      }
      else
      {
         return repository;
      }
   }

   /**
    * @return the workspace
    */
   public String getWorkspace()
   {
      return workspace;
   }

   /**
    * @return the node
    */
   public String getNode()
   {
      return node;
   }

}
