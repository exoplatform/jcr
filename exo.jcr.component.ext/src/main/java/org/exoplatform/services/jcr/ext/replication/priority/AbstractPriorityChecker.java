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
package org.exoplatform.services.jcr.ext.replication.priority;

import org.exoplatform.services.jcr.ext.replication.Packet;
import org.exoplatform.services.jcr.ext.replication.transport.AbstractPacket;
import org.exoplatform.services.jcr.ext.replication.transport.ChannelManager;
import org.exoplatform.services.jcr.ext.replication.transport.MemberAddress;
import org.exoplatform.services.jcr.ext.replication.transport.PacketListener;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: PriorityChecker.java 111 2008-11-11 11:11:11Z rainf0x $
 */

public abstract class AbstractPriorityChecker implements PacketListener
{

   /**
    * The definition max priority value.
    */
   public static final int MAX_PRIORITY = 100;

   /**
    * The wait timeout.
    */
   public static final int WAIT_TIMEOUT = 100;

   /**
    * The definition timeout for information.
    */
   private static final int INFORM_TIMOUT = 2000;

   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.AbstractPriorityChecker");

   /**
    * The ChannelManager will be transmitted the Packets.
    */
   protected final ChannelManager channelManager;

   /**
    * The own priority value.
    */
   protected final int ownPriority;

   /**
    * The own name in cluster.
    */
   protected final String ownName;

   /**
    * The list of names to other participants cluster.
    */
   protected final List<String> otherParticipants;

   /**
    * The HashMap of participants who are now online.
    */
   protected HashMap<String, Integer> currentParticipants;

   /**
    * The identification string.
    */
   protected String identifier;

   /**
    * AbstractPriorityChecker constructor.
    * 
    * @param channelManager
    *          the ChannelManager
    * @param ownPriority
    *          the own priority value
    * @param ownName
    *          the own name
    * @param otherParticipants
    *          the list of names to other participants.
    */
   public AbstractPriorityChecker(ChannelManager channelManager, int ownPriority, String ownName,
      List<String> otherParticipants)
   {

      this.ownPriority = ownPriority;
      this.ownName = ownName;
      this.otherParticipants = new ArrayList<String>(otherParticipants);

      this.channelManager = channelManager;
      this.channelManager.addPacketListener(this);

      currentParticipants = new HashMap<String, Integer>();
   }

   /**
    * {@inheritDoc}
    */
   public abstract void receive(AbstractPacket packet, MemberAddress sourceAddress);

   /**
    * informAll. If was changed members in cluster, then will be called this method.
    */
   public void informAll()
   {
      try
      {
         identifier = IdGenerator.generate();
         currentParticipants = new HashMap<String, Integer>();

         Packet pktInformer = new Packet(Packet.PacketType.GET_ALL_PRIORITY, ownName, (long)ownPriority, identifier);
         this.waitView();
         channelManager.sendPacket(pktInformer);

         try
         {
            if (log.isDebugEnabled())
               log.debug("<!-- isInterrupted == " + Thread.currentThread().isInterrupted());

            Thread.sleep(INFORM_TIMOUT);
         }
         catch (InterruptedException ie)
         {
            // ignored InterruptedException
            if (log.isDebugEnabled())
            {
               log.debug("InterruptedException");
               log.debug("--> isInterrupted == " + Thread.currentThread().isInterrupted());
            }

            Thread.sleep(INFORM_TIMOUT);
         }
      }
      catch (Exception e)
      {
         log.error("Can not informed the other participants", e);
      }
   }

   /**
    * printOnlineMembers. Write to console the current members.
    */
   protected void printOnlineMembers()
   {
      log.info(channelManager.getChannel().getClusterName() + " : " + identifier + " :");
      for (String memberName : currentParticipants.keySet())
         log.debug("    " + memberName + ":" + currentParticipants.get(memberName));
   }

   /**
    * isMaxPriority.
    * 
    * @return boolean if current time this is max priority then return 'true'
    */
   public abstract boolean isMaxPriority();

   /**
    * isMaxOnline.
    * 
    * @return boolean if max priority member is online then return 'true'
    */
   public boolean isMaxOnline()
   {

      if (ownPriority == MAX_PRIORITY)
         return true;

      for (String nodeName : currentParticipants.keySet())
         if (currentParticipants.get(nodeName).intValue() == MAX_PRIORITY)
            return true;

      return false;
   }

   /**
    * isAllOnline.
    * 
    * @return boolean if all member is online then return 'true'
    */
   public boolean isAllOnline()
   {
      return otherParticipants.size() == currentParticipants.size();
   }

   /**
    * hasDuplicatePriority.
    * 
    * @return boolean when duplicate the priority then return 'true'
    */
   public boolean hasDuplicatePriority()
   {
      List<Integer> other = new ArrayList<Integer>(currentParticipants.values());

      if (other.contains(ownPriority))
         return true;

      for (int i = 0; i < other.size(); i++)
      {
         int pri = other.get(i);
         List<Integer> oth = new ArrayList<Integer>(other);
         oth.remove(i);

         if (oth.contains(pri))
            return true;
      }

      return false;
   }

   /**
    * getOtherPriorities.
    * 
    * @return List
    *           the list of priorities of other participants.
    */
   public final List<Integer> getOtherPriorities()
   {
      return new ArrayList<Integer>(currentParticipants.values());
   }

   /**
    * waitView.
    * 
    * @throws InterruptedException
    *           Will be generated the InterruptedException
    */
   protected final void waitView() throws InterruptedException
   {
      while (channelManager.getChannel().getView() == null)
         Thread.sleep(WAIT_TIMEOUT);
   }
}
