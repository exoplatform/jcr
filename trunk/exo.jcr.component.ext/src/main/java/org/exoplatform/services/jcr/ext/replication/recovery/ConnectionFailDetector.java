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
package org.exoplatform.services.jcr.ext.replication.recovery;

import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.ext.replication.PriorityDucplicatedException;
import org.exoplatform.services.jcr.ext.replication.ReplicationService;
import org.exoplatform.services.jcr.ext.replication.priority.AbstractPriorityChecker;
import org.exoplatform.services.jcr.ext.replication.priority.DynamicPriorityChecker;
import org.exoplatform.services.jcr.ext.replication.priority.GenericPriorityChecker;
import org.exoplatform.services.jcr.ext.replication.priority.StaticPriorityChecker;
import org.exoplatform.services.jcr.ext.replication.transport.ChannelManager;
import org.exoplatform.services.jcr.ext.replication.transport.StateEvent;
import org.exoplatform.services.jcr.ext.replication.transport.StateListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ConectionFailDetector.java 111 2008-11-11 11:11:11Z rainf0x $
 */

public class ConnectionFailDetector implements StateListener
{
   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.ConnectionFailDetector");

   /**
    * Definition the VIEW_CHECK timeout.
    */
   private static final int VIEW_CHECK = 200;

   /**
    * The definition timeout for information.
    */
   private static final int INFORM_TIMOUT = 5000;

   /**
    * Definition the BEFORE_CHECK timeout.
    */
   private static final int BEFORE_CHECK = 10000;

   /**
    * Definition the BEFORE_INIT timeout.
    */
   private static final int BEFORE_INIT = 60000;

   /**
    * Definition the AFTER_INIT timeout.
    */
   private static final int AFTER_INIT = 60000;

   /**
    * The ChannelManager will be transmitted or receive the Packets.
    */
   private final ChannelManager channelManager;

   /**
    * The name of workspace.
    */
   private final String workspaceName;

   /**
    * The channel name.
    */
   private String channelName;

   /**
    * The ReconectTtread will be initialized reconnect to cluster.
    */
   private ReconectTtread reconectTtread;

   /**
    * Start value for lastViewSize.
    */
   private int lastViewSize = 2;

   /**
    * Start value for allInited.
    */
   private boolean allInited = false;

   /**
    * The PersistentDataManager will be used to workspace for set state 'read-only'.
    */
   private final PersistentDataManager dataManager;

   /**
    * The RecoveryManager will be initialized cluster node synchronization.
    */
   private final RecoveryManager recoveryManager;

   /**
    * The own priority value.
    */
   private final int ownPriority;

   /**
    * The own name in cluster.
    */
   private final String ownName;

   /**
    * The list of names to other participants in cluster.
    */
   private final List<String> otherPartisipants;

   /**
    * The priority checker (static or dynamic).
    */
   private final AbstractPriorityChecker priorityChecker;

   /**
    * The view checker.
    */
   private final ViewChecker viewChecker;

   /**
    * ConnectionFailDetector constructor.
    * 
    * @param channelManager
    *          the ChannelManager
    * @param dataManager
    *          the PersistentDataManager
    * @param recoveryManager
    *          the RecoveryManager
    * @param ownPriority
    *          the own priority
    * @param otherParticipants
    *          the list of names to other participants in cluster
    * @param ownName
    *          the own name in cluster
    * @param priprityType
    *          the priority type (dynamic or static)s
    * @param workspaceName
    *          String, the name of workspace         
    */
   public ConnectionFailDetector(ChannelManager channelManager, PersistentDataManager dataManager,
      RecoveryManager recoveryManager, int ownPriority, List<String> otherParticipants, String ownName,
      String priprityType, String workspaceName)
   {
      this.channelManager = channelManager;

      this.dataManager = dataManager;
      this.workspaceName = workspaceName;
      this.recoveryManager = recoveryManager;

      this.ownPriority = ownPriority;

      this.ownName = ownName;
      this.otherPartisipants = new ArrayList<String>(otherParticipants);

      if (priprityType.equals(ReplicationService.PRIORITY_STATIC_TYPE))
         priorityChecker = new StaticPriorityChecker(channelManager, ownPriority, ownName, otherParticipants);
      else if (priprityType.equals(ReplicationService.PRIORITY_DYNAMIC_TYPE))
         priorityChecker = new DynamicPriorityChecker(channelManager, ownPriority, ownName, otherParticipants);
      else
         priorityChecker = new GenericPriorityChecker(channelManager, ownPriority, ownName, otherParticipants);

      viewChecker = new ViewChecker();
      viewChecker.start();
   }

   /**
    * {@inheritDoc}
    */
   public void onStateChanged(StateEvent event)
   {
      viewChecker.putView(event);
   }

   /**
    * viewAccepted.
    *
    * @param viewSize
    *          int, the view size
    * @throws InterruptedException
    *           will be generated the exception InterruptedException  
    * @throws PriorityDucplicatedException
    *           will be generated the exception PriorityDucplicatedException 
    */
   private void viewAccepted(int viewSize) throws InterruptedException, PriorityDucplicatedException
   {
      priorityChecker.informAll();

      Thread.sleep(INFORM_TIMOUT);

      if (viewSize > 1)
         allInited = true;

      if (allInited == true)
         lastViewSize = viewSize;

      if (priorityChecker.hasDuplicatePriority())
      {
         log.info(workspaceName + " set read-only");
         dataManager.setReadOnly(true);

         throw new PriorityDucplicatedException("The priority was duplicated :  own priority = " + ownPriority
            + ", other priority = " + priorityChecker.getOtherPriorities());
      }

      if (priorityChecker.isAllOnline())
      {
         if (reconectTtread != null)
         {
            reconectTtread.setStop(false);
            reconectTtread = null;
         }

         memberRejoin();
         return;
      }

      if (priorityChecker instanceof GenericPriorityChecker)
      {
         if (lastViewSize == 1 && (reconectTtread == null || reconectTtread.isStoped() == true))
         {
            reconectTtread = new ReconectTtread(true);
            reconectTtread.start();
         }
      }
      else if (priorityChecker instanceof StaticPriorityChecker || otherPartisipants.size() == 1)
      {

         if (log.isDebugEnabled())
         {
            log.debug("lastViewSize == 1 && !priorityChecker.isMaxPriority() == "
               + (lastViewSize == 1 && !priorityChecker.isMaxPriority()));
            log.debug("lastViewSize > 1 && !priorityChecker.isMaxOnline() == "
               + (lastViewSize > 1 && !priorityChecker.isMaxOnline()));
         }

         if (lastViewSize == 1 && !priorityChecker.isMaxPriority())
         {
            if (reconectTtread == null || reconectTtread.isStoped() == true)
            {
               reconectTtread = new ReconectTtread(true);
               reconectTtread.start();
               memberSuspect();
            }
         }
         else if (reconectTtread != null && priorityChecker.isAllOnline())
         {
            reconectTtread.setStop(false);
            reconectTtread = null;
         }
         else if (lastViewSize > 1 && !priorityChecker.isMaxOnline())
         {
            if (reconectTtread == null || reconectTtread.isStoped() == true)
            {
               reconectTtread = new ReconectTtread(true);
               reconectTtread.start();
               memberSuspect();
            }
         }
      }
      else if (priorityChecker instanceof DynamicPriorityChecker && otherPartisipants.size() > 1)
      {

         if (lastViewSize == 1 && !priorityChecker.isMaxPriority())
         {
            if (reconectTtread == null || reconectTtread.isStoped() == true)
            {
               reconectTtread = new ReconectTtread(true);
               reconectTtread.start();
               memberSuspect();
            }
         }
         else if (reconectTtread != null && priorityChecker.isAllOnline())
         {
            reconectTtread.setStop(false);
            reconectTtread = null;
         }
      }
   }

   /**
    * The view checker. Will be check View.
    * 
    */
   private class ViewChecker extends Thread
   {
      /**
       * The view queue.
       */
      private final ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<Integer>();

      public ViewChecker()
      {
         super("ViewChecker");
      }
      
      /**
       * putView.
       *
       * @param event
       *          StateEvent, the event
       */
      public void putView(StateEvent event)
      {
         queue.offer(event.getMembers().size());
      }

      /**
       * {@inheritDoc}
       */
      public void run()
      {
         while (true)
         {
            try
            {
               Integer viewSize = queue.poll();

               if (viewSize != null)
                  viewAccepted(viewSize);

               sleep(VIEW_CHECK * 2);
            }
            catch (PriorityDucplicatedException e)
            {
               log.error("The wrong priority :", e);
            }
            catch (Throwable t)
            {
               log.error("View check error :", t);
            }
         }

      }
   }

   /**
    * The ReconectTtread will be initialized reconnect to cluster.
    */
   private class ReconectTtread extends Thread
   {
      /**
       * The 'isStop' is a flag to run() stop.
       */
      private boolean isStop;

      /**
       * ReconectTtread constructor.
       * 
       * @param isStop
       *          the 'isStop' value
       */
      public ReconectTtread(boolean isStop)
      {
         super("ReconectTtread");
         log.info("Thread '" + getName() + "' is init ...");
         this.isStop = isStop;
      }

      /**
       * {@inheritDoc}
       */
      public void run()
      {
         log.info("Thread '" + getName() + "' is run ...");
         while (isStop)
         {
            try
            {
               log.info("Connect to channel : " + channelName);
               Thread.sleep(BEFORE_CHECK);

               int curruntOnlin = 1;

               if (channelManager.getChannel() != null)
               {
                  while (channelManager.getChannel().getView() == null)
                     Thread.sleep(VIEW_CHECK);

                  curruntOnlin = channelManager.getChannel().getView().size();
               }

               if (isStop && (curruntOnlin <= 1 || ((curruntOnlin > 1) && !priorityChecker.isMaxOnline())))
               {
                  channelManager.disconnect();

                  Thread.sleep(BEFORE_INIT);

                  channelManager.connect();
               }
               else
               {
                  isStop = false;
               }
               Thread.sleep(AFTER_INIT);
            }
            catch (Exception e)
            {
               log.info(e, e);
            }
         }
      }

      /**
       * setStop.
       * 
       * @param isStop
       *          the 'isStop' value
       */
      public void setStop(boolean isStop)
      {
         this.isStop = isStop;
      }

      /**
       * isStoped.
       * 
       * @return boolean return the 'isStop' value
       */
      public boolean isStoped()
      {
         return !isStop;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void memberRejoin()
   {
      if (!(priorityChecker instanceof GenericPriorityChecker))
      {
         log.info(workspaceName + " set not read-only");
         dataManager.setReadOnly(false);
      }

      log.info(workspaceName + " recovery start ...");
      recoveryManager.startRecovery();
   }

   /**
    * Call this method if maxPriority member was suspected.
    * 
    */
   public void memberSuspect()
   {
      if (!(priorityChecker instanceof GenericPriorityChecker))
      {
         log.info(workspaceName + " set read-only");
         dataManager.setReadOnly(true);
      }
   }

}
