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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.impl.proccess.WorkerThread;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: SessionRegistry.java 12049 2008-03-18 12:22:03Z gazarenkov $
 */
@Managed
@NameTemplate(@Property(key = "service", value = "SessionRegistry"))
public final class SessionRegistry implements Startable
{
   private final Map<String, SessionImpl> sessionsMap;

   // 1 min
   public final static int DEFAULT_CLEANER_TIMEOUT = 60 * 1000;

   protected static Log log = ExoLogger.getLogger("jcr.SessionRegistry");

   private SessionCleaner sessionCleaner;

   protected long timeOut;

   @Managed
   @ManagedDescription("How many sessions are currently active")
   public int getSize()
   {
      return sessionsMap.size();
   }

   @Managed
   @ManagedDescription("The session time out")
   public long getTimeOut()
   {
      return timeOut;
   }

   @Managed
   @ManagedDescription("Perform a cleanup of timed out sessions")
   public void runCleanup()
   {
      try
      {
         sessionCleaner.callPeriodically();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public SessionRegistry(RepositoryEntry entry)
   {
      sessionsMap = new ConcurrentHashMap<String, SessionImpl>();
      if (entry != null)
      {
         this.timeOut = entry.getSessionTimeOut() > 0 ? entry.getSessionTimeOut() : 0;
      }
   }

   public void registerSession(SessionImpl session)
   {
      sessionsMap.put(session.getId(), session);
   }

   public void unregisterSession(String sessionId)
   {
      sessionsMap.remove(sessionId);
   }

   public SessionImpl getSession(String sessionId)
   {
      return sessionId == null ? null : sessionsMap.get(sessionId);
   }

   public boolean isInUse(String workspaceName)
   {
      if (workspaceName == null)
      {
         if (log.isDebugEnabled())
            log.debug("Session in use " + sessionsMap.size());
         return sessionsMap.size() > 0;
      }
      for (SessionImpl session : sessionsMap.values())
      {
         if (session.getWorkspace().getName().equals(workspaceName))
         {
            if (log.isDebugEnabled())
               log.debug("Session for workspace " + workspaceName + " in use." + " Session id:" + session.getId()
                  + " user: " + session.getUserID());
            return true;
         }
      }
      return false;
   }

   public void start()
   {
      sessionsMap.clear();

      if (timeOut > 0)
         sessionCleaner = new SessionCleaner(DEFAULT_CLEANER_TIMEOUT, timeOut);
   }

   public void stop()
   {
      if (timeOut > 0 && sessionCleaner != null)
         sessionCleaner.halt();
      sessionsMap.clear();
   }

   /**
    * closeSessions will be closed the all sessions on specific workspace.
    *
    * @param workspaceName
    *          the workspace name.
    * @return int
    *           how many sessions was closed.
    */
   public int closeSessions(String workspaceName)
   {
      int closedSessions = 0;

      for (SessionImpl session : sessionsMap.values())
      {
         if (session.getWorkspace().getName().equals(workspaceName))
         {
            session.logout();
            closedSessions++;
         }
      }

      return closedSessions;
   }

   private class SessionCleaner extends WorkerThread
   {

      private final long sessionTimeOut;

      public SessionCleaner(long workTime, long sessionTimeOut)
      {
         super(workTime);
         this.sessionTimeOut = sessionTimeOut;
         setName("SessionCleaner " + getId());
         setPriority(Thread.MIN_PRIORITY);
         setDaemon(true);
         start();
         if (log.isDebugEnabled())
            log.debug("SessionCleaner instantiated name= " + getName() + " workTime= " + workTime + " sessionTimeOut="
               + sessionTimeOut);
      }

      @Override
      protected void callPeriodically() throws Exception
      {
         for (SessionImpl session : sessionsMap.values())
         {
            if (session.getLastAccessTime() + sessionTimeOut < System.currentTimeMillis())
            {
               session.logout();
            }
         }
      }
   }
}
