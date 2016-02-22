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
package org.exoplatform.services.jcr.ext.hierarchy;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

import javax.jcr.Node;

/**
 * This service is used as an helper to initialize the JCR
 * 
 * Created by The eXo Platform SAS Author : Dang Van Minh minh.dang@exoplatform.com Nov 15, 2007
 * 10:10:10 AM
 */
public interface NodeHierarchyCreator
{

   /**
    * Gets the JCR path corresponding to the given alias
    * @param alias the alias of the path to retrieve
    * @return the corresponding JCR path
    */
   String getJcrPath(String alias);

   /**
    * Initialize the given repository thanks to all the registered plugins
    * @param repository the repository to initialize
    * @throws Exception if an exception occurs
    * @deprecated use init() instead
    */
   void init(String repository) throws Exception;

   /**
    * Initialize the current repository thanks to all the registered plugins
    * @throws Exception if an exception occurs
    */
   void init() throws Exception;

   /**
    * Remove the JCR node corresponding to the root node of the user workspace
    * @param sessionProvider the session provider to use to remove the root node
    * @param userName the user name for which we want to remove the root node of his workspace
    * @throws Exception if an exception occurs
    */
   void removeUserNode(SessionProvider sessionProvider, String userName) throws Exception;

   /**
    * Gets the JCR node corresponding to the root node of the user workspace
    * @param sessionProvider the session provider to use to get the root node
    * @param userName the user name for which we want to find the root node of his workspace
    * @return the root node of the workspace of the given user
    * @throws Exception if an exception occurs
    */
   Node getUserNode(SessionProvider sessionProvider, String userName) throws Exception;

   /**
    * Gets the JCR node corresponding to the root node of the user's applications
    * @param sessionProvider the session provider to use to get the root node
    * @param userName the user name for which we want to find the root node of his applications
    * @return the root node of the user's applications of the given user
    * @throws Exception if an exception occurs
    */
   Node getUserApplicationNode(SessionProvider sessionProvider, String userName) throws Exception;

   /**
    * Gets the JCR node corresponding to the root node of the public applications
    * @param sessionProvider the session provider to use to get the root node
    * @return the root node of the public applications
    * @throws Exception if an exception occurs
    */
   Node getPublicApplicationNode(SessionProvider sessionProvider) throws Exception;

   /**
    * Registers a new plugins
    * @param plugin the plugin to register
    */
   void addPlugin(ComponentPlugin plugin);
}
