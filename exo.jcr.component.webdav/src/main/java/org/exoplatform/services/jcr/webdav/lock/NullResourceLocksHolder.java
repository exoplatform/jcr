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
package org.exoplatform.services.jcr.webdav.lock;

import org.exoplatform.services.jcr.util.IdGenerator;

import java.util.HashMap;
import java.util.List;

import javax.jcr.Session;
import javax.jcr.lock.LockException;

/**
 * Created by The eXo Platform SAS .<br>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class NullResourceLocksHolder
{

   /**
    * repo -> Map(/workspace/path/ -> token).
    */
   private final HashMap<String, String> nullResourceLocks;

   /**
    * Default constructor.
    */
   public NullResourceLocksHolder()
   {
      this.nullResourceLocks = new HashMap<String, String>();
   }

   /**
    * Locks the node.
    * 
    * @param session current session
    * @param path node path
    * @return thee lock token key
    * @throws LockException {@link LockException}
    */
   public String addLock(Session session, String path) throws LockException
   {

      String repoPath = session.getRepository().hashCode() + "/" + session.getWorkspace().getName() + "/" + path;

      if (!nullResourceLocks.containsKey(repoPath))
      {
         String newLockToken = IdGenerator.generate();
         session.addLockToken(newLockToken);
         nullResourceLocks.put(repoPath, newLockToken);
         return newLockToken;
      }

      // check if lock owned by this session
      String currentToken = nullResourceLocks.get(repoPath);
      for (String t : session.getLockTokens())
      {
         if (t.equals(currentToken))
            return t;
      }
      throw new LockException("Resource already locked " + repoPath);
   }

   /**
    * Removes lock from the node.
    * 
    * @param session current session
    * @param path nodepath
    */
   public void removeLock(Session session, String path)
   {
      String repoPath = session.getRepository().hashCode() + "/" + session.getWorkspace().getName() + "/" + path;
      String token = nullResourceLocks.get(repoPath);
      session.removeLockToken(token);
      nullResourceLocks.remove(repoPath);
   }

   /**
    * Checks if the node is locked.
    * 
    * @param session current session
    * @param path node path
    * @return true if the node is locked false if not
    */
   public boolean isLocked(Session session, String path)
   {
      String repoPath = session.getRepository().hashCode() + "/" + session.getWorkspace().getName() + "/" + path;

      if (nullResourceLocks.get(repoPath) != null)
      {
         return true;
      }
      return false;
   }

   /**
    * Checks if the node can be unlocked using current tokens.
    * 
    * @param session current session
    * @param path node path
    * @param tokens tokens
    * @throws LockException {@link LockException}
    */
   public void checkLock(Session session, String path, List<String> tokens) throws LockException
   {
      String repoPath = session.getRepository().hashCode() + "/" + session.getWorkspace().getName() + "/" + path;

      String currentToken = nullResourceLocks.get(repoPath);

      if (currentToken == null)
      {
         return;
      }

      if (tokens != null)
      {
         for (String token : tokens)
         {
            if (token.equals(currentToken))
            {
               return;
            }
         }
      }

      throw new LockException("Resource already locked " + repoPath);
   }

}
