/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.lock.cacheable;

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.lock.AbstractSessionLockManager;
import org.exoplatform.services.jcr.impl.core.lock.LockImpl;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: CacheableSessionLockManager.java 2137 2010-03-25 15:31:56Z sergiykarpenko $
 */
public class CacheableSessionLockManager extends AbstractSessionLockManager
{
   /**
    * Logger
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.CacheableSessionLockManager");

   /**
    * Session identifier.
    */
   private final String sessionID;

   /**
    * Lock tokens held by this session.
    * [token name, tokens hash]
    */
   private final Map<String, String> tokens;

   /**
    * Map of nodes locked in this session. Need to remove session scoped lock on session close.
    * [node identifier, lock data]
    */
   private final Map<String, LockData> lockedNodes;

   /**
    * Set of pending locked nodes identifiers.
    */
   private final Set<String> pendingLocks;

   /**
    * Workspace lock manager
    */
   private final CacheableLockManager lockManager;

   /**
    * Constructor.
    * 
    * @param sessionID - session identifier
    * @param lockManager - workspace lock manager
    */
   public CacheableSessionLockManager(String sessionID, CacheableLockManager lockManager,
      SessionDataManager transientManager)
   {
      super(transientManager);
      this.sessionID = sessionID;
      this.tokens = new HashMap<String, String>();
      this.lockedNodes = new HashMap<String, LockData>();
      this.pendingLocks = new HashSet<String>();
      this.lockManager = lockManager;
   }

   /**
    * {@inheritDoc}
    */
   public Lock addLock(NodeImpl node, boolean isDeep, boolean isSessionScoped, long timeOut) throws LockException,
      RepositoryException
   {

      String lockToken = IdGenerator.generate();

      NodeData data = (NodeData)node.getData();

      LockData lData = lockManager.getExactNodeOrCloseParentLock(data);
      if (lData != null)
      {
         if (lData.getNodeIdentifier().equals(node.getIdentifier()))
         {
            throw new LockException("Node already locked: " + data.getQPath());
         }
         else if (lData.isDeep())
         {
            throw new LockException("Parent node has deep lock.");
         }
      }

      if (isDeep && lockManager.getClosedChild(data) != null)
      {
         throw new LockException("Some child node is locked.");
      }

      String lockTokenHash = lockManager.getLockTokenHash(lockToken);

      lData =
         new LockData(node.getIdentifier(), lockTokenHash, isDeep, isSessionScoped, node.getSession().getUserID(),
            timeOut > 0 ? timeOut : lockManager.getDefaultLockTimeOut());

      lockedNodes.put(node.getInternalIdentifier(), lData);
      pendingLocks.add(node.getInternalIdentifier());
      tokens.put(lockToken, lData.getTokenHash());

      LockImpl lock = new CacheLockImpl(node.getSession(), lData, this);

      return lock;
   }

   /**
    * {@inheritDoc}
    */
   public void addLockToken(String lt)
   {
      tokens.put(lt, lockManager.getLockTokenHash(lt));
   }

   /**
    * {@inheritDoc}
    */
   public LockImpl getLock(NodeImpl node) throws LockException, RepositoryException
   {
      LockData lData = lockManager.getExactNodeOrCloseParentLock((NodeData)node.getData());

      if (lData == null || (!node.getInternalIdentifier().equals(lData.getNodeIdentifier()) && !lData.isDeep()))
      {
         throw new LockException("Node not locked: " + node.getData().getQPath());
      }
      return new CacheLockImpl(node.getSession(), lData, this);
   }

   /**
    * {@inheritDoc}
    */
   public String[] getLockTokens()
   {
      String[] arr = new String[tokens.size()];
      tokens.keySet().toArray(arr);
      return arr;
   }

   /**
    * {@inheritDoc}
    */
   public boolean holdsLock(NodeData node) throws RepositoryException
   {
      return lockManager.lockExist(node.getIdentifier());//.getExactNodeLock(node) != null;
   }

   /**
    * {@inheritDoc}
    */
   protected boolean isLockedPersisted(NodeData node) throws LockException
   {
      LockData lData = null;
      try
      {
         lData = lockManager.getExactNodeOrCloseParentLock(node);
      }
      catch (RepositoryException e)
      {
         throw new LockException(e.getMessage(), e);
      }

      if (lData == null || (!node.getIdentifier().equals(lData.getNodeIdentifier()) && !lData.isDeep()))
      {
         return false;
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   protected boolean isPersistedLockHolder(NodeData node) throws RepositoryException
   {
      LockData lData = lockManager.getExactNodeOrCloseParentLock(node);
      return lData != null && isLockHolder(lData);
   }

   /**
    * {@inheritDoc}
    */
   public void onCloseSession(ExtendedSession session)
   {
      SessionImpl sessionImpl = (SessionImpl)session;

      int length = lockedNodes.size();
      if (length > 0)
      {
         String[] nodeIds = new String[length];
         lockedNodes.keySet().toArray(nodeIds);

         for (int i = 0; i < length; i++)
         {
            String nodeId = nodeIds[i];
            LockData lock = lockedNodes.remove(nodeId);

            if (lock.isSessionScoped() && !pendingLocks.contains(nodeId))
            {
               try
               {
                  NodeImpl node =
                     ((NodeImpl)sessionImpl.getTransientNodesManager()
                        .getItemByIdentifier(lock.getNodeIdentifier(), false));

                  if (node != null)
                  {
                     node.unlock();
                  }

               }
               catch (UnsupportedRepositoryOperationException e)
               {
                  LOG.error(e.getLocalizedMessage());
               }
               catch (LockException e)
               {
                  LOG.error(e.getLocalizedMessage());
               }
               catch (AccessDeniedException e)
               {
                  LOG.error(e.getLocalizedMessage());
               }
               catch (RepositoryException e)
               {
                  LOG.error(e.getLocalizedMessage());
               }
            }
         }
         lockedNodes.clear();
      }

      pendingLocks.clear();
      tokens.clear();

      lockManager.closeSessionLockManager(sessionID);
   }

   /**
    * {@inheritDoc}
    */
   public void removeLockToken(String lt)
   {
      tokens.remove(lt);
   }

   /**
    * Checks if session has token to this lock data or session is System.
    * 
    * @param lockData
    * @return
    */
   private boolean isLockHolder(LockData lockData)
   {
      return tokens.containsValue(lockData.getTokenHash());
   }

   /**
    * Is session contains pending lock for node by nodeId.
    * @param nodeId - node ID string
    * @return boolean
    */
   public boolean containsPendingLock(String nodeId)
   {
      return pendingLocks.contains(nodeId);
   }

   /**
    * Returns real token, if session has it.
    * 
    * @param tokenHash - token hash string
    * @return lock token string
    */
   protected String getLockToken(String tokenHash)
   {
      for (String token : tokens.keySet())
      {
         if (tokens.get(token).equals(tokenHash))
         {
            return token;
         }
      }
      return null;
   }

   /**
    * Return pending lock.
    * 
    * @param nodeId - ID of locked node
    * @return pending lock or null
    */
   public LockData getPendingLock(String nodeId)
   {
      if (pendingLocks.contains(nodeId))
      {
         return lockedNodes.get(nodeId);
      }
      else
      {
         return null;
      }
   }

   /**
    * Check is lock alive. That means lock must exist in LockManager storage (cache or map, etc). 
    * 
    * @param nodeIdentifier - locked node id
    * @return
    */
   protected boolean isLockLive(String nodeIdentifier) throws LockException
   {

      if (lockManager.isLockLive(nodeIdentifier))
      {
         return true;
      }
      else
      {
         return pendingLocks.contains(nodeIdentifier);
      }
   }

   public void notifyLockPersisted(String nodeIdentifier)
   {
      pendingLocks.remove(nodeIdentifier);
   }

   /**
    * Notify SessionLockManager that node is unlocked.
    * 
    * @param nodeIdentifier - unlocked node identifier
    */
   public void notifyLockRemoved(String nodeIdentifier)
   {
      lockedNodes.remove(nodeIdentifier);
   }

   /**
    * Refresh lockData. 
    * 
    * @param newLockData
    * @throws LockException
    */
   protected void refresh(LockData newLockData) throws LockException
   {
      lockManager.refreshLockData(newLockData);
   }

   /**
    * {@inheritDoc}
    */
   protected boolean checkPersistedLocks(NodeData node) throws LockException
   {
      LockData lData = null;
      try
      {
         lData = lockManager.getExactNodeOrCloseParentLock(node);
      }
      catch (RepositoryException e)
      {
         throw new LockException(e.getMessage(), e);
      }

      if (lData == null || (!node.getIdentifier().equals(lData.getNodeIdentifier()) && !lData.isDeep()))
      {
         return true;
      }

      // lock exist, so lets check is current session is LockHolder
      return isLockHolder(lData);
   }

}
