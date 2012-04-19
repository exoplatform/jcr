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

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ObjectFactory;
import org.exoplatform.services.rest.ext.groovy.ResourceId;
import org.exoplatform.services.rest.resource.AbstractResourceDescriptor;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: GroovyScript2RestUpdateListener.java 34445 2009-07-24 07:51:18Z
 *          dkatayev $
 */
@SuppressWarnings("deprecation")
public class GroovyScript2RestUpdateListener implements EventListener
{
   /** Logger. */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.GroovyScript2RestUpdateListener");

   /** Repository name. */
   private final ManageableRepository repository;

   /** Workspace name. */
   private final String workspaceName;

   /** See {@link GroovyScript2RestLoader}. */
   private final GroovyScript2RestLoader groovyScript2RestLoader;

   /**
    * @param repository repository
    * @param workspace workspace name
    * @param groovyScript2RestLoader See {@link GroovyScript2RestLoader}
    * @param session JCR session
    */
   public GroovyScript2RestUpdateListener(ManageableRepository repository, String workspace,
      GroovyScript2RestLoader groovyScript2RestLoader)
   {
      this.repository = repository;
      this.workspaceName = workspace;
      this.groovyScript2RestLoader = groovyScript2RestLoader;
   }

   /**
    * {@inheritDoc}
    */
   public void onEvent(EventIterator eventIterator)
   {
      // waiting for Event.PROPERTY_ADDED, Event.PROPERTY_REMOVED,
      // Event.PROPERTY_CHANGED
      Session session = null;
      try
      {
         while (eventIterator.hasNext())
         {
            Event event = eventIterator.nextEvent();
            String path = event.getPath();

            if (path.endsWith("/jcr:data"))
            {
               // jcr:data removed 'exo:groovyResourceContainer' then unbind resource
               if (event.getType() == Event.PROPERTY_REMOVED)
               {
                  unloadScript(path.substring(0, path.lastIndexOf('/')));
               }
               else if (event.getType() == Event.PROPERTY_ADDED || event.getType() == Event.PROPERTY_CHANGED)
               {
                  if (session == null)
                  {
                     session = repository.getSystemSession(workspaceName);
                  }

                  Node node = session.getItem(path).getParent();
                  if (node.getProperty("exo:autoload").getBoolean())
                     loadScript(node);
               }
            }
         }
      }
      catch (Exception e)
      {
         LOG.error("Process event failed. ", e);
      }
      finally
      {
         if (session != null)
         {
            session.logout();
         }
      }
   }

   /**
    * Load script form supplied node.
    *
    * @param node JCR node
    * @throws Exception if any error occurs
    */
   private void loadScript(Node node) throws Exception
   {
      ResourceId key = new NodeScriptKey(repository.getConfiguration().getName(), workspaceName, node);
      ObjectFactory<AbstractResourceDescriptor> resource =
         groovyScript2RestLoader.groovyPublisher.unpublishResource(key);
      if (resource != null)
      {
         groovyScript2RestLoader.groovyPublisher.publishPerRequest(node.getProperty("jcr:data").getStream(), key,
            resource.getObjectModel().getProperties());
      }
      else
      {
         groovyScript2RestLoader.groovyPublisher.publishPerRequest(node.getProperty("jcr:data").getStream(), key, null);
      }
   }

   /**
    * Unload script.
    *
    * @param path unified JCR node path
    * @throws Exception if any error occurs
    */
   private void unloadScript(String path) throws Exception
   {
      ResourceId key = new NodeScriptKey(repository.getConfiguration().getName(), workspaceName, path);
      groovyScript2RestLoader.groovyPublisher.unpublishResource(key);
   }

}
