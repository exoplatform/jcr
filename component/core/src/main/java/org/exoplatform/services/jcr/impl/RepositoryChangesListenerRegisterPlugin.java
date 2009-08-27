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
package org.exoplatform.services.jcr.impl;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: StoreChangesPlugin.java 111 2008-11-11 11:11:11Z $
 */
public class RepositoryChangesListenerRegisterPlugin
   extends BaseComponentPlugin
{

   private String repositoryName = null;

   private String workspaces = null;

   private String listenerClassName = null;

   /**
    * StoreChangesPlugin constructor.
    * 
    * @param params
    */
   public RepositoryChangesListenerRegisterPlugin(InitParams params)
   {
      if (params != null)
      {
         ValueParam valueParam = params.getValueParam("repository-name");
         if (valueParam != null)
         {
            repositoryName = valueParam.getValue();
         }

         valueParam = params.getValueParam("workspaces");
         if (valueParam != null)
         {
            workspaces = valueParam.getValue();
         }

         valueParam = params.getValueParam("component-class-name");
         if (valueParam != null)
         {
            listenerClassName = valueParam.getValue();
         }
      }
   }

   /**
    * Return repository name.
    * 
    * @return The repository name
    */
   public String getRepositoryName()
   {
      return repositoryName;
   }

   /**
    * Return workspaces.
    * 
    * @return The workspaces
    */
   public String getWorkspaces()
   {
      return workspaces;
   }

   /**
    * Return listenerClassName.
    * 
    * @return The listenerClassName.
    */
   public String getListenerClassName()
   {
      return listenerClassName;
   }

}
