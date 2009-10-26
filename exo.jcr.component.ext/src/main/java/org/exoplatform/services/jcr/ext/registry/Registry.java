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
package org.exoplatform.services.jcr.ext.registry;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.ext.common.NodeWrapper;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS .<br/>
 * JCR based Services Registry abstraction. As interchange object all the methods use Nodes'
 * wrappers to not to let using an arbitrary Type of Node. There is 2 phase modification of
 * RegistryEntry (1) get or create RegistryEntry retrieves or creates new object in memory and (2)
 * register/unregister stores the object permanently
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public abstract class Registry
{

   /**
    * Returns Registry node object which wraps Node of "exo:registry" type (the whole registry tree)
    * 
    * @param sessionProvider
    * @param repository
    * @return egistry node object
    * @throws RepositoryException
    */
   public abstract RegistryNode getRegistry(SessionProvider sessionProvider) throws RepositoryConfigurationException,
      RepositoryException;

   /**
    * Returns existed RegistryEntry which wraps Node of "exo:registryEntry" type
    * 
    * @param sessionProvider
    * @param entryPath
    * @return existed RegistryEntry
    * @throws PathNotFoundException
    *           if entry not found
    * @throws RepositoryException
    */
   public abstract RegistryEntry getEntry(SessionProvider sessionProvider, String entryPath)
      throws PathNotFoundException, RepositoryException;

   /**
    * creates an entry in the group. In a case if the group does not exist it will be silently
    * created as well
    * 
    * @param sessionProvider
    * @param groupPath
    *          related path (w/o leading slash) to group
    * @param entry
    * @throws RepositoryConfigurationException
    * @throws RepositoryException
    */
   public abstract void createEntry(SessionProvider sessionProvider, String groupPath, RegistryEntry entry)
      throws RepositoryException;

   /**
    * updates an entry in the group
    * 
    * @param sessionProvider
    * @param groupPath
    *          related path (w/o leading slash) to group
    * @param entry
    * @throws RepositoryConfigurationException
    * @throws RepositoryException
    */
   public abstract void recreateEntry(SessionProvider sessionProvider, String groupPath, RegistryEntry entry)
      throws RepositoryException;

   /**
    * removes entry located on entryPath (concatenation of group path / entry name)
    * 
    * @param sessionProvider
    * @param entryPath
    *          related path (w/o leading slash) to entry
    * @throws RepositoryConfigurationException
    * @throws RepositoryException
    */
   public abstract void removeEntry(SessionProvider sessionProvider, String entryPath) throws RepositoryException;

   /**
    * Internal Node wrapper which ensures the node of "exo:registry" type inside
    */
   public final class RegistryNode extends NodeWrapper
   {
      protected RegistryNode(final Node node) throws RepositoryException
      {
         super(node);
      }
   }

}
