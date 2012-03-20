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

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.resource.UnifiedNodeReference;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class GroovyScriptAddRepoPlugin extends BaseComponentPlugin
{

   /** Logger. */
   private static final Log LOG = ExoLogger.getLogger(GroovyScriptAddRepoPlugin.class);

   private final InitParams params;

   private final RepositoryService repositoryService;

   public GroovyScriptAddRepoPlugin(InitParams params, RepositoryService repoService)
   {
      this.params = params;
      this.repositoryService = repoService;
   }

   @SuppressWarnings("unchecked")
   public Collection<URL> getRepositories()
   {
      if (params == null)
         return Collections.emptyList();

      final Set<URL> repos = new HashSet<URL>();
      Iterator<PropertiesParam> iterator = params.getPropertiesParamIterator();
      while (iterator.hasNext())
      {
         PropertiesParam p = iterator.next();
         final String repository = getWorkingRepositoryName(p);

         final String workspace = p.getProperty("workspace");
         final String path = p.getProperty("path");
         try
         {
            SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
            {
               public Void run() throws MalformedURLException
               {
                  repos.add(new UnifiedNodeReference(repository, workspace, path).getURL());
                  return null;
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            // MalformedURLException
            LOG.error("Failed add groovy script repository. " + e.getCause().getMessage());
         }
      }
      return repos;
   }

   /**
    * Get working repository name. Returns the repository name from configuration 
    * if it previously configured and returns the name of current repository in other case.
    *
    * @props  PropertiesParam
    *           the properties parameters 
    * @return String
    *           repository name
    * @throws RepositoryException
    */
   private String getWorkingRepositoryName(PropertiesParam props)
   {
      
      if (props.getProperty("repository") == null)
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
         return props.getProperty("repository");
      }
   }
}
