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
package org.exoplatform.services.jcr.impl.storage.inmemory;

import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.WorkspaceDataContainerBase;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: InmemoryContainerImpl.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class InmemoryContainerImpl extends WorkspaceDataContainerBase
{

   private static Log log = ExoLogger.getLogger("jcr.InmemoryContainerImpl");

   private String name;

   public InmemoryContainerImpl(WorkspaceEntry wsEntry) throws RepositoryException
   {

      this.name = wsEntry.getUniqueName();
      log.debug("ContainerImpl() name: " + name);
   }

   /**
    * {@inheritDoc}
    */
   public String getName()
   {
      return name;
   }

   /**
    * {@inheritDoc}
    */
   public String getInfo()
   {
      String str = "Info: Inmemory (for testing only) based container \n";
      str += "Name: " + name + "\n";
      return str;
   }

   /**
    * {@inheritDoc}
    */
   public WorkspaceStorageConnection openConnection()
   {
      return new InmemoryStorageConnection(name);
   }

   public WorkspaceStorageConnection reuseConnection(WorkspaceStorageConnection original) throws RepositoryException
   {
      return openConnection();
   }

   public String getStorageVersion()
   {
      return "1.0";
   }

   /**
    * {@inheritDoc}
    */
   public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
   {
      return new InmemoryStorageConnection(name);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSame(WorkspaceDataContainer another)
   {
      return this.equals(another);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isCheckSNSNewConnection()
   {
      return true;
   }
}
