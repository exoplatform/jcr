/*
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
package org.exoplatform.services.jcr.impl.core.lock;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.security.IdentityConstants;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;

/**
 * @author <a href="mailto:peter.nedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id:AbstractSessionLockManager.java 1111 2010-01-01 00:00:01Z pnedonosko $
 *
 */
public abstract class AbstractSessionLockManager implements SessionLockManager
{

   protected final SessionDataManager transientManager;

   public AbstractSessionLockManager(SessionDataManager transientManager)
   {
      this.transientManager = transientManager;
   }

   /**
    * Check if the node locked (locks persisted in internal storage).
    * @param node NodeData to check
    * @return boolean, <code>true</code> if the node is locked, <code>false</code> otherwise
    * @throws LockException
    */
   protected abstract boolean isLockedPersisted(NodeData node) throws LockException;

   protected abstract boolean isPersistedLockHolder(NodeData node) throws RepositoryException;

   /**
    * Checks the node, is it accessible according to possible locks. If node is locked and current 
    * session is not lockHolder <code> false</code> will be returned.
    *  
    * @param data - node that must be checked
    * @return true - if lock not exist or current session is LockOwner; false - in other case;
    * @throws LockException - if lock engine exception happens
    */
   protected abstract boolean checkPersistedLocks(NodeData node) throws LockException;

   /**
    * {@inheritDoc}
    */
   public boolean checkLocking(NodeData data) throws LockException
   {
      //check is new and find close persisted parent

      while (transientManager.isNew(data.getIdentifier()))
      {
         // The node is new, so we will check directly its parent instead
         try
         {
            data = (NodeData)transientManager.getItemData(data.getParentIdentifier());
            if (data == null)
            {
               // The node is the root node and is new, so we consider it as unlocked
               return true;
            }
         }
         catch (RepositoryException e)
         {
            throw new LockException(e);
         }
      }

      return checkPersistedLocks(data);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLockHolder(NodeImpl node) throws RepositoryException
   {
      NodeData nodeData = (NodeData)node.getData();

      // check if session has system credentials 
      if (IdentityConstants.SYSTEM.equals(node.getSession().getUserID()))
      {
         return true;
      }

      //check is parent node also new
      if (transientManager.isNew(nodeData.getIdentifier()) && transientManager.isNew(nodeData.getParentIdentifier()))
      {
         return true;
      }
      else
      {
         return isPersistedLockHolder(nodeData);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLockHolder(NodeData node) throws RepositoryException
   {
      //check is parent node also new
      if (transientManager.isNew(node.getIdentifier()) && transientManager.isNew(node.getParentIdentifier()))
      {
         return true;
      }
      else
      {
         return isPersistedLockHolder(node);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLocked(NodeData data) throws LockException
   {
      if (transientManager.isNew(data.getIdentifier()))
      {
         // The node is new, so we will check directly its parent instead
         try
         {
            NodeData parentData = (NodeData)transientManager.getItemData(data.getParentIdentifier());
            if (parentData == null)
            {
               // The node is the root node and is new, so we consider it as unlocked
               return false;
            }
            else
            {
               // the node has a parent that we need to test
               return isLocked(parentData);
            }
         }
         catch (RepositoryException e)
         {
            throw new LockException(e);
         }
      }
      else
      {
         // The node already exists so we need to check if it is locked
         return isLockedPersisted(data);
      }
   }

}
