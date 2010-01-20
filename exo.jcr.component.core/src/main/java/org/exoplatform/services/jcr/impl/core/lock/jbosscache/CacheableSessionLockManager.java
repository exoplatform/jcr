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
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.lock.LockImpl;
import org.exoplatform.services.jcr.impl.core.lock.SessionLockManager;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;
import java.util.Map;

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
 * @version $Id: SessionLockManager.java 111 2008-11-11 11:11:11Z serg $
 */
public class CacheableSessionLockManager implements SessionLockManager
{
   /**
    * Logger
    */
   private final Log log = ExoLogger.getLogger("jcr.lock.SessionLockManager");

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
    * Workspace lock manager
    */
   private final CacheableLockManager lockManager;

   /**
    * Constructor.
    * 
    * @param sessionID - session identifier
    * @param lockManager - workspace lock manager
    */
   public CacheableSessionLockManager(String sessionID, CacheableLockManager lockManager)
   {
      this.sessionID = sessionID;
      this.tokens = new HashMap<String, String>();
      this.lockedNodes = new HashMap<String, LockData>();
      this.lockManager = lockManager;
   }

   /**
    * {@inheritDoc}
    */
   public Lock addLock(NodeImpl node, boolean isDeep, boolean isSessionScoped, long timeOut) throws LockException,
      RepositoryException
   {

      String nodeIdentifier = node.getInternalIdentifier();
      LockData lData =
         lockManager.getLockData((NodeData)node.getData(), CacheableLockManager.SEARCH_EXECMATCH
            | CacheableLockManager.SEARCH_CLOSEDPARENT);
      if (lData != null)
      {
         if (lData.getNodeIdentifier().equals(node.getInternalIdentifier()))
         {
            throw new LockException("Node already locked: " + node.getData().getQPath());
         }
         else if (lData.isDeep())
         {
            throw new LockException("Parent node has deep lock.");
         }
      }

      if (isDeep && lockManager.getLockData((NodeData)node.getData(), CacheableLockManager.SEARCH_CLOSEDCHILD) != null)
      {
         throw new LockException("Some child node is locked.");
      }

      String lockToken = IdGenerator.generate();
      String lockTokenHash = lockManager.getHash(lockToken);

      lData =
         new LockData(nodeIdentifier, lockTokenHash, isDeep, isSessionScoped, node.getSession().getUserID(),
            timeOut > 0 ? timeOut : lockManager.getDefaultLockTimeOut());

      lockedNodes.put(node.getInternalIdentifier(), lData);
      lockManager.addPendingLock(nodeIdentifier, lData);
      tokens.put(lockToken, lockTokenHash);

      LockImpl lock = new CacheLockImpl(node.getSession(), lData, this);

      return lock;

   }

   /**
    * {@inheritDoc}
    */
   public void addLockToken(String lt)
   {
      tokens.put(lt, lockManager.getHash(lt));
   }

   /**
    * {@inheritDoc}
    */
   public LockImpl getLock(NodeImpl node) throws LockException, RepositoryException
   {
      LockData lData =
         lockManager.getLockData((NodeData)node.getData(), CacheableLockManager.SEARCH_EXECMATCH
            | CacheableLockManager.SEARCH_CLOSEDPARENT);

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
      return lockManager.getLockData(node, CacheableLockManager.SEARCH_EXECMATCH) != null;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLocked(NodeData node)
   {
      LockData lData =
         lockManager
            .getLockData(node, CacheableLockManager.SEARCH_EXECMATCH | CacheableLockManager.SEARCH_CLOSEDPARENT);

      if (lData == null || (!node.getIdentifier().equals(lData.getNodeIdentifier()) && !lData.isDeep()))
      {
         return false;
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLockHolder(NodeImpl node) throws RepositoryException
   {
      LockData lData =
         lockManager.getLockData((NodeData)node.getData(), CacheableLockManager.SEARCH_EXECMATCH
            | CacheableLockManager.SEARCH_CLOSEDPARENT);

      return lData != null && isLockHolder(lData);
   }

   /**
    * {@inheritDoc}
    */
   public void onCloseSession(ExtendedSession session)
   {
      SessionImpl sessionImpl = (SessionImpl)session;

      String[] nodeIds = new String[lockedNodes.size()];
      lockedNodes.keySet().toArray(nodeIds);

      for (String nodeId : nodeIds)
      {
         LockData lock = lockedNodes.remove(nodeId);

         if (lock.isSessionScoped())
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
               log.error(e.getLocalizedMessage());
            }
            catch (LockException e)
            {
               log.error(e.getLocalizedMessage());
            }
            catch (AccessDeniedException e)
            {
               log.error(e.getLocalizedMessage());
            }
            catch (RepositoryException e)
            {
               log.error(e.getLocalizedMessage());
            }
         }
         else
         {
            lockManager.removePendingLock(nodeId);

            lockedNodes.remove(nodeId);

            //remove token
            String hash = lock.getTokenHash();
            for (String token : tokens.keySet())
            {
               if (tokens.get(token).equals(hash))
               {
                  tokens.remove(token);
                  break;
               }
            }
         }
      }
      lockManager.closeSession(sessionID);
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
      return (SystemIdentity.SYSTEM.equals(sessionID) || tokens.containsValue(lockData.getTokenHash()));
   }

   /**
    * Returns real token, if session has it
    * 
    * @param lockData
    * @return
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
    * Check is lock alive. That means lock must exist in LockManager storage (cache or map, etc). 
    * 
    * @param nodeIdentifier - locked node id
    * @return
    */
   protected boolean isLockLive(String nodeIdentifier)
   {
      return lockManager.isLockLive(nodeIdentifier);
   }

   /**
    * Refresh lockData. 
    * 
    * @param newLockData
    * @throws LockException
    */
   protected void refresh(LockData newLockData) throws LockException
   {
      lockManager.refresh(newLockData);
   }

   /**
    * Notify SessionLockManager that node is unlocked.
    * 
    * @param nodeIdentifier - unlocked node identifier
    */
   protected void notifyLockRemoved(String nodeIdentifier)
   {
      lockedNodes.remove(nodeIdentifier);
   }

}
